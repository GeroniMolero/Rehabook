package com.example.rehabook.models

data class ChatPreview(
    val chatId: String = "",
    val usuario: Usuario = Usuario(),
    val lastTimestamp: Long? = null,
    val lastReadAdmin: Long? = null
)

