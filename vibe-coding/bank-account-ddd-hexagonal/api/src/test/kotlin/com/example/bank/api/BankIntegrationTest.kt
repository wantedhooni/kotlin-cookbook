package com.example.bank.api

import com.example.bank.application.*
import com.example.bank.domain.BankException
import com.example.bank.infrastructure.OutboxPublisher
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.assertions.throwables.shouldThrow
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.time.Duration
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer

@SpringBootTest(classes=[BankApplication::class],webEnvironment=SpringBootTest.WebEnvironment.NONE)
class BankIntegrationTest : BehaviorSpec() {
    override fun extensions() = listOf(SpringExtension)

    @Autowired lateinit var bank:BankService
    @Autowired lateinit var jdbc:JdbcTemplate
    @Autowired lateinit var publisher:OutboxPublisher

    companion object {
        val postgres=PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
            .withDatabaseName("bank").withUsername("bank").withPassword("bank")
        val kafka=KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"))
        init { postgres.start(); kafka.start() }
        @JvmStatic @DynamicPropertySource
        fun properties(registry:DynamicPropertyRegistry) {
            registry.add("spring.datasource.url",postgres::getJdbcUrl)
            registry.add("spring.datasource.username",postgres::getUsername)
            registry.add("spring.datasource.password",postgres::getPassword)
            registry.add("spring.kafka.bootstrap-servers",kafka::getBootstrapServers)
            registry.add("app.outbox.poll-ms") { "3600000" }
        }
    }

    init {
        Given("동일한 요청 ID의 계좌이체가 이미 처리된 상태에서") {
            When("같은 요청으로 계좌이체를 다시 실행하면") {
                Then("이체는 한 번만 처리되고 원장의 차변과 대변 합계가 일치한다") { scenario_idempotencyAndBalancedJournal() }
            }
        }
        Given("출금 계좌 잔액보다 많은 금액을 동시에 이체하려는 상황에서") {
            When("여러 계좌이체 요청을 병렬로 실행하면") {
                Then("잔액을 초과하는 이체는 거부되고 잔액이 음수가 되지 않는다") { scenario_concurrentTransfers() }
            }
        }
        Given("하루 이체 한도를 초과하는 요청이 있을 때") {
            When("한도 초과 계좌이체를 시도하면") {
                Then("요청이 거부되고 계좌 잔액이 변경되지 않는다") { scenario_dailyLimitRejectedAndRolledBack() }
            }
        }
        Given("동일한 카프카 이벤트가 중복 수신된 상황에서") {
            When("같은 이벤트를 두 번 처리하면") {
                Then("감사 로그에는 한 번만 기록된다") { scenario_consumerDeduplicatesByEventId() }
            }
        }
        Given("발행 대기 중인 계좌 생성 이벤트가 있을 때") {
            When("아웃박스 발행 작업을 실행하면") {
                Then("계좌 생성 이벤트가 카프카에 게시된다") { scenario_kafkaOutboxPublish() }
            }
        }
    }
    private fun scenario_idempotencyAndBalancedJournal() {
            val a=bank.open("BANK001","A-${UUID.randomUUID()}","Alice")
            val b=bank.open("BANK001","B-${UUID.randomUUID()}","Bob")
            bank.deposit(a.id,BigDecimal("100.00"))
            val cmd=TransferCommand(UUID.randomUUID(),a.id,b.id,BigDecimal("20.00"))
            val first=bank.transfer(cmd)
            val second=bank.transfer(cmd)
            first.referenceId shouldBe second.referenceId
            second.replayed shouldBe true
            bank.get(a.id).balance shouldBe BigDecimal("80.00")
            bank.get(b.id).balance shouldBe BigDecimal("20.00")
            jdbc.queryForObject("SELECT COUNT(*) FROM ledger_postings WHERE reference_id = ?",Int::class.java,first.referenceId) shouldBe 2
            jdbc.queryForObject("""SELECT COALESCE(SUM(CASE WHEN side='CREDIT' THEN amount ELSE -amount END),0)
                                   FROM ledger_postings WHERE reference_id=?""",BigDecimal::class.java,first.referenceId)!!.compareTo(BigDecimal.ZERO) shouldBe 0
            shouldThrow<BankException>{ bank.transfer(cmd.copy(amount=BigDecimal("21.00"))) }
    }
    private fun scenario_concurrentTransfers() {
            val a=bank.open("BANK001","C-${UUID.randomUUID()}","Alice")
            val b=bank.open("BANK001","D-${UUID.randomUUID()}","Bob")
            bank.deposit(a.id,BigDecimal("100.00"))
            val pool=Executors.newFixedThreadPool(8)
            try {
                val calls=(1..12).map { pool.submit<Boolean> {
                    try { bank.transfer(TransferCommand(UUID.randomUUID(),a.id,b.id,BigDecimal("15.00"))); true }
                    catch(e:BankException) { if(e.code!="INSUFFICIENT_FUNDS") throw e; false }
                }}
                calls.count{it.get(40,TimeUnit.SECONDS)} shouldBe 6
                bank.get(a.id).balance shouldBe BigDecimal("10.00")
                bank.get(b.id).balance shouldBe BigDecimal("90.00")
            } finally { pool.shutdownNow() }
    }
    private fun scenario_dailyLimitRejectedAndRolledBack() {
        val a=bank.open("BANK001","L-${UUID.randomUUID()}","Alice")
        val b=bank.open("BANK001","M-${UUID.randomUUID()}","Bob")
        bank.deposit(a.id,BigDecimal("100.00"))
        // Test app limit is high; directly reserve almost all limit first.
        jdbc.update("INSERT INTO daily_transfer_usage(account_id,business_date,used_amount) VALUES(?,CURRENT_DATE,?)",a.id,BigDecimal("9999995.00"))
        val failure=shouldThrow<BankException> {
            bank.transfer(TransferCommand(UUID.randomUUID(),a.id,b.id,BigDecimal("10.00")))
        }
        failure.code shouldBe "DAILY_LIMIT_EXCEEDED"
        bank.get(a.id).balance shouldBe BigDecimal("100.00")
        bank.get(b.id).balance shouldBe BigDecimal("0.00")
    }
    private fun scenario_consumerDeduplicatesByEventId() {
        val consumer=com.example.bank.infrastructure.AccountEventAuditConsumer(jdbc)
        val eventId=UUID.randomUUID()
        val body="""{"eventId":"$eventId","type":"TRANSFER_OUT"}"""
        val record=org.apache.kafka.clients.consumer.ConsumerRecord("account-events",0,0L,"123",body)
        consumer.onEvent(record)
        consumer.onEvent(record)
        jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE action='EVENT_CONSUMED' AND detail=?",Int::class.java,body) shouldBe 1
    }
    private fun scenario_kafkaOutboxPublish() {
            val a=bank.open("BANK001","E-${UUID.randomUUID()}","Kafka")
            val props=mapOf(
                "bootstrap.servers" to kafka.bootstrapServers,
                "group.id" to "test-${UUID.randomUUID()}",
                "auto.offset.reset" to "earliest",
                "key.deserializer" to StringDeserializer::class.java.name,
                "value.deserializer" to StringDeserializer::class.java.name)
            KafkaConsumer<String,String>(props).use { consumer ->
                consumer.subscribe(listOf("account-events"))
                publisher.publishPending()
                val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20)
                var found=false
                while(System.nanoTime()<deadline && !found) {
                    found=consumer.poll(Duration.ofMillis(500)).any { it.value().contains("\"aggregateId\":${a.id}") }
                }
                found shouldBe true
            }
    }
}
