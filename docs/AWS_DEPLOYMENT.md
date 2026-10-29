# AWS Cloud Deployment Guide

This guide provides complete instructions for deploying the **Inventory & Billing Management System** to Amazon Web Services (AWS) using either **Terraform Infrastructure as Code (IaC)** or the **AWS Management Console**.

---

## 1. Target AWS Cloud Architecture

```
+-----------------------------------------------------------------------------------+
|                                  AWS Cloud Region                                 |
|                                                                                   |
|  +-----------------------------------------------------------------------------+  |
|  |                           Virtual Private Cloud (VPC)                       |  |
|  |                                                                             |  |
|  |   [ Internet Gateway (IGW) ] <---------------------------- (Internet)       |  |
|  |                |                                                            |  |
|  |   +------------v--------------------------------------------------------+   |  |
|  |   | Public Subnet (AZ-1 & AZ-2)                                         |   |  |
|  |   |                                                                     |   |  |
|  |   |   +-------------------------------------------------------------+   |   |  |
|  |   |   | EC2 Application Container Host (t4g.small / ARM64)          |   |   |  |
|  |   |   |                                                             |   |  |
|  |   |   |   - Pulls Docker image from Amazon ECR                      |   |   |  |
|  |   |   |   - Reads DB password & JWT secret from Secrets Manager     |   |   |  |
|  |   |   |   - Runs container: inventory-billing-system:latest         |   |   |  |
|  |   |   |   - Security Group: Inbound 8080 (App), 22 (SSH)            |   |   |  |
|  |   |   +-------------------------------------------------------------+   |   |  |
|  |   +---------------------------------------------------------------------+   |  |
|  |                |                                                            |  |
|  |                | Private SQL Traffic (Port 3306)                            |  |
|  |                v                                                            |  |
|  |   +---------------------------------------------------------------------+   |  |
|  |   | Private Subnet (AZ-1 & AZ-2) - No Public Routing                    |   |  |
|  |   |                                                                     |   |  |
|  |   |   +-------------------------------------------------------------+   |   |  |
|  |   |   | Amazon RDS for MySQL 8.0 (db.t4g.micro - Free Tier)         |   |   |  |
|  |   |   |                                                             |   |  |
|  |   |   |   - Encrypted at rest (gp3 volume)                          |   |   |  |
|  |   |   |   - Automated daily snapshots                               |   |   |  |
|  |   |   |   - Security Group: Inbound 3306 ONLY from EC2 App Host     |   |   |  |
|  |   |   +-------------------------------------------------------------+   |   |  |
|  |   +---------------------------------------------------------------------+   |  |
|  +-----------------------------------------------------------------------------+  |
|                                                                                   |
|  [ Supporting Managed Services ]                                                  |
|    - Amazon ECR: Encrypted container repository with vulnerability scanning      |
|    - AWS Secrets Manager: Automatic key generation and storage                    |
|    - AWS IAM: Instance profile with least-privilege read permissions              |
+-----------------------------------------------------------------------------------+
```

---

## 2. Option A: Automated Deployment via Terraform (Recommended)

When you are ready to deploy to AWS, Terraform automates the entire provisioning process.

### Prerequisites:
1. An AWS Account.
2. [AWS CLI](https://aws.amazon.com/cli/) installed and configured (`aws configure`).
3. [Terraform](https://www.terraform.io/) (v1.5+) installed.

### Step-by-Step Commands:

1. **Navigate to the Terraform directory**:
   ```bash
   cd inventory-billing-system/terraform
   ```

2. **Create your variables file**:
   ```bash
   cp terraform.tfvars.example terraform.tfvars
   ```
   Edit `terraform.tfvars` if you wish to adjust the region (default: `us-east-1`) or instance types.

3. **Initialize Terraform plugins**:
   ```bash
   terraform init
   ```

4. **Review the execution plan**:
   ```bash
   terraform plan
   ```
   *Displays all resources to be created (VPC, Subnets, Security Groups, ECR, RDS, Secrets Manager, EC2).*

5. **Apply the configuration**:
   ```bash
   terraform apply
   ```
   Type `yes` when prompted. Provisioning takes approximately 5–7 minutes.

6. **View outputs**:
   Terraform will display your live resources:
   ```text
   app_public_ip       = "54.210.xx.xx"
   app_url             = "http://54.210.xx.xx:8080"
   swagger_ui_url      = "http://54.210.xx.xx:8080/swagger-ui/index.html"
   ecr_repository_url  = "123456789012.dkr.ecr.us-east-1.amazonaws.com/inventory-billing-app"
   rds_endpoint        = "inventory-billing-mysql.xxxxxx.us-east-1.rds.amazonaws.com:3306"
   ```

7. **Push your Docker image to ECR**:
   ```bash
   # Log in to ECR
   aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <ECR_REPOSITORY_URL>

   # Tag and push
   docker tag inventory-billing-system-app:latest <ECR_REPOSITORY_URL>:latest
   docker push <ECR_REPOSITORY_URL>:latest
   ```

---

## 3. Option B: Step-by-Step AWS Console Walkthrough

If you prefer using the AWS Web Console:

### Step 1: Create Virtual Private Cloud (VPC)
1. Go to **VPC Console** $\rightarrow$ Click **Create VPC**.
2. Select **VPC and more**.
3. Name: `inventory-billing-vpc`.
4. CIDR: `10.0.0.0/16`.
5. Number of Availability Zones: `2`.
6. Public subnets: `2`, Private subnets: `2`.
7. NAT Gateways: None (to minimize cost).

### Step 2: Configure Security Groups
1. **App Security Group (`app-sg`)**:
   - Inbound: Port 8080 (Custom TCP, `0.0.0.0/0`), Port 22 (SSH, your IP).
   - Outbound: All traffic.
2. **Database Security Group (`db-sg`)**:
   - Inbound: Port 3306 (MySQL/Aurora), Source: select `app-sg`.
   - Outbound: All traffic.

### Step 3: Create Amazon RDS MySQL Instance
1. Go to **RDS Console** $\rightarrow$ Click **Create database**.
2. Engine: **MySQL 8.0**.
3. Template: **Free Tier**.
4. DB instance identifier: `inventory-billing-mysql`.
5. Master username: `inv_app`, Master password: (enter secure password).
6. DB instance class: `db.t4g.micro` or `db.t3.micro`.
7. Storage: 20 GB gp3.
8. Connectivity: Select your VPC, DB Subnet Group (private subnets), Publicly accessible: **No**, Security group: `db-sg`.

### Step 4: Create Amazon ECR Repository
1. Go to **ECR Console** $\rightarrow$ Click **Create repository**.
2. Visibility: **Private**.
3. Repository name: `inventory-billing-app`.
4. Enable **Tag immutability** and **Scan on push**.

### Step 5: Launch EC2 Host Instance
1. Go to **EC2 Console** $\rightarrow$ Click **Launch Instance**.
2. AMI: **Amazon Linux 2023** (ARM64 if selecting t4g, x86_64 if selecting t3).
3. Instance type: `t4g.small` or `t3.small`.
4. Network settings: Select your VPC, Public Subnet, Auto-assign public IP: **Enable**, Security group: `app-sg`.
5. IAM Instance Profile: Attach a role containing `AmazonEC2ContainerRegistryReadOnly`.
6. Launch instance and connect via SSH or EC2 Instance Connect.

---

## 4. Production Security Checklist

- [ ] RDS database is in a **private subnet** with no public IP.
- [ ] Database Security Group allows inbound connections **strictly from the App Security Group**.
- [ ] Passwords and JWT secrets are stored in **AWS Secrets Manager**, never hardcoded.
- [ ] Container runs as non-root user (`appuser:1001`).
- [ ] EC2 IAM instance profile follows least privilege (`AmazonEC2ContainerRegistryReadOnly`).
