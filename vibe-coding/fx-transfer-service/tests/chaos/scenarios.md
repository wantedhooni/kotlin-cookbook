# Failure / recovery drills (staging only)

1. **Bank timeout**: simulate bank accepting a transfer but dropping the HTTP response. Confirm `UNKNOWN`, unchanged reserved debit, status query success => exactly one final `SUCCEEDED`.
2. **Bank explicit reject**: status endpoint returns authoritative REJECTED and reference. Verify exactly one refund and one compensation outbox event, even after repeated reconciliation.
3. **Bank status API unavailable**: return 503; verify escalating `reconcile_after`, no refund, lease eventual expiry and operator alert on exhaustion.
4. **Worker killed after lease claim**: terminate pod; after 90 s lease another replica may claim (subject to backoff and due time).
5. **Kafka consumer crash**: interrupt handler before DB commit. Replay same message and verify `consumed_events` uniqueness and one audit row.
6. **DLT**: send malformed event; verify retry and DLT publish; manually replay after correction with original `eventId`.
7. **HPA scale**: generate CPU load in staging and watch HPA 2→N pods; verify resource requests and metrics-server prerequisites.
8. **Outbox publish vs commit failure**: force DB failure after Kafka ack; verify duplicate event but single consumer side effect.

Observe `/actuator/prometheus`, DLT lag and bank-side reconciliation evidence. Never use live funds for chaos drills.
