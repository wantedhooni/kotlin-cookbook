ALTER TABLE trading_accounts ADD CONSTRAINT chk_cash_holds CHECK (reserved_cash + settlement_cash_hold <= cash_balance);
ALTER TABLE trading_accounts ADD CONSTRAINT chk_credit_holds CHECK (used_credit + reserved_credit + settlement_credit_hold <= credit_limit);
