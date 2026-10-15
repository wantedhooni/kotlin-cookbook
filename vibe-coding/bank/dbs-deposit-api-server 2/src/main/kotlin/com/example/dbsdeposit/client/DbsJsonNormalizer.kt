package com.example.dbsdeposit.client

import com.example.dbsdeposit.api.model.NormalizedDepositAccount
import com.example.dbsdeposit.support.ProviderCallException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

@Component
class DbsJsonNormalizer(
    private val objectMapper: ObjectMapper,
) {
    fun normalizeList(root: JsonNode): List<NormalizedDepositAccount> {
        val nodes = when {
            root.isArray -> root.toList()
            root.path("depositAccounts").isArray -> root.path("depositAccounts").toList()
            root.path("accounts").isArray -> root.path("accounts").toList()
            root.path("deposits").isArray -> root.path("deposits").toList()
            else -> throw ProviderCallException(
                "Unsupported DBS Deposit response shape. Configure/extend DbsJsonNormalizer for the sandbox schema.",
            )
        }
        return nodes.map(::normalizeAccount)
    }

    fun normalizeAccount(node: JsonNode): NormalizedDepositAccount {
        val accountId = firstText(node, "accountId", "depositAccountId", "id", "accountIdentifier")
            ?: throw ProviderCallException("Deposit account response has no recognizable account identifier")

        val balanceNode = firstNode(node, "availableBalance", "balance", "currentBalance")
        val amount = when {
            balanceNode == null -> null
            balanceNode.isNumber -> balanceNode.decimalValue()
            balanceNode.isTextual -> balanceNode.asText().toBigDecimalOrNull()
            else -> firstNode(balanceNode, "amount", "value")?.asText()?.toBigDecimalOrNull()
        }

        val currency = firstText(node, "currency", "currencyCode")
            ?: firstText(balanceNode, "currency", "currencyCode")

        return NormalizedDepositAccount(
            accountId = accountId,
            accountNumberMasked = firstText(node, "maskedAccountNumber", "accountNumber", "displayAccountNumber"),
            productName = firstText(node, "productName", "accountName", "name"),
            productType = firstText(node, "productType", "accountType", "type"),
            currency = currency?.take(3)?.uppercase(),
            availableBalance = amount,
            providerUpdatedAt = firstText(node, "lastUpdated", "updatedAt", "lastUpdateDateTime")?.toInstantOrNull(),
            rawJson = objectMapper.writeValueAsString(node),
        )
    }

    private fun firstText(node: JsonNode?, vararg names: String): String? {
        if (node == null) return null
        return names.asSequence()
            .map { node.path(it) }
            .firstOrNull { !it.isMissingNode && !it.isNull && it.isValueNode }
            ?.asText()
            ?.takeIf { it.isNotBlank() }
    }

    private fun firstNode(node: JsonNode?, vararg names: String): JsonNode? {
        if (node == null) return null
        return names.asSequence()
            .map { node.path(it) }
            .firstOrNull { !it.isMissingNode && !it.isNull }
    }

    private fun String.toInstantOrNull(): Instant? = runCatching { Instant.parse(this) }.getOrNull()
}
