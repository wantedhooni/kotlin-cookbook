package com.portfolio.payments.settlement

import com.portfolio.payments.tax.TaxCalculator
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.math.RoundingMode

class SettlementInvariantSpec : BehaviorSpec({
    Given("정산 금액") {
        val gross = 100_000L
        When("플랫폼 수수료 3%와 원천징수 3.3%를 차감하면") {
            val fee = BigDecimal.valueOf(gross).multiply(BigDecimal("0.030")).setScale(0, RoundingMode.DOWN).longValueExact()
            val tax = TaxCalculator().calculate(gross).withholdingTax
            val payout = gross - fee - tax
            Then("차변 총액과 세 개의 대변 합계가 일치해야 한다") {
                (payout + fee + tax) shouldBe gross
            }
        }
    }
})
