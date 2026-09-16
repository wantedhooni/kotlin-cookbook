package com.example.pgsettlement.domain.pg.settlement

enum class PgSettlementStatus {
    MATCHED,
    ORDER_NOT_FOUND,
    PAYMENT_AMOUNT_MISMATCH,
    INVALID_CANCEL_AMOUNT,
}
