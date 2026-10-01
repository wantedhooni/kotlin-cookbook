package com.example.revy.domain.fx

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
import java.time.Instant

@Entity
@Table(
    name = "fx_spread_policy",
    indexes = [
        Index(
            name = "idx_fx_spread_policy_corridor",
            columnList = "corridor_id, enabled"
        )
    ]
)
class FxSpreadPolicy(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corridor_id", nullable = false)
    val corridor: FxCorridor,

    @Column(nullable = false, length = 100)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val spreadType: SpreadType,

    @Column(nullable = false)
    val effectiveFrom: Instant,

    val effectiveTo: Instant? = null,

    @Column(nullable = false)
    var enabled: Boolean = true
)

enum class SpreadType {
    BPS,
    PERCENTAGE,
    ABSOLUTE
}