package com.example.bank.application

import com.example.bank.domain.*
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

open class BankService(private val accounts: AccountPort, private val transfers: TransferPort,
                       private val ledger: LedgerPort, private val events: EventPort,
                       private val policy: TransferPolicyPort = NoLimitPolicy) {
    @Transactional
    open fun open(bankCode: String, number: String, owner: String): Account = accounts.open(bankCode, number, owner).also {
        events.append(it.id, "ACCOUNT_OPENED", UUID.randomUUID(), BigDecimal.ZERO.setScale(2))
    }
    @Transactional(readOnly = true)
    open fun get(id: Long): Account = accounts.find(id) ?: throw BankException("ACCOUNT_NOT_FOUND", "Account not found")
    @Transactional(readOnly = true)
    open fun search(bankCode: String?, owner: String?, status: AccountStatus?): List<Account> = accounts.search(bankCode, owner, status)
    @Transactional(readOnly = true)
    open fun history(id: Long): List<TransactionRecord> { get(id); return ledger.history(id) }

    @Transactional
    open fun deposit(id: Long, raw: BigDecimal): Account = adjust(id, raw, true)
    @Transactional
    open fun withdraw(id: Long, raw: BigDecimal): Account = adjust(id, raw, false)
    private fun adjust(id: Long, raw: BigDecimal, deposit: Boolean): Account {
        val amount = Money.valid(raw)
        val original = accounts.lock(id) ?: throw BankException("ACCOUNT_NOT_FOUND", "Account not found")
        val updated = accounts.save(if (deposit) original.credit(amount) else original.debit(amount))
        val reference = UUID.randomUUID()
        ledger.append(Journal(reference, listOf(
            Posting(updated.id, Book.CUSTOMER, if (deposit) EntrySide.CREDIT else EntrySide.DEBIT, amount),
            Posting(null, Book.CLEARING, if (deposit) EntrySide.DEBIT else EntrySide.CREDIT, amount))))
        events.append(id, if (deposit) "DEPOSIT" else "WITHDRAWAL", reference, amount)
        return updated
    }
    @Transactional
    open fun transfer(command: TransferCommand): TransferResult {
        val amount = Money.valid(command.amount)
        if (command.fromId == command.toId) throw BankException("SAME_ACCOUNT", "Cannot transfer to same account")
        // An insert with unique requestId flushes before account locking; retries return the same result.
        if (!transfers.claim(command.copy(amount = amount))) {
            val prior = transfers.find(command.requestId) ?: throw BankException("IDEMPOTENCY_CONFLICT", "Transfer not available")
            if (prior.fromId != command.fromId || prior.toId != command.toId || prior.amount.compareTo(amount) != 0)
                throw BankException("IDEMPOTENCY_CONFLICT", "Request key used with different payload")
            return TransferResult(prior.referenceId ?: throw BankException("TRANSFER_PENDING", "Transfer pending"), true)
        }
        policy.checkAndReserve(command.fromId, amount)
        val locked = listOf(command.fromId, command.toId).sorted().associateWith { id ->
            accounts.lock(id) ?: throw BankException("ACCOUNT_NOT_FOUND", "Account $id not found")
        }
        accounts.save(requireNotNull(locked[command.fromId]).debit(amount))
        accounts.save(requireNotNull(locked[command.toId]).credit(amount))
        val reference = UUID.randomUUID()
        ledger.append(Journal(reference, listOf(
            Posting(command.fromId, Book.CUSTOMER, EntrySide.DEBIT, amount),
            Posting(command.toId, Book.CUSTOMER, EntrySide.CREDIT, amount))))
        transfers.complete(command.requestId, reference)
        events.append(command.fromId, "TRANSFER_OUT", reference, amount)
        events.append(command.toId, "TRANSFER_IN", reference, amount)
        return TransferResult(reference, false)
    }
}
