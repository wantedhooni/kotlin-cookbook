package com.example.dbsdeposit.service

import com.example.dbsdeposit.config.IdempotencyProperties
import com.example.dbsdeposit.domain.IdempotencyRecordEntity
import com.example.dbsdeposit.domain.IdempotencyStatus
import com.example.dbsdeposit.repository.IdempotencyRecordRepository
import com.example.dbsdeposit.support.IdempotencyInProgressException
import com.example.dbsdeposit.support.IdempotencyKeyConflictException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.time.Instant

sealed interface IdempotencyReservation {
    data class Owned(val recordId: Long) : IdempotencyReservation
    data class Replay(val httpStatus: Int, val responseBody: String) : IdempotencyReservation
}

@Service
class IdempotencyService(
    private val repository: IdempotencyRecordRepository,
    private val writer: IdempotencyTxWriter,
    private val props: IdempotencyProperties,
) {
    fun reserve(operation: String, idempotencyKey: String, requestHash: String): IdempotencyReservation {
        validateKey(idempotencyKey)
        repository.findByOperationAndIdempotencyKey(operation, idempotencyKey)?.let { existing ->
            val now = Instant.now()
            val expired = existing.expiresAt.isBefore(now)
            val processingTimedOut = existing.status == IdempotencyStatus.PROCESSING &&
                existing.updatedAt.plus(props.processingTimeout).isBefore(now)

            if (expired || processingTimedOut) {
                runCatching { writer.delete(requireNotNull(existing.id)) }
            } else {
                return fromExisting(existing, requestHash)
            }
        }

        val now = Instant.now()
        val newRecord = IdempotencyRecordEntity(
            operation = operation,
            idempotencyKey = idempotencyKey,
            requestHash = requestHash,
            expiresAt = now.plus(props.retention),
        )

        try {
            val saved = writer.insert(newRecord)
            return IdempotencyReservation.Owned(requireNotNull(saved.id))
        } catch (_: DataIntegrityViolationException) {
            val raced = repository.findByOperationAndIdempotencyKey(operation, idempotencyKey)
                ?: throw IdempotencyInProgressException("Idempotency reservation is being committed; retry shortly")
            return fromExisting(raced, requestHash)
        }
    }

    fun completeSuccess(recordId: Long, httpStatus: Int, responseBody: String) {
        writer.complete(recordId, IdempotencyStatus.SUCCEEDED, httpStatus, responseBody)
    }

    fun abandon(recordId: Long) {
        runCatching { writer.delete(recordId) }
    }

    private fun fromExisting(record: IdempotencyRecordEntity, requestHash: String): IdempotencyReservation {
        if (record.requestHash != requestHash) {
            throw IdempotencyKeyConflictException("The same Idempotency-Key was used for a different request")
        }
        return when (record.status) {
            IdempotencyStatus.PROCESSING -> throw IdempotencyInProgressException("The same request is still processing")
            IdempotencyStatus.SUCCEEDED -> IdempotencyReservation.Replay(
                httpStatus = record.httpStatus ?: 200,
                responseBody = record.responseBody ?: "{}",
            )
            IdempotencyStatus.FAILED -> throw IdempotencyInProgressException("Previous failed record is awaiting cleanup; retry shortly")
        }
    }

    private fun validateKey(key: String) {
        require(key.isNotBlank()) { "Idempotency-Key must not be blank" }
        require(key.length <= 128) { "Idempotency-Key must be <= 128 characters" }
    }
}
