CREATE TABLE settlement_obligations (
 id BIGSERIAL PRIMARY KEY, execution_id VARCHAR(120) NOT NULL UNIQUE, order_id UUID NOT NULL, account_id VARCHAR(100) NOT NULL,
 side VARCHAR(10) NOT NULL, gross_amount NUMERIC(19,4) NOT NULL, trade_date DATE NOT NULL, settlement_date DATE NOT NULL, status VARCHAR(20) NOT NULL,
 shortage_amount NUMERIC(19,4) NOT NULL DEFAULT 0, created_at TIMESTAMPTZ NOT NULL, settled_at TIMESTAMPTZ
);
CREATE INDEX idx_settlement_due ON settlement_obligations(status,settlement_date);
CREATE INDEX idx_settlement_account ON settlement_obligations(account_id,settlement_date DESC);
