package com.example.workman.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workman.ui.theme.BgColor
import com.example.workman.ui.theme.CardBg
import com.example.workman.ui.theme.PrimaryBlue
import com.example.workman.ui.theme.TextDark
import com.example.workman.ui.theme.TextMuted
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "",
    val jobId: String = "",
    val isRead: Boolean = false,
    val timestamp: Any? = null,
    val timeAgo: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNotificationClick: (NotificationItem) -> Unit = {}
) {
    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(userId) {
        if (userId == null) {
            isLoading = false
            return@LaunchedEffect
        }
        try {
            val snapshot = db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            notifications = snapshot.documents.map { doc ->
                val timestamp = doc.getTimestamp("timestamp")
                val timeAgo = timestamp?.let { formatTimeAgo(it.toDate().time) } ?: ""
                NotificationItem(
                    id = doc.id,
                    title = doc.getString("title") ?: "Notification",
                    body = doc.getString("body") ?: "",
                    type = doc.getString("type") ?: "",
                    jobId = doc.getString("jobId") ?: "",
                    isRead = doc.getBoolean("isRead") ?: false,
                    timestamp = timestamp,
                    timeAgo = timeAgo
                )
            }
        } catch (_: Exception) {
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", color = TextDark, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                actions = {
                    if (notifications.any { !it.isRead }) {
                        TextButton(onClick = {
                            // Mark all as read
                            notifications.filter { !it.isRead }.forEach { notif ->
                                db.collection("notifications").document(notif.id)
                                    .update("isRead", true)
                            }
                            notifications = notifications.map { it.copy(isRead = true) }
                        }) {
                            Text("Mark all read", color = PrimaryBlue, fontSize = 12.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = BgColor
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No notifications yet", color = TextMuted, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "You'll see job updates here",
                        color = TextMuted.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications) { notification ->
                    NotificationCard(
                        notification = notification,
                        onClick = {
                            // Mark as read
                            if (!notification.isRead) {
                                db.collection("notifications").document(notification.id)
                                    .update("isRead", true)
                                notifications = notifications.map {
                                    if (it.id == notification.id) it.copy(isRead = true) else it
                                }
                            }
                            onNotificationClick(notification)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationItem,
    onClick: () -> Unit
) {
    val bgColor = if (notification.isRead) CardBg else PrimaryBlue.copy(alpha = 0.05f)
    val iconColor = when (notification.type) {
        "work_accepted" -> Color(0xFF4CAF50)
        "work_completed" -> Color(0xFF2196F3)
        "new_review" -> Color(0xFFFFD700)
        "review_request" -> Color(0xFFFFC107)
        else -> PrimaryBlue
    }
    val icon = when (notification.type) {
        "work_accepted" -> Icons.Default.Check
        "review_request", "new_review" -> Icons.Default.Star
        else -> Icons.Default.Notifications
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(if (notification.isRead) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = notification.title,
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextDark,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = notification.timeAgo,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = notification.body,
                    fontSize = 13.sp,
                    color = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Unread dot
            if (!notification.isRead) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue)
                )
            }
        }
    }
}

private fun formatTimeAgo(timestampMillis: Long): String {
    val diff = System.currentTimeMillis() - timestampMillis
    val minutes = diff / 60000
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}

