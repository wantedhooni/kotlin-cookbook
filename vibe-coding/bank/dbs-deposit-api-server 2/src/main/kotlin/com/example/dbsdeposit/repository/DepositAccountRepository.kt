package com.example.dbsdeposit.repository

import com.example.dbsdeposit.domain.DepositAccountEntity
import org.springframework.data.jpa.repository.JpaRepository

interface DepositAccountRepository : JpaRepository<DepositAccountEntity, Long> {
    fun findAllByPartyIdOrderByIdAsc(partyId: String): List<DepositAccountEntity>
    fun findByPartyIdAndExternalAccountId(partyId: String, externalAccountId: String): DepositAccountEntity?
}
