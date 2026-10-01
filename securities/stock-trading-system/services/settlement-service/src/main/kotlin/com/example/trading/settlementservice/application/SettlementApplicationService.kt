package com.example.trading.settlementservice.application
import com.example.trading.domain.*
import com.example.trading.event.ExecutionBookedEvent
import com.example.trading.settlementservice.adapter.out.http.AccountSettlementPort
import com.example.trading.settlementservice.adapter.out.persistence.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.*

@Service class SettlementApplicationService(
    private val repo:SettlementObligationJpaRepository, private val account:AccountSettlementPort,
    @Value("\${settlement.holidays:}") holidayCsv:String,
){
    private val calendar=WeekendAndHolidayCalendar(holidayCsv.split(',').map{it.trim()}.filter{it.isNotEmpty()}.map{LocalDate.parse(it)}.toSet())

    @Transactional fun schedule(event:ExecutionBookedEvent){
        if(repo.existsByExecutionId(event.executionId))return
        val tradeDate=event.occurredAt.atZone(ZoneId.of("Asia/Seoul")).toLocalDate();val due=calendar.plusBusinessDays(tradeDate,2)
        repo.save(SettlementObligationEntity(executionId=event.executionId,orderId=event.orderId,accountId=event.accountId,side=event.side,
            grossAmount=event.price.multiply(BigDecimal.valueOf(event.quantity)).setScale(4),tradeDate=tradeDate,settlementDate=due))
    }

    @Transactional fun processDue(businessDate:LocalDate):SettlementBatchResult{
        val due=repo.findAllByStatusAndSettlementDateLessThanEqual(SettlementStatus.SCHEDULED,businessDate)
        var settled=0;var overdue=0
        due.forEach { o ->
            val result=runCatching{account.settle(o.executionId)}.getOrElse { return@forEach }
            o.settledAt=Instant.now()
            if(result.settled){o.status=SettlementStatus.SETTLED;o.shortageAmount=BigDecimal.ZERO;settled++}
            else{o.status=SettlementStatus.OVERDUE;o.shortageAmount=result.shortageAmount;overdue++}
        }
        return SettlementBatchResult(due.size,settled,overdue)
    }

    @Transactional(readOnly=true) fun accountSummary(accountId:String):SettlementAccountSummary{
        val rows=repo.findAllByAccountIdOrderBySettlementDateDesc(accountId)
        fun sum(side:Side,status:SettlementStatus)=rows.filter{it.side==side&&it.status==status}.fold(BigDecimal.ZERO){a,b->a+b.grossAmount}
        val overdue=rows.filter{it.status==SettlementStatus.OVERDUE}.fold(BigDecimal.ZERO){a,b->a+b.shortageAmount}
        return SettlementAccountSummary(accountId,sum(Side.BUY,SettlementStatus.SCHEDULED),sum(Side.SELL,SettlementStatus.SCHEDULED),overdue,rows.map{it.toView()})
    }
}
data class SettlementBatchResult(val candidates:Int,val settled:Int,val overdue:Int)
data class SettlementView(val executionId:String,val side:Side,val grossAmount:BigDecimal,val tradeDate:LocalDate,val settlementDate:LocalDate,val status:SettlementStatus,val shortageAmount:BigDecimal)
data class SettlementAccountSummary(val accountId:String,val scheduledPayable:BigDecimal,val scheduledReceivable:BigDecimal,val overdueAmount:BigDecimal,val obligations:List<SettlementView>)
private fun SettlementObligationEntity.toView()=SettlementView(executionId,side,grossAmount,tradeDate,settlementDate,status,shortageAmount)
