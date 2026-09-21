package com.example.revy.library_app.dto.book.command

data class BookLoanCommand(
    val userName: String,
    val bookName: String,
)
