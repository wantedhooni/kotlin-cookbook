package com.example.bank.application

import com.example.bank.domain.*
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.math.BigDecimal
import java.util.UUID

class BankServiceTest : BehaviorSpec({
    Given("이미 처리된 이체 요청 ID가 존재할 때") {
        When("같은 요청 ID로 다시 이체하면") {
            Then("기록된 결과를 재사용하며 계좌와 원장을 변경하지 않는다") {
        val accounts=mockk<AccountPort>()
        val transfers=mockk<TransferPort>()
        val ledger=mockk<LedgerPort>()
        val events=mockk<EventPort>()
        val ref=UUID.randomUUID()
        val request=UUID.randomUUID()
        val cmd=TransferCommand(request,1,2,BigDecimal("20.00"))
        every{transfers.claim(any())} returns false
        every{transfers.find(request)} returns StoredTransfer(1,2,BigDecimal("20.00"),ref)
        BankService(accounts,transfers,ledger,events).transfer(cmd) shouldBe TransferResult(ref,true)
        verify(exactly=0){accounts.lock(any())}
        verify(exactly=0){ledger.append(any())}
            }
        }
    }
    Given("출금 계좌에 충분한 잔액이 있을 때") {
        When("이체를 실행하면") {
            Then("균형 잡힌 분개를 한 쌍 기록한다") {
        val accounts=mockk<AccountPort>()
        val transfers=mockk<TransferPort>()
        val ledger=mockk<LedgerPort>()
        val events=mockk<EventPort>()
        val journals=mutableListOf<Journal>()
        every{transfers.claim(any())} returns true
        every{accounts.lock(1)} returns Account(1,"B","1","A",BigDecimal("100.00"))
        every{accounts.lock(2)} returns Account(2,"B","2","B",BigDecimal("0.00"))
        every{accounts.save(any())} answers { firstArg() }
        every{ledger.append(capture(journals))} just Runs
        every{transfers.complete(any(),any())} just Runs
        every{events.append(any(),any(),any(),any())} just Runs
        BankService(accounts,transfers,ledger,events).transfer(TransferCommand(UUID.randomUUID(),1,2,BigDecimal("30.00")))
        journals.single().postings.sumOf{it.signed} shouldBe BigDecimal("0.00")
        verify(exactly=2){accounts.save(any())}
            }
        }
    }
})
