package com.example.dbsdeposit.api

import com.example.dbsdeposit.client.DbsDepositClient
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/provider/deposits")
class ProviderController(
    private val client: DbsDepositClient,
) {
    @GetMapping("/{accountId}")
    fun getLive(@PathVariable accountId: String): JsonNode = client.getDepositAccount(accountId)
}
