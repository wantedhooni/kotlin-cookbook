package com.example.revy.service


import com.example.revy.domain.LegacyUserRepository
import com.example.revy.domain.User
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono


@Service
class LegacyUserService(private val legacyUserRepository: LegacyUserRepository) {

    fun findById(id: String): Mono<User> = legacyUserRepository.findById(id)
}
