CREATE TABLE executions (
    id BIGSERIAL PRIMARY KEY,
    execution_id VARCHAR(100) NOT NULL,
    order_id UUID NOT NULL,
    account_id VARCHAR(100) NOT NULL,
    symbol VARCHAR(6) NOT NULL,
    side VARCHAR(10) NOT NULL,
    quantity BIGINT NOT NULL CHECK (quantity > 0),
    price NUMERIC(19,4) NOT NULL CHECK (price > 0),
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_execution_id UNIQUE(execution_id)
);
CREATE INDEX idx_executions_order ON executions(order_id, occurred_at);

CREATE TABLE execution_outbox (
    id UUID PRIMARY KEY,
    topic VARCHAR(200) NOT NULL,
    event_key VARCHAR(200) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);
CREATE INDEX idx_execution_outbox_status_created ON execution_outbox(status, created_at);
