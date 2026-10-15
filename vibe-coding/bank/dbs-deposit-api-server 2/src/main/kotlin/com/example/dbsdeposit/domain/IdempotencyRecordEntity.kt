package com.example.dbsdeposit.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

enum class IdempotencyStatus {
    PROCESSING,
    SUCCEEDED,
    FAILED,
}

@Entity
@Table(
    name = "idempotency_record",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_idempotency_operation_key",
            columnNames = ["operation", "idempotency_key"],
        ),
    ],
)
class IdempotencyRecordEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "operation", nullable = false, length = 80)
    var operation: String,

    @Column(name = "idempotency_key", nullable = false, length = 128)
    var idempotencyKey: String,

    @Column(name = "request_hash", nullable = false, length = 64)
    var requestHash: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: IdempotencyStatus = IdempotencyStatus.PROCESSING,

    @Column(name = "http_status")
    var httpStatus: Int? = null,

    @Lob
    @Column(name = "response_body")
    var responseBody: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,
)
