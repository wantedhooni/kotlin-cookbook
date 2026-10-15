# Architecture Notes

## Request flow

```text
Client
  |
  | POST /api/v1/parties/{partyId}/deposits/sync
  | Idempotency-Key: ...
  v
IdempotencyService
  |-- unique(operation, key)
  |-- request hash validation
  v
SyncLeaseService
  |-- atomic DB lease per partyId
  |-- short transaction only for acquire/release
  v
DbsDepositClient
  |-- clientId / accessToken / accept-version / uuid headers
  v
DBS Sandbox API
  |
  v
DbsJsonNormalizer -> DepositPersistenceService -> deposit_account
```

## Why a DB lease instead of a pessimistic lock around the HTTP call?

Holding a `PESSIMISTIC_WRITE` row lock while making a remote HTTP request also holds a database transaction/connection for the entire network latency. This implementation instead stores `locked_until` and acquires it by atomic conditional update. The lease is valid across multiple application nodes without keeping a transaction open during the remote call.

The configured remote HTTP read timeout must be comfortably lower than `app.sync.lease-duration`.

## Idempotency states

- `PROCESSING`: the first request owns the key.
- `SUCCEEDED`: response body is persisted and replayed for the same request hash.
- `FAILED`: failure result is also fixed for that key during the retention window.

The unique key is `(operation, idempotency_key)`. A SHA-256 request hash prevents the same key being reused for a different `partyId`.

## Data consistency

`deposit_account` also has a unique constraint on `(party_id, external_account_id)`. The distributed sync lease prevents concurrent writers in the normal path, and the database constraint is the final safety net.

The sync is an upsert only. It intentionally does not delete accounts missing from one provider response, because a partial/truncated upstream response should not remove existing local state.
