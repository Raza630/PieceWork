//package com.example.workman
//
//import android.Manifest
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.content.Context
//import android.content.pm.PackageManager
//import android.os.Build
//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle
//import androidx.core.app.ActivityCompat
//import androidx.core.app.NotificationCompat
//import androidx.core.app.NotificationManagerCompat
//
//
//class notification : AppCompatActivity() {
//
//    // Dummy Leaderboard Data
//    private val leaderboard = listOf(
//        Pair("Alice", 1500),
//        Pair("Bob", 1450),
//        Pair("Charlie", 1400),
//        Pair("Diana", 1350),
//        Pair("Eve", 1300),
//        Pair("Frank", 1250),
//        Pair("Grace", 1200),
//        Pair("Heidi", 1150),
//        Pair("Ivan", 1100),
//        Pair("Judy", 1050)
//    )
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_notification)
//
//        val sharedPreferencesHelper = SharedPreferencesHelper(this)
//
//        // Retrieve username from SharedPreferences
//        val username = sharedPreferencesHelper.getUsername()
//        if (username.isNullOrEmpty()) {
//            println("No user is logged in.")
//            return
//        }
//
//        println("Logged in as: $username")
//
//        // Create Notification Channel (for API 26+)
//        createNotificationChannel()
//
//        // Get User Rank
//        val userRank = getUserRank(username, leaderboard)
//
//        // Display Leaderboard in Logs (for UI, you can update a TextView or RecyclerView)
//        displayLeaderboard(leaderboard)
//
//        // Trigger Notification with User Rank
//        sendNotification(username, userRank)
//    }
//
//    // Function to Get User Rank
//    private fun getUserRank(username: String, leaderboard: List<Pair<String, Int>>): Int {
//        val sortedLeaderboard = leaderboard.sortedByDescending { it.second }
//        return sortedLeaderboard.indexOfFirst { it.first == username } + 1
//    }
//
//    // Function to Display Leaderboard in Logs
//    private fun displayLeaderboard(leaderboard: List<Pair<String, Int>>) {
//        val sortedLeaderboard = leaderboard.sortedByDescending { it.second }
//        println("Leaderboard:")
//        sortedLeaderboard.forEachIndexed { index, entry ->
//            println("${index + 1}. ${entry.first} - ${entry.second} points")
//        }
//    }
//
//    // Function to Send Notification
//    private fun sendNotification(username: String, rank: Int) {
//        val notificationId = 1
//        val channelId = "leaderboard_channel"
//
//        val notification = NotificationCompat.Builder(this, channelId)
//            .setSmallIcon(android.R.drawable.ic_dialog_info)
//            .setContentTitle("Leaderboard Update")
//            .setContentText("$username, you are currently ranked #$rank on the leaderboard!")
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .build()
//
//        val notificationManager =
//            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        notificationManager.notify(notificationId, notification)
//    }
//
//    // Function to Create Notification Channel
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val name = "Leaderboard Notifications"
//            val descriptionText = "Notifications for leaderboard ranks"
//            val importance = NotificationManager.IMPORTANCE_HIGH
//            val channel = NotificationChannel("leaderboard_channel", name, importance).apply {
//                description = descriptionText
//            }
//            val notificationManager =
//                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(channel)
//        }
//    }
//}
