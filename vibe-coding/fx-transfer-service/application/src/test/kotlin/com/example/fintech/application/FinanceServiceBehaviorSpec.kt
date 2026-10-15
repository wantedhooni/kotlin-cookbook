package com.example.fintech.application

import com.example.fintech.domain.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Currency
import java.util.UUID

class FinanceServiceBehaviorSpec : BehaviorSpec({
    val krw = Currency.getInstance("KRW")
    val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)

    fun setup(): Triple<FinanceService, MutableMap<UUID, Account>, MutableList<TransferReceipt>> {
        val from = UUID.randomUUID()
        val to = UUID.randomUUID()
        val store = mutableMapOf(from to Account(from, krw, BigDecimal("10000")),
            to to Account(to, krw, BigDecimal("1000")))
        val receipts = mutableListOf<TransferReceipt>()
        val keys = mutableMapOf<Pair<String, String>, StoredRequest>()
        val accounts = object : AccountPort {
            override fun lockAccounts(ids: List<UUID>) = ids.mapNotNull { id -> store[id]?.let { id to it } }.toMap()
            override fun save(account: Account) { store[account.id] = account }
            override fun find(id: UUID) = store[id]
            override fun create(account: Account): Account { store[account.id] = account; return account }
        }
        val service = FinanceService(accounts,
            object : FxRatePort {
                override fun getQuote(base: Currency, counter: Currency) =
                    FxQuote(base, counter, BigDecimal("0.001"), Instant.now(clock))
            },
            object : TransferPort { override fun save(receipt: TransferReceipt) { receipts += receipt } },
            object : ExchangePort { override fun save(receipt: ExchangeReceipt) {} },
            object : IdempotencyPort {
                override fun find(operation: String, key: String) = keys[operation to key]
                override fun save(operation: String, key: String, fingerprint: String, resourceId: UUID) {
                    keys[operation to key] = StoredRequest(fingerprint, resourceId)
                }
            }, clock,
            object : OutboxPort {
                override fun append(eventId: UUID, eventType: String, aggregateId: UUID, payload: String) {}
            })
        return Triple(service, store, receipts)
    }

    Given("유효한 이체 요청과 멱등성 키가 주어졌을 때") {
        When("동일한 이체를 다시 요청하면") {
            val (service, store, receipts) = setup()
            val ids = store.keys.toList()
            val amount = Money(BigDecimal("100"), krw)
            val first = service.transfer(ids[0], ids[1], amount, "key-1")
            val retry = service.transfer(ids[0], ids[1], amount, "key-1")
            Then("중복 출금 없이 동일한 이체 결과를 반환한다") {
                retry shouldBe first
                store[ids[0]]!!.balance shouldBe BigDecimal("9900")
                store[ids[1]]!!.balance shouldBe BigDecimal("1100")
                receipts.size shouldBe 1
            }
        }
    }

    Given("이미 사용한 멱등성 키가 주어졌을 때") {
        When("다른 금액의 이체에 같은 키를 사용하면") {
            val (service, store, _) = setup()
            val ids = store.keys.toList()
            service.transfer(ids[0], ids[1], Money(BigDecimal("100"), krw), "key-2")
            Then("요청 정보가 일치하지 않아 거절된다") {
                shouldThrow<DomainException> {
                    service.transfer(ids[0], ids[1], Money(BigDecimal("101"), krw), "key-2")
                }
            }
        }
    }

    Given("통화가 동일한 두 계좌가 주어졌을 때") {
        When("두 계좌 간 환전을 요청하면") {
            val (service, store, _) = setup()
            val ids = store.keys.toList()
            Then("유효하지 않은 환전 요청이 거절된다") {
                shouldThrow<DomainException> {
                    service.exchange(ids[0], ids[1], Money(BigDecimal("100"), krw), "key-3")
                }
            }
        }
    }
})
