package com.portfolio.payments.settlement

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

data class TransferRequest(val settlementId: String, val sellerId: String, val amount: Long)
data class TransferResponse(val accepted: Boolean, val transferId: String?, val code: String)

interface BankClient { fun transfer(request: TransferRequest): TransferResponse }

@Component
class HttpBankClient(@Value("\${integrations.bank.base-url}") baseUrl: String) : BankClient {
    private val client = RestClient.builder().baseUrl(baseUrl).build()
    override fun transfer(request: TransferRequest): TransferResponse = client.post()
        .uri("/bank/transfers").body(request).retrieve().body(TransferResponse::class.java)!!
}
