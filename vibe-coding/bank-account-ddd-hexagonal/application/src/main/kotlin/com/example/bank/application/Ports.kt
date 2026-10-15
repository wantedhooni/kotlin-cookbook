package com.example.bank.application

import com.example.bank.domain.*
import java.math.BigDecimal
import java.util.UUID

data class TransferCommand(val requestId: UUID, val fromId: Long, val toId: Long, val amount: BigDecimal)
data class TransferResult(val referenceId: UUID, val replayed: Boolean)
data class TransactionRecord(val id: Long, val accountId: Long, val referenceId: UUID,
                             val side: EntrySide, val amount: BigDecimal)
interface AccountPort {
    fun open(bankCode: String, number: String, owner: String): Account
    fun find(id: Long): Account?
    fun lock(id: Long): Account?
    fun save(account: Account): Account
    fun search(bankCode: String?, owner: String?, status: AccountStatus?): List<Account>
}
interface TransferPort {
    /** Must insert and flush a DB-unique request key. Conflicting concurrent requests serialize at the DB. */
    fun claim(command: TransferCommand): Boolean
    fun find(requestId: UUID): StoredTransfer?
    fun complete(requestId: UUID, referenceId: UUID)
}
data class StoredTransfer(val fromId: Long, val toId: Long, val amount: BigDecimal, val referenceId: UUID?)
interface LedgerPort {
    fun append(journal: Journal)
    fun history(accountId: Long): List<TransactionRecord>
}
interface TransferPolicyPort { fun checkAndReserve(fromAccountId: Long, amount: BigDecimal) }
object NoLimitPolicy : TransferPolicyPort { override fun checkAndReserve(fromAccountId: Long, amount: BigDecimal) = Unit }
interface AuditPort { fun record(subject: String, action: String, resource: String, detail: String) }
interface EventPort { fun append(aggregateId: Long, kind: String, referenceId: UUID, amount: BigDecimal) }
