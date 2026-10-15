package com.example.bank.infrastructure

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.UUID

/** Durable claim is committed BEFORE network I/O. Reclaimed on lease expiry after a crash. */
@Component
class OutboxPublisher(private val jdbc: JdbcTemplate, private val kafka: KafkaTemplate<String,String>,
    private val tx: TransactionTemplate,
    @Value("\${app.kafka.topic:account-events}") private val topic: String,
    @Value("\${app.kafka.dlq-topic:account-events.DLQ}") private val dlqTopic: String) {
    private val log=LoggerFactory.getLogger(javaClass)
    private val workerId=UUID.randomUUID()
    data class Task(val id:UUID,val aggregateId:Long,val payload:String,val attempts:Int,val dead:Boolean)

    @Scheduled(fixedDelayString="\${app.outbox.poll-ms:2000}")
    fun publishPending() {
        repeat(30) {
            val task=claim() ?: return
            try {
                kafka.send(if(task.dead) dlqTopic else topic,task.aggregateId.toString(),task.payload).get()
                tx.executeWithoutResult {
                    if(task.dead) jdbc.update("UPDATE outbox_events SET dead_lettered_at=NOW(),claimed_by=NULL,claimed_until=NULL WHERE id=? AND claimed_by=?",task.id,workerId)
                    else jdbc.update("UPDATE outbox_events SET published_at=NOW(),claimed_by=NULL,claimed_until=NULL WHERE id=? AND claimed_by=?",task.id,workerId)
                }
            } catch (ex: Exception) {
                log.warn("Outbox send failure id={} attempts={}",task.id,task.attempts,ex)
                val backoff=minOf(3600L, 1L shl minOf(10,task.attempts))
                tx.executeWithoutResult {
                    jdbc.update("""UPDATE outbox_events SET claimed_by=NULL,claimed_until=NULL,
                        next_attempt_at=NOW()+(? * INTERVAL '1 second'), error_message=?
                        WHERE id=? AND claimed_by=?""",backoff,ex.message?.take(500) ?: "Kafka error",task.id,workerId)
                }
            }
        }
    }
    private fun claim():Task? = tx.execute {
        val found=jdbc.query("""SELECT id, aggregate_id, payload, attempts FROM outbox_events
            WHERE published_at IS NULL AND dead_lettered_at IS NULL AND next_attempt_at <= NOW()
            AND (claimed_until IS NULL OR claimed_until < NOW())
            ORDER BY created_at LIMIT 1 FOR UPDATE SKIP LOCKED""") { rs,_ ->
            Task(rs.getObject(1,UUID::class.java),rs.getLong(2),rs.getString(3),rs.getInt(4)+1,rs.getInt(4)>=5)
        }.firstOrNull() ?: return@execute null
        jdbc.update("""UPDATE outbox_events SET claimed_by=?,claimed_until=NOW()+INTERVAL '90 seconds',attempts=attempts+1 WHERE id=?""",workerId,found.id)
        found
    }
}

/** Transactional inbox makes an at-least-once event handler exactly-once for its DB side effect. */
@Component
class AccountEventAuditConsumer(private val jdbc: JdbcTemplate) {
    @KafkaListener(topics=["\${app.kafka.topic:account-events}"],groupId="bank-audit")
    @Transactional
    fun onEvent(record: ConsumerRecord<String,String>) {
        // Check unique event identity extracted from the integration envelope, not Kafka offset.
        val payload=record.value()
        val id=Regex("\"eventId\":\"([a-fA-F0-9-]{36})\"").find(payload)?.groupValues?.get(1)
            ?: throw IllegalArgumentException("eventId missing")
        val inserted=jdbc.update("""INSERT INTO consumed_events(consumer_name,event_id) VALUES('bank-audit',?)
            ON CONFLICT DO NOTHING""",UUID.fromString(id))
        if(inserted==1) jdbc.update("""INSERT INTO audit_logs(subject,action,resource,detail)
            VALUES('kafka:bank-audit','EVENT_CONSUMED',?,?)""",record.key(),payload.take(1000))
    }
}
