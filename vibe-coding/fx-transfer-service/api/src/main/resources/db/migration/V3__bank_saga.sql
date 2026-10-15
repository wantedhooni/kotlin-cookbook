CREATE TABLE bank_transfer_sagas (
 id UUID PRIMARY KEY,
 source_account_id UUID NOT NULL REFERENCES accounts(id),
 bank_code VARCHAR(30) NOT NULL,
 destination_account VARCHAR(120) NOT NULL,
 amount NUMERIC(24,4) NOT NULL CHECK(amount > 0),
 currency VARCHAR(3) NOT NULL,
 request_key VARCHAR(120) NOT NULL UNIQUE,
 fingerprint VARCHAR(64) NOT NULL,
 status VARCHAR(24) NOT NULL CHECK(status IN ('RESERVED','SUCCEEDED','COMPENSATED','UNKNOWN')),
 provider_reference VARCHAR(160),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_bank_saga_status_updated ON bank_transfer_sagas(status, updated_at);
CREATE TABLE consumed_events (
 consumer_name VARCHAR(120) NOT NULL,
 event_id UUID NOT NULL,
 received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 PRIMARY KEY(consumer_name,event_id)
);
CREATE TABLE payment_event_audit (
 event_id UUID PRIMARY KEY,
 event_type VARCHAR(100) NOT NULL,
 aggregate_id UUID NOT NULL,
 received_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
