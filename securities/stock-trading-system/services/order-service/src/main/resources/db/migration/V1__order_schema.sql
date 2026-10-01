CREATE TABLE orders (
    id UUID PRIMARY KEY,
    account_id VARCHAR(100) NOT NULL,
    symbol VARCHAR(6) NOT NULL,
    side VARCHAR(10) NOT NULL,
    order_type VARCHAR(20) NOT NULL,
    quantity BIGINT NOT NULL CHECK (quantity > 0),
    price NUMERIC(19,4),
    filled_quantity BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (filled_quantity >= 0 AND filled_quantity <= quantity)
);
CREATE INDEX idx_orders_account_created ON orders(account_id, created_at DESC);

CREATE TABLE order_outbox (
    id UUID PRIMARY KEY,
    topic VARCHAR(200) NOT NULL,
    event_key VARCHAR(200) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);
CREATE INDEX idx_order_outbox_status_created ON order_outbox(status, created_at);

CREATE TABLE order_processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
