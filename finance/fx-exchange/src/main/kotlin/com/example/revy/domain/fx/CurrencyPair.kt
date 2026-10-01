package com.example.revy.domain.fx

import com.example.revy.domain.common.Currency
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "currency_pair",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_currency_pair",
            columnNames = ["base_currency", "quote_currency"]
        )
    ]
)
class CurrencyPair(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_currency", nullable = false)
    val baseCurrency: Currency,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_currency", nullable = false)
    val quoteCurrency: Currency,

    @Column(nullable = false)
    val enabled: Boolean = true
)