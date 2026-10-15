-- Read-only alerting query. Investigate these transactions with the bank; do NOT refund blindly.
SELECT id,status,source_account_id,amount,created_at,updated_at
FROM bank_transfer_sagas
WHERE status IN ('RESERVED','UNKNOWN') AND updated_at < now() - interval '5 minutes'
ORDER BY updated_at;
