package com.example.dbsdeposit.client

import com.fasterxml.jackson.databind.JsonNode

interface DbsDepositClient {
    fun listDepositAccounts(partyId: String): JsonNode
    fun getDepositAccount(accountId: String): JsonNode
}
