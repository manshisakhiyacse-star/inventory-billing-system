# Security Architecture & Hardening Guide

This document describes the security principles, cryptographic controls, authentication mechanisms, and OWASP Top 10 countermeasures implemented across the **Inventory & Billing Management System**.

---

## 1. Defense-in-Depth Security Model

Security is implemented across multiple layers rather than relying on a single perimeter:

```
[ Layer 1: Network & Infrastructure ] -> Private Subnets, Security Group Whitelists
[ Layer 2: Container Runtime ]        -> Non-Root User (UID 1001), Minimal JRE
[ Layer 3: Application Perimeter ]    -> CORS Whitelist, CSRF Protection for API
[ Layer 4: Identity & Authentication]-> Cryptographic JWT (HMAC-SHA256), BCrypt-12
[ Layer 5: Authorization (RBAC) ]     -> Granular Method-Level Security (@PreAuthorize)
[ Layer 6: Data & Transaction ]       -> Pessimistic Concurrency Locking, JPA Parameterization
[ Layer 7: Auditability ]             -> Centralized, Immutable Business Audit Log
```

---

## 2. Authentication & JWT Architecture

### Cryptographic Standard
- **Algorithm**: HMAC using SHA-256 (`HS256` / `HS512`).
- **Key Length**: Strictly enforced to be at least **256 bits (32 bytes)**. `JwtTokenProvider` throws an exception during startup if a weak secret is configured.
- **Payload Claims**:
  - `sub`: Username.
  - `roles`: Assigned system authorities (`ROLE_ADMIN`, `ROLE_MANAGER`, etc.).
  - `iss`: Issuer identifier (`inventory-billing-system`).
  - `iat` / `exp`: Issued-at and expiration timestamps (default 1 hour).

### Token Validation Checks:
Every request intercepted by `JwtAuthenticationFilter` validates:
1. Valid cryptographic signature against the secret key.
2. Token expiration time.
3. Malformed header or payload structure.
4. User active status in the database.

---

## 3. Password Security with BCrypt

- **Algorithm**: BCrypt (`$2a$`).
- **Work Factor / Strength**: `12` (equivalent to $2^{12} = 4,096$ hashing iterations).
- **Adaptive Hashing**: Protects against brute-force attacks and rainbow table lookups by incorporating a cryptographically random 128-bit salt into every hash.
- **No Plaintext Passwords**: Password fields in DTOs are mapped directly to hashes and are never stored or returned in responses (`UserResponse` explicitly omits password hashes).

---

## 4. Role-Based Access Control (RBAC) Matrix

| Endpoint Pattern | Method | ADMIN | MANAGER | BILLING_CLERK | INVENTORY_CLERK | Public |
|---|---|:---:|:---:|:---:|:---:|:---:|
| `/api/v1/auth/login` | POST | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/actuator/health` | GET | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/api/v1/users/**` | ALL | ✅ | ❌ | ❌ | ❌ | ❌ |
| `/api/v1/products` | POST/PUT | ✅ | ✅ | ❌ | ❌ | ❌ |
| `/api/v1/products/{id}` | DELETE | ✅ | ❌ | ❌ | ❌ | ❌ |
| `/api/v1/products/low-stock`| GET | ✅ | ✅ | ❌ | ✅ | ❌ |
| `/api/v1/inventory/adjust` | POST | ✅ | ✅ | ❌ | ✅ | ❌ |
| `/api/v1/invoices` | POST | ✅ | ✅ | ✅ | ❌ | ❌ |
| `/api/v1/invoices/{id}/cancel` | PATCH | ✅ | ✅ | ✅ | ❌ | ❌ |
| `/api/v1/reports/**` | GET | ✅ | ✅ | ❌ | ❌ | ❌ |

---

## 5. OWASP Top 10 Mitigation

### 1. Injection (SQL Injection)
- **Mitigation**: All database queries are executed through Spring Data JPA and Hibernate using parameterized prepared statements. No concatenated dynamic SQL strings are used anywhere in the codebase.

### 2. Broken Authentication
- **Mitigation**: Stateless Bearer tokens with strict expiration. Passwords hashed with BCrypt (strength 12). Account activation toggles (`active: false`) instantly revoke login access.

### 3. Sensitive Data Exposure
- **Mitigation**:
  - `server.error.include-stacktrace: never` prevents stack trace leakage in production.
  - Generic 500 error messages return sanitized problem details.
  - Passwords and secrets are injected via environment variables, never hardcoded.

### 4. Broken Access Control
- **Mitigation**: URL authorization rules in `SecurityConfig.java` combined with `@EnableMethodSecurity` and `@PreAuthorize` annotations on controller endpoints.

### 5. Security Misconfiguration
- **Mitigation**: Distinct configuration profiles (`application-local.yml` vs `application-prod.yml`). In production:
  - Swagger UI is disabled by default.
  - SQL statement logging is suppressed.
  - JSON structured logging is used for SIEM integration.
