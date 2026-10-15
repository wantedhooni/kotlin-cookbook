ALTER TABLE outbox_events ADD COLUMN attempts INT NOT NULL DEFAULT 0;
ALTER TABLE outbox_events ADD COLUMN next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
ALTER TABLE outbox_events ADD COLUMN claimed_until TIMESTAMPTZ;
ALTER TABLE outbox_events ADD COLUMN claimed_by UUID;
ALTER TABLE outbox_events ADD COLUMN dead_lettered_at TIMESTAMPTZ;
ALTER TABLE outbox_events ADD COLUMN error_message VARCHAR(500);
CREATE INDEX idx_outbox_retry ON outbox_events(next_attempt_at,created_at)
 WHERE published_at IS NULL AND dead_lettered_at IS NULL;
CREATE TABLE consumed_events(
 consumer_name VARCHAR(120) NOT NULL, event_id UUID NOT NULL,
 processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 PRIMARY KEY(consumer_name,event_id));
CREATE TABLE audit_logs(
 id BIGSERIAL PRIMARY KEY, subject VARCHAR(160) NOT NULL,
 action VARCHAR(100) NOT NULL, resource VARCHAR(160) NOT NULL,
 detail VARCHAR(1000) NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW());
CREATE INDEX idx_audit_subject_time ON audit_logs(subject,created_at DESC);
CREATE TABLE daily_transfer_usage(
 account_id BIGINT NOT NULL REFERENCES accounts(id),business_date DATE NOT NULL,
 used_amount NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK(used_amount >= 0),
 PRIMARY KEY(account_id,business_date));
