package com.example.pgsettlement.domain.pg.transaction

enum class PgTransactionType {
    APPROVAL,
    PARTIAL_CANCEL,
    CANCEL,
}
