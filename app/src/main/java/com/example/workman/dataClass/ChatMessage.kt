package com.example.workman.dataClass



data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val messageText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val replyTo: String? = null,                 // Stores messageId of the replied message
    val replyToMessageText: String? = null       // Stores the actual text of the replied message
)









//data class ChatMessage(
//    val messageId: String = "",
//    val senderId: String = "",
//    val messageText: String = "",
//    val timestamp: Long = System.currentTimeMillis(),
//    val isRead: Boolean = false,
//    val replyTo: String? = null // <-- NE
//)
