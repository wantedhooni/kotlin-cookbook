package com.example.dbsdeposit.service

import com.example.dbsdeposit.domain.SyncLeaseEntity
import com.example.dbsdeposit.repository.SyncLeaseRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class SyncLeaseTxWriter(
    private val repository: SyncLeaseRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun insert(partyId: String) {
        repository.saveAndFlush(SyncLeaseEntity(partyId = partyId))
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun tryAcquire(partyId: String, owner: String, now: Instant, lockedUntil: Instant): Int =
        repository.tryAcquire(partyId, owner, now, lockedUntil)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun release(partyId: String, owner: String, now: Instant): Int =
        repository.release(partyId, owner, now)
}
