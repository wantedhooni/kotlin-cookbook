package com.example.revy.domain.account


import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "security_position",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_security_position",
            columnNames = [
                "account_id",
                "instrument_id"
            ]
        )
    ]
)
class SecurityPosition(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "account_id",
        nullable = false
    )
    val account: Account,

    @Column(
        name = "instrument_id",
        nullable = false
    )
    val instrumentId: Long,

    @Column(
        nullable = false,
        precision = 24,
        scale = 8
    )
    var quantity: BigDecimal,

    @Column(
        name = "available_quantity",
        nullable = false,
        precision = 24,
        scale = 8
    )
    var availableQuantity: BigDecimal,

    @Column(
        name = "average_price",
        nullable = false,
        precision = 24,
        scale = 8
    )
    var averagePrice: BigDecimal,

    @Version
    var version: Long = 0
)