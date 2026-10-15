package com.example.dbsdeposit.service

import com.example.dbsdeposit.config.SyncProperties
import com.example.dbsdeposit.repository.SyncLeaseRepository
import com.example.dbsdeposit.support.ConcurrentSyncException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SyncLeaseService(
    private val repository: SyncLeaseRepository,
    private val writer: SyncLeaseTxWriter,
    private val props: SyncProperties,
) {
    fun acquire(partyId: String, owner: String) {
        if (!repository.existsById(partyId)) {
            try {
                writer.insert(partyId)
            } catch (_: DataIntegrityViolationException) {
                // Another node inserted the same lease row first. Continue to CAS acquisition.
            }
        }

        val now = Instant.now()
        val acquired = writer.tryAcquire(partyId, owner, now, now.plus(props.leaseDuration))
        if (acquired != 1) {
            throw ConcurrentSyncException("A deposit sync for partyId=$partyId is already running")
        }
    }

    fun release(partyId: String, owner: String) {
        writer.release(partyId, owner, Instant.now())
    }
}
