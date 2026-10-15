package com.example.dbsdeposit

import com.example.dbsdeposit.service.DepositSyncService
import com.example.dbsdeposit.support.ConcurrentSyncException
import com.example.dbsdeposit.support.IdempotencyInProgressException
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest(properties = ["dbs.mock-delay=500ms"])
@ApplyExtension(SpringExtension::class)
class SyncConcurrencyBehaviorSpec : BehaviorSpec() {

    @Autowired
    private lateinit var syncService: DepositSyncService

    init {
        Given("같은 고객에 서로 다른 멱등성 키를 가진 동기화 요청 두 건이 동시에 들어올 때") {
            When("두 요청이 동시에 분산 락용 DB 임대 획득을 시도하면") {
                Then("정확히 하나의 요청만 DB 임대를 획득하고 다른 요청은 충돌하며, 충돌한 멱등성 키는 이후 재시도할 수 있다") {
                    val partyId = "bdd-concurrent-party"
                    val pool = Executors.newFixedThreadPool(2)
                    val start = CountDownLatch(1)

                    try {
                        val futures = (1..2).map { index ->
                            val key = "bdd-concurrent-$index"
                            pool.submit<Pair<String, String>> {
                                start.await()
                                val status = try {
                                    syncService.sync(partyId, key)
                                    "OK"
                                } catch (_: ConcurrentSyncException) {
                                    "CONFLICT"
                                }
                                key to status
                            }
                        }

                        start.countDown()
                        val outcomes = futures.map { it.get(5, TimeUnit.SECONDS) }
                        outcomes.map { it.second }.sorted() shouldBe listOf("CONFLICT", "OK")

                        val retryKey = outcomes.single { it.second == "CONFLICT" }.first
                        val retry = syncService.sync(partyId, retryKey)
                        retry.replayed shouldBe false
                        retry.body.receivedCount shouldBe 2
                    } finally {
                        pool.shutdownNow()
                    }
                }
            }
        }

        Given("같은 고객과 같은 멱등성 키를 사용하는 요청 두 건이 동시에 들어올 때") {
            When("첫 번째 요청이 PROCESSING 상태를 선점한 동안 두 번째 요청이 들어오면") {
                Then("중복 실행을 막고 IDEMPOTENCY_IN_PROGRESS를 반환하며, 첫 요청 완료 후에는 저장된 응답을 재사용한다") {
                    val partyId = "bdd-idempotency-race-party"
                    val key = "bdd-idempotency-race-key"
                    val pool = Executors.newFixedThreadPool(2)
                    val start = CountDownLatch(1)

                    try {
                        val futures = (1..2).map {
                            pool.submit<String> {
                                start.await()
                                try {
                                    syncService.sync(partyId, key)
                                    "OK"
                                } catch (_: IdempotencyInProgressException) {
                                    "IN_PROGRESS"
                                }
                            }
                        }

                        start.countDown()
                        val outcomes = futures.map { it.get(5, TimeUnit.SECONDS) }.sorted()
                        outcomes shouldBe listOf("IN_PROGRESS", "OK")

                        val replay = syncService.sync(partyId, key)
                        replay.replayed shouldBe true
                        replay.body.receivedCount shouldBe 2
                    } finally {
                        pool.shutdownNow()
                    }
                }
            }
        }
    }
}
