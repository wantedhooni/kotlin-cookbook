package com.example.revy.library_app.dto.user.result

import com.example.revy.library_app.domain.user.User

data class UserResult(
    val id: Long,
    val name: String,
    val age: Int?,
) {

    companion object {
        fun of(user: User): UserResult {
            return UserResult(
                id = user.id!!,
                name = user.name,
                age = user.age
            )
        }
    }

}
