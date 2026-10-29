# AWS Cost Estimation & Resource Teardown Guide

This document details expected operational costs on AWS, Free Tier eligibility breakdowns, and step-by-step instructions for tearing down all cloud infrastructure to prevent unexpected charges.

---

## 1. AWS Cost Estimate & Free Tier Breakdown

The system's cloud architecture is deliberately sized to fit within the **AWS 12-Month Free Tier** for new accounts.

### Monthly Cost Breakdown:

| Service | Architecture Component | Free Tier Allowance | Estimated Monthly Cost (Free Tier) | Estimated Monthly Cost (Post Free Tier) |
|---|---|---|:---:|:---:|
| **Amazon EC2** | Application host (`t4g.small` or `t3.micro`) | 750 hours / month of `t3.micro` or `t2.micro` | **$0.00** | ~$6.50 – $12.00 |
| **Amazon RDS** | MySQL 8 Database (`db.t4g.micro` or `db.t3.micro`) | 750 hours / month + 20 GB gp3 storage | **$0.00** | ~$13.00 – $17.00 |
| **Amazon ECR** | Container image repository | 500 MB / month private storage | **$0.00** | ~$0.10 (with 10-image retention policy) |
| **AWS Secrets Manager**| DB credentials & JWT secret | 30-day trial per secret | **$0.00** (Trial) | $0.40 / secret / month |
| **VPC & Internet Gateway** | Public/Private routing | Always Free | **$0.00** | $0.00 |
| **Data Transfer** | Inbound & Outbound traffic | 100 GB / month free internet egress | **$0.00** | < $0.50 |
| **TOTAL** | | | **$0.00 – $0.40 / month** | **~$20.00 – $30.00 / month** |

---

## 2. Cost Optimization Best Practices

1. **ECR Image Retention**:
   - Our Terraform configuration includes an automatic lifecycle policy that retains only the **last 10 container images**, automatically deleting old layers so you never incur excess storage charges.

2. **ARM64 Graviton Architecture**:
   - Using `t4g` instances provides up to 40% better price-performance compared to comparable x86 instances.

3. **Stop EC2 When Not Testing**:
   - If using a personal learning account, stop the EC2 instance outside working hours:
     ```bash
     aws ec2 stop-instances --instance-ids <YOUR_INSTANCE_ID>
     ```

---

## 3. Complete Resource Teardown Instructions

When you are done testing on AWS, follow these steps to guarantee that **all** cloud resources are deleted and zero lingering charges occur.

### If Deployed via Terraform:
The quickest and most reliable teardown is through Terraform:

```bash
cd inventory-billing-system/terraform
terraform destroy
```
- Terraform will scan your state file and display a list of every resource to be deleted.
- Type `yes` when prompted.
- All EC2 instances, RDS databases, ECR repositories, Secrets Manager secrets, and VPC networking components will be destroyed in reverse order.

---

### If Deployed via AWS Management Console:
Follow this specific deletion order to prevent dependency errors:

1. **Delete EC2 Instance**:
   - Navigate to **EC2** $\rightarrow$ **Instances** $\rightarrow$ Select your instance $\rightarrow$ Click **Instance State** $\rightarrow$ **Terminate Instance**.
2. **Delete Amazon RDS Database**:
   - Navigate to **RDS** $\rightarrow$ **Databases** $\rightarrow$ Select `inventory-billing-mysql` $\rightarrow$ Click **Actions** $\rightarrow$ **Delete**.
   - Uncheck "Create final snapshot", acknowledge the confirmation prompt, and confirm deletion.
3. **Delete Amazon ECR Repository**:
   - Navigate to **ECR** $\rightarrow$ **Repositories** $\rightarrow$ Select `inventory-billing-app` $\rightarrow$ Click **Delete** $\rightarrow$ Confirm deletion of all images.
4. **Delete Secrets Manager Secret**:
   - Navigate to **Secrets Manager** $\rightarrow$ Select `inventory-billing-secrets` $\rightarrow$ Click **Actions** $\rightarrow$ **Delete secret** $\rightarrow$ Select **Immediately** (without recovery window).
5. **Delete VPC**:
   - Navigate to **VPC** $\rightarrow$ **Your VPCs** $\rightarrow$ Select `inventory-billing-vpc` $\rightarrow$ Click **Actions** $\rightarrow$ **Delete VPC** (this automatically deletes attached subnets, route tables, and security groups).

---

## 4. Local Machine Cleanup

To free local disk space used by local testing containers:

```bash
cd inventory-billing-system

# Stop and remove containers and database volume
docker compose down -v

# Remove local application image
docker rmi inventory-billing-system-app:latest
```
