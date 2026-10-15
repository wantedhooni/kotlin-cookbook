package com.example.trs.repository

import com.example.trs.domain.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import jakarta.persistence.LockModeType

interface MerchantRepository : JpaRepository<Merchant, Long>
interface TouristRepository : JpaRepository<Tourist, Long>
interface PurchaseRepository : JpaRepository<Purchase, Long> {
    fun findByMerchantIdAndExternalTransactionId(merchantId: Long, externalTransactionId: String): Purchase?
}
interface RefundDocumentRepository : JpaRepository<RefundDocument, Long>
interface RefundRepository : JpaRepository<Refund, Long>, RefundQueryRepository {
    fun existsByDocumentId(documentId: Long): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from Refund r where r.id = :id")
    fun findByIdForUpdate(@org.springframework.data.repository.query.Param("id") id: Long): Refund?
}
interface PayoutRepository : JpaRepository<Payout, Long> {
    fun existsByIdempotencyKey(idempotencyKey: String): Boolean
}
