package com.example.dbsdeposit.repository

import com.example.dbsdeposit.domain.IdempotencyRecordEntity
import org.springframework.data.jpa.repository.JpaRepository

interface IdempotencyRecordRepository : JpaRepository<IdempotencyRecordEntity, Long> {
    fun findByOperationAndIdempotencyKey(operation: String, idempotencyKey: String): IdempotencyRecordEntity?
}
