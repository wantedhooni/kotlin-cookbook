CREATE TABLE banks (id BIGSERIAL PRIMARY KEY, code VARCHAR(20) NOT NULL UNIQUE, name VARCHAR(120) NOT NULL);
CREATE TABLE accounts (
 id BIGSERIAL PRIMARY KEY, bank_id BIGINT NOT NULL REFERENCES banks(id),
 account_number VARCHAR(40) NOT NULL UNIQUE, owner_name VARCHAR(120) NOT NULL,
 balance NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK(balance >= 0),
 status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW());
CREATE INDEX idx_accounts_bank_owner ON accounts(bank_id,owner_name);
CREATE TABLE transfers (
 request_id UUID PRIMARY KEY, from_account_id BIGINT NOT NULL REFERENCES accounts(id),
 to_account_id BIGINT NOT NULL REFERENCES accounts(id), amount NUMERIC(19,2) NOT NULL CHECK(amount > 0),
 reference_id UUID UNIQUE, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW());
CREATE TABLE ledger_postings (
 id BIGSERIAL PRIMARY KEY, reference_id UUID NOT NULL, account_id BIGINT REFERENCES accounts(id),
 book VARCHAR(20) NOT NULL, side VARCHAR(10) NOT NULL, amount NUMERIC(19,2) NOT NULL CHECK(amount > 0),
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 CHECK((book='CLEARING' AND account_id IS NULL) OR (book='CUSTOMER' AND account_id IS NOT NULL)));
CREATE INDEX idx_ledger_account ON ledger_postings(account_id,id DESC);
CREATE INDEX idx_ledger_reference ON ledger_postings(reference_id);
CREATE TABLE outbox_events (
 id UUID PRIMARY KEY, aggregate_id BIGINT NOT NULL, event_type VARCHAR(60) NOT NULL,
 payload TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), published_at TIMESTAMPTZ);
CREATE INDEX idx_outbox_pending ON outbox_events(created_at) WHERE published_at IS NULL;
INSERT INTO banks(code,name) VALUES ('BANK001','Sample Bank');
