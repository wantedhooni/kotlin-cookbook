package com.example.fintech.application

import com.example.fintech.domain.*
import java.util.Currency
import java.util.UUID

interface AccountPort {
    /** Call within a transaction; pessimistic locks are acquired in UUID order by the adapter. */
    fun lockAccounts(ids: List<UUID>): Map<UUID, Account>
    fun save(account: Account)
    fun find(id: UUID): Account?
    fun create(account: Account): Account
}
interface FxRatePort { fun getQuote(base: Currency, counter: Currency): FxQuote }
interface TransferPort { fun save(receipt: TransferReceipt) }
interface ExchangePort { fun save(receipt: ExchangeReceipt) }
interface IdempotencyPort {
    fun find(operation: String, key: String): StoredRequest?
    fun save(operation: String, key: String, fingerprint: String, resourceId: UUID)
}
data class StoredRequest(val fingerprint: String, val resourceId: UUID)

interface OutboxPort { fun append(eventId: UUID, eventType: String, aggregateId: UUID, payload: String) }
interface BankTransferPort { fun requestTransfer(request: BankTransferRequest): BankTransferResult }
data class BankTransferRequest(val requestId: UUID, val bankCode: String, val destinationAccount: String, val amount: java.math.BigDecimal)
data class BankTransferResult(val providerReference: String, val accepted: Boolean)


/** A bank status query must use the original, stable transfer request ID. */
interface BankReconciliationPort { fun lookup(requestId: UUID, bankCode: String): BankReconciliationResult }
sealed interface BankReconciliationResult {
    data class Confirmed(val providerReference: String): BankReconciliationResult
    data class Rejected(val providerReference: String): BankReconciliationResult
    data object Pending: BankReconciliationResult
}
