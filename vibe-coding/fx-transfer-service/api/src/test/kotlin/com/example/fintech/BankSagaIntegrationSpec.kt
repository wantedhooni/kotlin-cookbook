package com.example.fintech

import com.example.fintech.adapter.out.bank.BankTransferSaga
import com.example.fintech.adapter.out.bank.BankReconciliationScheduler
import com.example.fintech.application.BankReconciliationPort
import com.example.fintech.application.BankReconciliationResult
import com.example.fintech.adapter.out.persistence.FinancialEventConsumer
import com.example.fintech.application.BankTransferPort
import com.example.fintech.application.BankTransferRequest
import com.example.fintech.application.BankTransferResult
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/** RUN_CONTAINER_TESTS=true required. Exercises Flyway, row locking, compensation and consumer dedupe. */
@SpringBootTest
@ActiveProfiles("demo")
@Import(BankSagaIntegrationSpec.BankStubConfig::class)
class BankSagaIntegrationSpec : BehaviorSpec() {
 override fun extensions() = listOf(SpringExtension)

 @TestConfiguration class BankStubConfig {
  @Bean @Primary fun bankStub(): StubBank = StubBank()
  @Bean @Primary fun reconciliationStub(): StubReconciliation = StubReconciliation()
 }
 class StubBank : BankTransferPort {
  val mode = AtomicReference("SUCCESS")
  override fun requestTransfer(request: BankTransferRequest): BankTransferResult {
   if (mode.get() == "TIMEOUT") throw RuntimeException("ambiguous timeout")
   return BankTransferResult("BANK-"+request.requestId,mode.get()=="SUCCESS")
  }
 }
 class StubReconciliation: BankReconciliationPort {
  val outcome = AtomicReference<BankReconciliationResult>(BankReconciliationResult.Pending)
  override fun lookup(requestId: UUID, bankCode: String) = outcome.get()
 }
 @Autowired lateinit var scheduler: BankReconciliationScheduler
 @Autowired lateinit var lookup: StubReconciliation
 @Autowired lateinit var saga: BankTransferSaga
 @Autowired lateinit var db: JdbcTemplate
 @Autowired lateinit var bank: StubBank
 @Autowired lateinit var consumer: FinancialEventConsumer

 private fun assertEquals(expected: Any?, actual: Any?) { actual shouldBe expected }

 private fun account(): UUID {
  val id=UUID.randomUUID()
  db.update("INSERT INTO accounts(id,currency,balance,version) VALUES (?,'KRW',100,0)",id)
  return id
 }
 private fun balance(id: UUID) = db.queryForObject("SELECT balance FROM accounts WHERE id=?",BigDecimal::class.java,id)!!
 private fun command(id: UUID)=BankTransferSaga.Command(id,"TEST","00001111",BigDecimal("25.0000"),"KRW")








 init {
  Given("PostgreSQL과 Kafka가 실행 중인 환경에서") {
   When("은행이 이체를 수락하고 같은 요청을 재전송하면") {
    Then("자금 정합성과 멱등성이 보장된다") {
     bank.mode.set("SUCCESS"); val id=account(); val key=UUID.randomUUID().toString()
       val first=saga.initiate(command(id),key)
       val second=saga.initiate(command(id),key)
       assertEquals("SUCCEEDED",first.status); assertEquals(first.id,second.id)
       assertEquals(0,balance(id).compareTo(BigDecimal("75")))
    }
   }
  }
  Given("PostgreSQL과 Kafka가 실행 중인 환경에서") {
   When("은행이 이체를 명시적으로 거절하면") {
    Then("자금 정합성과 멱등성이 보장된다") {
     bank.mode.set("REJECT"); val id=account(); val first=saga.initiate(command(id),UUID.randomUUID().toString())
       assertEquals("COMPENSATED",first.status)
       assertEquals(0,balance(id).compareTo(BigDecimal("100")))
       assertEquals("COMPENSATED",saga.reconcile(first.id,false,"confirmed-rejected").status)
       assertEquals(0,balance(id).compareTo(BigDecimal("100")))
    }
   }
  }
  Given("PostgreSQL과 Kafka가 실행 중인 환경에서") {
   When("은행 호출이 타임아웃된 뒤 확정된 거절 증빙으로 대사하면") {
    Then("자금 정합성과 멱등성이 보장된다") {
     bank.mode.set("TIMEOUT"); val id=account(); val first=saga.initiate(command(id),UUID.randomUUID().toString())
       assertEquals("UNKNOWN",first.status); assertEquals(0,balance(id).compareTo(BigDecimal("75")))
       assertEquals("COMPENSATED",saga.reconcile(first.id,false,"verified-not-executed").status)
       assertEquals(0,balance(id).compareTo(BigDecimal("100")))
    }
   }
  }
  Given("PostgreSQL과 Kafka가 실행 중인 환경에서") {
   When("은행 처리 상태가 미확정이면") {
    Then("자금 정합성과 멱등성이 보장된다") {
     bank.mode.set("TIMEOUT"); lookup.outcome.set(BankReconciliationResult.Pending)
       val id=account(); val pending=saga.initiate(command(id),UUID.randomUUID().toString())
       db.update("UPDATE bank_transfer_sagas SET reconcile_after=now()+interval '1 day' WHERE status='UNKNOWN' AND id<>?",pending.id)
       val claimed=scheduler.claimOne()!!
       assertEquals(pending.id,claimed.id)
       scheduler.process(claimed)
       assertEquals("UNKNOWN",saga.find(pending.id).status)
       assertEquals(0,balance(id).compareTo(BigDecimal("75")))
    }
   }
  }
  Given("PostgreSQL과 Kafka가 실행 중인 환경에서") {
   When("은행이 확인한 거절 결과로 자동 대사를 수행하면") {
    Then("자금 정합성과 멱등성이 보장된다") {
     bank.mode.set("TIMEOUT"); lookup.outcome.set(BankReconciliationResult.Rejected("BANK-PROOF"))
       val id=account(); val pending=saga.initiate(command(id),UUID.randomUUID().toString())
       db.update("UPDATE bank_transfer_sagas SET reconcile_after=now()+interval '1 day' WHERE status='UNKNOWN' AND id<>?",pending.id)
       val claim=scheduler.claimOne()!!
       assertEquals(pending.id,claim.id)
       scheduler.process(claim)
       scheduler.process(claim)
       assertEquals("COMPENSATED",saga.find(pending.id).status)
       assertEquals(0,balance(id).compareTo(BigDecimal("100")))
       assertEquals(1,db.queryForObject("SELECT count(*) FROM bank_reconciliation_evidence WHERE saga_id=?",Int::class.java,pending.id))
    }
   }
  }
  Given("PostgreSQL과 Kafka가 실행 중인 환경에서") {
   When("동일한 Kafka 이벤트를 두 번 소비하면") {
    Then("자금 정합성과 멱등성이 보장된다") {
     val eventId=UUID.randomUUID(); val transactionId=UUID.randomUUID()
       val value="""{"eventId":"$eventId","eventType":"BankTransferSucceeded","payload":{"transactionId":"$transactionId"}}"""
       consumer.consume(value); consumer.consume(value)
       assertEquals(1,db.queryForObject("SELECT count(*) FROM payment_event_audit WHERE event_id=?",Int::class.java,eventId))
    }
   }
  }
 }
 companion object {
  private val pg=PostgreSQLContainer<Nothing>("postgres:16-alpine")
  private val kafka=KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"))
  init { pg.start(); kafka.start() }
  @JvmStatic @DynamicPropertySource fun config(registry: DynamicPropertyRegistry) {
   run {
    registry.add("spring.datasource.url",pg::getJdbcUrl)
    registry.add("spring.datasource.username",pg::getUsername)
    registry.add("spring.datasource.password",pg::getPassword)
    registry.add("spring.kafka.bootstrap-servers",kafka::getBootstrapServers)
   }
  }
 }
}
