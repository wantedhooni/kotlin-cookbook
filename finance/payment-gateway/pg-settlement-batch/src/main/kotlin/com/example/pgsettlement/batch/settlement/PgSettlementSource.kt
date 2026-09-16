package com.example.pgsettlement.batch.settlement

import java.math.BigDecimal
import java.time.LocalDate

data class PgSettlementSource(
    val settlementDate: LocalDate,
    val pgTid: String,
    val orderNo: String,
    val orderId: Long?,
    val orderAmount: BigDecimal?,
    val pgApprovedAmount: BigDecimal,
    val pgCanceledAmount: BigDecimal,
    val approvalAmount: BigDecimal,
    val cancelAmount: BigDecimal,
    val feeAmount: BigDecimal,
)
