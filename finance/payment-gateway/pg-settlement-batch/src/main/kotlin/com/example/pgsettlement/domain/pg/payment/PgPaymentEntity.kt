package com.example.pgsettlement.domain.pg.payment

import com.example.pgsettlement.domain.order.OrderEntity
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

@Entity
@Table(
    name = "pg_payment",
    indexes = [
        Index(name = "idx_pg_payment_order_no", columnList = "order_no"),
    ],
)
class PgPaymentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "pg_tid", nullable = false, unique = true, length = 100)
    var pgTid: String,

    @Column(name = "order_no", nullable = false, length = 64)
    var orderNo: String,

    @Column(name = "approved_amount", nullable = false, precision = 19, scale = 2)
    var approvedAmount: BigDecimal,

    @Column(name = "canceled_amount", nullable = false, precision = 19, scale = 2)
    var canceledAmount: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    var status: PgPaymentStatus,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "order_no",
        referencedColumnName = "order_no",
        insertable = false,
        updatable = false,
    )
    var order: OrderEntity? = null,
)
