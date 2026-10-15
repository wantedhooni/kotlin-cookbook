package com.example.dbsdeposit.repository

import com.example.dbsdeposit.domain.SyncLeaseEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface SyncLeaseRepository : JpaRepository<SyncLeaseEntity, String> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update SyncLeaseEntity s
           set s.lockedUntil = :lockedUntil,
               s.lockedBy = :owner,
               s.updatedAt = :now
         where s.partyId = :partyId
           and (s.lockedUntil is null or s.lockedUntil < :now)
        """,
    )
    fun tryAcquire(
        @Param("partyId") partyId: String,
        @Param("owner") owner: String,
        @Param("now") now: Instant,
        @Param("lockedUntil") lockedUntil: Instant,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update SyncLeaseEntity s
           set s.lockedUntil = null,
               s.lockedBy = null,
               s.updatedAt = :now
         where s.partyId = :partyId
           and s.lockedBy = :owner
        """,
    )
    fun release(
        @Param("partyId") partyId: String,
        @Param("owner") owner: String,
        @Param("now") now: Instant,
    ): Int
}
