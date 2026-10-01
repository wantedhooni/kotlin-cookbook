package com.example.trading.settlementservice.adapter.`in`.kafka
import com.example.trading.event.*
import com.example.trading.settlementservice.application.SettlementApplicationService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
@Component class ExecutionSettlementListener(private val service:SettlementApplicationService,private val mapper:ObjectMapper){
    @KafkaListener(topics=[Topics.EXECUTION_BOOKED],groupId="settlement-service") fun consume(payload:String)=service.schedule(mapper.readValue(payload,ExecutionBookedEvent::class.java))
}
