package com.example.fintech.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import java.time.Clock

@Configuration
class AppConfig {
    @Bean fun clock(): Clock = Clock.systemUTC()
    /** Demo only: replace with OAuth2/JWT and service-to-service authentication before production. */
    @Bean fun security(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }
        .authorizeHttpRequests { it.requestMatchers("/actuator/health/**").permitAll().anyRequest().permitAll() }
        .build()
}
