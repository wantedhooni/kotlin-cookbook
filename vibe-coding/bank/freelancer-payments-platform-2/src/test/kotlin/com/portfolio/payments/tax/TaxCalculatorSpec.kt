package com.portfolio.payments.tax

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class TaxCalculatorSpec : BehaviorSpec({
    Given("100만원의 사업소득성 금액과 3.3% 원천징수 모델") {
        When("가정 최종 실효세율이 2%이면") {
            val result = TaxCalculator().calculate(1_000_000, BigDecimal("0.020"))
            Then("원천징수 33,000원과 단순 추정 환급 13,000원을 계산한다") {
                result.withholdingTax shouldBe 33_000
                result.assumedFinalTax shouldBe 20_000
                result.estimatedRefund shouldBe 13_000
            }
        }
    }
})
