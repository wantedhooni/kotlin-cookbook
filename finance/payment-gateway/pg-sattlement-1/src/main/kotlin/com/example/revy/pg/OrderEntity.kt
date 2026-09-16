package com.example.revy.pg

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "orders")
class OrderEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "order_no", nullable = false, unique = true)
    var orderNo: String,
    @Column(name = "payment_amount", nullable = false)
    var paymentAmount: BigDecimal,
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    var paymentStatus: PaymentStatus,
)

enum class PaymentStatus {
    PAID,
    CANCELLED,
    REFUNDED,
}

@Entity
@Table(name = "pg_payment")
class PgPaymentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "pg_tid", nullable = false, unique = true)
    var pgTid: String,
    @Column(name = "order_no", nullable = false)
    var orderNo: String,
    @Column(name = "payment_amount", nullable = false)
    var paymentAmount: BigDecimal,
    @Column(name = "pg_fee", nullable = false)
    var pgFee: BigDecimal,
    @Column(name = "approved_at", nullable = false)
    var approvedAt: LocalDateTime,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "order_no",
        referencedColumnName = "order_no",
        insertable = false,
        updatable = false,
    )
    var order: OrderEntity? = null,
)

@Entity
@Table(name = "pg_settlement")
class PgSettlementEntity(
    @Id
    @Column(name = "pg_tid")
    var pgTid: String,
    @Column(name = "settlement_date", nullable = false)
    var settlementDate: LocalDate,
    @Column(name = "order_no", nullable = false)
    var orderNo: String,
    @Column(name = "payment_amount", nullable = false)
    var paymentAmount: BigDecimal,
    @Column(name = "pg_fee", nullable = false)
    var pgFee: BigDecimal,
    @Column(name = "settlement_amount", nullable = false)
    var settlementAmount: BigDecimal,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: SettlementStatus,
)

enum class SettlementStatus {
    MATCHED,
    ORDER_NOT_FOUND,
    AMOUNT_MISMATCH,
}
