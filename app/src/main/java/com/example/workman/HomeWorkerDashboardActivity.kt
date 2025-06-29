package com.example.workman

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.WindowManager
import android.widget.ListView
import android.widget.Toast

import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.workman.adaptes.WorkOfferAdapter
import com.example.workman.dataClass.WorkOffer
import com.example.workman.notificationsModel.MyFirebaseMessagingService
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Locale


class HomeWorkerDashboardActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var workListView: ListView
    private lateinit var workOfferAdapter: WorkOfferAdapter
    private val workOffers = mutableListOf<WorkOffer>()
    private lateinit var db: FirebaseFirestore

    companion object {
        const val CHANNEL_ID = "work_offer_notifications"
        const val NOTIFICATION_ID = 1001 // Unique ID for notifications
        const val REQUEST_NOTIFICATION_PERMISSION = 101 // Request code for notification permission
    }

    private lateinit var screenshotObserver: ScreenshotObserver
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_worker_dashboard)
//        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        val handler = Handler(Looper.getMainLooper())
        screenshotObserver = ScreenshotObserver(handler, this)
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            screenshotObserver
        )

        FirebaseApp.initializeApp(this)

//        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
//            if (!task.isSuccessful) {
//                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
//                return@addOnCompleteListener
//            }
//
//            // Get the FCM token
//            val token = task.result
//            Log.d("FCM", "FCM Token: $token")
//
//            // Save the token to Firestore or your server
//            saveTokenToFirestore(token)
//        }


        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        db = FirebaseFirestore.getInstance()

        createNotificationChannel()
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        toolbar = findViewById(R.id.toolbar)
        workListView = findViewById(R.id.workListView)



        // Setup Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.open_drawer, R.string.close_drawer
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()


        // Handle navigation item clicks
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_create_work -> {
                    startActivity(Intent(this, CreateWorkActivity::class.java))
                }

                R.id.nav_profile -> {
                    startActivity(Intent(this, Profile::class.java))
                }
//                G8iV5Ci38lTbj8rt8dw3

                R.id.nav_Chat -> {
                    val chatId = "G8iV5Ci38lTbj8rt8dw3"
                    val intent = Intent(this, ChatActivity::class.java).apply {
                        putExtra("CHAT_ID", chatId)
                        putExtra("USER_ID", FirebaseAuth.getInstance().currentUser?.uid) // Optional
                    }
                    startActivity(intent)
//                    startActivity(Intent(this, ChatActivity::class.java))
                }

            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
        // Initialize the adapter
        workOfferAdapter = WorkOfferAdapter(this, workOffers, db)
        workListView.adapter = workOfferAdapter

        // Load work offers from Firestore
        loadWorkOffers()
        requestNotificationPermission()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Work Offer Notifications"
            val descriptionText = "Notifications for work offer updates"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun loadWorkOffers() {
        db.collection("workOffers")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Failed to load work offers", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                workOffers.clear()
                for (document in snapshots!!) {
                    val title = document.getString("title") ?: "Untitled"
                    val description = document.getString("description") ?: "No description"
                    val date = document.getString("date") ?: "No date"
                    val createdAt = document.getTimestamp("createdAt")?.toDate()
                    val acceptedBy = document.getString("acceptedBy")
                    val isAccepted = document.getBoolean("isAccepted") ?: false

                    val workOffer = WorkOffer(
                        title,
                        description,
                        date,
                        createdAt?.let {
                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                        } ?: "No date available",
                        document.get("images") as? List<String> ?: emptyList(),
                        document.id,
                        acceptedBy,
                        isAccepted // Add this field
                    )
                    workOffers.add(workOffer)
                    if (isAccepted && acceptedBy != null) {
                        sendNotificationToCreator(title, "$title has been accepted.")
                    }


                    // Notify the creator if the offer was accepted
//                    if (isAccepted && acceptedBy != null) {
//                        sendNotificationToCreator(acceptedBy, title)
//                    }
                }
                workOfferAdapter.notifyDataSetChanged()
            }
    }

    override fun onResume() {
        super.onResume()
        loadWorkOffers() // Reload work offers whenever the activity resumes
    }

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(screenshotObserver)
    }

    private fun sendNotificationToCreator(workTitle: String, messageBody: String) {
        // Check if notifications are enabled before sending them.
        if (areNotificationsEnabled()) {
            val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_email) // Your notification icon
                .setContentTitle(workTitle)
                .setContentText(messageBody)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            // Check for notification permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, show the notification
                with(NotificationManagerCompat.from(this)) {
                    notify(NOTIFICATION_ID, notificationBuilder.build())
                }
            } else {
                // Permission not granted, request permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        } else {
            Toast.makeText(this, "Notifications are disabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startFirebaseMessagingService() {
        // Start the service that will handle notifications
        val intent = Intent(this, MyFirebaseMessagingService::class.java)
        startService(intent)
    }


    private fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(this).areNotificationsEnabled()
    }

//    private fun requestNotificationPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33 or higher
//            if (ContextCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.POST_NOTIFICATIONS
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                ActivityCompat.requestPermissions(
//                    this,
//                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
//                    REQUEST_NOTIFICATION_PERMISSION
//                )
//            } else {
//                // Permission already granted; you can send notifications immediately.
//            }
//        }
//    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33 or higher
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request the permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            } else {
                // Permission already granted; you can send notifications immediately.
                startFirebaseMessagingService()
            }
        } else {
            // No need to request permission for older versions
            startFirebaseMessagingService()
        }
    }



//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//
//        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
//            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//                // Permission granted; you can send notifications.
//            } else {
//                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start the service
                startFirebaseMessagingService()
            } else {
                // Permission denied, you can handle accordingly
                Toast.makeText(this, "Notification permission required.", Toast.LENGTH_SHORT).show()
            }
        }
    }




//    private fun saveTokenToFirestore(token: String) {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
//
//        val tokenMap = mapOf("fcmToken" to token)
//
//        db.collection("users")
//            .document(userId)
//            .set(tokenMap, SetOptions.merge())
//            .addOnSuccessListener {
//                Log.d("FCM", "Token saved successfully")
//            }
//            .addOnFailureListener { e ->
//                Log.w("FCM", "Error saving token", e)
//            }
//    }

}













//class HomeListActivity : AppCompatActivity() {
//
//    private lateinit var toggle: ActionBarDrawerToggle
//    private lateinit var drawerLayout: DrawerLayout
//    private lateinit var navView: NavigationView
//    private lateinit var toolbar: Toolbar
//    private lateinit var workListView: ListView
//    private lateinit var workOfferAdapter: WorkOfferAdapter
//    private val workOffers = mutableListOf<WorkOffer>()
//    private lateinit var db: FirebaseFirestore
//
//    @SuppressLint("MissingInflatedId")
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_boss_dashboard)
//        // Initialize the Toolbar
//        toolbar = findViewById(R.id.toolbar)
//        setSupportActionBar(toolbar)
//
//        db = FirebaseFirestore.getInstance()
//
//        drawerLayout = findViewById(R.id.drawer_layout)
//        navView = findViewById(R.id.nav_view)
//        toolbar = findViewById(R.id.toolbar)
//        workListView = findViewById(R.id.workListView)
//
//        // Setup Toolbar
//        setSupportActionBar(toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        val toggle = ActionBarDrawerToggle(
//            this, drawerLayout, toolbar,
//            R.string.open_drawer, R.string.close_drawer
//        )
//        drawerLayout.addDrawerListener(toggle)
//        toggle.syncState()
//
//
////        addSampleData()
//        // Handle navigation item clicks
//        navView.setNavigationItemSelectedListener { menuItem ->
//            when (menuItem.itemId) {
//                R.id.nav_create_work -> {
//                    // Handle Create Work click
////                    openAddWorkDialog()
//                    startActivity(Intent(this, CreateWorkActivity::class.java))
//                }
//                R.id.nav_profile -> {
//                    // Handle Profile click
//                    startActivity(Intent(this, Profile::class.java))
//                }
//                R.id.nav_Chat ->{
//                    startActivity(Intent(this, ChatActivity::class.java))
//                }
//
//            }
//            drawerLayout.closeDrawer(GravityCompat.START)
//            true
//
//        }
//
//
//        // Initialize the adapter
//        workOfferAdapter = WorkOfferAdapter(this, workOffers, db)
//        workListView.adapter = workOfferAdapter
//
//        // Load work offers
//        loadWorkOffers()
//
//
//
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        if (toggle.onOptionsItemSelected(item)) {
//            return true
//        }
//        return super.onOptionsItemSelected(item)
//    }
//
//
//    private fun loadWorkOffers() {
//        db.collection("workOffers").get()
//            .addOnSuccessListener { documents ->
//                workOffers.clear() // Clear previous data
//
//                for (document in documents) {
//                    val title = document.getString("title") ?: "Untitled"
//                    val description = document.getString("description") ?: "No description"
//                    val date = document.getString("date") ?: "No date"
//                    val createdAt = document.getTimestamp("createdAt") // Fetch createdAt as Timestamp
//
//                    // Fetch image URLs
//                    val imageUrls = document.get("imageUrls") as? List<String> ?: emptyList()
//                    // Format the Timestamp if it's not null
//                    val createdAtFormatted = createdAt?.let {
//                        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
//                        sdf.format(it.toDate())
//                    } ?: "No date available" // Fallback if createdAt is null
//
//                    // Add the work offer to the list
//                    val workOffer = WorkOffer(title, description, date, createdAtFormatted, imageUrls, document.id,)  // Pass formatted date
//                    workOffers.add(workOffer)
//                }
//
//                workOfferAdapter.notifyDataSetChanged() // Refresh ListView
//            }
//            .addOnFailureListener { exception ->
//                Toast.makeText(this, "Error getting work offers: ${exception.message}", Toast.LENGTH_SHORT).show()
//            }
//    }
//
//
////    private fun loadWorkOffers() {
////        db.collection("workOffers").get()
////            .addOnSuccessListener { documents ->
////                workOffers.clear() // Clear previous data
////
////                for (document in documents) {
////                    val title = document.getString("title") ?: "Untitled"
////                    val description = document.getString("description") ?: "No description"
////                    val date = document.getString("date") ?: "No date"
////                    val createdAt = document.getTimestamp("createdAt")
////
////                    // Add the work offer to the list
////                    val workOffer = WorkOffer(title, description, date, createdAt, document.id)  // Save the doc ID
////                    workOffers.add(workOffer)
////                }
////
////                workOfferAdapter.notifyDataSetChanged() // Refresh ListView
////            }
////            .addOnFailureListener { exception ->
////                Toast.makeText(this, "Error getting work offers: ${exception.message}", Toast.LENGTH_SHORT).show()
////            }
////    }
//
//    override fun onBackPressed() {
//        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
//            drawerLayout.closeDrawer(GravityCompat.START)
//        } else {
//            super.onBackPressed()
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        loadWorkOffers() // Reload work offers whenever the activity resumes
//    }
//
//
////    private fun addSampleData() {
////        val sampleWorks = listOf(
////            WorkOffer(
////                title = "Graphic Designer Needed",
////                description = "Looking for a creative graphic designer to create social media posts.",
////                date = "2024-10-30",
////                imageUrls = listOf("https://example.com/image1.jpg", "https://example.com/image2.jpg"),
////                createdAt = FieldValue.serverTimestamp(),
////                id = "1"
////            ),
////            WorkOffer(
////                title = "Web Development Project",
////                description = "Need a web developer to build a small business website.",
////                date = "2024-11-05",
////                imageUrls = listOf("https://example.com/image3.jpg"),
////                createdAt = FieldValue.serverTimestamp(),
////                id = "2"
////            ),
////            WorkOffer(
////                title = "Content Writer Required",
////                description = "Seeking a content writer for blog posts and articles.",
////                date = "2024-10-28",
////                imageUrls = listOf("https://example.com/image4.jpg", "https://example.com/image5.jpg"),
////                createdAt = FieldValue.serverTimestamp(),
////                id = "3"
////            )
////        )
////
////        // Add sample work data to Firestore
////        for (work in sampleWorks) {
////            db.collection("workOffers").add(work)
////                .addOnSuccessListener { documentReference ->
////                    Log.d("HomeActivity", "Sample work added with ID: ${documentReference.id}")
////                }
////                .addOnFailureListener { e ->
////                    Log.w("HomeActivity", "Error adding sample work", e)
////                }
////        }
////    }
//
//
//
//
//
//}
















//    private fun loadWorkOffers() {
//        db.collection("workOffers").get()
//            .addOnSuccessListener { documents ->
//                workOffers.clear() // Clear previous data
//
//                for (document in documents) {
//                    val title = document.getString("title") ?: "Untitled"
//                    val description = document.getString("description") ?: "No description"
//                    val date = document.getString("date") ?: "No date"
//                    val createdAt = document.getTimestamp("createdAt")
//
//                    // Add the work offer to the list
//                    val workOffer = WorkOffer(title, description, date, createdAt)
//                    workOffers.add(workOffer)
//                }
//
//                workOfferAdapter.notifyDataSetChanged() // Refresh ListView
//            }
//            .addOnFailureListener { exception ->
//                Toast.makeText(this, "Error getting work offers: ${exception.message}", Toast.LENGTH_SHORT).show()
//            }
//    }



//    private fun loadWorkOffers() {
//        // Fetch work offers from Firestore and update the workOffers list
//        // After fetching, update the adapter
//        // Example:
//        val db = FirebaseFirestore.getInstance()
//        db.collection("workOffers").get()
//            .addOnSuccessListener { documents ->
//                for (document in documents) {
//                    val workOffer = document.getString("description") // Adjust to your Firestore structure
//                    workOffer?.let { workOffers.add(it) }
//                }
//                workListAdapter.notifyDataSetChanged() // Notify the adapter to refresh the ListView
//            }
//            .addOnFailureListener { exception ->
//                Toast.makeText(this, "Error getting work offers: ${exception.message}", Toast.LENGTH_SHORT).show()
//            }
//    }

//    private fun openAddWorkDialog() {
//        // Logic to open a dialog for adding new work offer
//        // You can use AlertDialog with EditText to take input from the user
//        val builder = AlertDialog.Builder(this)
//        val input = EditText(this)
//        builder.setTitle("Add New Work Offer")
//        builder.setView(input)
//        builder.setPositiveButton("Add") { dialog, which ->
//            val workDescription = input.text.toString()
//            addWorkOffer(workDescription)
//        }
//        builder.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
//        builder.show()
//    }

//    private fun addWorkOffer(description: String) {
//        // Save the new work offer to Firestore
//        val db = FirebaseFirestore.getInstance()
//        val newOffer = hashMapOf(
//            "description" to description
//        )
//        db.collection("workOffers").add(newOffer)
//            .addOnSuccessListener {
//                workOffers.add(description) // Add to local list
//                workListAdapter.notifyDataSetChanged() // Update the ListView
//                Toast.makeText(this, "Work offer added", Toast.LENGTH_SHORT).show()
//            }
//            .addOnFailureListener { e ->
//                Toast.makeText(this, "Error adding work offer: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//    }



//}
