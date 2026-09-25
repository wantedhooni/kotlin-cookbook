package com.example.revy.library_app.service

import com.example.revy.library_app.domain.user.User
import com.example.revy.library_app.domain.user.UserRepository
import com.example.revy.library_app.dto.user.command.UserCreateCommand
import com.example.revy.library_app.dto.user.result.UserResult
import org.springframework.stereotype.*
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserHandler(
    private val userRepository: UserRepository,
) {

    @Transactional
    fun saveUser(command: UserCreateCommand) {
        val newUser = User(command.name, command.age)
        userRepository.save(newUser)
    }

    @Transactional(readOnly = true)
    fun getUsers(): List<UserResult> {
        return userRepository.findAll()
            .map { user -> UserResponse.of(user) }
        // .map { UserResponse(it) } // it 으로 참조
        // .map(::UserResponse) // 생성자 호출 (Java: UserResponse::new)
    }

    @Transactional
    fun updateUserName(request: UserUpdateRequest) {
        val user = userRepository.findByIdOrThrow(request.id)
        user.updateName(request.name)
    }

    @Transactional
    fun deleteUser(name: String) {
        val user = userRepository.findByName(name) ?: fail()
        userRepository.delete(user)
    }

    @Transactional(readOnly = true)
    fun getUserLoanHistories(): List<UserLoanHistoryResponse> {
        return userRepository.findAllWithHistories().map(UserLoanHistoryResponse::of)
    }

}