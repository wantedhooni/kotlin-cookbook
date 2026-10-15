package com.example.accountadmin.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun userDetailsService(
        @Value("\${app.admin.username}") username: String,
        @Value("\${app.admin.password}") password: String,
        encoder: PasswordEncoder
    ): UserDetailsService {
        require(password.length >= 12) { "ADMIN_PASSWORD must have at least 12 characters" }
        return InMemoryUserDetailsManager(
            User.withUsername(username).password(encoder.encode(password)).roles("ADMIN").build()
        )
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        // JSON-only mutation APIs + manually supplied Authorization header; use SSO and CSRF protection in production.
        .csrf { it.disable() }
        .cors { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers("/", "/index.html", "/app.js", "/app.css", "/favicon.ico").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").denyAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().denyAll()
        }
        .httpBasic(Customizer.withDefaults())
        .build()
}
