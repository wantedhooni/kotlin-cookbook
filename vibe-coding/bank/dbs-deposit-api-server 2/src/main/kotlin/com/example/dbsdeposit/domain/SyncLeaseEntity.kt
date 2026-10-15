package com.example.dbsdeposit.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "sync_lease")
class SyncLeaseEntity(
    @Id
    @Column(name = "party_id", nullable = false, length = 128)
    var partyId: String,

    @Column(name = "locked_until")
    var lockedUntil: Instant? = null,

    @Column(name = "locked_by", length = 64)
    var lockedBy: String? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
