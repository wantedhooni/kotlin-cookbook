package com.example.trading.accountservice.application

import com.example.trading.accountservice.adapter.out.persistence.*
import com.example.trading.domain.*
import com.example.trading.event.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

@Service
class AccountApplicationService(
    private val accounts:TradingAccountJpaRepository, private val reservations:BuyReservationJpaRepository,
    private val holds:AccountSettlementHoldJpaRepository, private val processed:AccountProcessedEventJpaRepository,
    @Value("\${trading.margin.default-rate:1.00}") private val defaultMarginRate:BigDecimal,
    @Value("\${trading.margin.market-buffer-rate:0.10}") private val marketBufferRate:BigDecimal,
    @Value("\${trading.margin.market-reference-price:70000}") private val marketReferencePrice:BigDecimal,
){
    init{MarginPolicy.validateRate(defaultMarginRate);require(marketBufferRate>=BigDecimal.ZERO);require(marketReferencePrice>BigDecimal.ZERO)}

    @Transactional fun seed(accountId:String,cashBalance:BigDecimal,creditLimit:BigDecimal):AccountView{
        require(accountId.isNotBlank());require(cashBalance>=BigDecimal.ZERO&&creditLimit>=BigDecimal.ZERO)
        val e=accounts.findForUpdate(accountId)?:TradingAccountEntity(accountId=accountId)
        check(e.reservedCash.signum()==0 && e.reservedCredit.signum()==0){"cannot seed account with active reservations"}
        check(e.usedCredit.signum()==0){"cannot seed account with outstanding credit"}
        check(e.settlementCashHold.signum()==0&&e.settlementCreditHold.signum()==0&&e.pendingSettlementReceivable.signum()==0){"cannot seed account with pending settlements"}
        check(e.overdueAmount.signum()==0){"cannot seed account with overdue amount"}
        e.cashBalance=cashBalance.money();e.creditLimit=creditLimit.money();e.updatedAt=Instant.now();accounts.save(e);return e.toView()
    }

    @Transactional fun reserveBuy(c:ReserveBuyCommand):AccountView{
        if(reservations.existsById(c.orderId))return get(c.accountId)
        val e=accounts.findForUpdate(c.accountId)?:error("account not found: ${c.accountId}");val a=e.toDomain()
        val protected=MarginPolicy.protectedUnitPrice(c.orderType,c.price,marketReferencePrice,marketBufferRate)
        val notional=protected*BigDecimal.valueOf(c.quantity);val r=a.reserveForBuy(notional.money(),defaultMarginRate);e.apply(a)
        reservations.save(BuyReservationEntity(orderId=c.orderId,accountId=c.accountId,symbol=c.symbol,quantity=c.quantity,protectedUnitPrice=protected.money(),marginRate=defaultMarginRate,remainingCash=r.cash.money(),remainingCredit=r.credit.money()))
        return e.toView()
    }

    @Transactional fun releaseBuy(orderId:UUID){ val r=reservations.findForUpdate(orderId)?:return;if(r.status!=BuyReservationStatus.ACTIVE)return;val e=accounts.findForUpdate(r.accountId)?:error("account not found");val a=e.toDomain();a.release(FundsReservation(r.remainingCash,r.remainingCredit));e.apply(a);r.remainingCash=zero();r.remainingCredit=zero();r.status=BuyReservationStatus.RELEASED;r.updatedAt=Instant.now() }
    @Transactional fun applyExecution(event:ExecutionBookedEvent)=once(event.eventId){if(event.side==Side.BUY)commitBuy(event) else registerSell(event)}
    @Transactional fun onCanceled(event:OrderCanceledEvent)=once(event.eventId){releaseCanceledQuantity(event.orderId,event.canceledQuantity)}
    @Transactional fun onRejected(event:OrderRejectedEvent)=once(event.eventId){releaseBuy(event.orderId)}

    @Transactional fun settleExecution(executionId:String):AccountSettlementResultView{
        val h=holds.findForUpdate(executionId)?:error("settlement hold not found: $executionId")
        if(h.status==SettlementHoldStatus.SETTLED)return AccountSettlementResultView(true,BigDecimal.ZERO,h.grossAmount)
        val e=accounts.findForUpdate(h.accountId)?:error("account not found: ${h.accountId}");val a=e.toDomain()
        return if(h.side==Side.BUY){
            val result=a.settleBuy(FundsReservation(h.cashAmount,h.creditAmount));e.apply(a);h.settledAt=Instant.now()
            h.status=if(result.settled)SettlementHoldStatus.SETTLED else SettlementHoldStatus.OVERDUE
            AccountSettlementResultView(result.settled,result.shortageAmount,h.grossAmount)
        }else{
            a.settleSellReceivable(h.grossAmount);e.apply(a);h.status=SettlementHoldStatus.SETTLED;h.settledAt=Instant.now();AccountSettlementResultView(true,BigDecimal.ZERO,h.grossAmount)
        }
    }

    @Transactional(readOnly=true) fun get(accountId:String):AccountView=accounts.findById(accountId).orElseThrow{NoSuchElementException("account not found: $accountId")}.toView()

    private fun releaseCanceledQuantity(orderId:UUID,canceledQuantity:Long){
        if(canceledQuantity<=0)return;val r=reservations.findForUpdate(orderId)?:return;if(r.status!=BuyReservationStatus.ACTIVE)return
        val unaccounted=r.quantity-r.consumedQuantity-r.canceledQuantity;check(canceledQuantity<=unaccounted)
        val notional=(r.protectedUnitPrice*BigDecimal.valueOf(canceledQuantity)).money();val cash=notional.multiply(r.marginRate).money(RoundingMode.CEILING);val credit=(notional-cash).max(BigDecimal.ZERO).money()
        val e=accounts.findForUpdate(r.accountId)?:error("account not found");val a=e.toDomain();a.release(FundsReservation(cash,credit));e.apply(a)
        r.remainingCash=(r.remainingCash-cash).money();r.remainingCredit=(r.remainingCredit-credit).money();r.canceledQuantity+=canceledQuantity;r.updatedAt=Instant.now();finishReservationIfDone(r,e)
    }

    private fun commitBuy(event:ExecutionBookedEvent){
        val r=reservations.findForUpdate(event.orderId)?:error("buy reservation missing for order ${event.orderId}");check(r.status==BuyReservationStatus.ACTIVE)
        val remaining=r.quantity-r.consumedQuantity-r.canceledQuantity;check(event.quantity in 1..remaining);check(event.price<=r.protectedUnitPrice)
        val e=accounts.findForUpdate(r.accountId)?:error("account not found");val a=e.toDomain()
        val protected=(r.protectedUnitPrice*BigDecimal.valueOf(event.quantity)).money();val pc=protected.multiply(r.marginRate).money(RoundingMode.CEILING);val pcr=(protected-pc).max(BigDecimal.ZERO).money()
        val actual=(event.price*BigDecimal.valueOf(event.quantity)).money();val hold=a.commitBuyForSettlement(FundsReservation(pc,pcr),actual,r.marginRate);e.apply(a)
        holds.save(AccountSettlementHoldEntity(event.executionId,event.orderId,event.accountId,Side.BUY,hold.cash.money(),hold.credit.money(),actual,SettlementHoldStatus.PENDING))
        r.remainingCash=(r.remainingCash-pc).money();r.remainingCredit=(r.remainingCredit-pcr).money();r.consumedQuantity+=event.quantity;r.updatedAt=Instant.now();finishReservationIfDone(r,e)
    }

    private fun registerSell(event:ExecutionBookedEvent){
        val e=accounts.findForUpdate(event.accountId)?:error("account not found: ${event.accountId}");val a=e.toDomain();val gross=(event.price*BigDecimal.valueOf(event.quantity)).money();a.registerSellReceivable(gross);e.apply(a)
        holds.save(AccountSettlementHoldEntity(event.executionId,event.orderId,event.accountId,Side.SELL,zero(),zero(),gross,SettlementHoldStatus.PENDING))
    }

    private fun finishReservationIfDone(r:BuyReservationEntity,e:TradingAccountEntity){
        if(r.consumedQuantity+r.canceledQuantity==r.quantity){if(r.remainingCash>BigDecimal.ZERO||r.remainingCredit>BigDecimal.ZERO){val a=e.toDomain();a.release(FundsReservation(r.remainingCash,r.remainingCredit));e.apply(a);r.remainingCash=zero();r.remainingCredit=zero()};r.status=if(r.canceledQuantity>0)BuyReservationStatus.RELEASED else BuyReservationStatus.CONSUMED}
    }
    private fun once(id:UUID,b:()->Unit){if(processed.existsById(id))return;b();processed.save(AccountProcessedEventEntity(id))}
}

data class ReserveBuyCommand(val orderId:UUID,val accountId:String,val symbol:String,val orderType:OrderType,val quantity:Long,val price:BigDecimal?)
data class AccountSettlementResultView(val settled:Boolean,val shortageAmount:BigDecimal,val grossAmount:BigDecimal)
data class AccountView(val accountId:String,val cashBalance:BigDecimal,val reservedCash:BigDecimal,val availableCash:BigDecimal,val creditLimit:BigDecimal,val usedCredit:BigDecimal,val reservedCredit:BigDecimal,val availableCredit:BigDecimal,val orderableAmount:BigDecimal,val pendingSettlementPayable:BigDecimal,val pendingSettlementReceivable:BigDecimal,val overdueAmount:BigDecimal,val updatedAt:Instant)
private fun TradingAccountEntity.toDomain()=TradingAccount(accountId,cashBalance,reservedCash,creditLimit,usedCredit,reservedCredit,settlementCashHold,settlementCreditHold,pendingSettlementReceivable,overdueAmount)
private fun TradingAccountEntity.apply(a:TradingAccount){cashBalance=a.cashBalance.money();reservedCash=a.reservedCash.money();creditLimit=a.creditLimit.money();usedCredit=a.usedCredit.money();reservedCredit=a.reservedCredit.money();settlementCashHold=a.settlementCashHold.money();settlementCreditHold=a.settlementCreditHold.money();pendingSettlementReceivable=a.pendingSettlementReceivable.money();overdueAmount=a.overdueAmount.money();updatedAt=Instant.now()}
private fun TradingAccountEntity.toView():AccountView{val a=toDomain();return AccountView(accountId,cashBalance,reservedCash,a.availableCash,creditLimit,usedCredit,reservedCredit,a.availableCredit,a.orderableAmount,a.pendingSettlementPayable,a.pendingSettlementReceivable,a.overdueAmount,updatedAt)}
private fun BigDecimal.money(r:RoundingMode=RoundingMode.HALF_UP)=setScale(4,r)
private fun zero()=BigDecimal.ZERO.setScale(4)
