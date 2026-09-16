CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    order_no        VARCHAR(64) NOT NULL UNIQUE,
    order_amount    NUMERIC(19, 2) NOT NULL
);

CREATE TABLE pg_payment (
    id                  BIGSERIAL PRIMARY KEY,
    pg_tid              VARCHAR(100) NOT NULL UNIQUE,
    order_no            VARCHAR(64) NOT NULL,
    approved_amount     NUMERIC(19, 2) NOT NULL,
    canceled_amount     NUMERIC(19, 2) NOT NULL DEFAULT 0,
    status              VARCHAR(30) NOT NULL
);
CREATE INDEX idx_pg_payment_order_no ON pg_payment(order_no);

CREATE TABLE pg_transaction (
    id                  BIGSERIAL PRIMARY KEY,
    pg_event_id         VARCHAR(120) NOT NULL UNIQUE,
    pg_payment_id       BIGINT NOT NULL REFERENCES pg_payment(id),
    transaction_type    VARCHAR(30) NOT NULL,
    amount              NUMERIC(19, 2) NOT NULL,
    fee_amount          NUMERIC(19, 2) NOT NULL,
    occurred_at         TIMESTAMP NOT NULL,
    settlement_date     DATE NOT NULL
);
CREATE INDEX idx_pg_tx_settlement_date ON pg_transaction(settlement_date);
CREATE INDEX idx_pg_tx_payment ON pg_transaction(pg_payment_id);

CREATE TABLE pg_settlement (
    settlement_key      VARCHAR(140) PRIMARY KEY,
    settlement_date     DATE NOT NULL,
    pg_tid              VARCHAR(100) NOT NULL,
    order_no            VARCHAR(64) NOT NULL,
    approval_amount     NUMERIC(19, 2) NOT NULL,
    cancel_amount       NUMERIC(19, 2) NOT NULL,
    fee_amount          NUMERIC(19, 2) NOT NULL,
    settlement_amount   NUMERIC(19, 2) NOT NULL,
    status              VARCHAR(40) NOT NULL
);
CREATE INDEX idx_pg_settlement_date ON pg_settlement(settlement_date);
