package com.example.revy.domain

import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

/**
 * Official Spring Data R2DBC coroutine repository - no wrapping needed at all.
 */
interface UserRepository : CoroutineCrudRepository<User, String> {
    override suspend fun findById(id: String): User?
}

interface OrderRepository : CoroutineCrudRepository<Order, String> {

    @Query("""SELECT * FROM "orders" WHERE "user_id" = :userId""")
    fun findByUserId(userId: String): Flow<Order>
}

/**
 * Pre-existing reactive repository, kept as-is to demonstrate bridging
 * unconverted Reactor code from new coroutine endpoints (see LegacyUserService).
 */
interface LegacyUserRepository : ReactiveCrudRepository<User, String> {
    override fun findById(id: String): Mono<User>
}
