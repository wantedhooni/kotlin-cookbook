package com.example.bank.api

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableMethodSecurity
class SecurityConfiguration {
    @Bean fun filterChain(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests { it.requestMatchers("/actuator/health").permitAll(); it.anyRequest().authenticated() }
        .oauth2ResourceServer { it.jwt { } }
        .build()

    @Bean fun jwtDecoder(@Value("\${app.security.issuer-uri}") issuer: String,
                        @Value("\${app.security.jwk-set-uri}") jwks: String,
                        @Value("\${app.security.audience}") audience: String): JwtDecoder {
        val decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build()
        val issuerValidator = JwtValidators.createDefaultWithIssuer(issuer)
        val audienceValidator = OAuth2TokenValidator<Jwt> { jwt ->
            if (jwt.audience.contains(audience)) OAuth2TokenValidatorResult.success()
            else OAuth2TokenValidatorResult.failure(org.springframework.security.oauth2.core.OAuth2Error("invalid_token", "Required audience missing", null))
        }
        decoder.setJwtValidator(org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator(issuerValidator, audienceValidator))
        return decoder
    }
}
