package com.example.fintech.application

import com.example.fintech.domain.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Currency
import java.util.UUID

@Service
class FinanceService(
    private val accounts: AccountPort,
    private val rates: FxRatePort,
    private val transfers: TransferPort,
    private val exchanges: ExchangePort,
    private val idempotency: IdempotencyPort,
    private val clock: Clock,
    private val outbox: OutboxPort,
) {
    @Transactional
    fun createAccount(currency: Currency, initialBalance: BigDecimal = BigDecimal.ZERO): Account =
        accounts.create(Account(UUID.randomUUID(), currency, initialBalance))

    @Transactional(readOnly = true)
    fun account(id: UUID): Account = accounts.find(id) ?: throw DomainException("Account not found")

    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun transfer(from: UUID, to: UUID, amount: Money, key: String): UUID {
        if (from == to) throw DomainException("Source and target must differ")
        amount.positive()
        val fp = fingerprint("$from|$to|${amount.amount.stripTrailingZeros().toPlainString()}|${amount.currency.currencyCode}")
        checkReplay("TRANSFER", key, fp)?.let { return it }
        val locked = accounts.lockAccounts(listOf(from, to))
        val source = locked[from] ?: throw DomainException("Source account not found")
        val target = locked[to] ?: throw DomainException("Target account not found")
        accounts.save(source.debit(amount))
        accounts.save(target.credit(amount))
        val id = UUID.randomUUID()
        transfers.save(TransferReceipt(id, from, to, amount, Instant.now(clock)))
        outbox.append(UUID.randomUUID(), "TransferCompleted", id, """{"transactionId":"$id","from":"$from","to":"$to","amount":"${amount.amount}","currency":"${amount.currency.currencyCode}"}""")
        idempotency.save("TRANSFER", key, fp, id)
        return id
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    fun exchange(from: UUID, to: UUID, amount: Money, key: String): UUID {
        if (from == to) throw DomainException("Source and target must differ")
        amount.positive()
        val fp = fingerprint("$from|$to|${amount.amount.stripTrailingZeros().toPlainString()}|${amount.currency.currencyCode}")
        checkReplay("EXCHANGE", key, fp)?.let { return it }
        val locked = accounts.lockAccounts(listOf(from, to))
        val source = locked[from] ?: throw DomainException("Source account not found")
        val target = locked[to] ?: throw DomainException("Target account not found")
        if (source.currency == target.currency) throw DomainException("Exchange requires distinct currencies")
        val quote = rates.getQuote(source.currency, target.currency)
        if (Duration.between(quote.observedAt, Instant.now(clock)).abs() > Duration.ofMinutes(5))
            throw DomainException("FX quote is stale")
        val debit = Money(amount.amount, source.currency)
        val credit = quote.convert(debit).positive()
        accounts.save(source.debit(debit))
        accounts.save(target.credit(credit))
        val id = UUID.randomUUID()
        exchanges.save(ExchangeReceipt(id, from, to, debit, credit, quote.rate, Instant.now(clock)))
        outbox.append(UUID.randomUUID(), "ExchangeCompleted", id, """{"transactionId":"$id","from":"$from","to":"$to","sourceAmount":"${debit.amount}","targetAmount":"${credit.amount}","rate":"${quote.rate}"}""")
        idempotency.save("EXCHANGE", key, fp, id)
        return id
    }

    private fun checkReplay(operation: String, key: String, fingerprint: String): UUID? {
        require(key.isNotBlank() && key.length <= 120) { "Invalid idempotency key" }
        val previous = idempotency.find(operation, key) ?: return null
        if (previous.fingerprint != fingerprint) throw DomainException("Idempotency key reused with different request")
        return previous.resourceId
    }

    private fun fingerprint(s: String): String = MessageDigest.getInstance("SHA-256")
        .digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
