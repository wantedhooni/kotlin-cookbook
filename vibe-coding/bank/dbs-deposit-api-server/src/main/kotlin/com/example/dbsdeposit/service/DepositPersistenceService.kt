package com.example.dbsdeposit.service

import com.example.dbsdeposit.api.model.NormalizedDepositAccount
import com.example.dbsdeposit.api.model.PersistenceResult
import com.example.dbsdeposit.domain.DepositAccountEntity
import com.example.dbsdeposit.repository.DepositAccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class DepositPersistenceService(
    private val repository: DepositAccountRepository,
) {
    @Transactional
    fun synchronize(partyId: String, accounts: List<NormalizedDepositAccount>): PersistenceResult {
        var inserted = 0
        var updated = 0
        val now = Instant.now()

        accounts.forEach { incoming ->
            val existing = repository.findByPartyIdAndExternalAccountId(partyId, incoming.accountId)
            if (existing == null) {
                repository.save(
                    DepositAccountEntity(
                        partyId = partyId,
                        externalAccountId = incoming.accountId,
                        accountNumberMasked = incoming.accountNumberMasked,
                        productName = incoming.productName,
                        productType = incoming.productType,
                        currency = incoming.currency,
                        availableBalance = incoming.availableBalance,
                        rawJson = incoming.rawJson,
                        providerUpdatedAt = incoming.providerUpdatedAt,
                        syncedAt = now,
                    ),
                )
                inserted++
            } else {
                existing.accountNumberMasked = incoming.accountNumberMasked
                existing.productName = incoming.productName
                existing.productType = incoming.productType
                existing.currency = incoming.currency
                existing.availableBalance = incoming.availableBalance
                existing.rawJson = incoming.rawJson
                existing.providerUpdatedAt = incoming.providerUpdatedAt
                existing.syncedAt = now
                updated++
            }
        }
        repository.flush()
        return PersistenceResult(insertedCount = inserted, updatedCount = updated)
    }
}
