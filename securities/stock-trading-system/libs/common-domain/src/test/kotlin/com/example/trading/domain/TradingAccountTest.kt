package com.example.trading.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import java.math.BigDecimal

class TradingAccountTest : StringSpec({
    "40% 증거금 매수 주문은 현금과 신용을 각각 예약한다" {
        val account = TradingAccount("A1", BigDecimal("1000000"), creditLimit = BigDecimal("1000000"))
        val reservation = account.reserveForBuy(BigDecimal("500000"), BigDecimal("0.40"))
        reservation.cash shouldBe BigDecimal("200000.0000")
        reservation.credit shouldBe BigDecimal("300000.0000")
        account.availableCash shouldBe BigDecimal("800000.0000")
        account.availableCredit shouldBe BigDecimal("700000.0000")
    }

    "주문가능금액을 넘으면 예약을 거절한다" {
        val account = TradingAccount("A1", BigDecimal("100000"), creditLimit = BigDecimal("100000"))
        shouldThrow<IllegalStateException> { account.reserveForBuy(BigDecimal("500000"), BigDecimal("0.40")) }
    }

    "체결 시 예약금은 D+2 정산 보류금으로 이동한다" {
        val account = TradingAccount("A1", BigDecimal("1000000"), creditLimit = BigDecimal("1000000"))
        val reservation = account.reserveForBuy(BigDecimal("500000"), BigDecimal("0.40"))
        val hold = account.commitBuyForSettlement(reservation, BigDecimal("450000"), BigDecimal("0.40"))
        hold.cash shouldBe BigDecimal("180000.0000")
        hold.credit shouldBe BigDecimal("270000.0000")
        account.cashBalance shouldBe BigDecimal("1000000")
        account.pendingSettlementPayable shouldBe BigDecimal("450000.0000")
        account.availableCash shouldBe BigDecimal("820000.0000")
        account.availableCredit shouldBe BigDecimal("730000.0000")
        account.settleBuy(hold)
        account.cashBalance shouldBe BigDecimal("820000.0000")
        account.usedCredit shouldBe BigDecimal("270000.0000")
        account.pendingSettlementPayable shouldBe BigDecimal("0.0000")
    }

    "매도 체결대금은 정산예정금으로 잡힌 뒤 D+2에 예수금으로 전환된다" {
        val account = TradingAccount("A1", BigDecimal("100000"))
        account.registerSellReceivable(BigDecimal("700000"))
        account.cashBalance shouldBe BigDecimal("100000")
        account.pendingSettlementReceivable shouldBe BigDecimal("700000")
        account.settleSellReceivable(BigDecimal("700000"))
        account.cashBalance shouldBe BigDecimal("800000")
        account.pendingSettlementReceivable shouldBe BigDecimal.ZERO
    }
})
