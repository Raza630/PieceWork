package com.example.workman.notificationsModel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.example.workman.HomeBossDashboardActivity
import com.example.workman.HomeWorkerDashboardActivity
import com.example.workman.NotificationsActivity
import com.example.workman.R
import com.example.workman.SharedPreferencesHelper
import com.example.workman.WorkOfferDetailsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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

        // Persist an in-app record so the notification also shows up in
        // NotificationsActivity, not just the system tray. The Cloud Functions
        // already write records for work_accepted / work_completed, so we skip
        // those to avoid duplicates — this covers push-only types (e.g. urgent_job).
        if (type != "work_accepted" && type != "work_completed") {
            saveNotificationToFirestore(title, body, type, jobId)
        }

        // Bosses get high-priority alerts (they're waiting on worker actions);
        // workers get default priority. Both are tappable and deep-link properly.
        val isBoss = getUserRole() == "Hiring"
        showNotification(title = title, body = body, jobId = jobId, highPriority = isBoss)
    }

    /**
     * Builds and posts the system-tray notification.
     *
     * Every notification gets a UNIQUE id AND a unique PendingIntent request code.
     * Without the unique request code, `FLAG_UPDATE_CURRENT` would make all
     * notifications share one PendingIntent, so tapping an older notification
     * would open the most recent job instead of its own.
     */
    private fun showNotification(
        title: String,
        body: String,
        jobId: String,
        highPriority: Boolean
    ) {
        createNotificationChannel()

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Unique per notification — also used as the PendingIntent request code.
        val notificationId = (System.currentTimeMillis() and 0xFFFFFFF).toInt()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            // Let long bodies expand instead of being truncated
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(R.drawable.ic_email)
            .setAutoCancel(true)
            .setPriority(
                if (highPriority) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(createContentIntent(jobId, notificationId))
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * Where tapping the notification takes the user.
     *
     * - With a jobId  → the job details screen (the actual "Job Accepted /
     *   Completed" job), with the correct dashboard placed beneath it in the
     *   back stack so Back returns to the app instead of exiting.
     * - Without a jobId → the in-app Notifications list.
     */
    private fun createContentIntent(jobId: String, requestCode: Int): PendingIntent? {
        val homeClass = if (getUserRole() == "Hiring") {
            HomeBossDashboardActivity::class.java
        } else {
            HomeWorkerDashboardActivity::class.java
        }

        val targetIntent = if (jobId.isNotBlank()) {
            // NOTE: the key must be "OFFER_ID" — that's what
            // WorkOfferDetailsActivity reads. The old code passed "jobId" to the
            // dashboard, which ignored it entirely.
            Intent(this, WorkOfferDetailsActivity::class.java)
                .putExtra("OFFER_ID", jobId)
        } else {
            Intent(this, NotificationsActivity::class.java)
        }

        return TaskStackBuilder.create(this)
            .addNextIntent(Intent(this, homeClass))
            .addNextIntent(targetIntent)
            .getPendingIntent(
                requestCode,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
    }

    /**
     * Mirrors an incoming push into the `notifications` collection so it appears
     * in the in-app Notifications screen. Best-effort: failures are logged only.
     */
    private fun saveNotificationToFirestore(
        title: String,
        body: String,
        type: String,
        jobId: String
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrBlank()) {
            Log.w(TAG, "No signed-in user; skipping in-app notification record")
            return
        }

        val record = hashMapOf(
            "recipientId" to uid,
            "title" to title,
            "body" to body,
            "type" to type,
            "jobId" to jobId,
            "isRead" to false,
            "timestamp" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance()
            .collection("notifications")
            .add(record)
            .addOnSuccessListener { Log.d(TAG, "In-app notification saved") }
            .addOnFailureListener { Log.w(TAG, "Failed to save in-app notification", it) }
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
