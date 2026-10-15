# Performance / EKS operating guide

## TPS verification

A 1,000 TPS target is **not** a measured result. k6 `constant-arrival-rate` starts 1,000 requests/second by default. Use at least 1,000 separate funded disposable accounts (single account row lock serializes writes), sufficient preallocated VUs, isolated benchmark infrastructure and real observability. Avoid exposing the demo no-auth API publicly.

Run: `k6 run -e SOURCE_ACCOUNT_IDS=uuid1,uuid2,... scripts/k6-1000tps.js`; check dropped iterations, p95 latency, HTTP errors, DB CPU, connection pool, lock waits, Kafka lag, Outbox backlog. Baseline first, then 100/250/500/1000 TPS steps; record 5 runs per level and compare p50/p95/p99; measure and inspect `pg_stat_activity`/`pg_locks` for lock waits and deadlocks. Acknowledge money movements must not be retried with a new idempotency key.

The demo bank does not establish production settlement correctness or sustainable throughput. Conduct an additional concurrent same-key replay test; expected exactly one reservation, consistent transfer ID and no double debit.

## Deployment prerequisites

1. Provision private networking, EKS nodes spanning >=2 AZs, managed PostgreSQL/Redis/Kafka, ingress+TLS, IAM permissions, monitoring and backup/recovery using platform infrastructure as code.
2. Install EKS Pod Identity Agent, External Secrets Operator and a correctly authenticated `ClusterSecretStore/aws-secretsmanager`. The sample ExternalSecret is illustrative; an ESO controller service account usually needs its **own** IAM role unless you configure a per-store service account, and merely granting the API pod secret read permission does **not** grant ESO permissions.
3. Store `DB_PASSWORD`, `BANK_API_KEY`, `FX_PROVIDER_API_KEY` in existing `fx-transfer/production` Secrets Manager JSON secret. Never commit secrets. Set `secret_arn` to that exact secret ARN. Ensure DB_USER/DB_URL and providers are configured in ConfigMap; do not deploy demo profile publicly.
4. Replace ECR image placeholder with signed immutable image digest, Argo repo URL and git revision with actual values. Validate manifests, policy and staging rollouts. ArgoCD application references CRDs which must be installed first. Do not confuse readiness with actual financial-provider availability.
5. Terraform role/pod association is opt-in (`enable_pod_identity=true`) and does not install EKS itself; ensure permissions and least privilege, encryption and rollback procedures.

## Known limitations

No actual bank contract, high-confidence final settlement, production security or measured 1,000 TPS are supplied. Running k6 against a single funded account is a deliberately pathological lock-contention test, not a 1,000 TPS capacity result.
