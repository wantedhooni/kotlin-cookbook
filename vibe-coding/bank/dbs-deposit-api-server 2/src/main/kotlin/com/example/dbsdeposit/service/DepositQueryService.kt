package com.example.dbsdeposit.service

import com.example.dbsdeposit.api.model.DepositAccountResponse
import com.example.dbsdeposit.domain.DepositAccountEntity
import com.example.dbsdeposit.repository.DepositAccountRepository
import com.example.dbsdeposit.support.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DepositQueryService(
    private val repository: DepositAccountRepository,
) {
    @Transactional(readOnly = true)
    fun list(partyId: String): List<DepositAccountResponse> =
        repository.findAllByPartyIdOrderByIdAsc(partyId).map(::toResponse)

    @Transactional(readOnly = true)
    fun get(partyId: String, accountId: String): DepositAccountResponse =
        repository.findByPartyIdAndExternalAccountId(partyId, accountId)
            ?.let(::toResponse)
            ?: throw ResourceNotFoundException("Deposit account not found: partyId=$partyId, accountId=$accountId")

    private fun toResponse(entity: DepositAccountEntity) = DepositAccountResponse(
        partyId = entity.partyId,
        accountId = entity.externalAccountId,
        accountNumberMasked = entity.accountNumberMasked,
        productName = entity.productName,
        productType = entity.productType,
        currency = entity.currency,
        availableBalance = entity.availableBalance,
        providerUpdatedAt = entity.providerUpdatedAt,
        syncedAt = entity.syncedAt,
    )
}
