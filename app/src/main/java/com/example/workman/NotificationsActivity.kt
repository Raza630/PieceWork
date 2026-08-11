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
                        when {
                            // Job-related notifications (job accepted / completed /
                            // review requested / urgent job) open that job.
                            notification.jobId.isNotEmpty() ->
                                NavigationUtils.navigateToOfferDetails(this, notification.jobId)

                            // A review left about you is shown on your profile.
                            notification.type == "new_review" ->
                                NavigationUtils.navigateToProfile(this)

                            // Nothing specific to open — stay on the list.
                            else -> Unit
                        }
                    }
                )
            }
        }
    }
}

