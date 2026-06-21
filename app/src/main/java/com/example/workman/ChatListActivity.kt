package com.example.workman

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.workman.screens.ChatListScreen

class ChatListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ChatListScreen(
                    onBack = { finish() },
                    onChatClick = { chatId ->
                        val intent = Intent(this, ChatActivity::class.java).apply {
                            putExtra("CHAT_ID", chatId)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

