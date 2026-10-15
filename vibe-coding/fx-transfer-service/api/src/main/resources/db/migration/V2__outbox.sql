CREATE TABLE outbox_events (
 id UUID PRIMARY KEY,
 event_type VARCHAR(100) NOT NULL,
 aggregate_id UUID NOT NULL,
 payload TEXT NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 created_at TIMESTAMPTZ NOT NULL,
 published_at TIMESTAMPTZ
);
CREATE INDEX idx_outbox_pending ON outbox_events(created_at) WHERE status = 'PENDING';
