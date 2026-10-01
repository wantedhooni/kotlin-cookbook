CREATE TABLE positions (
    id BIGSERIAL PRIMARY KEY,
    account_id VARCHAR(100) NOT NULL,
    symbol VARCHAR(6) NOT NULL,
    quantity BIGINT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved_quantity BIGINT NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    average_price NUMERIC(19,4) NOT NULL DEFAULT 0,
    realized_pnl NUMERIC(19,4) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_position_account_symbol UNIQUE(account_id, symbol),
    CHECK (reserved_quantity <= quantity)
);

CREATE TABLE sell_reservations (
    order_id UUID PRIMARY KEY,
    account_id VARCHAR(100) NOT NULL,
    symbol VARCHAR(6) NOT NULL,
    quantity BIGINT NOT NULL CHECK (quantity > 0),
    consumed_quantity BIGINT NOT NULL DEFAULT 0 CHECK (consumed_quantity >= 0),
    canceled_quantity BIGINT NOT NULL DEFAULT 0 CHECK (canceled_quantity >= 0),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CHECK (consumed_quantity + canceled_quantity <= quantity)
);

CREATE TABLE balance_processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
