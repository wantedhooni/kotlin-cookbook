# ADR 0002: Transactional outbox and at-least-once events
Balances, receipts, idempotency rows, and outbox rows are committed in one PostgreSQL transaction.
A polling worker uses `FOR UPDATE SKIP LOCKED` and publishes to Kafka before marking published.
If a process crashes after sending and before marking, consumers may receive a duplicate.
Consumers MUST deduplicate using the included `eventId` envelope; reference consumer implementation is not included.
For production: add bounded retries/metrics/DLQ, avoid holding DB locks across network I/O, reconcile event counts.
