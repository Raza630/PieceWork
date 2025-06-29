package com.example.workman.notificationsModel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.workman.HomeBossDashboardActivity
import com.example.workman.HomeBossDashboardActivity.Companion.CHANNEL_ID
import com.example.workman.HomeBossDashboardActivity.Companion.NOTIFICATION_ID
import com.example.workman.HomeBossDashboardActivity.Companion.REQUEST_NOTIFICATION_PERMISSION
import com.example.workman.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage



class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Extract the notification data
        val title = remoteMessage.data["title"]
        val body = remoteMessage.data["body"]
        val jobId = remoteMessage.data["jobId"] // Example to send the jobId with the notification

        // Check if the user is a boss or worker
        val currentUserRole = getUserRole() // Function to get the user’s role from Firebase or SharedPreferences

        // If the user is a boss, handle the notification differently
        if (currentUserRole == "Hiring") {
            Toast.makeText(this, "hiring notification is on ", Toast.LENGTH_SHORT).show()
            sendNotificationToBoss(title, body, jobId)
        } else {
            // Handle for workers (if needed, e.g., show a different type of notification or no notification)

            sendNotificationToWorker(title, body, jobId)
            Toast.makeText(this, "worker notification is on ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendNotificationToBoss(title: String?, body: String?, jobId: String?) {
        // Customize notification for the boss
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(this, "work_channel")
            .setContentTitle(title)
            .setContentText("Worker accepted the job!")
            .setSmallIcon(R.drawable.ic_email)
            .setAutoCancel(true)
            .setContentIntent(createIntentForBoss(jobId)) // Redirect boss to their dashboard
            .build()

        notificationManager.notify(1, notification)
    }

    private fun sendNotificationToWorker(title: String?, body: String?, jobId: String?) {
        // Handle worker notifications if necessary

    }

    private fun createIntentForBoss(jobId: String?): PendingIntent {
        // Create an intent to open the boss's dashboard or a specific screen
        val intent = Intent(this, HomeBossDashboardActivity::class.java).apply {
            putExtra("jobId", jobId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun getUserRole(): String {
        // Retrieve user role from SharedPreferences or Firestore
        // For this example, we use SharedPreferences
        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPrefs.getString("role", "worker") ?: "worker"
    }
}




//class MyFirebaseMessagingService : FirebaseMessagingService() {
//
//    // This function is triggered when a notification message is received.
//    override fun onMessageReceived(remoteMessage: RemoteMessage) {
//        super.onMessageReceived(remoteMessage)
//
//        // Handle incoming messages here
//        remoteMessage.notification?.let {
//            showNotification(it.title, it.body)
//        }
//    }
//
//    // This function is triggered when the FCM token is refreshed.
//    override fun onNewToken(token: String) {
//        super.onNewToken(token)
//        Log.d("FCM", "Refreshed token: $token")
//        saveTokenToFirestore(token)
//    }
//
//    // Save the FCM token to Firestore
//    private fun saveTokenToFirestore(token: String) {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
//
//        val tokenMap = mapOf("fcmToken" to token)
//
//        FirebaseFirestore.getInstance().collection("users")
//            .document(userId)
//            .set(tokenMap, SetOptions.merge())
//            .addOnSuccessListener {
//                Log.d("FCM", "Token saved successfully")
//            }
//            .addOnFailureListener { e ->
//                Log.w("FCM", "Error saving token", e)
//            }
//    }
//
//    // Show the notification
//    @SuppressLint("MissingPermission")
//    private fun showNotification(title: String?, body: String?) {
//        // Create a notification channel for Android 8.0 and above
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            createNotificationChannel()
//        }
//
//        // Create the notification builder
//        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
//            .setSmallIcon(R.drawable.ic_email) // Your notification icon
//            .setContentTitle(title)
//            .setContentText(body)
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//            .setAutoCancel(true)
//
//        // Show the notification directly (no need to check permissions here)
//        with(NotificationManagerCompat.from(this)) {
//            notify(NOTIFICATION_ID, notificationBuilder.build())
//        }
//
//        // Check if the notification permission is granted (for Android 13 and above)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//                // Request notification permission for Android 13 and above
//                ActivityCompat.requestPermissions(
//                    this as Activity,
//                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
//                    REQUEST_NOTIFICATION_PERMISSION
//                )
//            }
//        }
//    }
//
//    // Create a notification channel (required for Android 8.0 and above)
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val name = "Default Channel"
//            val descriptionText = "Channel for general notifications"
//            val importance = NotificationManager.IMPORTANCE_DEFAULT
//            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
//                description = descriptionText
//            }
//
//            // Register the channel with the system
//            val notificationManager: NotificationManager =
//                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(channel)
//        }
//    }
//
//    companion object {
//        private const val CHANNEL_ID = "default_channel"
//        private const val NOTIFICATION_ID = 1
//        private const val REQUEST_NOTIFICATION_PERMISSION = 1001
//    }
//}






//class MyFirebaseMessagingService : FirebaseMessagingService() {
//
//    // This function is triggered when a notification message is received.
//    override fun onMessageReceived(remoteMessage: RemoteMessage) {
//        super.onMessageReceived(remoteMessage)
//
//        // Handle incoming messages here
//        remoteMessage.notification?.let {
//            showNotification(it.title, it.body)
//        }
//    }
//
//    // Show the notification
//    @SuppressLint("MissingPermission")
//    private fun showNotification(title: String?, body: String?) {
//        // Create a notification channel for Android 8.0 and above
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            createNotificationChannel()
//        }
//
//        // Create the notification builder
//        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
//            .setSmallIcon(R.drawable.ic_email) // Your notification icon
//            .setContentTitle(title)
//            .setContentText(body)
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//            .setAutoCancel(true)
//
//
//
////        / Show the notification directly (no need to check permissions here)
//        with(NotificationManagerCompat.from(this)) {
//            notify(NOTIFICATION_ID, notificationBuilder.build())
//        }
//
//        // Check if the notification permission is granted (for Android 13 and above)
////        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
////            // Permission granted, show the notification
////            with(NotificationManagerCompat.from(this)) {
////                notify(NOTIFICATION_ID, notificationBuilder.build())
////            }
////        } else {
////            // Permission not granted, request permission
////            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
////                // Request notification permission for Android 13 and above
////                ActivityCompat.requestPermissions(
////                    this,
////                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
////                    REQUEST_NOTIFICATION_PERMISSION
////                )
////            }
////        }
//    }
//
//    // Create a notification channel (required for Android 8.0 and above)
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val name = "Default Channel"
//            val descriptionText = "Channel for general notifications"
//            val importance = NotificationManager.IMPORTANCE_DEFAULT
//            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
//                description = descriptionText
//            }
//
//            // Register the channel with the system
//            val notificationManager: NotificationManager =
//                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(channel)
//        }
//    }
//}















//package com.example.workman.notificationsModel
//
//import android.Manifest
//import android.content.Context
//import android.content.pm.PackageManager
//import androidx.core.app.ActivityCompat
//import androidx.core.app.NotificationCompat
//import androidx.core.app.NotificationManagerCompat
//import com.example.workman.HomeBossDashboardActivity.Companion.CHANNEL_ID
//import com.example.workman.HomeBossDashboardActivity.Companion.NOTIFICATION_ID
//import com.example.workman.HomeBossDashboardActivity.Companion.REQUEST_NOTIFICATION_PERMISSION
//import com.example.workman.R
//import com.google.firebase.messaging.FirebaseMessagingService
//import com.google.firebase.messaging.RemoteMessage
//
//
//class MyFirebaseMessagingService : FirebaseMessagingService() {
//
//    override fun onMessageReceived(remoteMessage: RemoteMessage) {
//        super.onMessageReceived(remoteMessage)
//
//        // Handle incoming messages here
//        remoteMessage.notification?.let {
//            showNotification(it.title, it.body)
//        }
//    }
//
//
//    private fun showNotification(title: String?, body: String?) {
//        // Create the notification builder
//        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
//            .setSmallIcon(R.drawable.ic_email) // Your notification icon
//            .setContentTitle(title)
//            .setContentText(body)
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//
//        // Check if the notification permission is granted
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
//            // Permission granted, show the notification
//            with(NotificationManagerCompat.from(this)) {
//                notify(NOTIFICATION_ID, notificationBuilder.build())
//            }
//        } else {
//            // Permission not granted, request permission
////            ActivityCompat.requestPermissions(
////                this,
////                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
////                REQUEST_NOTIFICATION_PERMISSION
////            )
//        }
//    }
//
////    private fun showNotification(title: String?, body: String?) {
////        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
////            .setSmallIcon(R.drawable.ic_email) // Your notification icon
////            .setContentTitle(title)
////            .setContentText(body)
////            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
////        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
////            // Permission granted, show the notification
////            with(NotificationManagerCompat.from(this)) {
////                notify(NOTIFICATION_ID, notificationBuilder.build())
////            }
////        } else {
////            // Permission not granted, request permission
////            ActivityCompat.requestPermissions(
////                this,
////                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
////                REQUEST_NOTIFICATION_PERMISSION
////            )
////        }
////
////    }
//}