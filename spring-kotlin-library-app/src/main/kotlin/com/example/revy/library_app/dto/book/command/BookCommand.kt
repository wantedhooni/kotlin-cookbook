package com.example.revy.library_app.dto.book.command

import com.example.revy.library_app.common.enums.BookType


data class BookCommand(
    val name: String,
    val type: BookType,
)
