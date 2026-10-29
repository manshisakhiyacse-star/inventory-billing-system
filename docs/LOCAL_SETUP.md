# Local Development Setup Guide

This guide walks you through setting up and running the **Inventory & Billing Management System** on your local workstation (macOS, Linux, or Windows).

---

## 1. Prerequisites

Before starting, ensure you have the following installed:

| Tool | Recommended Version | Verification Command |
|---|---|---|
| **Java** | OpenJDK 21 LTS | `java -version` |
| **Docker** | 24.x or newer (Docker Desktop) | `docker --version` |
| **Git** | 2.30+ | `git --version` |
| **Maven** | 3.9+ (or use included `./mvnw`) | `./mvnw -version` |
| **MySQL Client** | 8.0+ (optional, for direct CLI queries) | `mysql --version` |

---

## 2. Java 21 Environment Setup

### macOS (Apple Silicon or Intel)
Using Homebrew:
```bash
brew install openjdk@21
```
Add Java 21 to your shell profile (`~/.zshrc` or `~/.bash_profile`):
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
```
Reload your terminal session:
```bash
source ~/.zshrc
java -version
```

### Ubuntu / Debian Linux
```bash
sudo apt update
sudo apt install -y openjdk-21-jdk
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

---

## 3. Clone Repository & Environment File

1. Navigate to the project directory:
   ```bash
   cd inventory-billing-system
   ```

2. Create your local environment configuration from the template:
   ```bash
   cp .env.example .env
   ```

3. Default environment variables in `.env`:
   ```properties
   DB_HOST=localhost
   DB_PORT=3306
   DB_NAME=inventory_billing
   DB_USERNAME=inv_app
   DB_PASSWORD=localdevpassword
   JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
   JWT_EXPIRATION_MS=3600000
   CORS_ALLOWED_ORIGINS=http://localhost:3000
   ```

---

## 4. Running the Database (Two Options)

### Option A: Using Docker (Recommended)
Launch the isolated MySQL 8 container defined in `compose.yaml`:
```bash
docker compose up -d mysql
```
Verify the container is healthy:
```bash
docker compose ps
```

### Option B: Using Local MySQL Service
If you already have MySQL installed via Homebrew (`brew services start mysql`):
```bash
mysql -u root <<EOF
CREATE DATABASE IF NOT EXISTS inventory_billing CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'inv_app'@'localhost' IDENTIFIED BY 'localdevpassword';
GRANT ALL PRIVILEGES ON inventory_billing.* TO 'inv_app'@'localhost';
FLUSH PRIVILEGES;
EOF
```

---

## 5. Starting the Spring Boot Backend

### Run via Maven Wrapper:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Upon boot:
1. **Flyway** connects to MySQL and applies migrations `V1` through `V8`.
2. Initial administrator account is seeded (`admin` / `admin123`).
3. Tomcat starts on port `8080`.

---

## 6. Verifying the Application

### 1. Actuator Health Endpoint
```bash
curl http://localhost:8080/actuator/health
```
*Response:* `{"status":"UP"}`

### 2. Interactive Swagger UI
Open in your browser:
```
http://localhost:8080/swagger-ui/index.html
```

### 3. Obtain Admin JWT Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

---

## 7. Running the Automated Test Suite

Run the full suite of unit, validation, and MockMvc integration tests:
```bash
./mvnw clean test
```
All tests should pass with 0 failures:
```text
[INFO] Tests run: 40, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```
Surefire test reports are generated in `target/surefire-reports/`.
