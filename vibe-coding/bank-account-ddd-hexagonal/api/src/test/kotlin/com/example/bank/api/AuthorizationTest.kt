package com.example.bank.api

import com.example.bank.application.*
import com.example.bank.domain.Account
import io.mockk.*
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.BehaviorSpec
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import java.math.BigDecimal

class AuthorizationTest : BehaviorSpec({
    Given("다른 사용자 소유의 계좌가 존재할 때") {
        When("권한 없는 사용자가 계좌를 조회하면") {
            Then("계좌 접근이 거부된다") {
        val svc=mockk<BankService>()
        val audit=mockk<AuditPort>()
        every { svc.get(42) } returns Account(42,"BANK001","42","alice",BigDecimal.ZERO)
        val jwt=Jwt.withTokenValue("test").header("alg","none").subject("bob").build()
        val thrown=runCatching { AccountController(svc,audit).get(jwt,42) }.exceptionOrNull()
        (thrown?.message) shouldBe "Not your account"
        verify(exactly=1) { svc.get(42) }
                }
        }
    }
})
