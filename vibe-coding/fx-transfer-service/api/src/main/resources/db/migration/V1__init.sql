CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    currency VARCHAR(3) NOT NULL,
    balance NUMERIC(24, 4) NOT NULL CHECK (balance >= 0),
    version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE transfers (
    id UUID PRIMARY KEY,
    source_account_id UUID NOT NULL REFERENCES accounts(id),
    target_account_id UUID NOT NULL REFERENCES accounts(id),
    amount NUMERIC(24, 4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT transfer_different_accounts CHECK (source_account_id <> target_account_id)
);
CREATE TABLE exchanges (
    id UUID PRIMARY KEY,
    source_account_id UUID NOT NULL REFERENCES accounts(id),
    target_account_id UUID NOT NULL REFERENCES accounts(id),
    source_amount NUMERIC(24,4) NOT NULL CHECK (source_amount > 0),
    target_amount NUMERIC(24,4) NOT NULL CHECK (target_amount > 0),
    rate NUMERIC(24,10) NOT NULL CHECK (rate > 0),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE idempotency_keys (
    operation VARCHAR(20) NOT NULL,
    request_key VARCHAR(120) NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    resource_id UUID NOT NULL,
    PRIMARY KEY (operation, request_key)
);
CREATE INDEX idx_transfers_source_created ON transfers(source_account_id, created_at DESC);
CREATE INDEX idx_exchanges_source_created ON exchanges(source_account_id, created_at DESC);
