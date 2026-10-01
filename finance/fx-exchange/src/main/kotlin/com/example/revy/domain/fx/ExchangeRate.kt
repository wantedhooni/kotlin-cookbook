package com.example.revy.domain.fx

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant


@Entity
@Table(
    name = "exchange_rate",
    indexes = [
        Index(
            name = "idx_exchange_rate_pair_time",
            columnList = "currency_pair_id, quoted_at"
        )
    ]
)
class ExchangeRate(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_pair_id", nullable = false)
    val currencyPair: CurrencyPair,

    @Column(nullable = false, precision = 19, scale = 8)
    val midRate: BigDecimal,

    @Column(precision = 19, scale = 8)
    val bidRate: BigDecimal? = null,

    @Column(precision = 19, scale = 8)
    val askRate: BigDecimal? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val source: RateSource,

    @Column(nullable = false)
    val quotedAt: Instant,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)

enum class RateSource {
    BLOOMBERG,
    REUTERS,
    BANK_OF_KOREA,
    INTERNAL
}