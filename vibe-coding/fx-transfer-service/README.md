# FX & Transfer — Java 21 / Kotlin / Spring Boot

## Modules
- `domain`: pure Kotlin domain models, no framework
- `application`: FX/transfer use cases and ports, transactional orchestration
- `infrastructure`: PostgreSQL JPA, outbox/Kafka, Redis, bank and FX outbound adapters
- `api`: REST controllers, Flyway, application wiring, configuration, Testcontainers smoke test

## Local
`docker compose up --build` — starts PostgreSQL, Kafka (KRaft), Redis, API on port 8080.
`gradle :api:bootRun` (requires the dependencies already up and Java 21 + Gradle 8.14)
`gradle test`; `RUN_CONTAINER_TESTS=true gradle :api:test` (Docker available).

## Profiles
`demo` (default): deterministic sample FX and bank response. **Not suitable for real money**.
`seoul-fx`: adapter expects *your own contracted normalized gateway* `GET /quotes?base=KRW&counter=USD` returning `{base,counter,rate,observedAt}` with auth `X-API-Key`. No official Seoul Foreign Exchange Brokerage endpoint is claimed.
`bank-http`: contract-template `POST /transfers`; customize for bank/PG signed requests, timeouts, status polling, callbacks, retries and reconciliation.

## Reliability
- Account locking in UUID order and DB check constraints.
- Same-transaction transfer/exchange receipt, idempotency key and outbox entry.
- Poller publishes Kafka at least once; **consumer deduplication by the included eventId is mandatory before production use**.
- Redis quote cache component is scaffolded and deliberately not used for balances; integration with validated FX quotes still needed.
- No real settlement, 1 KRW account verification, double-entry ledger, reconciliation, or regulatory compliance included.
- Demo accounts may be created with initial balance and unauthenticated endpoints; LOCK DOWN before production.

## EKS scaffolds
`deploy/k8s` includes rolling update, probes, resource budgets and example config.
`deploy/argocd` contains a sample Application; `deploy/terraform` looks up an existing EKS cluster. Replace all placeholders, use Secrets Manager, IRSA, private VPC and managed PostgreSQL/MSK/Redis before deployment.

## Caveats
No guaranteed build execution in this environment. An official exchange-rate interface requires provider contract, auth method and format; never assume a public endpoint. Integration test is a container availability smoke test, not comprehensive transactional testing.

## API sample
```bash
curl -X POST localhost:8080/api/v1/accounts -H 'Content-Type: application/json' \
  -d '{"currency":"KRW","initialBalance":100000}'
# Create another KRW account and transfer with IDs from responses:
curl -X POST localhost:8080/api/v1/transfers -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-payment-001' \
  -d '{"fromAccountId":"SOURCE_UUID","toAccountId":"TARGET_UUID","amount":100,"currency":"KRW"}'
```


## Bank payout Saga (extension)

`POST /api/v1/bank-transfers` with `Idempotency-Key` and body:

```json
{"sourceAccountId":"<uuid>","bankCode":"DEMO","destinationAccount":"123456","amount":1000,"currency":"KRW"}
```

`GET /api/v1/bank-transfers/{id}` returns `RESERVED`, `SUCCEEDED`, `COMPENSATED`, or `UNKNOWN`. Initial request returns HTTP 202. Demo bank approves immediately. HTTP bank adapter requires a bank-specific contract. 

**Safety:** Ambiguous timeouts leave funds reserved. There is no publicly exposed reconciliation endpoint. The service-level `reconcile(id, confirmedSuccess, providerReference)` must only be invoked by a privileged, authenticated reconciliation process using verified bank evidence. This example does not include settlement confirmation, bank callbacks, authentication, or a full automated reconciliation worker. `SUCCEEDED` means bank accepted the instruction, not that funds have finally settled.

Kafka event consumer `financial-audit-v1` inserts deduplication token and audit in one PostgreSQL transaction. Delivery is at-least-once. Outbox polling still holds database locks while awaiting Kafka acknowledgments; a production implementation should move to leases/batches or CDC.

### Tests

```bash
./gradlew test                    # when a Gradle wrapper is available
RUN_CONTAINER_TESTS=true ./gradlew :api:test  # Docker + PostgreSQL/Kafka Testcontainers
k6 run -e SOURCE_ACCOUNT_ID=<funded-account-uuid> scripts/k6-bank-payout.js
```

Repository does not include Gradle wrapper; install Gradle 8.14+ or generate a wrapper before invoking these commands. k6 requires separate installation. Load tests debit demo funds; use disposable accounts only. Testcontainers suites are opt-in.

See `docs/adr/0003-bank-saga.md` and `scripts/reconcile-stale.sql`.


## Expansion: bank auto-reconciliation + HA (v0.4)

- `BankReconciliationScheduler`: PostgreSQL lease-based claim (`SKIP LOCKED`), configurable poll size/interval, retry backoff and manual triage after max attempts. Only authenticated definitive bank results settle UNKNOWN; demo status is always PENDING.
- `bank_reconciliation_evidence`: durable provider reference captured in the same DB transaction as finalization/refund/outbox.
- `KafkaResilienceConfig`: exponential backoff and `.DLT` publishing (pre-provision topics with identical partitions); deduplication is retained.
- `/actuator/prometheus` gauges + Prometheus alerts + starter Grafana dashboard in `deploy/monitoring`.
- EKS HPA, PodDisruptionBudget in `deploy/k8s`; requires metrics-server, multi-AZ nodes, appropriate probes, external secrets and topic provisioning.
- Staging fault-injection drill guide: `tests/chaos/scenarios.md`.

**Limits:** illustrative bank status URL/schema, not wired to any contracted bank; no real bank authorization, signatures, reconciliation SLA, or complete production HA guarantee. Do not use real funds without a bank integration review.


## Expansion: concurrency, performance, delivery (v0.5)

- `scripts/k6-1000tps.js`: open-loop 1,000 TPS **target** with latency/error/drop thresholds (not a performance guarantee).
- `scripts/k6-idempotency.js`: same-key concurrent replays; inspect account and transfer rows to verify exactly-once reservation.
- `deploy/terraform/pod-identity.tf`: opt-in least-privilege role example for Secrets Manager with EKS Pod Identity.
- `deploy/k8s/external-secret.yaml`: External Secrets Operator to Kubernetes Secret mapping; ESO controller auth must be configured separately.
- `scripts/verify.sh`: actual local compile/test/assemble runner; exits non-zero if Gradle unavailable.
- See `docs/performance-and-deployment.md`. No production performance or integration-test success is asserted without a real run.


## Kotest BehaviorSpec tests

Kotest BehaviorSpec의 `Given / When / Then` 단계 설명은 모두 한글로 작성했습니다. (DSL 키워드는 Kotest 문법이므로 영문을 유지합니다.)

```bash
gradle test                         # unit tests (no Docker)
RUN_CONTAINER_TESTS=true gradle :api:test  # PostgreSQL + Kafka integration tests (Docker required)
```

The container specs are excluded from Gradle test discovery unless `RUN_CONTAINER_TESTS=true`.
Kotest JUnit 5 runner and SpringExtension are configured for Spring Boot integration tests.
