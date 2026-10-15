package com.portfolio.payments.tax

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/api/tax")
class TaxController(private val calculator: TaxCalculator) {
    @GetMapping("/estimate")
    fun estimate(
        @RequestParam grossIncome: Long,
        @RequestParam(defaultValue = "0.020") assumedEffectiveFinalTaxRate: BigDecimal,
    ) = calculator.calculate(grossIncome, assumedEffectiveFinalTaxRate)
}
