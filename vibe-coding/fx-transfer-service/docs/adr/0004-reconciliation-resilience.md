# ADR 0004 — UNKNOWN bank settlement reconciliation and Kafka resilience

- Only bank-originated, authenticated definitive status with provider reference can settle an UNKNOWN transfer; PENDING and errors cannot trigger refund.
- Claim work via PostgreSQL `FOR UPDATE SKIP LOCKED` with a renewable-by-expiry lease. Each worker has an owner token; stale workers cannot record final evidence. Backoff exponential, capped at one hour; exhausted transactions require human triage.
- Final status transition, compensation, evidence row and outbox event share a single DB transaction.
- Kafka uses at-least-once delivery. Exponential retries and a `topic.DLT` capture poison messages; consumer deduplication is transactional.
- Kubernetes HPA requires metrics-server, PodDisruptionBudget and at least two availability-zone placements for actual HA.
- Tradeoff: reconciliation is polling-based and depends on bank contract semantics; an accepted transfer may not imply final settlement. Production bank adapters must map final settlement evidence only.
