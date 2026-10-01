package com.example.revy.domain.fx

import jakarta.persistence.Column
import jakarta.persistence.Entity
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
    name = "fx_spread_tier",
    indexes = [
        Index(
            name = "idx_fx_spread_tier_policy_amount",
            columnList = "policy_id, min_amount, max_amount"
        )
    ]
)
class FxSpreadTier(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    val policy: FxSpreadPolicy,

    /**
     * inclusive
     */
    @Column(
        name = "min_amount",
        nullable = false,
        precision = 24,
        scale = 8
    )
    val minAmount: BigDecimal,

    /**
     * exclusive
     *
     * null = 상한 없음
     */
    @Column(
        name = "max_amount",
        precision = 24,
        scale = 8
    )
    val maxAmount: BigDecimal?,

    @Column(
        name = "spread_value",
        nullable = false,
        precision = 12,
        scale = 6
    )
    val spreadValue: BigDecimal,

    @Column(nullable = false)
    val priority: Int = 0,

    @Column(nullable = false)
    var enabled: Boolean = true
)