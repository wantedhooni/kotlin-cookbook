terraform {
 required_version = ">= 1.6.0"
 required_providers { aws = { source = "hashicorp/aws", version = "~> 5.0" } }
}
provider "aws" { region = var.aws_region }
variable "aws_region" { type = string; default = "ap-northeast-2" }
# Scaffold only: attach to an existing EKS cluster; do not create a public VPC by default.
variable "eks_cluster_name" { type = string }
data "aws_eks_cluster" "current" { name = var.eks_cluster_name }
output "eks_endpoint" { value = data.aws_eks_cluster.current.endpoint }
