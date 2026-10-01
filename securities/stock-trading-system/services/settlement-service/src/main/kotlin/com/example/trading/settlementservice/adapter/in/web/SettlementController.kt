package com.example.trading.settlementservice.adapter.`in`.web
import com.example.trading.settlementservice.application.SettlementApplicationService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
@RestController class SettlementController(private val service:SettlementApplicationService){
    @GetMapping("/api/v1/settlements/accounts/{accountId}") fun account(@PathVariable accountId:String)=service.accountSummary(accountId)
    @PostMapping("/internal/v1/settlements/process") fun process(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) businessDate:LocalDate)=service.processDue(businessDate)
}
