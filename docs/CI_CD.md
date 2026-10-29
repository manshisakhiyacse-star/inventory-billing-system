# CI/CD Automation & GitHub Actions Guide

This guide details the Continuous Integration (CI) and Continuous Deployment (CD) workflows implemented for the **Inventory & Billing Management System**.

---

## 1. Overview of the Pipelines

The project implements a modern DevOps lifecycle split into two specialized GitHub Actions workflows:

```
[ Developer Git Push / Pull Request ]
                 |
                 v
+-----------------------------------------------------------------------------------+
|                        1. CI Pipeline (.github/workflows/ci.yml)                   |
|                                                                                   |
|  Step 1: Check out code                                                           |
|  Step 2: Set up Java 21 (Temurin) with automated Maven ~/.m2 cache                |
|  Step 3: Run ./mvnw clean verify (Flyway syntax, 40 Unit & MockMvc tests)        |
|  Step 4: Upload Surefire XML Test Reports artifact                                |
|  Step 5: Test multi-stage Docker build with Buildx (no registry push)             |
+-----------------------------------------------------------------------------------+
                 |
                 | (Merge to 'main' or manual trigger)
                 v
+-----------------------------------------------------------------------------------+
|                        2. CD Pipeline (.github/workflows/cd.yml)                   |
|                                                                                   |
|  Step 1: Authenticate to AWS using OpenID Connect (OIDC) - Zero Static Keys!     |
|  Step 2: Authenticate Docker client with Amazon ECR                              |
|  Step 3: Build & Tag Docker image with Git Commit SHA & latest                   |
|  Step 4: Push image to Amazon ECR                                                 |
|  Step 5: Rolling deployment to EC2 via AWS Systems Manager (SSM)                 |
|  Step 6: Verify health check at /actuator/health (auto-rollback on failure)       |
+-----------------------------------------------------------------------------------+
```

---

## 2. Continuous Integration Workflow (`ci.yml`)

### Triggers:
- Every `push` to `main` or `master`.
- Every `pull_request` targeting `main` or `master`.
- Manual trigger via `workflow_dispatch`.

### Key Jobs:
1. **`build-and-test`**:
   - Compiles all 72 source files with Java 21 release flags.
   - Executes 40 automated tests covering:
     - BCrypt password strength and hash verification.
     - JWT token generation, parsing, expiration, and tampering rejection.
     - Jakarta Bean Validation constraints on request DTOs.
     - Business services with mock transactions.
     - MockMvc REST endpoints with RFC 7807 problem details validation.
   - Saves test reports to GitHub artifacts.
2. **`docker-build-check`**:
   - Validates that the multi-stage Dockerfile builds successfully using GitHub Actions cache.

---

## 3. Continuous Deployment Workflow (`cd.yml`)

### Why We Use AWS OIDC (OpenID Connect)
Traditional CI/CD pipelines store static AWS Access Key IDs and Secret Access Keys in GitHub Secrets. If compromised, an attacker can access your cloud resources indefinitely.
- **OIDC Solution**: GitHub requests a temporary, short-lived JWT token from AWS Security Token Service (STS) for each workflow run. The role can only be assumed by your specific GitHub repository and branch. **Zero static credentials are stored.**

---

## 4. Configuring GitHub Repository Secrets for AWS

When you are ready to configure the CD pipeline on GitHub:

1. In your GitHub repository, navigate to **Settings** $\rightarrow$ **Secrets and variables** $\rightarrow$ **Actions**.
2. Click **New repository secret**:
   - **Name**: `AWS_ROLE_TO_ASSUME`
   - **Value**: The ARN of the IAM Role created for GitHub Actions (e.g. `arn:aws:iam::123456789012:role/github-actions-cd-role`).

---

## 5. Branching Strategy Best Practices

- **`main`**: Protected production branch. All code must pass the CI pipeline before merging.
- **Feature Branches** (`feat/feature-name`): Branch off `main`, develop features, and submit Pull Requests.
- **Pull Request Checks**: Configure GitHub Branch Protection Rules to require the `Build & Test (Java 21)` status check to pass before merging.
