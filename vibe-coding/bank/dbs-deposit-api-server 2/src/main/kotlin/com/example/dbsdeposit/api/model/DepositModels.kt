package com.example.dbsdeposit.api.model

import java.math.BigDecimal
import java.time.Instant

data class DepositAccountResponse(
    val partyId: String,
    val accountId: String,
    val accountNumberMasked: String?,
    val productName: String?,
    val productType: String?,
    val currency: String?,
    val availableBalance: BigDecimal?,
    val providerUpdatedAt: Instant?,
    val syncedAt: Instant,
)

data class SyncDepositsResponse(
    val partyId: String,
    val receivedCount: Int,
    val insertedCount: Int,
    val updatedCount: Int,
    val syncedAt: Instant,
)

data class NormalizedDepositAccount(
    val accountId: String,
    val accountNumberMasked: String?,
    val productName: String?,
    val productType: String?,
    val currency: String?,
    val availableBalance: BigDecimal?,
    val providerUpdatedAt: Instant?,
    val rawJson: String,
)

data class PersistenceResult(
    val insertedCount: Int,
    val updatedCount: Int,
)

data class IdempotentResult<T>(
    val body: T,
    val replayed: Boolean,
)
