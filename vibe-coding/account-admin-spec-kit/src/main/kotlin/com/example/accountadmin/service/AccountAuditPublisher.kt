package com.example.accountadmin.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AccountAuditPublisher(
    private val kafkaTemplate: KafkaTemplate<String, AccountAuditEvent>,
    @Value("\${app.audit.topic}") private val topic: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun publish(event: AccountAuditEvent) {
        try {
            kafkaTemplate.send(topic, event.accountId.toString(), event)
                .whenComplete { _, error ->
                    if (error != null) log.error("Audit publish failed for account {}", event.accountId, error)
                }
        } catch (ex: Exception) {
            // Publish is best-effort for this starter; a transactional outbox is needed for guaranteed delivery.
            log.error("Audit publish failed for account {}", event.accountId, ex)
        }
    }
}
