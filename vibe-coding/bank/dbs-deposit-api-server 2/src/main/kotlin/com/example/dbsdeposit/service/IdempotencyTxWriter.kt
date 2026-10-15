package com.example.dbsdeposit.service

import com.example.dbsdeposit.domain.IdempotencyRecordEntity
import com.example.dbsdeposit.domain.IdempotencyStatus
import com.example.dbsdeposit.repository.IdempotencyRecordRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class IdempotencyTxWriter(
    private val repository: IdempotencyRecordRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun insert(record: IdempotencyRecordEntity): IdempotencyRecordEntity = repository.saveAndFlush(record)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun delete(id: Long) {
        repository.deleteById(id)
        repository.flush()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun complete(
        id: Long,
        status: IdempotencyStatus,
        httpStatus: Int,
        responseBody: String,
    ) {
        val entity = repository.findById(id).orElseThrow()
        entity.status = status
        entity.httpStatus = httpStatus
        entity.responseBody = responseBody
        entity.updatedAt = Instant.now()
    }
}
