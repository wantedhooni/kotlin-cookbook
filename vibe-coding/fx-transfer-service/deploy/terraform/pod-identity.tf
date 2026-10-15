# Existing EKS cluster; requires EKS Pod Identity Agent addon and External Secrets Operator.
variable "namespace" { type = string; default = "fx-transfer" }
variable "secret_arn" { type = string; description = "Pre-existing Secrets Manager secret ARN" }
variable "enable_pod_identity" { type = bool; default = false }

data "aws_iam_policy_document" "pod_assume" {
  statement {
    actions = ["sts:AssumeRole", "sts:TagSession"]
    principals { type = "Service"; identifiers = ["pods.eks.amazonaws.com"] }
  }
}
resource "aws_iam_role" "api" {
  count = var.enable_pod_identity ? 1 : 0
  name = "fx-transfer-api-pod-identity"
  assume_role_policy = data.aws_iam_policy_document.pod_assume.json
}
data "aws_iam_policy_document" "secret_read" {
  statement {
    actions = ["secretsmanager:GetSecretValue", "secretsmanager:DescribeSecret"]
    resources = [var.secret_arn]
  }
}
resource "aws_iam_role_policy" "secret_read" {
  count = var.enable_pod_identity ? 1 : 0
  name = "secret-read-minimal"
  role = aws_iam_role.api[0].id
  policy = data.aws_iam_policy_document.secret_read.json
}
resource "aws_eks_pod_identity_association" "api" {
  count = var.enable_pod_identity ? 1 : 0
  cluster_name = var.eks_cluster_name
  namespace = var.namespace
  service_account = "fx-transfer-api"
  role_arn = aws_iam_role.api[0].arn
}
