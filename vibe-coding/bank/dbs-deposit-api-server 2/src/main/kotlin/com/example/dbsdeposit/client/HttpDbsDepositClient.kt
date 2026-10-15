package com.example.dbsdeposit.client

import com.example.dbsdeposit.config.DbsProperties
import com.example.dbsdeposit.support.ProviderCallException
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.util.UUID

@Component
@ConditionalOnProperty(prefix = "dbs", name = ["mode"], havingValue = "http")
class HttpDbsDepositClient(
    private val restClient: RestClient,
    private val props: DbsProperties,
) : DbsDepositClient {

    override fun listDepositAccounts(partyId: String): JsonNode =
        get(props.paths.listDeposits, partyId)

    override fun getDepositAccount(accountId: String): JsonNode =
        get(props.paths.depositDetail, accountId)

    private fun get(path: String, pathVariable: String): JsonNode {
        validateCredentials()
        try {
            return restClient.get()
                .uri(path, pathVariable)
                .accept(MediaType.APPLICATION_JSON)
                .header("clientId", props.clientId)
                .header("accessToken", props.accessToken)
                .header("accept-version", props.acceptVersion)
                .header("uuid", UUID.randomUUID().toString())
                .retrieve()
                .body(JsonNode::class.java)
                ?: throw ProviderCallException("DBS returned an empty response body")
        } catch (e: RestClientResponseException) {
            throw ProviderCallException(
                "DBS call failed: HTTP ${e.statusCode.value()} ${e.statusText}",
                e,
            )
        } catch (e: ProviderCallException) {
            throw e
        } catch (e: Exception) {
            throw ProviderCallException("DBS call failed: ${e.message}", e)
        }
    }

    private fun validateCredentials() {
        require(props.clientId.isNotBlank()) { "DBS_CLIENT_ID is required when DBS_MODE=http" }
        require(props.accessToken.isNotBlank()) { "DBS_ACCESS_TOKEN is required when DBS_MODE=http" }
    }
}
