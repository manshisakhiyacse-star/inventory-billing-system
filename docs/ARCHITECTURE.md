# Architecture & System Design Documentation

This document describes the technical architecture, data models, concurrency control mechanisms, and design decisions powering the **Inventory & Billing Management System**.

---

## 1. High-Level System Architecture

The application implements a decoupled, layered micro-architecture built on **Spring Boot 3** and **MySQL 8.0**:

```
+---------------------------------------------------------------------------------+
|                             API Gateway / Client Tier                           |
|       (Web Browsers, Mobile Apps, Postman, Third-Party Billing Webhooks)        |
+---------------------------------------------------------------------------------+
                                       |
                                       | HTTPS (Port 443 / 8080)
                                       v
+---------------------------------------------------------------------------------+
|                         Spring Security Filter Pipeline                         |
|                                                                                 |
|   1. CorsFilter               -> Validates allowed origins & pre-flight OPTIONS |
|   2. JwtAuthenticationFilter  -> Validates HMAC-SHA256 signature & expiration  |
|   3. AuthorizationFilter      -> Enforces Method/URL Role-Based Access (RBAC)  |
|   4. ExceptionHandling        -> Maps 401/403 to RFC 7807 Problem Details JSON  |
+---------------------------------------------------------------------------------+
                                       |
                                       v
+---------------------------------------------------------------------------------+
|                           Presentation / Controller Tier                        |
|                                                                                 |
|   - AuthController        - ProductController       - CustomerController        |
|   - InvoiceController     - InventoryController     - UserController            |
|   - ReportController      - ActuatorHealthController                            |
+---------------------------------------------------------------------------------+
                                       |
                                       v
+---------------------------------------------------------------------------------+
|                               Business Service Tier                             |
|                                                                                 |
|   - Transaction Boundaries (@Transactional)                                     |
|   - Concurrency Control (Pessimistic Write Locking: SELECT ... FOR UPDATE)      |
|   - Financial Calculations (BigDecimal with HALF_UP Rounding)                    |
|   - Audit Logging & Idempotent Stock Reversal                                   |
+---------------------------------------------------------------------------------+
                                       |
                                       v
+---------------------------------------------------------------------------------+
|                               Data Access Layer (JPA)                           |
|                                                                                 |
|   - Spring Data JPA Repositories                                                |
|   - MapStruct Compile-Time Mappers (Zero Reflection Overhead)                   |
|   - Hibernate Second-Level Cache & Statement Optimization                       |
+---------------------------------------------------------------------------------+
                                       |
                                       v
+---------------------------------------------------------------------------------+
|                            Database Tier (MySQL 8.0)                            |
|                                                                                 |
|   - InnoDB Storage Engine (Row-Level Locking, ACID Compliance)                  |
|   - Flyway Schema Migrations (V1 through V8)                                    |
|   - B-Tree Indexes on SKU, Invoice Number, Email, and Foreign Keys              |
+---------------------------------------------------------------------------------+
```

---

## 2. Entity Relationship Model (ERD)

```
       +------------------+                    +---------------------+
       |      users       |                    |     audit_log       |
       +------------------+                    +---------------------+
       | id (PK)          |                    | id (PK)             |
       | username (UQ)    |                    | action              |
       | email (UQ)       |                    | entity_type         |
       | password_hash    |                    | entity_id           |
       | role             |                    | details             |
       | active           |                    | performed_by        |
       | created_at       |                    | created_at          |
       +------------------+                    +---------------------+
                 ^
                 |
       +------------------+                    +---------------------+
       |    customers     |                    |      products       |
       +------------------+                    +---------------------+
       | id (PK)          |                    | id (PK)             |
       | name             |                    | sku (UQ)            |
       | phone            |                    | name                |
       | email            |                    | description         |
       | billing_address  |                    | category            |
       | active           |                    | unit_price          |
       | created_at       |                    | cost_price          |
       +------------------+                    | quantity_in_stock   |
                 |                             | reorder_threshold   |
                 | 1                           | version (Lock)      |
                 |                             | active              |
                 v N                           +---------------------+
       +------------------+                               |
       |     invoices     |                               | 1
       +------------------+                               |
       | id (PK)          |                               |
       | invoice_number(UQ)                               v N
       | customer_id (FK) |                    +---------------------+
       | status           |                    |   stock_movements   |
       | subtotal         |                    +---------------------+
       | discount_amount  |                    | id (PK)             |
       | tax_rate         |                    | product_id (FK)     |
       | tax_amount       |                    | movement_type       |
       | grand_total      |                    | quantity_change     |
       | payment_method   |                    | quantity_after      |
       | payment_status   |                    | reference_id        |
       | notes            |                    | reason              |
       | created_by       |                    | performed_by        |
       | stock_restored   |                    | created_at          |
       | created_at       |                    +---------------------+
       +------------------+                               ^
                 | 1                                      |
                 |                                        |
                 v N                                      |
       +------------------------------------+             |
       |        invoice_line_items          |             |
       +------------------------------------+             |
       | id (PK)                            |             |
       | invoice_id (FK)                    |             |
       | product_id (FK) -------------------+-------------+
       | product_sku (Snapshot)             |
       | product_name (Snapshot)            |
       | unit_price (Snapshot)              |
       | quantity                           |
       | line_total                         |
       +------------------------------------+
```

---

## 3. Concurrency & Inventory Consistency

When multiple clerks issue invoices simultaneously for high-demand products, typical web applications suffer from **lost updates** and **negative inventory drift**.

```
THREAD A: Read Product (Stock=1) --------------------------> Decrement (Stock=0) -> Save (Stock=0)
THREAD B: ----------> Read Product (Stock=1) [STALE] ------> Decrement (Stock=0) -> Save (Stock=0)
                      RESULT: 2 items sold, but stock only decremented by 1!
```

### Our Defense-in-Depth Solution:

#### 1. Pessimistic Write Locking (`PESSIMISTIC_WRITE`)
In `ProductRepository.java`:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Optional<Product> findByIdWithLock(@Param("id") Long id);
```
- When `InvoiceService` or `InventoryService` acquires a product, Hibernate generates:
  ```sql
  SELECT ... FROM products WHERE id = ? FOR UPDATE;
  ```
- Any concurrent transaction attempting to read or modify the same product row is held at the database lock manager until the first transaction commits or rolls back.

#### 2. Optimistic Locking Fallback (`@Version`)
In `Product.java`:
```java
@Version
private Long version;
```
- If any concurrent update bypasses the pessimistic lock, Hibernate verifies that `version` matches before issuing `UPDATE ... WHERE id = ? AND version = ?`. If stale, an `OptimisticLockException` is raised automatically.

---

## 4. Invoicing Design Decisions

### 1. Snapshotting vs. Referencing
In standard schemas, line items only store `product_id`. If the product’s name or price changes in the future, all past historical invoices recalculate incorrectly!
- **Our Decision**: `InvoiceLineItem` explicitly stores `product_sku`, `product_name`, and `unit_price` as immutable snapshots at the instant the invoice is generated.

### 2. Idempotent Invoice Cancellation
When an invoice is cancelled:
1. `Invoice.status` changes to `CANCELLED`.
2. Line items are iterated, and each item's quantity is returned to the respective product's `quantity_in_stock`.
3. A `StockMovement` of type `CANCELLATION_REVERSAL` is recorded.
4. **`Invoice.stock_restored`** is toggled to `TRUE`.
5. If a client retries or calls `/cancel` again, the idempotency check detects `stock_restored == true` and refuses to duplicate inventory replenishment.

### 3. Financial Precision with `BigDecimal`
- All money fields (`unit_price`, `subtotal`, `discount_amount`, `tax_amount`, `grand_total`) use Java `BigDecimal` and MySQL `DECIMAL(12,2)`.
- All rounding uses `RoundingMode.HALF_UP`.
- Floating point primitives (`float`, `double`) are strictly prohibited in the codebase.

---

## 5. Security & Role-Based Access Control (RBAC)

The system enforces 4 distinct roles:

| Role | Description | Permissions |
|---|---|---|
| `ADMIN` | Superuser / Owner | Full system access: User management, Catalog, Invoicing, Stock, Reports, Deactivations |
| `MANAGER` | Store / Operations Manager | Catalog updates, Stock adjustments, Invoicing, Customer management, Sales & Inventory Reports |
| `BILLING_CLERK` | Cashier / Billing Staff | Customer management, Invoice generation, Invoice cancellation, Invoice payment |
| `INVENTORY_CLERK` | Warehouse / Stock Keeper | Catalog viewing, Stock adjustments, Stock history queries |

---

## 6. RFC 7807 Error Response Standard

All API errors return standardized JSON compliant with **RFC 7807 Problem Details**:
```json
{
  "type": "https://api.inventory-system.com/errors/insufficient-stock",
  "title": "Insufficient Stock",
  "status": 400,
  "detail": "Insufficient stock for product 'SKU-PROD-2026' (ID: 1). Requested: 50, Available: 13",
  "instance": "/api/v1/invoices",
  "timestamp": "2026-10-29T12:40:28.927"
}
```
Validation failures append a `validationErrors` map linking specific invalid request fields to user-friendly error messages.
