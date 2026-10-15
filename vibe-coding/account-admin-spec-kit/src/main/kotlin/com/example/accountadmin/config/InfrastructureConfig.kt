package com.example.accountadmin.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.kafka.config.TopicBuilder
import java.time.Duration

@Configuration
@EnableCaching
class InfrastructureConfig {
    @Bean
    fun cacheManager(factory: RedisConnectionFactory): RedisCacheManager {
        val config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .disableCachingNullValues()
        return RedisCacheManager.builder(factory).cacheDefaults(config).build()
    }

    @Bean
    fun accountAuditTopic(@Value("\${app.audit.topic}") name: String): NewTopic =
        TopicBuilder.name(name).partitions(1).replicas(1).build()
}
