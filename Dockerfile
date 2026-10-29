# ==============================================================================
# Multi-Stage Dockerfile for Inventory & Billing Management System
# Base: Eclipse Temurin OpenJDK 21 LTS (Jammy)
# Security: Runs as non-root user (appuser:1001) with minimal JRE runtime
# ==============================================================================

# Stage 1: Build & Package
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copy Maven wrapper & POM
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Copy source code and build production JAR directly
COPY src/ ./src/
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Lightweight, Secure Runtime
FROM eclipse-temurin:21-jre-jammy AS runtime

# Install curl for container healthcheck
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Create dedicated non-root application user and group
RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -s /bin/false -m appuser

WORKDIR /app

# Copy executable jar from builder stage with correct file ownership
COPY --from=builder --chown=appuser:appgroup /app/target/*.jar app.jar

# Run as non-root user for security best practice
USER appuser

EXPOSE 8080

# Health check to ensure service readiness
HEALTHCHECK --interval=20s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Launch JVM with secure random seed
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
