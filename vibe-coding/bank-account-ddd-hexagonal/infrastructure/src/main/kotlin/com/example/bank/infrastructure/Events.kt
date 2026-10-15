package com.example.bank.infrastructure

import com.example.bank.application.EventPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Repository
class JpaEventAdapter(private val outbox:OutboxJpa):EventPort {
    override fun append(aggregateId:Long,kind:String,referenceId:UUID,amount:BigDecimal) {
        // Stable event ID supports consumer deduplication under at-least-once delivery.
        val eventId=UUID.randomUUID()
        val payload="""{"eventId":"$eventId","type":"$kind","aggregateId":$aggregateId,"referenceId":"$referenceId","amount":"$amount"}"""
        outbox.save(OutboxEntity(id=eventId,aggregateId=aggregateId,eventType=kind,payload=payload))
    }
}
