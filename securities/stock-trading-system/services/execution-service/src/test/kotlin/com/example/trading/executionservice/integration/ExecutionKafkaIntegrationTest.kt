package com.example.trading.executionservice.integration

import com.example.trading.domain.Side
import com.example.trading.event.KrxExecutionEvent
import com.example.trading.event.Topics
import com.example.trading.executionservice.adapter.out.persistence.ExecutionJpaRepository
import com.example.trading.executionservice.adapter.out.persistence.ExecutionOutboxJpaRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.util.Properties
import java.util.UUID

@SpringBootTest
@Testcontainers
class ExecutionKafkaIntegrationTest @Autowired constructor(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val mapper: ObjectMapper,
    private val executions: ExecutionJpaRepository,
    private val outbox: ExecutionOutboxJpaRepository,
) {
    companion object {
        @Container
        @JvmField
        val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine")
            .withDatabaseName("trading")
            .withUsername("trading")
            .withPassword("trading")

        @Container
        @JvmField
        val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"))

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
        }
    }

    @Test
    fun `동일 executionId 이벤트를 Kafka로 두 번 보내도 한 번만 저장한다`() {
        val event = KrxExecutionEvent(
            eventId = UUID.randomUUID(),
            executionId = "DUP-${UUID.randomUUID()}",
            orderId = UUID.randomUUID(),
            accountId = "ACC-001",
            symbol = "005930",
            side = Side.BUY,
            executionQuantity = 10,
            executionPrice = BigDecimal("70000"),
            occurredAt = Instant.now(),
        )
        val payload = mapper.writeValueAsString(event)
        kafkaTemplate.send(Topics.KRX_EXECUTION, event.orderId.toString(), payload).get()
        kafkaTemplate.send(Topics.KRX_EXECUTION, event.orderId.toString(), payload).get()

        await("execution saved") { executions.existsByExecutionId(event.executionId) }
        Thread.sleep(500)
        assertEquals(1, executions.findAll().count { it.executionId == event.executionId })
        assertEquals(1, outbox.findAll().count { it.eventKey == event.orderId.toString() })
    }

    @Test
    fun `역직렬화 실패 이벤트는 재시도 후 DLT로 이동한다`() {
        val consumer = dltConsumer()
        consumer.subscribe(listOf("${Topics.KRX_EXECUTION}.DLT"))
        val key = "bad-${UUID.randomUUID()}"
        kafkaTemplate.send(Topics.KRX_EXECUTION, key, "{not-valid-json").get()

        var received: String? = null
        val deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos()
        while (System.nanoTime() < deadline && received == null) {
            val records = consumer.poll(Duration.ofMillis(500))
            received = records.firstOrNull { it.key() == key }?.value()
        }
        consumer.close()
        assertNotNull(received, "malformed event must be published to DLT")
        assertEquals("{not-valid-json", received)
    }

    private fun dltConsumer(): KafkaConsumer<String, String> {
        val props = Properties()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafka.bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = "dlt-test-${UUID.randomUUID()}"
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        return KafkaConsumer(props)
    }

    private fun await(name: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos()
        while (System.nanoTime() < deadline) {
            if (condition()) return
            Thread.sleep(100)
        }
        error("timeout waiting for $name")
    }
}
