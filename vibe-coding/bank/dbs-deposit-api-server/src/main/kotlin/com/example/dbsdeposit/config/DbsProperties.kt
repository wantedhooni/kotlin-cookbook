package com.example.dbsdeposit.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "dbs")
data class DbsProperties(
    val mode: String = "mock",
    val baseUrl: String = "https://www.dbs.com/sandbox",
    val clientId: String = "",
    val accessToken: String = "",
    val acceptVersion: String = "application/json;v=1.0",
    val paths: Paths = Paths(),
    val http: Http = Http(),
    val mockDelay: Duration = Duration.ZERO,
) {
    data class Paths(
        val listDeposits: String = "/api/sg/v1/parties/{partyId}/deposits",
        val depositDetail: String = "/api/sg/v1/deposits/{accountId}",
    )

    data class Http(
        val connectTimeout: Duration = Duration.ofSeconds(3),
        val readTimeout: Duration = Duration.ofSeconds(5),
    )
}

@ConfigurationProperties(prefix = "app.sync")
data class SyncProperties(
    val leaseDuration: Duration = Duration.ofSeconds(30),
)

@ConfigurationProperties(prefix = "app.idempotency")
data class IdempotencyProperties(
    val retention: Duration = Duration.ofHours(24),
    val processingTimeout: Duration = Duration.ofMinutes(1),
)
