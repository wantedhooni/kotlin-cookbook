package com.example.pgsettlement.domain.pg.settlement

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(
    name = "pg_settlement",
    indexes = [
        Index(name = "idx_pg_settlement_date", columnList = "settlement_date"),
    ],
)
class PgSettlementEntity(
    /** settlementDate + pgTid. 동일 정산일 재실행 시 같은 Row를 갱신하기 위한 멱등 키. */
    @Id
    @Column(name = "settlement_key", nullable = false, length = 140)
    var settlementKey: String,

    @Column(name = "settlement_date", nullable = false)
    var settlementDate: LocalDate,

    @Column(name = "pg_tid", nullable = false, length = 100)
    var pgTid: String,

    @Column(name = "order_no", nullable = false, length = 64)
    var orderNo: String,

    @Column(name = "approval_amount", nullable = false, precision = 19, scale = 2)
    var approvalAmount: BigDecimal,

    @Column(name = "cancel_amount", nullable = false, precision = 19, scale = 2)
    var cancelAmount: BigDecimal,

    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 2)
    var feeAmount: BigDecimal,

    @Column(name = "settlement_amount", nullable = false, precision = 19, scale = 2)
    var settlementAmount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    var status: PgSettlementStatus,
)
