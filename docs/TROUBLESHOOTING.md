# Troubleshooting & Diagnostic Guide

This guide covers common issues, error codes, and step-by-step solutions for developers and operators working with the **Inventory & Billing Management System**.

---

## 1. Environment & Build Issues

### Issue 1: `class file has wrong version 65.0, should be 61.0`
- **Root Cause**: You are attempting to compile or run the application with Java 17 or older. Java 21 LTS produces class file version 65.0.
- **Solution**: Set your `JAVA_HOME` to Java 21:
  ```bash
  # macOS
  export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
  java -version
  ```

### Issue 2: Maven Wrapper Permission Denied (`./mvnw: Permission denied`)
- **Root Cause**: The executable bit on the wrapper script was not set.
- **Solution**:
  ```bash
  chmod +x mvnw
  ```

---

## 2. Port & Network Conflicts

### Issue 3: Port 8080 is Already in Use
- **Error Message**: `Web server failed to start. Port 8080 was already in use.`
- **Diagnosis**: Another process (or a previous instance of the app) is running on port 8080.
- **Solution**:
  ```bash
  # Identify process ID
  lsof -i :8080

  # Terminate the lingering process
  kill -9 <PID>
  ```

### Issue 4: Port 3306 Conflict During `docker compose up`
- **Error Message**: `driver failed programming external connectivity on endpoint inventory-mysql: Bind for 0.0.0.0:3306 failed: port is already allocated`
- **Root Cause**: You have a local MySQL server already running on your host machine on port 3306.
- **Solution**: Our `compose.yaml` is pre-configured to bind the container's MySQL port to host port **`3307`** (`3307:3306`). If you customized `compose.yaml`, ensure you map to `3307:3306`. Inside the Docker network, the app always connects to `mysql:3306`.

---

## 3. Database & Flyway Migration Issues

### Issue 5: `Access denied for user 'inv_app'@'localhost'`
- **Diagnosis**: The MySQL user `inv_app` does not exist or has incorrect privileges.
- **Solution**: Connect to MySQL as `root` and re-grant permissions:
  ```sql
  CREATE USER IF NOT EXISTS 'inv_app'@'localhost' IDENTIFIED BY 'localdevpassword';
  GRANT ALL PRIVILEGES ON inventory_billing.* TO 'inv_app'@'localhost';
  FLUSH PRIVILEGES;
  ```

### Issue 6: `FlywayValidateException: Migration checksum mismatch`
- **Root Cause**: An existing SQL migration file in `db/migration/` was modified after it had already been executed against the database.
- **Solution**:
  - In development: Drop the database and allow Flyway to re-apply:
    ```bash
    mysql -u root -e "DROP DATABASE inventory_billing; CREATE DATABASE inventory_billing;"
    ```
  - In production: Never modify existing migration files; write a new forward migration (`V9__...sql`).

---

## 4. Authentication & JWT Errors

### Issue 7: `401 Unauthorized` on Protected Endpoints
- **Common Causes**:
  1. The `Authorization` header is missing or lacks the `Bearer ` prefix.
  2. The JWT token has expired (default expiration is 1 hour).
  3. The `JWT_SECRET` used to sign the token differs from the running backend's secret.
- **Solution**: Request a fresh token via `POST /api/v1/auth/login` and verify header formatting:
  ```http
  Authorization: Bearer <valid_token>
  ```

### Issue 8: `403 Forbidden` on User Management or Reports
- **Root Cause**: Your authenticated user does not have the required role (e.g. attempting to call `/api/v1/users` as a `BILLING_CLERK`).
- **Solution**: Log in as `admin` to perform administrative operations.

---

## 5. Diagnostic Commands Reference

| Purpose | Command |
|---|---|
| Check app health | `curl -i http://localhost:8080/actuator/health` |
| Check Docker container status | `docker compose ps` |
| Stream container logs | `docker compose logs -f app` |
| View active database tables | `mysql -u inv_app -plocaldevpassword -e "USE inventory_billing; SHOW TABLES;"` |
| Run test suite with full logs | `./mvnw clean test -X` |
