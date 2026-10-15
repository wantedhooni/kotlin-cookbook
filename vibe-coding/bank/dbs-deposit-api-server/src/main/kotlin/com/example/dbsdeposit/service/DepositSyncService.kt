package com.example.dbsdeposit.service

import com.example.dbsdeposit.api.model.IdempotentResult
import com.example.dbsdeposit.api.model.SyncDepositsResponse
import com.example.dbsdeposit.client.DbsDepositClient
import com.example.dbsdeposit.client.DbsJsonNormalizer
import com.example.dbsdeposit.support.ConcurrentSyncException
import com.example.dbsdeposit.support.ProviderCallException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class DepositSyncService(
    private val dbsClient: DbsDepositClient,
    private val normalizer: DbsJsonNormalizer,
    private val persistenceService: DepositPersistenceService,
    private val idempotencyService: IdempotencyService,
    private val leaseService: SyncLeaseService,
    private val hashService: RequestHashService,
    private val objectMapper: ObjectMapper,
) {
    fun sync(partyId: String, idempotencyKey: String): IdempotentResult<SyncDepositsResponse> {
        val operation = "SYNC_DEPOSITS"
        val requestHash = hashService.sha256("partyId=$partyId")

        return when (val reservation = idempotencyService.reserve(operation, idempotencyKey, requestHash)) {
            is IdempotencyReservation.Replay -> IdempotentResult(
                body = objectMapper.readValue(reservation.responseBody, SyncDepositsResponse::class.java),
                replayed = true,
            )

            is IdempotencyReservation.Owned -> executeOwned(partyId, reservation.recordId)
        }
    }

    private fun executeOwned(partyId: String, recordId: Long): IdempotentResult<SyncDepositsResponse> {
        val owner = UUID.randomUUID().toString()
        var leaseAcquired = false
        try {
            leaseService.acquire(partyId, owner)
            leaseAcquired = true

            val raw = dbsClient.listDepositAccounts(partyId)
            val normalized = normalizer.normalizeList(raw)
            val persisted = persistenceService.synchronize(partyId, normalized)
            val response = SyncDepositsResponse(
                partyId = partyId,
                receivedCount = normalized.size,
                insertedCount = persisted.insertedCount,
                updatedCount = persisted.updatedCount,
                syncedAt = Instant.now(),
            )
            idempotencyService.completeSuccess(recordId, 200, objectMapper.writeValueAsString(response))
            return IdempotentResult(response, replayed = false)
        } catch (e: ConcurrentSyncException) {
            idempotencyService.abandon(recordId)
            throw e
        } catch (e: ProviderCallException) {
            idempotencyService.abandon(recordId)
            throw e
        } catch (e: Exception) {
            idempotencyService.abandon(recordId)
            throw e
        } finally {
            if (leaseAcquired) {
                runCatching { leaseService.release(partyId, owner) }
            }
        }
    }

}
