# Inventory & Billing Management System

[![CI Pipeline](https://github.com/example/inventory-billing-system/actions/workflows/ci.yml/badge.svg)](https://github.com/example/inventory-billing-system/actions)
[![Java 21](https://img.shields.io/badge/Java-21%20LTS-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-orange.svg)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Multi--stage-2496ED.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)]()

A production-grade, secure, multi-tier **Inventory & Billing Management REST API** built with **Java 21 LTS**, **Spring Boot 3.4.5**, and **MySQL 8.0**. Designed for distributors, retailers, and small-to-medium businesses to manage products, customers, stock movements, invoices, and financial reports with enterprise-grade concurrency control and auditability.

---

## Key Highlights

- **Stateless Security**: Cryptographic JWT Bearer token authentication (HMAC-SHA256) with Role-Based Access Control (`ADMIN`, `MANAGER`, `BILLING_CLERK`, `INVENTORY_CLERK`).
- **Race Condition Prevention**: Pessimistic write locking (`SELECT ... FOR UPDATE`) guarantees inventory consistency during simultaneous orders.
- **Auditable & Immutable Billing**: Invoices snapshot exact product SKUs, names, and unit prices at the time of sale. Historical bills remain accurate even if catalog items change later.
- **Idempotent Cancellation**: Cancelling an invoice restores inventory safely without double-restoration risks.
- **Precise Monetary Arithmetic**: Exact `BigDecimal` calculations with `HALF_UP` rounding ensure zero fractional-cent drift.
- **Database Evolution**: 8 Flyway database migrations version-control all tables, indices, and seed accounts.
- **Containerized & Cloud-Ready**: Multi-stage Docker build running as non-root user (`appuser:1001`) with automated health checks and Terraform templates for AWS.

---

## Architecture Overview

```
+-----------------------------------------------------------------------------------+
|                                 Client Requests                                    |
|          (Swagger UI / Postman / Frontend Web Apps / Mobile Clients)              |
+-----------------------------------------------------------------------------------+
                                          |
                                          | (HTTPS / Port 8080)
                                          v
+-----------------------------------------------------------------------------------+
|                            Spring Boot 3 Backend                                  |
|                                                                                   |
|  [ Security Filter Chain ]  -->  JwtAuthenticationFilter  -->  Role Authorization |
|                                                                                   |
|  [ REST Controllers ]       -->  Auth, Users, Products, Inventory, Invoices,       |
|                                  Customers, Reports, Actuator Health             |
|                                                                                   |
|  [ Service Layer ]          -->  Pessimistic Locking & Transaction Management     |
|                                  (@Transactional & SELECT ... FOR UPDATE)         |
|                                                                                   |
|  [ JPA & MapStruct ]        -->  Optimistic Locking (@Version) & DTO Mappers      |
+-----------------------------------------------------------------------------------+
                                          |
                                          | (Port 3306)
                                          v
+-----------------------------------------------------------------------------------+
|                              MySQL 8.0 Database                                   |
|                                                                                   |
|  [ Tables ]   users, products, customers, invoices, invoice_line_items,           |
|               stock_movements, audit_log, flyway_schema_history                   |
+-----------------------------------------------------------------------------------+
```

---

## Quick Start (Run Locally in 60 Seconds)

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.
- (Optional) [Java 21](https://adoptium.net/) & Maven if running without Docker.

### 1. Launch with Docker Compose
Clone the repository and spin up the backend and MySQL database:
```bash
cd inventory-billing-system
docker compose up -d
```

### 2. Verify System Health
Check that both containers are running and healthy:
```bash
docker compose ps
curl http://localhost:8080/actuator/health
```
*Expected response:* `{"status":"UP"}`

### 3. Open Interactive Swagger UI
Visit the interactive OpenAPI documentation in your browser:
```
http://localhost:8080/swagger-ui/index.html
```

### 4. Authenticate & Explore
Login with the default seed administrator credentials:
- **Username**: `admin`
- **Password**: `admin123`

---

## Project Structure

```text
inventory-billing-system/
├── .github/
│   └── workflows/
│       ├── ci.yml                 # Automated build, test, and container check
│       └── cd.yml                 # Automated AWS ECR & EC2 deployment (OIDC)
├── docs/                          # Comprehensive technical documentation
│   ├── ARCHITECTURE.md            # Deep-dive system architecture & data model
│   ├── API.md                     # Full REST endpoint reference
│   ├── LOCAL_SETUP.md             # Developer environment setup guide
│   ├── DOCKER.md                  # Container architecture & troubleshooting
│   ├── AWS_DEPLOYMENT.md          # AWS deployment guide (Terraform & Console)
│   ├── CI_CD.md                   # Pipeline guide & GitHub secret management
│   ├── SECURITY.md                # Threat model, RBAC, and defense-in-depth
│   ├── TROUBLESHOOTING.md         # Common error codes and solutions
│   └── COST_AND_CLEANUP.md        # AWS cost estimate & resource teardown
├── terraform/                     # Complete AWS Infrastructure-as-Code
│   ├── main.tf                    # VPC, Subnets, ECR, RDS MySQL, EC2, IAM
│   ├── variables.tf               # Configurable region, sizing, CIDR parameters
│   ├── outputs.tf                 # ECR URL, RDS host, EC2 public IP
│   └── terraform.tfvars.example   # Example variables file
├── src/
│   ├── main/
│   │   ├── java/com/inventory/system/
│   │   │   ├── config/            # Security, CORS, OpenAPI, JPA Audit
│   │   │   ├── controller/        # REST Controllers (7 controllers)
│   │   │   ├── dto/               # Request & Response DTOs with Bean Validation
│   │   │   ├── entity/            # JPA Entities & Enums (7 entities)
│   │   │   ├── exception/         # RFC 7807 Global Exception Handling
│   │   │   ├── mapper/            # MapStruct compile-time mappers
│   │   │   ├── repository/        # Spring Data JPA Repositories (Locking)
│   │   │   ├── security/          # JWT Token Provider & Filter Chain
│   │   │   └── service/           # Business logic & transaction boundaries
│   │   └── resources/
│   │       ├── application.yml    # Base configuration
│   │       ├── application-local.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/      # Flyway SQL migrations (V1 to V8)
│   └── test/                      # Comprehensive unit and MockMvc test suite
├── Dockerfile                     # Secure multi-stage build running as non-root
├── compose.yaml                   # Local orchestration for app + database
├── inventory-billing-api.postman_collection.json # Ready-to-import Postman collection
└── pom.xml                        # Maven dependencies & build plugins
```

---

## Documentation Links

For in-depth guides, see the `docs/` folder:
- [System Architecture & Concurrency Model](docs/ARCHITECTURE.md)
- [REST API Specification](docs/API.md)
- [Local Setup Guide](docs/LOCAL_SETUP.md)
- [Docker & Container Guide](docs/DOCKER.md)
- [AWS Cloud Deployment Guide](docs/AWS_DEPLOYMENT.md)
- [CI/CD Automation](docs/CI_CD.md)
- [Security & RBAC Guide](docs/SECURITY.md)
- [Troubleshooting & FAQs](docs/TROUBLESHOOTING.md)
- [AWS Cost & Teardown Guide](docs/COST_AND_CLEANUP.md)

---

## License

Proprietary & Confidential. All rights reserved. with ♥ by Manshi Patel
