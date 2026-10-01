package com.example.trading.settlementservice.job
import com.example.trading.settlementservice.application.SettlementApplicationService
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
@Component class SettlementScheduler(private val service:SettlementApplicationService,@Value("\${settlement.processing-date-override:}") private val override:String){
    @Scheduled(cron="\${settlement.cron:0 5 16 * * MON-FRI}",zone="Asia/Seoul") fun run(){service.processDue(if(override.isBlank())LocalDate.now() else LocalDate.parse(override))}
}
