package com.example.trading.executionservice.config

import com.example.trading.event.Topics
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

@Configuration
class KafkaErrorConfig {
    @Bean
    fun executionKafkaErrorHandler(template: KafkaTemplate<String, String>): DefaultErrorHandler {
        val recoverer = DeadLetterPublishingRecoverer(template) { record, _ ->
            TopicPartition("${record.topic()}.DLT", record.partition())
        }
        return DefaultErrorHandler(recoverer, FixedBackOff(100, 2))
    }

    @Bean
    fun executionTopic() = NewTopic(Topics.KRX_EXECUTION, 3, 1.toShort())

    @Bean
    fun executionDltTopic() = NewTopic("${Topics.KRX_EXECUTION}.DLT", 3, 1.toShort())
}
