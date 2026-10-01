package com.example.revy.domain.account


import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "brokerage_account",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_brokerage_account",
            columnNames = ["account_id"]
        )
    ]
)
class BrokerageAccount(

    @Id
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "account_id",
        nullable = false
    )
    val account: Account,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val brokerageType: BrokerageAccountType,

    @Column(nullable = false)
    val marginEnabled: Boolean = false,

    @Column(nullable = false)
    val overseasTradingEnabled: Boolean = false
)

enum class BrokerageAccountType {
    GENERAL,
    CMA,
    ISA,
    PENSION
}