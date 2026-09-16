package com.example.pgsettlement.domain.pg.transaction

import com.example.pgsettlement.domain.pg.payment.PgPaymentEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "pg_transaction",
    indexes = [
        Index(name = "idx_pg_tx_settlement_date", columnList = "settlement_date"),
        Index(name = "idx_pg_tx_payment", columnList = "pg_payment_id"),
    ],
)
class PgTransactionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "pg_event_id", nullable = false, unique = true, length = 120)
    var pgEventId: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pg_payment_id", nullable = false)
    var payment: PgPaymentEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    var type: PgTransactionType,

    /** 승인/취소 원금. 항상 양수로 저장한다. */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    var amount: BigDecimal,

    /**
     * 수수료 증감액.
     * 승인 시 PG 수수료는 양수, 취소 시 반환받는 수수료는 음수로 저장한다.
     */
    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 2)
    var feeAmount: BigDecimal,

    @Column(name = "occurred_at", nullable = false)
    var occurredAt: LocalDateTime,

    @Column(name = "settlement_date", nullable = false)
    var settlementDate: LocalDate,
)
