variable "aws_region" {
  description = "Target AWS Region for deployment"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Project name prefix for AWS resources"
  type        = string
  default     = "inventory-billing"
}

variable "environment" {
  description = "Deployment environment (e.g. staging, production)"
  type        = string
  default     = "production"
}

variable "vpc_cidr" {
  description = "CIDR block for the custom VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets (across 2 Availability Zones)"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets (for RDS database)"
  type        = list(string)
  default     = ["10.0.10.0/24", "10.0.11.0/24"]
}

variable "db_instance_class" {
  description = "Database instance size (db.t4g.micro is Free Tier eligible)"
  type        = string
  default     = "db.t4g.micro"
}

variable "db_name" {
  description = "Initial MySQL database name"
  type        = string
  default     = "inventory_billing"
}

variable "db_username" {
  description = "Master MySQL database username"
  type        = string
  default     = "inv_app"
}

variable "ec2_instance_type" {
  description = "EC2 instance size for hosting the backend container (t4g.small or t3.small)"
  type        = string
  default     = "t4g.small"
}
