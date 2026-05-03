package com.example.workman

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.workman.screens.ChatScreen
import com.example.workman.viewModels.ChatViewModel

class ChatActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chatId = intent.getStringExtra("CHAT_ID") ?: "G8iV5Ci38lTbj8rt8dw3"
        viewModel.initChat(chatId)

        setContent {
            ChatScreen(
                viewModel = viewModel,
                onBack = { finish() }
            )
        }
    }
}
