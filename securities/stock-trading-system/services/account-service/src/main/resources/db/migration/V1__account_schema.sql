CREATE TABLE trading_accounts (
    account_id VARCHAR(100) PRIMARY KEY,
    cash_balance NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (cash_balance >= 0),
    reserved_cash NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (reserved_cash >= 0),
    credit_limit NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (credit_limit >= 0),
    used_credit NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (used_credit >= 0),
    reserved_credit NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK (reserved_credit >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (reserved_cash <= cash_balance),
    CHECK (used_credit + reserved_credit <= credit_limit)
);

CREATE TABLE buy_reservations (
    order_id UUID PRIMARY KEY,
    account_id VARCHAR(100) NOT NULL,
    symbol VARCHAR(6) NOT NULL,
    quantity BIGINT NOT NULL CHECK (quantity > 0),
    consumed_quantity BIGINT NOT NULL DEFAULT 0 CHECK (consumed_quantity >= 0),
    canceled_quantity BIGINT NOT NULL DEFAULT 0 CHECK (canceled_quantity >= 0),
    protected_unit_price NUMERIC(19,4) NOT NULL CHECK (protected_unit_price > 0),
    margin_rate NUMERIC(8,6) NOT NULL CHECK (margin_rate > 0 AND margin_rate <= 1),
    remaining_cash NUMERIC(19,4) NOT NULL CHECK (remaining_cash >= 0),
    remaining_credit NUMERIC(19,4) NOT NULL CHECK (remaining_credit >= 0),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CHECK (consumed_quantity + canceled_quantity <= quantity)
);
CREATE INDEX idx_buy_reservation_account ON buy_reservations(account_id, status);

CREATE TABLE account_processed_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
