package com.example.workman

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.workman.screens.NotificationsScreen
import com.example.workman.utils.NavigationUtils

class NotificationsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                NotificationsScreen(
                    onBack = { finish() },
                    onNotificationClick = { notification ->
                        if (notification.jobId.isNotEmpty()) {
                            NavigationUtils.navigateToOfferDetails(this, notification.jobId)
                        }
                    }
                )
            }
        }
    }
}

