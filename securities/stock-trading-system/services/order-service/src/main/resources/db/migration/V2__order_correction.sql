CREATE TABLE order_corrections (
    correction_id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    old_price NUMERIC(19,4) NOT NULL,
    new_price NUMERIC(19,4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ
);
CREATE INDEX idx_order_corrections_order_requested ON order_corrections(order_id, requested_at DESC);
