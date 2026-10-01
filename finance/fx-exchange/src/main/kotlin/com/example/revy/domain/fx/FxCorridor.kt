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
    name = "fx_corridor",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_fx_corridor",
            columnNames = [
                "source_currency",
                "target_currency"
            ]
        )
    ]
)
class FxCorridor(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_currency", nullable = false)
    val sourceCurrency: Currency,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_currency", nullable = false)
    val targetCurrency: Currency,

    @Column(nullable = false, length = 50)
    val code: String,

    @Column(nullable = false)
    var enabled: Boolean = true
)