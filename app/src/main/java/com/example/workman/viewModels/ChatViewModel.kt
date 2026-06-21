package com.example.workman.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.workman.ProfanityFilter
import com.example.workman.dataClass.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
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
        ensureChatDocument(chatId)
        loadMessages(chatId)
    }

    /**
     * Makes sure the parent chats/{chatId} document exists with a `participants`
     * array, so the conversation shows up in BOTH users' chat lists.
     * The chatId is "uidA_uidB" (sorted), so we derive participants from it.
     * Runs client-side — no Cloud Function required.
     */
    private fun ensureChatDocument(chatId: String) {
        val parts = chatId.split("_")
        if (parts.size != 2) return // not a boss-worker chat id
        val uid1 = parts[0]
        val uid2 = parts[1]
        val chatRef = firestore.collection("chats").document(chatId)

        chatRef.get().addOnSuccessListener { snap ->
            if (snap.exists() && snap.get("participants") != null) return@addOnSuccessListener

            // Fetch both profiles, then create the conversation doc
            firestore.collection("users").document(uid1).get().addOnSuccessListener { u1 ->
                firestore.collection("users").document(uid2).get().addOnSuccessListener { u2 ->
                    val data = hashMapOf(
                        "participants" to listOf(uid1, uid2),
                        "participantNames" to mapOf(
                            uid1 to (u1.getString("name") ?: u1.getString("firstName") ?: "User"),
                            uid2 to (u2.getString("name") ?: u2.getString("firstName") ?: "User")
                        ),
                        "participantPhotos" to mapOf(
                            uid1 to (u1.getString("photoUrl") ?: ""),
                            uid2 to (u2.getString("photoUrl") ?: "")
                        ),
                        "lastActivity" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    chatRef.set(data, SetOptions.merge())
                }
            }
        }
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
                updateChatMeta(cid, filteredText)
                _uiState.value = _uiState.value.copy(replyTo = null)
            }
            .addOnFailureListener {
                _uiState.value = _uiState.value.copy(error = "Failed to send message")
            }
    }

    /** Updates the conversation preview + ordering fields (creates doc if missing). */
    private fun updateChatMeta(chatId: String, lastMessage: String) {
        firestore.collection("chats")
            .document(chatId)
            .set(
                mapOf(
                    "lastMessage" to lastMessage,
                    "lastMessageTime" to System.currentTimeMillis(),
                    "lastActivity" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .addOnFailureListener { Log.w("ChatViewModel", "Failed to update chat meta", it) }
    }

    fun setReplyTo(message: ChatMessage?) {
        _uiState.value = _uiState.value.copy(replyTo = message)
    }

    override fun onCleared() {
        messagesListener?.remove()
        super.onCleared()
    }
}
