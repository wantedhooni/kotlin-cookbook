package com.example.dbsdeposit.client

import com.example.dbsdeposit.config.DbsProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

@Component
@ConditionalOnProperty(prefix = "dbs", name = ["mode"], havingValue = "mock", matchIfMissing = true)
class MockDbsDepositClient(
    private val objectMapper: ObjectMapper,
    private val props: DbsProperties,
) : DbsDepositClient {

    override fun listDepositAccounts(partyId: String): JsonNode {
        sleepIfConfigured()
        val suffix = partyId.takeLast(4).padStart(4, '0')
        return objectMapper.valueToTree(
            mapOf(
                "depositAccounts" to listOf(
                    mapOf(
                        "accountId" to "dep-$suffix-001",
                        "accountNumber" to "****$suffix",
                        "productName" to "DBS Multiplier Account",
                        "accountType" to "MULTIPLIER",
                        "currency" to "SGD",
                        "availableBalance" to mapOf("amount" to BigDecimal("12500.25"), "currency" to "SGD"),
                        "lastUpdated" to Instant.now().toString(),
                    ),
                    mapOf(
                        "accountId" to "dep-$suffix-002",
                        "accountNumber" to "****${suffix.reversed()}",
                        "productName" to "DBS Multi-Currency Account",
                        "accountType" to "MULTI_CURRENCY",
                        "currency" to "USD",
                        "availableBalance" to mapOf("amount" to BigDecimal("845.10"), "currency" to "USD"),
                        "lastUpdated" to Instant.now().toString(),
                    ),
                ),
            ),
        )
    }

    override fun getDepositAccount(accountId: String): JsonNode {
        sleepIfConfigured()
        return objectMapper.valueToTree(
            mapOf(
                "accountId" to accountId,
                "productName" to "Mock Deposit Account",
                "accountType" to "SAVINGS",
                "currency" to "SGD",
                "availableBalance" to mapOf("amount" to BigDecimal("1000.00"), "currency" to "SGD"),
                "lastUpdated" to Instant.now().toString(),
            ),
        )
    }

    private fun sleepIfConfigured() {
        if (!props.mockDelay.isZero && !props.mockDelay.isNegative) {
            Thread.sleep(props.mockDelay.toMillis())
        }
    }
}
