# ADR-0001: Internal balance update consistency

- Status: Accepted for portfolio example
- Context: Concurrent transfer requests must not overdraw an internal account.
- Decision: Use a single PostgreSQL transaction for both debits and credits, acquire `PESSIMISTIC_WRITE` locks in stable UUID order, write receipt and idempotency record atomically.
- Consequences: Prevents competing balance updates for the same accounts in the single database; contention increases on hot accounts. Unique key collisions may return HTTP 409. External provider side effects and distributed transactions are deliberately out of scope; production requires an immutable ledger, outbox, reconciliation and a well-defined saga.
