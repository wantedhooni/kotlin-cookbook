ALTER TABLE bank_transfer_sagas ADD COLUMN reconcile_after TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE bank_transfer_sagas ADD COLUMN reconcile_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE bank_transfer_sagas ADD COLUMN reconcile_owner UUID;
ALTER TABLE bank_transfer_sagas ADD COLUMN reconcile_lease_until TIMESTAMPTZ;
ALTER TABLE bank_transfer_sagas ADD COLUMN reconcile_last_error VARCHAR(500);
CREATE INDEX idx_bank_reconcile_due ON bank_transfer_sagas(reconcile_after) WHERE status = 'UNKNOWN';
CREATE TABLE bank_reconciliation_evidence (
 id BIGSERIAL PRIMARY KEY, saga_id UUID NOT NULL REFERENCES bank_transfer_sagas(id),
 outcome VARCHAR(16) NOT NULL CHECK(outcome IN ('SUCCEEDED','REJECTED')),
 provider_reference VARCHAR(160) NOT NULL,
 checked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 UNIQUE(saga_id, outcome, provider_reference)
);
