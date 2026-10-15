package com.example.bank.domain

import java.math.BigDecimal
import java.util.UUID

/** Bank BC owns bank identifiers; other contexts refer to bankCode, not the JPA entity. */
data class BankIdentity(val code: String, val name: String)
/** Account BC owns balances, lifecycle, and customer identity. */
data class AccountOwner(val subject: String) {
    init { require(subject.isNotBlank()) }
}
/** Transfer BC owns idempotency keys and the daily transfer-policy decision. */
data class TransferIntent(val requestId: UUID, val sourceId: Long, val destinationId: Long, val amount: BigDecimal)
/** Contracts describe integration events; no BC may import another BC's persistence entities. */
data class IntegrationEvent(val id: UUID, val aggregateType: String, val aggregateId: Long,
                            val type: String, val referenceId: UUID, val amount: BigDecimal)
