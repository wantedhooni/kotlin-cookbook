package com.portfolio.payments.tax

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

data class TaxBreakdown(
    val grossIncome: Long,
    val withholdingTax: Long,
    val netAfterWithholding: Long,
    val assumedFinalTax: Long,
    val estimatedRefund: Long,
)

@Component
class TaxCalculator {
    /**
     * Portfolio domain model only. 3.3% is modeled as 3.0% income tax + 0.3% local income tax.
     * Actual tax depends on income type, deductions, other income, and applicable law.
     */
    fun calculate(grossIncome: Long, assumedEffectiveFinalTaxRate: BigDecimal = BigDecimal("0.020")): TaxBreakdown {
        val withholding = percent(grossIncome, BigDecimal("0.033"))
        val finalTax = percent(grossIncome, assumedEffectiveFinalTaxRate)
        return TaxBreakdown(
            grossIncome = grossIncome,
            withholdingTax = withholding,
            netAfterWithholding = grossIncome - withholding,
            assumedFinalTax = finalTax,
            estimatedRefund = (withholding - finalTax).coerceAtLeast(0),
        )
    }

    private fun percent(amount: Long, rate: BigDecimal): Long =
        BigDecimal.valueOf(amount).multiply(rate).setScale(0, RoundingMode.DOWN).longValueExact()
}
