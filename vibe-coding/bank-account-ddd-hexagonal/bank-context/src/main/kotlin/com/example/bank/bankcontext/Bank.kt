package com.example.bank.bankcontext

/** Bank bounded context: issuer identity and lifecycle. */
data class Bank(val code: String, val displayName: String, val enabled: Boolean = true) {
    init { require(code.isNotBlank() && displayName.isNotBlank()) }
    fun ensureAvailable() { check(enabled) { "Bank disabled" } }
}
interface BankCatalog { fun findByCode(code: String): Bank? }
