terraform { required_version = ">= 1.6.0" }
provider "aws" { region = var.aws_region }
variable "aws_region" { type = string; default = "ap-northeast-2" }
# Add VPC, EKS, RDS, ElastiCache, MSK and ECR modules per environment.
