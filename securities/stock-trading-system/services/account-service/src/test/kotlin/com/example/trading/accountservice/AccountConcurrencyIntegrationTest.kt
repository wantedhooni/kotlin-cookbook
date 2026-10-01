package com.example.trading.accountservice

import com.example.trading.accountservice.application.AccountApplicationService
import com.example.trading.accountservice.application.ReserveBuyCommand
import com.example.trading.domain.OrderType
import com.example.trading.domain.Side
import com.example.trading.event.ExecutionBookedEvent
import com.example.trading.event.OrderCanceledEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors

@SpringBootTest(properties = ["spring.kafka.listener.auto-startup=false", "trading.margin.default-rate=1.00"])
@Testcontainers
class AccountConcurrencyIntegrationTest @Autowired constructor(
    private val service: AccountApplicationService,
) {
    companion object {
        @Container
        @JvmField
        val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine")
            .withDatabaseName("trading")
            .withUsername("trading")
            .withPassword("trading")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }

    @BeforeEach
    fun seed() {
        runCatching { service.seed("ACC-CONC", BigDecimal("100000"), BigDecimal.ZERO) }
    }

    @Test
    fun `취소 이벤트가 늦은 체결보다 먼저 와도 체결분 증거금은 유지된다`() {
        service.seed("ACC-RACE", BigDecimal("1000000"), BigDecimal.ZERO)
        val orderId = UUID.randomUUID()
        service.reserveBuy(ReserveBuyCommand(orderId, "ACC-RACE", "005930", OrderType.LIMIT, 10, BigDecimal("70000")))

        service.onCanceled(OrderCanceledEvent(UUID.randomUUID(), orderId, 5, Instant.now()))
        val afterCancel = service.get("ACC-RACE")
        assertEquals(0, afterCancel.reservedCash.compareTo(BigDecimal("350000.0000")))

        service.applyExecution(
            ExecutionBookedEvent(
                UUID.randomUUID(), "LATE-${UUID.randomUUID()}", orderId, "ACC-RACE", "005930", Side.BUY,
                5, BigDecimal("70000"), Instant.now()
            )
        )
        val afterExecution = service.get("ACC-RACE")
        assertEquals(0, afterExecution.cashBalance.compareTo(BigDecimal("1000000.0000")))
        assertEquals(0, afterExecution.reservedCash.compareTo(BigDecimal("0.0000")))
        assertEquals(0, afterExecution.pendingSettlementPayable.compareTo(BigDecimal("350000.0000")))
        assertEquals(0, afterExecution.orderableAmount.compareTo(BigDecimal("650000.0000")))
    }

    @Test
    fun `체결 후 D plus 2 결제 시점에 예수금이 실제 차감된다`() {
        service.seed("ACC-SETTLE", BigDecimal("1000000"), BigDecimal.ZERO)
        val orderId = UUID.randomUUID()
        val executionId = "SETTLE-${UUID.randomUUID()}"
        service.reserveBuy(ReserveBuyCommand(orderId, "ACC-SETTLE", "005930", OrderType.LIMIT, 2, BigDecimal("70000")))
        service.applyExecution(ExecutionBookedEvent(UUID.randomUUID(), executionId, orderId, "ACC-SETTLE", "005930", Side.BUY, 2, BigDecimal("70000"), Instant.now()))

        val before = service.get("ACC-SETTLE")
        assertEquals(0, before.cashBalance.compareTo(BigDecimal("1000000.0000")))
        assertEquals(0, before.pendingSettlementPayable.compareTo(BigDecimal("140000.0000")))

        val result = service.settleExecution(executionId)
        assertTrue(result.settled)
        val after = service.get("ACC-SETTLE")
        assertEquals(0, after.cashBalance.compareTo(BigDecimal("860000.0000")))
        assertEquals(0, after.pendingSettlementPayable.compareTo(BigDecimal("0.0000")))
    }

    @Test
    fun `동시 매수 예약은 주문가능금액을 초과할 수 없다`() {
        val pool = Executors.newFixedThreadPool(2)
        val start = java.util.concurrent.CountDownLatch(1)
        val commands = listOf(UUID.randomUUID(), UUID.randomUUID()).map { orderId ->
            ReserveBuyCommand(orderId, "ACC-CONC", "005930", OrderType.LIMIT, 1, BigDecimal("80000"))
        }
        val futures = commands.map { command ->
            pool.submit(Callable {
                start.await()
                runCatching { service.reserveBuy(command) }
            })
        }
        start.countDown()
        val results = futures.map { it.get() }
        pool.shutdown()

        assertEquals(1, results.count { it.isSuccess })
        assertEquals(1, results.count { it.isFailure })
        val account = service.get("ACC-CONC")
        assertEquals(0, account.availableCash.compareTo(BigDecimal("20000.0000")))
        assertTrue(account.reservedCash.compareTo(BigDecimal("80000.0000")) == 0)
    }
}
