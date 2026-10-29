# REST API Reference Documentation

All requests and responses use `application/json` format. Protected endpoints require the HTTP header:
```http
Authorization: Bearer <your_jwt_token>
```

---

## 1. Authentication

### `POST /api/v1/auth/login`
Authenticates user credentials and returns a signed JWT bearer token.
- **Access**: Public
- **Request Body**:
  ```json
  {
    "username": "admin",
    "password": "admin123"
  }
  ```
- **Response** (`200 OK`):
  ```json
  {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600000,
    "username": "admin",
    "role": "ADMIN"
  }
  ```

---

## 2. Product Management

### `POST /api/v1/products`
Creates a new catalog product.
- **Access**: `ADMIN`, `MANAGER`
- **Request Body**:
  ```json
  {
    "sku": "SKU-MONITOR-4K",
    "name": "Dell UltraSharp 27 4K",
    "description": "27-inch 4K IPS Monitor",
    "category": "Displays",
    "unitPrice": 499.99,
    "costPrice": 350.00,
    "quantityInStock": 30,
    "reorderThreshold": 5
  }
  ```
- **Response** (`201 Created`): Returns created `ProductResponse`.

### `GET /api/v1/products`
Retrieves paginated active products with optional search query.
- **Access**: Authenticated
- **Query Parameters**:
  - `search` (optional): Case-insensitive keyword matching `name` or `sku`.
  - `page` (default: 0)
  - `size` (default: 20)
  - `sort` (default: `name,asc`)

### `GET /api/v1/products/{id}`
Returns details for a single product by ID.
- **Access**: Authenticated

### `GET /api/v1/products/low-stock`
Returns products where `quantityInStock <= reorderThreshold`.
- **Access**: `ADMIN`, `MANAGER`, `INVENTORY_CLERK`

### `PUT /api/v1/products/{id}`
Updates details of an existing product.
- **Access**: `ADMIN`, `MANAGER`

### `DELETE /api/v1/products/{id}`
Soft deletes / deactivates a product.
- **Access**: `ADMIN`

---

## 3. Inventory Adjustments & History

### `POST /api/v1/inventory/adjust`
Applies an atomic positive or negative stock adjustment with pessimistic write locking.
- **Access**: `ADMIN`, `MANAGER`, `INVENTORY_CLERK`
- **Request Body**:
  ```json
  {
    "productId": 1,
    "quantityChange": 10,
    "reason": "Shipment received from manufacturer"
  }
  ```
- **Response** (`200 OK`):
  ```json
  {
    "id": 15,
    "productId": 1,
    "productSku": "SKU-MONITOR-4K",
    "productName": "Dell UltraSharp 27 4K",
    "movementType": "PURCHASE",
    "quantityChange": 10,
    "quantityAfter": 40,
    "reason": "Shipment received from manufacturer",
    "performedBy": "admin",
    "createdAt": "2026-10-29T12:00:00"
  }
  ```

### `GET /api/v1/inventory/history/{productId}`
Returns chronological stock movement history for a specific product.
- **Access**: Authenticated

### `GET /api/v1/inventory/movements`
Returns chronological stock movements across all catalog items.
- **Access**: `ADMIN`, `MANAGER`, `INVENTORY_CLERK`

---

## 4. Customer Management

### `POST /api/v1/customers`
Registers a new customer for billing.
- **Access**: `ADMIN`, `MANAGER`, `BILLING_CLERK`
- **Request Body**:
  ```json
  {
    "name": "Wayne Enterprises",
    "phone": "+1-555-0199",
    "email": "purchasing@wayne.com",
    "billingAddress": "1007 Mountain Drive, Gotham"
  }
  ```

### `GET /api/v1/customers`
Returns paginated active customers with optional search across name, email, or phone.
- **Access**: Authenticated

---

## 5. Billing & Invoicing

### `POST /api/v1/invoices`
Generates an invoice, decrements inventory atomically, and creates immutable snapshots.
- **Access**: `ADMIN`, `MANAGER`, `BILLING_CLERK`
- **Request Body**:
  ```json
  {
    "customerId": 1,
    "lineItems": [
      {
        "productId": 1,
        "quantity": 2
      }
    ],
    "discountAmount": 20.00,
    "taxRate": 0.0825,
    "paymentMethod": "CARD",
    "notes": "Corporate purchase"
  }
  ```
- **Response** (`201 Created`):
  ```json
  {
    "id": 101,
    "invoiceNumber": "INV-20261029-0001",
    "customerId": 1,
    "customerName": "Wayne Enterprises",
    "status": "CREATED",
    "subtotal": 999.98,
    "discountAmount": 20.00,
    "taxRate": 0.0825,
    "taxAmount": 80.85,
    "grandTotal": 1060.83,
    "paymentMethod": "CARD",
    "paymentStatus": "COMPLETED",
    "notes": "Corporate purchase",
    "createdBy": "admin",
    "stockRestored": false,
    "lineItems": [
      {
        "id": 1,
        "productId": 1,
        "productSku": "SKU-MONITOR-4K",
        "productName": "Dell UltraSharp 27 4K",
        "unitPrice": 499.99,
        "quantity": 2,
        "lineTotal": 999.98
      }
    ],
    "createdAt": "2026-10-29T12:00:00"
  }
  ```

### `PATCH /api/v1/invoices/{id}/cancel`
Cancels an invoice and restores inventory idempotently.
- **Access**: `ADMIN`, `MANAGER`, `BILLING_CLERK`
- **Query Parameter**: `reason` (optional, default: "Customer requested cancellation")

### `PATCH /api/v1/invoices/{id}/pay`
Marks invoice status as `PAID` and payment status as `COMPLETED`.
- **Access**: `ADMIN`, `MANAGER`, `BILLING_CLERK`

### `GET /api/v1/invoices/{id}`
Retrieves invoice details by primary key ID.

### `GET /api/v1/invoices/number/{invoiceNumber}`
Retrieves invoice details by human-readable invoice number (e.g. `INV-20261029-0001`).

---

## 6. Analytics & Reports

### `GET /api/v1/reports/sales`
Aggregates sales performance within an optional date range.
- **Access**: `ADMIN`, `MANAGER`
- **Query Parameters**:
  - `startDate` (optional, ISO 8601: `2026-10-01T00:00:00`)
  - `endDate` (optional, ISO 8601: `2026-10-29T23:59:59`)
- **Response** (`200 OK`):
  ```json
  {
    "startDate": "2026-10-01T00:00:00",
    "endDate": "2026-10-29T23:59:59",
    "totalInvoices": 150,
    "paidInvoices": 142,
    "cancelledInvoices": 8,
    "totalSales": 125400.50,
    "totalDiscount": 2350.00,
    "totalTax": 10240.00,
    "netRevenue": 115160.50
  }
  ```

### `GET /api/v1/reports/inventory`
Provides overall catalog valuation and summary of low-stock items.
- **Access**: `ADMIN`, `MANAGER`
- **Response** (`200 OK`):
  ```json
  {
    "totalProducts": 45,
    "activeProducts": 42,
    "lowStockProductsCount": 3,
    "totalInventoryValue": 84950.00,
    "lowStockProducts": [...]
  }
  ```

---

## 7. Monitoring & Health

### `GET /actuator/health`
Returns system health status (`{"status":"UP"}`).
- **Access**: Public

### `GET /actuator/info`
Returns application build metadata.
- **Access**: Public
