package com.example.workman.screens

import android.content.Context
import android.util.Log
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workman.R
import com.example.workman.ui.theme.BgColor
import com.example.workman.ui.theme.CardBg
import com.example.workman.ui.theme.PrimaryBlue
import com.example.workman.ui.theme.TextDark
import com.example.workman.ui.theme.TextMuted
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "",
    val jobId: String = "",
    val isRead: Boolean = false,
    val timestampMillis: Long = 0L,
    val timeAgo: String = ""
)

private const val TAG = "NotificationsScreen"

/**
 * Live unread-notification count for the signed-in user.
 *
 * Counts client-side from the same `recipientId` query the list uses, so it
 * needs no extra Firestore index. Returns 0 when signed out or on error.
 */
@Composable
fun rememberUnreadNotificationCount(): Int {
    var count by remember { mutableIntStateOf(0) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    DisposableEffect(userId) {
        if (userId == null) {
            count = 0
            return@DisposableEffect onDispose { }
        }
        val registration = FirebaseFirestore.getInstance()
            .collection("notifications")
            .whereEqualTo("recipientId", userId)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                count = if (error != null) {
                    Log.w(TAG, "Unread count listener failed", error)
                    0
                } else {
                    snapshot?.documents?.count { it.getBoolean("isRead") != true } ?: 0
                }
            }
        onDispose { registration.remove() }
    }

    return count
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNotificationClick: (NotificationItem) -> Unit = {}
) {
    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val db = remember { FirebaseFirestore.getInstance() }
    val context = LocalContext.current
    val defaultTitle = stringResource(R.string.notification_default_title)

    // Real-time listener so newly created notifications (from Cloud Functions or
    // an incoming push) appear immediately without needing a manual refresh.
    //
    // NOTE: We deliberately do NOT use .orderBy("timestamp") here. Combining an
    // equality filter with orderBy requires a composite index; if that index
    // isn't deployed the whole query fails and the screen looks "empty". Sorting
    // a capped result set on the client is cheap and always works.
    DisposableEffect(userId) {
        if (userId == null) {
            isLoading = false
            return@DisposableEffect onDispose { }
        }

        val registration = db.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Failed to listen for notifications", error)
                    errorMessage = error.localizedMessage
                    isLoading = false
                    return@addSnapshotListener
                }

                errorMessage = null
                notifications = snapshot?.documents.orEmpty().map { doc ->
                    // `timestamp` is a server timestamp and is momentarily null on
                    // locally-written docs, so fall back to createdAt/now.
                    val millis = doc.getTimestamp("timestamp")?.toDate()?.time
                        ?: doc.getTimestamp("createdAt")?.toDate()?.time
                        ?: System.currentTimeMillis()

                    NotificationItem(
                        id = doc.id,
                        title = doc.getString("title") ?: defaultTitle,
                        body = doc.getString("body") ?: "",
                        type = doc.getString("type") ?: "",
                        jobId = doc.getString("jobId") ?: "",
                        isRead = doc.getBoolean("isRead") ?: false,
                        timestampMillis = millis,
                        timeAgo = formatTimeAgo(context, millis)
                    )
                }.sortedByDescending { it.timestampMillis } // newest first

                isLoading = false
            }

        onDispose { registration.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.notifications_title),
                        color = TextDark,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = TextDark
                        )
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
                            Text(
                                stringResource(R.string.mark_all_read),
                                color = PrimaryBlue,
                                fontSize = 12.sp
                            )
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
        } else if (errorMessage != null) {
            // Surface real failures (missing index / permission denied) instead of
            // pretending the inbox is empty — this is what hid the bug before.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = Color.LightGray
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.notifications_load_error),
                        color = TextMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        errorMessage ?: "",
                        color = TextMuted.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
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
                    Text(
                        stringResource(R.string.no_notifications),
                        color = TextMuted,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.notifications_hint),
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

private fun formatTimeAgo(context: Context, timestampMillis: Long): String {
    val diff = System.currentTimeMillis() - timestampMillis
    val minutes = diff / 60000
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> context.getString(R.string.time_just_now)
        minutes < 60 -> context.getString(R.string.time_minutes_ago, minutes.toInt())
        hours < 24 -> context.getString(R.string.time_hours_ago, hours.toInt())
        days < 7 -> context.getString(R.string.time_days_ago, days.toInt())
        else -> context.getString(R.string.time_weeks_ago, (days / 7).toInt())
    }
}

