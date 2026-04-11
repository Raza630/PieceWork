package com.example.workman

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.workman.adaptes.ChatAdapter
import com.example.workman.dataClass.ChatMessage
import com.example.workman.databinding.ActivityChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject

class ChatActivity : BaseBottomNavigationActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private val messagesList = mutableListOf<ChatMessage>()
    private var messagesListener: ListenerRegistration? = null
    private var currentReplyMessage: ChatMessage? = null  // Stores message being replied to

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private lateinit var chatId: String
    private val currentUserId: String by lazy {
        intent.getStringExtra("USER_ID") ?: auth.currentUser?.uid
        ?: run {
            showToast("User not authenticated")
            finish()
            ""
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ProfanityFilter.loadProfanityList(this)

        setupBottomNavigation()
        updateBottomNavigationSelection(binding.bottomNavigation, R.id.nav_Chat)


        chatId = intent.getStringExtra("CHAT_ID") ?: run {
            showToast("Chat room not specified")
            finish()
            return
        }

        if (currentUserId.isEmpty()) return

        setupRecyclerView()
        setupClickListeners()
        setupReplyPreview()
        loadMessages()
    }

    private fun setupBottomNavigation() {
        setupBottomNavigation(binding.bottomNavigation)
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(currentUserId)

        // Long press callback to select message for reply
        chatAdapter.onMessageLongClick = { message ->
            currentReplyMessage = message
            showReplyPreview(message)
        }

        binding.recyclerChat.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
                reverseLayout = false
            }
            adapter = chatAdapter
            setHasFixedSize(true)
            itemAnimator = DefaultItemAnimator()

            addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
                if (bottom < oldBottom) {
                    postDelayed({
                        if (chatAdapter.itemCount > 0) {
                            smoothScrollToPosition(chatAdapter.itemCount - 1)
                        }
                    }, 100)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonSend.setOnClickListener { sendMessage() }

        binding.editTextMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }

        binding.replyCancel.setOnClickListener {
            hideReplyPreview()
        }
    }

    private fun setupReplyPreview() {
        // Add a small layout at bottom to show reply (in XML you can create binding.replyLayout)
        binding.replyLayout.visibility = View.GONE
    }

    private fun showReplyPreview(message: ChatMessage) {
        binding.replyLayout.visibility = View.VISIBLE
        binding.replySenderName.text =
            if (message.senderId == currentUserId) "You replied to:" else "Other User replied to:"
        binding.replyMessageText.text = message.messageText
    }

    private fun hideReplyPreview() {
        currentReplyMessage = null
        binding.replyLayout.visibility = View.GONE
    }

    private fun loadMessages() {
        messagesListener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                when {
                    error != null -> {
                        Log.e("ChatActivity", "Listen failed", error)
                        showToast("Error loading messages")
                        return@addSnapshotListener
                    }

                    snapshots == null -> {
                        Log.w("ChatActivity", "Messages snapshot is null")
                        return@addSnapshotListener
                    }

                    else -> {
                        val newMessages = snapshots.documents.mapNotNull { doc ->
                            doc.toObject<ChatMessage>()?.copy(messageId = doc.id)
                        }

                        if (newMessages != messagesList) {
                            messagesList.clear()
                            messagesList.addAll(newMessages)
                            chatAdapter.submitList(newMessages.toList())
                        }

                        scrollToBottomIfNeeded()
                    }
                }
            }
    }

    private fun sendMessage() {
        val originalMessage = binding.editTextMessage.text.toString().trim()
        if (originalMessage.isEmpty()) {
            binding.editTextMessage.error = "Message cannot be empty"
            return
        }

        val finalMessage = if (ProfanityFilter.containsProfanity(originalMessage)) {
            ProfanityFilter.cleanMessage(originalMessage)
        } else originalMessage

        binding.buttonSend.isEnabled = false

        val newMessage = hashMapOf(
            "senderId" to currentUserId,
            "messageText" to finalMessage,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false,
            "replyToMessageId" to currentReplyMessage?.messageId,
            "replyToMessageText" to currentReplyMessage?.messageText
        )

        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(newMessage)
            .addOnCompleteListener { task ->
                binding.buttonSend.isEnabled = true
                if (task.isSuccessful) {
                    binding.editTextMessage.text?.clear()
                    updateLastActivityTime()
                    hideReplyPreview() // Reset reply after sending
                } else {
                    showToast("Failed to send message")
                    Log.e("ChatActivity", "Send failed", task.exception)
                }
            }
    }

    private fun scrollToBottomIfNeeded() {
        if (messagesList.isNotEmpty() && isLastItemVisible()) {
            binding.recyclerChat.post {
                binding.recyclerChat.smoothScrollToPosition(messagesList.size - 1)
            }
        }
    }

    private fun isLastItemVisible(): Boolean {
        val layoutManager = binding.recyclerChat.layoutManager as LinearLayoutManager
        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
        return lastVisiblePosition >= messagesList.size - 2
    }

    private fun updateLastActivityTime() {
        firestore.collection("chats")
            .document(chatId)
            .update("lastActivity", FieldValue.serverTimestamp())
            .addOnFailureListener { e ->
                Log.w("ChatActivity", "Failed to update last activity", e)
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        messagesListener?.remove()
        super.onDestroy()
    }
    override fun onResume() {
        super.onResume()
        updateBottomNavigationSelection(binding.bottomNavigation, R.id.nav_Chat)
    }
}






//class ChatActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityChatBinding
//    private lateinit var chatAdapter: ChatAdapter
//    private val messagesList = mutableListOf<ChatMessage>()
//    private var messagesListener: ListenerRegistration? = null
//    private var currentReplyMessage: ChatMessage? = null
//
//    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
//    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
//
//    private lateinit var chatId: String
//    private val currentUserId: String by lazy {
//        intent.getStringExtra("USER_ID") ?: auth.currentUser?.uid
//        ?: run {
//            showToast("User not authenticated")
//            finish()
//            ""
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityChatBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//        ProfanityFilter.loadProfanityList(this)
//
//        chatId = intent.getStringExtra("CHAT_ID") ?: run {
//            showToast("Chat room not specified")
//            finish()
//            return
//        }
//
//        if (currentUserId.isEmpty()) return
//
//        setupRecyclerView()
//        setupClickListeners()
//        loadMessages()
//    }
//
//    private fun setupRecyclerView() {
//        // Initialize adapter with just currentUserId (remove messagesList parameter)
//        chatAdapter = ChatAdapter(currentUserId)
//
//        binding.recyclerChat.apply {
//            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
//                stackFromEnd = true  // New messages appear at bottom
//                reverseLayout = false
//            }
//            adapter = chatAdapter
//            setHasFixedSize(true)
//            itemAnimator = DefaultItemAnimator()
//
//            // Smooth scroll to bottom when keyboard appears
//            addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
//                if (bottom < oldBottom) {
//                    postDelayed({
//                        if (chatAdapter.itemCount > 0) {
//                            smoothScrollToPosition(chatAdapter.itemCount - 1)
//                        }
//                    }, 100)
//                }
//            }
//        }
//    }
//
////    private fun setupRecyclerView() {
////        chatAdapter = ChatAdapter(messagesList, currentUserId)
////
////        binding.recyclerChat.apply {
////            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
////                stackFromEnd = true
////                reverseLayout = false
////            }
////            adapter = chatAdapter
////            setHasFixedSize(true)
////            itemAnimator = DefaultItemAnimator()
////
////            // Smooth scroll to bottom when keyboard appears
////            addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
////                if (bottom < oldBottom) {
////                    postDelayed({ smoothScrollToPosition(messagesList.size - 1) }, 100)
////                }
////            }
////        }
////    }
//
//    private fun setupClickListeners() {
//        binding.buttonSend.setOnClickListener { sendMessage() }
//
//        binding.editTextMessage.setOnEditorActionListener { _, actionId, _ ->
//            if (actionId == EditorInfo.IME_ACTION_SEND) {
//                sendMessage()
//                true
//            } else {
//                false
//            }
//        }
//    }
//
//    private fun loadMessages() {
//        messagesListener = firestore.collection("chats")
//            .document(chatId)
//            .collection("messages")
//            .orderBy("timestamp", Query.Direction.ASCENDING)
//            .addSnapshotListener { snapshots, error ->
//                when {
//                    error != null -> {
//                        Log.e("ChatActivity", "Listen failed", error)
//                        showToast("Error loading messages")
//                        return@addSnapshotListener
//                    }
//
//                    snapshots == null -> {
//                        Log.w("ChatActivity", "Messages snapshot is null")
//                        return@addSnapshotListener
//                    }
//
//                    else -> {
//                        val newMessages = snapshots.documents.mapNotNull { doc ->
//                            doc.toObject<ChatMessage>()?.copy(messageId = doc.id)
//                        }
//
//                        // Only update if messages actually changed
//                        if (newMessages != messagesList) {
//                            messagesList.clear()
//                            messagesList.addAll(newMessages)
//                            chatAdapter.submitList(newMessages.toList()) // Create new list instance
//                        }
//
//                        scrollToBottomIfNeeded()
//                    }
//                }
//            }
//    }
//
//
////    private fun loadMessages() {
////        messagesListener = firestore.collection("chats")
////            .document(chatId)
////            .collection("messages")
////            .orderBy("timestamp", Query.Direction.ASCENDING)
////            .addSnapshotListener { snapshots, error ->
////                when {
////                    error != null -> {
////                        Log.e("ChatActivity", "Listen failed", error)
////                        showToast("Error loading messages")
////                        return@addSnapshotListener
////                    }
////                    snapshots == null -> {
////                        Log.w("ChatActivity", "Messages snapshot is null")
////                        return@addSnapshotListener
////                    }
////
////                    else -> {
////                        val newMessages = snapshots.documents.mapNotNull { doc ->
////                            doc.toObject<ChatMessage>()?.copy(messageId = doc.id)
////                        }
////
////
////                        messagesList.apply {
////                            clear()
////                            addAll(newMessages)
////                        }
////
////                        chatAdapter.notifyDataSetChanged()
////                        chatAdapter.submitList(newMessages)
////                        scrollToBottomIfNeeded()
////
////                    }
////                }
////            }
////    }
//
//    private fun sendMessage() {
//        val originalMessage = binding.editTextMessage.text.toString().trim()
//        if (originalMessage.isEmpty()) {
//            binding.editTextMessage.error = "Message cannot be empty"
//            return
//        }
//
//        // Check and clean profanity
//        val finalMessage = if (ProfanityFilter.containsProfanity(originalMessage)) {
//            ProfanityFilter.cleanMessage(originalMessage)
//        } else {
//            originalMessage
//        }
//
//        binding.buttonSend.isEnabled = false
//
//        val newMessage = hashMapOf(
//            "senderId" to currentUserId,
//            "messageText" to finalMessage,
//            "timestamp" to System.currentTimeMillis(),
//            "isRead" to false,
//            "replyTo" to currentReplyMessage?.messageId  // 🔹 added
//        )
//
//        firestore.collection("chats")
//            .document(chatId)
//            .collection("messages")
//            .add(newMessage)
//            .addOnCompleteListener { task ->
//                binding.buttonSend.isEnabled = true
//
//                if (task.isSuccessful) {
//                    binding.editTextMessage.text?.clear()
//                    updateLastActivityTime()
//                    currentReplyMessage = null
//                } else {
//                    showToast("Failed to send message")
//                    Log.e("ChatActivity", "Send failed", task.exception)
//                }
//            }
//    }
//
//
////    private fun sendMessage() {
////        val messageText = binding.editTextMessage.text.toString().trim()
////        if (messageText.isEmpty()) return
////
////        val newMessage = hashMapOf(
////            "senderId" to currentUserId,
////            "messageText" to messageText,
////            "timestamp" to System.currentTimeMillis(),
////            "isRead" to false,
////            "replyTo" to currentReplyMessage?.messageId  // 🔹 added
////        )
////
////        firestore.collection("chats")
////            .document(chatId)
////            .collection("messages")
////            .add(newMessage)
////            .addOnSuccessListener {
////                binding.editTextMessage.text?.clear()
////                currentReplyMessage = null // reset reply after sending
////            }
////    }
//
//
////    private fun sendMessage() {
////        val messageText = binding.editTextMessage.text.toString().trim()
////        if (messageText.isEmpty()) {
////            binding.editTextMessage.error = "Message cannot be empty"
////            return
////        }
////
////        binding.buttonSend.isEnabled = false
////
////        val newMessage = hashMapOf(
////            "senderId" to currentUserId,
////            "messageText" to messageText,
////            "timestamp" to System.currentTimeMillis(),
////            "isRead" to false
////        )
////
////        firestore.collection("chats")
////            .document(chatId)
////            .collection("messages")
////            .add(newMessage)
////            .addOnCompleteListener { task ->
////                binding.buttonSend.isEnabled = true
////
////                if (task.isSuccessful) {
////                    binding.editTextMessage.text?.clear()
////                    updateLastActivityTime()
////                } else {
////                    showToast("Failed to send message")
////                    Log.e("ChatActivity", "Send failed", task.exception)
////                }
////            }
////    }
//
//    private fun scrollToBottomIfNeeded() {
//        if (messagesList.isNotEmpty() && isLastItemVisible()) {
//            binding.recyclerChat.post {
//                binding.recyclerChat.smoothScrollToPosition(messagesList.size - 1)
//            }
//        }
//    }
//
//    private fun isLastItemVisible(): Boolean {
//        val layoutManager = binding.recyclerChat.layoutManager as LinearLayoutManager
//        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
//        return lastVisiblePosition >= messagesList.size - 2 // -2 for better UX
//    }
//
//    private fun updateLastActivityTime() {
//        firestore.collection("chats")
//            .document(chatId)
//            .update("lastActivity", FieldValue.serverTimestamp())
//            .addOnFailureListener { e ->
//                Log.w("ChatActivity", "Failed to update last activity", e)
//            }
//    }
//
//    private fun showToast(message: String) {
//        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
//    }
//
//    override fun onDestroy() {
//        messagesListener?.remove()
//        super.onDestroy()
//    }
//}




















//class ChatActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityChatBinding
//    private lateinit var chatAdapter: ChatAdapter
//    private val messagesList = mutableListOf<ChatMessage>()
//
//    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
//    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
//
//    private lateinit var chatId: String
//    private val userId: String by lazy {
//        intent.getStringExtra("USER_ID") ?: auth.currentUser?.uid
//        ?: throw IllegalStateException("User ID not found")
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityChatBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        chatId = intent.getStringExtra("CHAT_ID")
//            ?: throw IllegalStateException("Chat ID not provided")
//
//        setupRecyclerView()
//        setupClickListeners()
//        loadMessages()
//    }
//
//    private fun setupRecyclerView() {
//        chatAdapter = ChatAdapter(messagesList, userId).apply {
//            // Add item click listeners if needed
//        }
//
//        with(binding.recyclerChat) {
//            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
//                stackFromEnd = true // Start from bottom
//            }
//            adapter = chatAdapter
//            setHasFixedSize(true)
//        }
//    }
//
//    private fun setupClickListeners() {
//        binding.buttonSend.setOnClickListener {
//            sendMessage()
//        }
//
//        // Optional: Send on Enter key press
//        binding.editTextMessage.setOnEditorActionListener { _, _, _ ->
//            sendMessage()
//            true
//        }
//    }
//
//    private fun loadMessages() {
//        firestore.collection("chats")
//            .document(chatId)
//            .collection("messages")
//            .orderBy("timestamp", Query.Direction.ASCENDING) // Show oldest first
//            .addSnapshotListener { snapshots, error ->
//                if (error != null) {
//                    Log.e("ChatActivity", "Listen failed", error)
//                    return@addSnapshotListener
//                }
//
//                snapshots?.let { querySnapshot ->
//                    val newMessages = querySnapshot.documents.mapNotNull { document ->
//                        document.toObject<ChatMessage>()?.copy(messageId = document.id)
//                    }
//
//                    messagesList.clear()
//                    messagesList.addAll(newMessages)
//                    chatAdapter.notifyDataSetChanged()
//
//                    // Scroll to bottom only if user hasn't manually scrolled up
//                    if (messagesList.isNotEmpty() && isLastItemVisible()) {
//                        binding.recyclerChat.smoothScrollToPosition(messagesList.size - 1)
//                    }
//                }
//            }
//    }
//
//    private fun sendMessage() {
//        val messageText = binding.editTextMessage.text.toString().trim()
//        if (messageText.isEmpty()) return
//
//        val newMessage = ChatMessage(
//            messageId = "", // Will be generated by Firestore
//            senderId = userId,
//            messageText = messageText,
//            timestamp = System.currentTimeMillis(),
//            isRead = false
//        )
//
//        // Add to Firestore
//        firestore.collection("chats")
//            .document(chatId)
//            .collection("messages")
//            .add(newMessage)
//            .addOnSuccessListener {
//                binding.editTextMessage.text.clear()
//                updateLastActivityTime()
//            }
//            .addOnFailureListener { e ->
//                Log.e("ChatActivity", "Error sending message", e)
//                // Show error to user
//            }
//    }
//
//    private fun updateLastActivityTime() {
//        firestore.collection("chats")
//            .document(chatId)
//            .update("lastActivity", FieldValue.serverTimestamp())
//            .addOnFailureListener { e ->
//                Log.e("ChatActivity", "Error updating last activity", e)
//            }
//    }
//
//    private fun isLastItemVisible(): Boolean {
//        val layoutManager = binding.recyclerChat.layoutManager as LinearLayoutManager
//        val visibleItemCount = layoutManager.childCount
//        val totalItemCount = layoutManager.itemCount
//        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
//
//        return (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 1
//    }
//
//    override fun onDestroy() {
//        // Clean up any listeners if needed
//        super.onDestroy()
//    }
//}














//package com.example.workman


//
//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle
//import android.util.Log
//import android.widget.Button
//import android.widget.EditText
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.example.workman.adaptes.ChatAdapter
//import com.example.workman.dataClass.ChatMessage
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//
//class ChatActivity : AppCompatActivity() {
//
//    private lateinit var recyclerChat: RecyclerView
//    private lateinit var editTextMessage: EditText
//    private lateinit var buttonSend: Button
//    private lateinit var chatAdapter: ChatAdapter
//    private val messagesList = mutableListOf<ChatMessage>()
//
//    private lateinit var firestore: FirebaseFirestore
//    private lateinit var chatId: String
//    private lateinit var userId: String // Set this based on the logged-in user
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_chat)
//
//        recyclerChat = findViewById(R.id.recyclerChat)
//        editTextMessage = findViewById(R.id.editTextMessage)
//        buttonSend = findViewById(R.id.buttonSend)
//
//        firestore = FirebaseFirestore.getInstance()
//        chatId = "G8iV5Ci38lTbj8rt8dw3" // Example chatId
//
//        userId = intent.getStringExtra("USER_ID") ?: FirebaseAuth.getInstance().currentUser?.uid
//                ?: throw IllegalStateException("User ID not found")
//
//        setupRecyclerView()
//        loadMessages()
//
//        buttonSend.setOnClickListener {
//            sendMessage()
//        }
//    }
//    private fun setupRecyclerView() {
//        chatAdapter = ChatAdapter(messagesList, userId)
//        recyclerChat.layoutManager = LinearLayoutManager(this)
//        recyclerChat.adapter = chatAdapter
//    }
//
//    private fun loadMessages() {
//        firestore.collection("chats")
//            .document(chatId)
//            .collection("messages")
//            .orderBy("timestamp")
//            .addSnapshotListener { snapshots, e ->
//                if (e != null) {
//                    Log.w("ChatActivity", "Listen failed.", e)
//                    return@addSnapshotListener
//                }
//
//                for (doc in snapshots!!) {
//                    val message = doc.toObject(ChatMessage::class.java)
//                    messagesList.add(message)
//                }
//                chatAdapter.notifyDataSetChanged()
//                recyclerChat.scrollToPosition(messagesList.size - 1)
//            }
//    }
//
//    private fun sendMessage() {
//        val messageText = editTextMessage.text.toString()
//        if (messageText.isNotEmpty()) {
//            val newMessage = ChatMessage(
//                senderId = userId,
//                messageText = messageText,
//                timestamp = System.currentTimeMillis(),
//                isRead = false
//            )
//
//            firestore.collection("chats")
//                .document(chatId)
//                .collection("messages")
//                .add(newMessage)
//                .addOnSuccessListener {
//                    editTextMessage.text.clear()
//                    recyclerChat.scrollToPosition(messagesList.size - 1)
//                }
//                .addOnFailureListener { e ->
//                    Log.e("ChatActivity", "Error sending message", e)
//                }
//        }
//    }
//}