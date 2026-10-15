package com.example.fintech.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

/** Only for local demo. Never enable demo profile on an Internet-facing service. */
@Configuration
@Profile("demo")
class DemoSecurityConfig {
 @Bean fun demoFilterChain(http: HttpSecurity): SecurityFilterChain = http
  .csrf { it.disable() }
  .authorizeHttpRequests { it.anyRequest().permitAll() }
  .build()
}
