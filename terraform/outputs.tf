output "ecr_repository_url" {
  description = "URL of the Amazon ECR Docker repository for pushing application images"
  value       = aws_ecr_repository.app_repo.repository_url
}

output "rds_endpoint" {
  description = "Connection endpoint address for the private Amazon RDS MySQL instance"
  value       = aws_db_instance.mysql.endpoint
}

output "rds_address" {
  description = "Hostname of the private Amazon RDS MySQL database"
  value       = aws_db_instance.mysql.address
}

output "app_public_ip" {
  description = "Public IP address of the EC2 application container host"
  value       = aws_instance.app_server.public_ip
}

output "app_url" {
  description = "Direct HTTP access URL for the application"
  value       = "http://${aws_instance.app_server.public_ip}:8080"
}

output "swagger_ui_url" {
  description = "Direct URL for the Swagger UI documentation"
  value       = "http://${aws_instance.app_server.public_ip}:8080/swagger-ui/index.html"
}

output "secrets_manager_arn" {
  description = "ARN of the AWS Secrets Manager secret holding production credentials"
  value       = aws_secretsmanager_secret.app_secrets.arn
}
