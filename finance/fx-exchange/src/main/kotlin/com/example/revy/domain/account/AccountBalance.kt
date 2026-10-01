package com.example.revy.domain.account

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(
    name = "account_balance",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_account_balance",
            columnNames = [
                "account_id",
                "currency"
            ]
        )
    ]
)
class AccountBalance(

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
        nullable = false,
        length = 3
    )
    val currency: String,

    @Column(
        name = "ledger_balance",
        nullable = false,
        precision = 24,
        scale = 8
    )
    var ledgerBalance: BigDecimal = BigDecimal.ZERO,

    @Column(
        name = "available_balance",
        nullable = false,
        precision = 24,
        scale = 8
    )
    var availableBalance: BigDecimal = BigDecimal.ZERO,

    @Column(
        name = "hold_amount",
        nullable = false,
        precision = 24,
        scale = 8
    )
    var holdAmount: BigDecimal = BigDecimal.ZERO,

    @Version
    var version: Long = 0,

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
)