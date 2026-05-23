package com.example.workman.notificationsModel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.workman.HomeBossDashboardActivity
import com.example.workman.R
import com.example.workman.SharedPreferencesHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Extract notification data — Cloud Function sends both notification + data payloads
        val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "WorkMan"
        val body = remoteMessage.data["body"] ?: remoteMessage.notification?.body ?: ""
        val type = remoteMessage.data["type"] ?: ""
        val jobId = remoteMessage.data["jobId"] ?: ""

        Log.d(TAG, "Notification received: type=$type, title=$title")

        // Route notification based on user role
        val currentUserRole = getUserRole()

        when {
            type == "work_accepted" && currentUserRole == "Hiring" -> {
                sendNotificationToBoss(title, body, jobId)
            }

            currentUserRole == "Hiring" -> {
                sendNotificationToBoss(title, body, jobId)
            }

            else -> {
                sendNotificationToWorker(title, body)
            }
        }
    }

    private fun sendNotificationToBoss(title: String, body: String, jobId: String?) {
        createNotificationChannel()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_email)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(createIntentForBoss(jobId))
            .build()

        // Use unique notification ID so multiple notifications don't replace each other
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun sendNotificationToWorker(title: String, body: String) {
        createNotificationChannel()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_email)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createIntentForBoss(jobId: String?): PendingIntent {
        val intent = Intent(this, HomeBossDashboardActivity::class.java).apply {
            putExtra("jobId", jobId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Work Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for work offers and job updates"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getUserRole(): String {
        val sharedPrefs = SharedPreferencesHelper(this)
        return sharedPrefs.getUserChoice() ?: "Worker"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")
        // Update token in Firestore if user is logged in
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .update("fcmToken", token)
                .addOnSuccessListener { Log.d(TAG, "FCM token updated in Firestore") }
                .addOnFailureListener { Log.w(TAG, "Failed to update FCM token", it) }
        }
    }

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "work_channel"
    }
}
