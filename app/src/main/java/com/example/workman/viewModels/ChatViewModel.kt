package com.example.workman.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.workman.dataClass.ChatMessage
import com.example.workman.ProfanityFilter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val replyTo: ChatMessage? = null
)

class ChatViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    private var messagesListener: ListenerRegistration? = null
    private var chatId: String? = null

    val currentUserId: String? get() = auth.currentUser?.uid

    fun initChat(chatId: String) {
        this.chatId = chatId
        loadMessages(chatId)
    }

    private fun loadMessages(chatId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        messagesListener?.remove()
        
        messagesListener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.message)
                    return@addSnapshotListener
                }

                val messages = snapshots?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(messageId = doc.id)
                } ?: emptyList()

                _uiState.value = _uiState.value.copy(messages = messages, isLoading = false)
            }
    }

    fun sendMessage(text: String) {
        val cid = chatId ?: return
        val uid = currentUserId ?: return
        if (text.isBlank()) return

        val filteredText = if (ProfanityFilter.containsProfanity(text)) {
            ProfanityFilter.cleanMessage(text)
        } else text

        val reply = _uiState.value.replyTo
        val newMessage = hashMapOf(
            "senderId" to uid,
            "messageText" to filteredText,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false,
            "replyToMessageId" to reply?.messageId,
            "replyToMessageText" to reply?.messageText
        )

        firestore.collection("chats")
            .document(cid)
            .collection("messages")
            .add(newMessage)
            .addOnSuccessListener {
                updateLastActivityTime(cid)
                _uiState.value = _uiState.value.copy(replyTo = null)
            }
            .addOnFailureListener {
                _uiState.value = _uiState.value.copy(error = "Failed to send message")
            }
    }

    private fun updateLastActivityTime(chatId: String) {
        firestore.collection("chats")
            .document(chatId)
            .update("lastActivity", FieldValue.serverTimestamp())
    }

    fun setReplyTo(message: ChatMessage?) {
        _uiState.value = _uiState.value.copy(replyTo = message)
    }

    override fun onCleared() {
        messagesListener?.remove()
        super.onCleared()
    }
}
