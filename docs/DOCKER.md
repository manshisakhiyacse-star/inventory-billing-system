# Docker Architecture & Containerization Guide

This document covers the container architecture, multi-stage build design, security controls, and orchestration workflow for the **Inventory & Billing Management System**.

---

## 1. Container Architecture Overview

The system utilizes two containerized services connected over an isolated Docker bridge network:

```
+---------------------------------------------------------------------------------+
|                         Host Machine (macOS / Linux / Windows)                  |
|                                                                                 |
|   Port 8080 (HTTP)                                   Port 3307 (MySQL Host)     |
+------|---------------------------------------------------------|----------------+
       |                                                         |
       v                                                         v
+---------------------------------------------------------------------------------+
|                       Docker Engine: app-network (Bridge)                       |
|                                                                                 |
|   +------------------------------------+   +--------------------------------+   |
|   |         inventory-app              |   |       inventory-mysql          |   |
|   |------------------------------------|   |--------------------------------|   |
|   | Image: inventory-billing-system-app|   | Image: mysql:8.0               |   |
|   | User: appuser (UID 1001)           |   | Internal Port: 3306            |   |
|   | JRE: Temurin 21 Jammy (Minimal)    |   | Health: mysqladmin ping        |   |
|   | Health: curl /actuator/health      |   | Volume: mysql_data             |   |
|   +------------------------------------+   +--------------------------------+   |
|                      |                                     ^                    |
|                      +--- jdbc:mysql://mysql:3306/db ------+                    |
+---------------------------------------------------------------------------------+
```

---

## 2. Multi-Stage Dockerfile Design

The [Dockerfile](file:///Users/manshipatel/Downloads/manshi%20folder/Project%20/inventory-billing-system/Dockerfile) implements an optimized two-stage build:

### Stage 1: Builder (`eclipse-temurin:21-jdk-jammy`)
- Installs the full JDK and Maven build tools.
- Copies dependency metadata (`pom.xml`, `.mvn`) and source code.
- Compiles source files and produces a production `.jar` (`inventory-billing-system-0.1.0-SNAPSHOT.jar`).
- Build tools, source code, and intermediate `.class` files are discarded after this stage.

### Stage 2: Runtime (`eclipse-temurin:21-jre-jammy`)
- Uses a minimal Java Runtime Environment (JRE) containing no compilers or build tools.
- Copies *only* the compiled JAR from Stage 1.
- Results in a tiny attack surface and an image size of only **~156 MB**.

---

## 3. Container Security Hardening

1. **Non-Root Execution**:
   - The container creates a dedicated system user:
     ```dockerfile
     RUN groupadd -g 1001 appgroup && \
         useradd -u 1001 -g appgroup -s /bin/false -m appuser
     USER appuser
     ```
   - If an application vulnerability is compromised, the attacker cannot gain root access on the host.

2. **Automated Health Checking**:
   - Both the database and application containers report health states to Docker Engine:
     ```dockerfile
     HEALTHCHECK --interval=20s --timeout=5s --start-period=30s --retries=3 \
         CMD curl -f http://localhost:8080/actuator/health || exit 1
     ```

3. **Dependency Ordering via Healthcheck**:
   - In `compose.yaml`, the application container waits until MySQL is fully initialized and passing healthchecks before booting:
     ```yaml
     depends_on:
       mysql:
         condition: service_healthy
     ```

---

## 4. Common Docker Commands

### Build and Start All Containers:
```bash
docker compose up --build -d
```

### Inspect Container Health:
```bash
docker compose ps
```

### View Live Application Logs:
```bash
docker compose logs -f app
```

### View Live MySQL Database Logs:
```bash
docker compose logs -f mysql
```

### Stop Containers (Preserving Database Volume):
```bash
docker compose down
```

### Stop Containers and Erase Database Volume (Clean Reset):
```bash
docker compose down -v
```

---

## 5. Troubleshooting Container Issues

### Issue 1: Port 8080 Already in Use
- **Symptom**: `Bind for 0.0.0.0:8080 failed: port is already allocated`
- **Fix**: Check what process is running on port 8080:
  ```bash
  lsof -i :8080
  kill -9 <PID>
  ```

### Issue 2: Port 3306 Conflict with Local MySQL
- **Symptom**: Conflict trying to bind host port 3306.
- **Fix**: Our `compose.yaml` maps the container to host port `3307:3306` by default, avoiding any collision with local MySQL. Inside the Docker network, the app always connects to `mysql:3306`.
