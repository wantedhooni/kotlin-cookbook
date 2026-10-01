ALTER TABLE trading_accounts ADD COLUMN settlement_cash_hold NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE trading_accounts ADD COLUMN settlement_credit_hold NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE trading_accounts ADD COLUMN pending_settlement_receivable NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE trading_accounts ADD COLUMN overdue_amount NUMERIC(19,4) NOT NULL DEFAULT 0;

CREATE TABLE account_settlement_holds (
    execution_id VARCHAR(120) PRIMARY KEY,
    order_id UUID NOT NULL,
    account_id VARCHAR(100) NOT NULL,
    side VARCHAR(10) NOT NULL,
    cash_amount NUMERIC(19,4) NOT NULL,
    credit_amount NUMERIC(19,4) NOT NULL,
    gross_amount NUMERIC(19,4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    settled_at TIMESTAMPTZ
);
CREATE INDEX idx_account_settlement_holds_account_status ON account_settlement_holds(account_id,status);
