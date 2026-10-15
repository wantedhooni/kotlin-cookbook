package com.example.dbsdeposit.service

import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@Service
class RequestHashService {
    fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
