package com.portfolio.payments.payment

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

data class VanAuthRequest(val paymentId: String, val amount: Long, val cardToken: String)
data class VanAuthResponse(val approved: Boolean, val vanTransactionId: String?, val code: String)
data class VanCancelRequest(val paymentId: String, val vanTransactionId: String, val reason: String)

interface VanClient {
    fun authorize(request: VanAuthRequest): VanAuthResponse
    fun cancel(request: VanCancelRequest)
}

@Component
class HttpVanClient(
    @Value("\${integrations.van.base-url}") baseUrl: String,
) : VanClient {
    private val client = RestClient.builder().baseUrl(baseUrl).build()

    override fun authorize(request: VanAuthRequest): VanAuthResponse = client.post()
        .uri("/van/authorizations")
        .contentType(MediaType.APPLICATION_JSON)
        .body(request)
        .retrieve()
        .body(VanAuthResponse::class.java)!!

    override fun cancel(request: VanCancelRequest) {
        client.post().uri("/van/cancellations")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve().toBodilessEntity()
    }
}
