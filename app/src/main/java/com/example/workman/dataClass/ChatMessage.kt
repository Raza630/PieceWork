package com.example.workman.dataClass

data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val messageText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
