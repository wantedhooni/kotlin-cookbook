# ADR 0003 — Bank payout Saga

- Reserve funds in a local transaction, then call bank outside transaction.
- Positive bank acceptance: SUCCEEDED; explicit rejection: COMPENSATED and credit exactly once.
- Timeout, crash or unknown provider result: UNKNOWN/RESERVED; no automatic refund because bank may have settled.
- Reconciliation must query authenticated provider result by stable operation ID and invoke privileged `reconcile`.
- The sample bank `accepted` flag means provider accepted the request, **not guaranteed final settlement**. Production needs a separate bank settlement status and callback reconciliation.
- Row locks serialize competing resolution; conflicting terminal outcomes require manual investigation.
- Idempotency keys prevent duplicates; bank must support stable request IDs for safe retry.
- Operational requirement: alert on stale RESERVED/UNKNOWN, with reconciliation runbook; do not auto-retry ambiguous payouts.
