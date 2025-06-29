package com.example.workman

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.workman.adaptes.WorkCategoryAdapter
import com.example.workman.dataClass.Category
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.workman.HomeWorkerDashboardActivity.Companion.NOTIFICATION_ID
import com.example.workman.HomeWorkerDashboardActivity.Companion.REQUEST_NOTIFICATION_PERMISSION
import com.example.workman.adaptes.WorkerAdapter
import com.example.workman.dataClass.WorkOffer
import com.example.workman.dataClass.Workerlist
import com.example.workman.dataClass.Workers
import com.example.workman.databinding.ActivityBossWorkListBinding
import com.example.workman.databinding.ActivityTestHomelistUiBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale


class HomeBossDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTestHomelistUiBinding
    private val db = FirebaseFirestore.getInstance()

    private lateinit var categoryAdapter: WorkCategoryAdapter
    private lateinit var workerAdapter: WorkerAdapter

    private val categories = listOf(
        Category("Professionals", R.drawable.ic_user),
        Category("Associate Professionals", R.drawable.ic_user),
        Category("Clerks", R.drawable.ic_user),
        Category("Service Workers and Shop & Market Sales Workers", R.drawable.ic_user),
        Category("Skilled Agricultural and Fishery Workers", R.drawable.ic_user),
        Category("Craft and Related Trades Workers", R.drawable.ic_user),
        Category("Plant and Machine Operators and Assemblers", R.drawable.ic_user),
        Category("Elementary Occupations", R.drawable.ic_user)
    )

    private val workerList = mutableListOf<Workerlist>()
    private var areSubFabsVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestHomelistUiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupDrawerNavigation()
        setupFABs()
        setupRecyclerViews()
        setupCustomSearchBar()
        createNotificationChannel()
        requestNotificationPermission()

        fetchWorkers()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        val toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.open_drawer, R.string.close_drawer)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun setupDrawerNavigation() {
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_create_work -> startActivity(Intent(this, CreateWorkActivity::class.java))
                R.id.nav_profile -> startActivity(Intent(this, Profile::class.java))
                R.id.nav_Chat -> startActivity(Intent(this, ChatActivity::class.java))
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupFABs() {
        binding.mainFab.setOnClickListener {
            areSubFabsVisible = !areSubFabsVisible
            binding.viewOffersFab.visibility = if (areSubFabsVisible) View.VISIBLE else View.GONE
            binding.createWorkFab.visibility = if (areSubFabsVisible) View.VISIBLE else View.GONE
            binding.viewOffersText.visibility = if (areSubFabsVisible) View.VISIBLE else View.GONE
            binding.createWorkText.visibility = if (areSubFabsVisible) View.VISIBLE else View.GONE
        }

        binding.viewOffersFab.setOnClickListener {
            startActivity(Intent(this, BossWorkListActivity::class.java))
        }

        binding.createWorkFab.setOnClickListener {
            startActivity(Intent(this, CreateWorkActivity::class.java))
        }
    }

    private fun setupRecyclerViews() {
        categoryAdapter = WorkCategoryAdapter(categories)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeBossDashboardActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        workerAdapter = WorkerAdapter(workerList) {
            startActivity(Intent(this, WorkerDetailsActivity::class.java))
        }
        binding.listofworkrecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeBossDashboardActivity)
            adapter = workerAdapter
        }

        categoryAdapter.setOnSelectionChangeListener { selectedCategories ->
            filterWorkersBySelectedCategories(selectedCategories)
        }
    }

    private fun setupCustomSearchBar() {
        binding.customSearchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterCategoriesAndWorkers(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.searchIcon.setOnClickListener {
            filterCategoriesAndWorkers(binding.customSearchBar.text.toString())
        }
    }

    private fun filterCategoriesAndWorkers(query: String) {
        val filteredCategories = categories.filter { it.name.contains(query, ignoreCase = true) }
        categoryAdapter.updateList(filteredCategories)

        val filteredWorkers = workerList.filter {
            it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
        }
        workerAdapter.updateList(filteredWorkers)
    }

    private fun filterWorkersBySelectedCategories(selectedCategories: Set<Category>) {
        val selectedCategoryNames = selectedCategories.map { it.name }
        val filteredWorkers = if (selectedCategoryNames.isEmpty()) {
            workerList
        } else {
            workerList.filter { selectedCategoryNames.contains(it.category) }
        }
        workerAdapter.updateList(filteredWorkers)
    }

    private fun fetchWorkers() {
        db.collection("users")
            .whereEqualTo("role", "Worker")
            .get()
            .addOnSuccessListener { documents ->
                workerList.clear()
                documents.forEach { doc ->
                    val worker = doc.toObject(Workerlist::class.java)
                    workerList.add(worker)
                }
                workerAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching workers", e)
                Toast.makeText(this, "Failed to load workers", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Work Offer Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for work offer updates"
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION && grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val CHANNEL_ID = "work_offer_notifications"
        const val NOTIFICATION_ID = 1001
        const val REQUEST_NOTIFICATION_PERMISSION = 101
    }
}




















//class HomeBossDashboardActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityTestHomelistUiBinding
//    private lateinit var db: FirebaseFirestore
//
//    private lateinit var categoryAdapter: WorkCategoryAdapter
//    private lateinit var categories: List<Category>
//    private lateinit var workerAdapter: WorkerAdapter
////    private lateinit var workers: List<Workers>
//
//    private val workerList = mutableListOf<Workerlist>()  // List to store worker data
//
//
//    // Boolean flag to track whether sub FABs are visible.
//    private var areSubFabsVisible = false
//
//    companion object {
//        const val CHANNEL_ID = "work_offer_notifications"
//        const val NOTIFICATION_ID = 1001 // Unique ID for notifications
//        const val REQUEST_NOTIFICATION_PERMISSION = 101 // Request code for notification permission
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityTestHomelistUiBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        db = FirebaseFirestore.getInstance()
//
//        // Setup custom search bar.
//        setupCustomSearchBar()
//        fetchWorkers()
//
//        // Setup Toolbar and DrawerLayout using binding.
//        setSupportActionBar(binding.toolbar)
//        val toggle = ActionBarDrawerToggle(
//            this, binding.drawerLayout, binding.toolbar,
//            R.string.open_drawer, R.string.close_drawer
//        )
//        binding.drawerLayout.addDrawerListener(toggle)
//        toggle.syncState()
//
//        binding.navView.setNavigationItemSelectedListener { menuItem ->
//            when (menuItem.itemId) {
//                R.id.nav_create_work -> startActivity(Intent(this, CreateWorkActivity::class.java))
//                R.id.nav_profile -> startActivity(Intent(this, Profile::class.java))
//                R.id.nav_Chat -> startActivity(Intent(this, ChatActivity::class.java))
//            }
//            binding.drawerLayout.closeDrawer(GravityCompat.START)
//            true
//        }
//
//        // Setup Floating Action Buttons and labels.
//        binding.mainFab.setOnClickListener {
//            if (!areSubFabsVisible) {
//                binding.viewOffersFab.show()
//                binding.createWorkFab.show()
////                binding.viewOffersText.visibility = View.VISIBLE
////                binding.createWorkText.visibility = View.VISIBLE
//            } else {
//                binding.viewOffersFab.hide()
//                binding.createWorkFab.hide()
//                binding.viewOffersText.visibility = View.GONE
//                binding.createWorkText.visibility = View.GONE
//            }
//            areSubFabsVisible = !areSubFabsVisible
//        }
//
//        binding.viewOffersFab.setOnClickListener {
//            startActivity(Intent(this, BossWorkListActivity::class.java))
//        }
//
//        binding.createWorkFab.setOnClickListener {
//            startActivity(Intent(this, CreateWorkActivity::class.java))
//        }
//
//        createNotificationChannel()
//        requestNotificationPermission()
//
//        // Initialize categories and workers.
//        categories = listOf(
//            Category("Professionals", R.drawable.ic_user),
//            Category("Associate Professionals", R.drawable.ic_user),
//            Category("Clerks", R.drawable.ic_user),
//            Category("Service Workers and Shop & Market Sales Workers", R.drawable.ic_user),
//            Category("Skilled Agricultural and Fishery Workers", R.drawable.ic_user),
//            Category("Craft and Related Trades Workers", R.drawable.ic_user),
//            Category("Plant and Machine Operators and Assemblers", R.drawable.ic_user),
//            Category("Elementary Occupations", R.drawable.ic_user)
//        )
//
////        workers = listOf(
////            Workers("John Doe", R.drawable.ic_icon__profile, "Professionals", 2.0f),
////            Workers("Jane Smith", R.drawable.ic_icon__profile, "Professionals", 4.5f),
////            Workers("Mike Johnson", R.drawable.ic_icon__profile, "Professionals", 5.0f),
////            Workers("John Doe", R.drawable.ic_icon__profile, "Professionals", 2.0f),
////            Workers("Aakarsh", R.drawable.ic_icon__profile, "Clerks", 3.5f),
////            Workers("Ibhan", R.drawable.ic_icon__profile, "Associate Professionals", 4.5f),
////            Workers("Idhayan", R.drawable.ic_icon__profile, "Associate Professionals", 3.5f),
////            Workers("Indranil", R.drawable.ic_icon__profile, "Clerks", 2.5f),
////            Workers("John Doe", R.drawable.ic_icon__profile, "Professionals",2.0f),
////            Workers("Jane Smith", R.drawable.ic_icon__profile, "Professionals",4.5f),
////            Workers("Mike Johnson", R.drawable.ic_icon__profile, "Professionals",5.0f),
////            Workers("Aahan", R.drawable.ic_icon__profile, "Associate Professionals",3.5f),
////            Workers("Advik", R.drawable.ic_icon__profile, "Associate Professionals",4.5f),
////            Workers("Arnav", R.drawable.ic_icon__profile, "Associate Professionals",3.5f),
////            Workers("Atharva", R.drawable.ic_icon__profile, "Clerks",2.5f),
////            Workers("Aakarsh", R.drawable.ic_icon__profile, "Clerks",3.5f),
////            Workers("Aanan", R.drawable.ic_icon__profile, "Clerks",4.1f),
////            Workers("Advay", R.drawable.ic_icon__profile, "Service Workers and Shop & Market Sales Workers",1.5f),
////            Workers("Aarav ", R.drawable.ic_icon__profile, "Service Workers and Shop & Market Sales Workers",4.5f),
////            Workers("Aarush", R.drawable.ic_icon__profile, "Service Workers and Shop & Market Sales Workers",3.5f),
////            Workers("Akul", R.drawable.ic_icon__profile, "Skilled Agricultural and Fishery Workers",3.3f),
////            Workers("Adyant", R.drawable.ic_icon__profile, "Skilled Agricultural and Fishery Workers",4.5f),
////            Workers("Adikrit", R.drawable.ic_icon__profile, "Skilled Agricultural and Fishery Workers",2.1f),
////            Workers("Amay", R.drawable.ic_icon__profile, "Craft and Related Trades Workers",2.3f),
////            Workers("Jane Smith", R.drawable.ic_icon__profile, "Craft and Related Trades Workers",2.4f),
////            Workers("Agniv", R.drawable.ic_icon__profile, "Craft and Related Trades Workers",5.0f),
////            Workers("Agastya", R.drawable.ic_icon__profile, "Plant and Machine Operators and Assemblers",2.3f),
////            Workers("Anvay", R.drawable.ic_icon__profile, "Plant and Machine Operators and Assemblers",2.4f),
////            Workers("Ainesh", R.drawable.ic_icon__profile, "Plant and Machine Operators and Assemblers",5.0f),
////            Workers("Anik", R.drawable.ic_icon__profile, "Elementary Occupations",2.3f),
////            Workers("Evyavan", R.drawable.ic_icon__profile, "Elementary Occupations",2.4f),
////            Workers("Ethiraj", R.drawable.ic_icon__profile, "Elementary Occupations",5.0f),
////            Workers("Ekagra", R.drawable.ic_icon__profile, "Professionals",2.0f),
////            Workers("Harshit", R.drawable.ic_icon__profile, "Professionals",4.5f),
////            Workers("Hriday", R.drawable.ic_icon__profile, "Professionals",5.0f),
////            Workers("Hardik", R.drawable.ic_icon__profile, "Associate Professionals",3.5f),
////            Workers("Ibhan", R.drawable.ic_icon__profile, "Associate Professionals",4.5f),
////            Workers("Idhayan", R.drawable.ic_icon__profile, "Associate Professionals",3.5f),
////            Workers("Indranil", R.drawable.ic_icon__profile, "Clerks",2.5f),
////            Workers("Ikshaan", R.drawable.ic_icon__profile, "Clerks",3.5f),
////            Workers("Jay", R.drawable.ic_icon__profile, "Clerks",4.1f),
////            Workers("Kavish", R.drawable.ic_icon__profile, "Service Workers and Shop & Market Sales Workers",1.5f),
////            Workers("Nadin", R.drawable.ic_icon__profile, "Service Workers and Shop & Market Sales Workers",4.5f),
////            Workers("Naman", R.drawable.ic_icon__profile, "Service Workers and Shop & Market Sales Workers",3.5f),
////            Workers("Neil", R.drawable.ic_icon__profile, "Skilled Agricultural and Fishery Workers",3.3f),
////            Workers("Ojasvat", R.drawable.ic_icon__profile, "Skilled Agricultural and Fishery Workers",4.5f),
////            Workers("Parthiv", R.drawable.ic_icon__profile, "Skilled Agricultural and Fishery Workers",2.1f),
////            Workers("Pranay", R.drawable.ic_icon__profile, "Craft and Related Trades Workers",2.3f),
////            Workers("Purnit", R.drawable.ic_icon__profile, "Craft and Related Trades Workers",2.4f),
////            Workers("Priyansh", R.drawable.ic_icon__profile, "Craft and Related Trades Workers",5.0f),
////            Workers("Purnit", R.drawable.ic_icon__profile, "Plant and Machine Operators and Assemblers",2.3f),
////            Workers("Priyansh", R.drawable.ic_icon__profile, "Plant and Machine Operators and Assemblers",2.4f),
////            Workers("Reyaansh", R.drawable.ic_icon__profile, "Plant and Machine Operators and Assemblers",5.0f),
////            Workers("Rijul", R.drawable.ic_icon__profile, "Elementary Occupations",2.3f),
////            Workers("Rohil", R.drawable.ic_icon__profile, "Elementary Occupations",2.4f),
////            Workers("Sahil", R.drawable.ic_icon__profile, "Elementary Occupations",5.0f)
////        )
//
//
////        workerList = fetchWorkers()
//        // Set up adapters for recycler views.
//        binding.recyclerView.layoutManager =
//            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
//        categoryAdapter = WorkCategoryAdapter(categories)
//        binding.recyclerView.adapter = categoryAdapter
//
//        binding.listofworkrecyclerView.layoutManager = LinearLayoutManager(this)
//        workerAdapter = WorkerAdapter(workerList) {
//            startActivity(Intent(this, WorkerDetailsActivity::class.java))
//        }
//        binding.listofworkrecyclerView.adapter = workerAdapter
//
//        // Listen for category selection changes.
////        categoryAdapter.setOnSelectionChangeListener { selectedCategories ->
////            filterWorkersBySelectedCategories(selectedCategories)
////        }
//
//    }
//
////    private fun filterWorkersBySelectedCategories(selectedCategories: Set<Category>) {
////        val selectedCategoryNames = selectedCategories.map { it.name }
////        val filteredWorkers = if (selectedCategoryNames.isEmpty()) {
////            workerList
////        } else {
////            workerList.filter { worker -> selectedCategoryNames.contains(worker.category) }
////        }
////        workerAdapter.updateList(filteredWorkers)
////    }
//
//    private fun setupCustomSearchBar() {
//        binding.customSearchBar.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                filterCategoriesAndWorkers(s.toString())
//            }
//            override fun afterTextChanged(s: Editable?) {}
//        })
//        binding.searchIcon.setOnClickListener {
//            filterCategoriesAndWorkers(binding.customSearchBar.text.toString())
//        }
//
//
//    }
//
//    private fun filterCategoriesAndWorkers(query: String) {
//        val filteredCategories = categories.filter {
//            it.name.contains(query, ignoreCase = true)
//        }
//        categoryAdapter.updateList(filteredCategories)
//
////        val filteredWorkers = workerList.filter {
////            it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
////        }
////        workerAdapter.updateList(filteredWorkers)
//    }
//
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val name = "Work Offer Notifications"
//            val descriptionText = "Notifications for work offer updates"
//            val importance = NotificationManager.IMPORTANCE_DEFAULT
//            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
//                description = descriptionText
//            }
//            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(channel)
//        }
//    }
//
//
//
//    private fun fetchWorkers() {
//        db.collection("users")
//            .whereEqualTo("role", "Worker") // Fetch only Worker users
//            .get()
//            .addOnSuccessListener { documents ->
//                workerList.clear()  // Clear list before adding new data
//                for (document in documents) {
//                    val worker = document.toObject(Workerlist::class.java)
//                    workerList.add(worker)
//                }
//                workerAdapter.notifyDataSetChanged() // Refresh RecyclerView
//            }
//            .addOnFailureListener { exception ->
//                Log.e("Firestore", "Error fetching workers", exception)
//            }
//    }
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//    private fun sendNotification(workTitle: String, messageBody: String) {
//        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
//            .setSmallIcon(R.drawable.ic_email)
//            .setContentTitle(workTitle)
//            .setContentText(messageBody)
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
//            with(NotificationManagerCompat.from(this)) {
//                notify(NOTIFICATION_ID, notificationBuilder.build())
//            }
//        } else {
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
//                REQUEST_NOTIFICATION_PERMISSION
//            )
//        }
//    }
//
//    private fun requestNotificationPermission() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(
//                    this,
//                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
//                    REQUEST_NOTIFICATION_PERMISSION
//                )
//            }
//        }
//    }
//
//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
//            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
//                // Permission granted; notifications can be sent.
//            } else {
//                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//}










//class HomeBossDashboardActivity : AppCompatActivity() {
//
//    private lateinit var categoryAdapter: WorkCategoryAdapter
//    private lateinit var categories: List<Category>
//    private lateinit var workerAdapter: WorkerAdapter
//    private lateinit var workers: List<Workers>
//    private lateinit var drawerLayout: DrawerLayout
//    private lateinit var toolbar: Toolbar
//
//    private lateinit var navView: NavigationView
//
//
//    @SuppressLint("MissingInflatedId")
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_test_homelist_ui)
//        val searchView: SearchView = findViewById(R.id.searchView)
//        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
//        val listofworkrecyclerView: RecyclerView = findViewById(R.id.listofworkrecyclerView)
//        recyclerView.visibility = View.VISIBLE
//        listofworkrecyclerView.visibility = View.VISIBLE
//
//        toolbar = findViewById(R.id.toolbar)
//        setSupportActionBar(toolbar)
//
//        drawerLayout = findViewById(R.id.drawer_layout)
//        val navView: NavigationView = findViewById(R.id.nav_view)
//
//
//        val toggle = ActionBarDrawerToggle(
//            this, drawerLayout, toolbar,
//            R.string.open_drawer, R.string.close_drawer
//        )
//        drawerLayout.addDrawerListener(toggle)
//        toggle.syncState()
//
//
//
//
//        // Handle navigation item clicks
//        navView.setNavigationItemSelectedListener { menuItem ->
//            when (menuItem.itemId) {
//                R.id.nav_create_work -> {
//                    startActivity(Intent(this, CreateWorkActivity::class.java))
//                }
//                R.id.nav_profile -> {
//                    startActivity(Intent(this, Profile::class.java))
//                }
//                R.id.nav_Chat -> {
//                    startActivity(Intent(this, ChatActivity::class.java))
//                }
//
//            }
//            drawerLayout.closeDrawer(GravityCompat.START)
//            true
//        }
//
//        categories = listOf(
//            Category("Professionals", R.drawable.ic_user),
//            Category("Associate Professionals", R.drawable.ic_user),
//            Category("Clerks", R.drawable.ic_user),
//            Category("Service Workers and Shop & Market Sales Workers", R.drawable.ic_user),
//            Category("Skilled Agricultural and Fishery Workers", R.drawable.ic_user),
//            Category("Craft and Related Trades Workers", R.drawable.ic_user),
//            Category("Plant and Machine Operators and Assemblers ", R.drawable.ic_user),
//            Category("Elementary Occupations", R.drawable.ic_user)
//
//            // Add more categories as needed
//        )
//
//        workers = listOf(
//            Workers("John Doe", R.drawable.ic_icon__profile, "Professionals",2.0f),
//            Workers("Jane Smith", R.drawable.ic_icon__profile, "Professionals",4.5f),
//            Workers("Mike Johnson", R.drawable.ic_icon__profile, "Professionals",5.0f),
//            Workers("Aahan", R.drawable.ic_icon__profile, "Associate Professionals",3.5f),
//            Workers("Advik", R.drawable.ic_icon__profile, "Associate Professionals",4.5f),
//            Workers("Arnav", R.drawable.ic_icon__profile, "Associate Professionals",3.5f),
//            Workers("Atharva", R.drawable.ic_icon__profile, "Clerks",2.5f),
//            Workers("Aakarsh", R.drawable.ic_icon__profile, "Clerks",3.5f),
//            Workers("Aanan", R.drawable.ic_icon__profile, "Clerks",4.1f),
//            Workers("Advay", R.drawable.ic_icon__profile, "Service Workers and Shop & Market Sales Workers",1.5f),
//            Workers("Aarav ", R.drawable.ic_icon__profile, "Service Workers and Shop & Market Sales Workers",4.5f),
//            Workers("Aarush", R.drawable.ic_icon__profile, "Service Workers and Shop & Market Sales Workers",3.5f),
//            Workers("Akul", R.drawable.ic_icon__profile, "Skilled Agricultural and Fishery Workers",3.3f),
//            Workers("Adyant", R.drawable.ic_icon__profile, "Skilled Agricultural and Fishery Workers",4.5f),
//            Workers("Adikrit", R.drawable.ic_icon__profile, "Skilled Agricultural and Fishery Workers",2.1f),
//            Workers("Amay", R.drawable.ic_icon__profile, "Craft and Related Trades Workers",2.3f),
//            Workers("Jane Smith", R.drawable.ic_icon__profile, "Craft and Related Trades Workers",2.4f),
//            Workers("Agniv", R.drawable.ic_icon__profile, "Craft and Related Trades Workers",5.0f),
//            Workers("Agastya", R.drawable.ic_icon__profile, "Plant and Machine Operators and Assemblers",2.3f),
//            Workers("Anvay", R.drawable.ic_icon__profile, "Plant and Machine Operators and Assemblers",2.4f),
//            Workers("Ainesh", R.drawable.ic_icon__profile, "Plant and Machine Operators and Assemblers",5.0f),
//            Workers("Anik", R.drawable.ic_icon__profile, "Elementary Occupations",2.3f),
//            Workers("Evyavan", R.drawable.ic_icon__profile, "Elementary Occupations",2.4f),
//            Workers("Ethiraj", R.drawable.ic_icon__profile, "Elementary Occupations",5.0f),
//            Workers("Ekagra", R.drawable.ic_icon__profile, "Professionals",2.0f),
//            Workers("Harshit", R.drawable.ic_icon__profile, "Professionals",4.5f),
//            Workers("Hriday", R.drawable.ic_icon__profile, "Professionals",5.0f),
//            Workers("Hardik", R.drawable.ic_icon__profile, "Associate Professionals",3.5f),
//            Workers("Ibhan", R.drawable.ic_icon__profile, "Associate Professionals",4.5f),
//            Workers("Idhayan", R.drawable.ic_icon__profile, "Associate Professionals",3.5f),
//            Workers("Indranil", R.drawable.ic_icon__profile, "Clerks",2.5f),
//            Workers("Ikshaan", R.drawable.ic_icon__profile, "Clerks",3.5f),
//            Workers("Jay", R.drawable.ic_icon__profile, "Clerks",4.1f),
//            Workers("Kavish", R.drawable.ic_icon__profile, "Service Workers and Shop & Market Sales Workers",1.5f),
//            Workers("Nadin", R.drawable.ic_icon__profile, "Service Workers and Shop & Market Sales Workers",4.5f),
//            Workers("Naman", R.drawable.ic_icon__profile, "Service Workers and Shop & Market Sales Workers",3.5f),
//            Workers("Neil", R.drawable.ic_icon__profile, "Skilled Agricultural and Fishery Workers",3.3f),
//            Workers("Ojasvat", R.drawable.ic_icon__profile, "Skilled Agricultural and Fishery Workers",4.5f),
//            Workers("Parthiv", R.drawable.ic_icon__profile, "Skilled Agricultural and Fishery Workers",2.1f),
//            Workers("Pranay", R.drawable.ic_icon__profile, "Craft and Related Trades Workers",2.3f),
//            Workers("Purnit", R.drawable.ic_icon__profile, "Craft and Related Trades Workers",2.4f),
//            Workers("Priyansh", R.drawable.ic_icon__profile, "Craft and Related Trades Workers",5.0f),
//            Workers("Purnit", R.drawable.ic_icon__profile, "Plant and Machine Operators and Assemblers",2.3f),
//            Workers("Priyansh", R.drawable.ic_icon__profile, "Plant and Machine Operators and Assemblers",2.4f),
//            Workers("Reyaansh", R.drawable.ic_icon__profile, "Plant and Machine Operators and Assemblers",5.0f),
//            Workers("Rijul", R.drawable.ic_icon__profile, "Elementary Occupations",2.3f),
//            Workers("Rohil", R.drawable.ic_icon__profile, "Elementary Occupations",2.4f),
//            Workers("Sahil", R.drawable.ic_icon__profile, "Elementary Occupations",5.0f)


//
//
//
//
//            // Add more workers as needed
//        )
//
//
//
//
//
//        categoryAdapter = WorkCategoryAdapter(categories)
//        workerAdapter = WorkerAdapter(workers) {
//            val intent = Intent(this, WorkerDetailsActivity::class.java)
//            startActivity(intent)
//        }
//
//
//        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
//        recyclerView.adapter = categoryAdapter
//
//
//        listofworkrecyclerView.layoutManager = LinearLayoutManager(this)
//        listofworkrecyclerView.adapter = workerAdapter
//
//
//
//
//
//
//
//        categoryAdapter.setOnItemClickListener { category ->
//            val filteredWorkers = workers.filter { it.category == category.name }
//            workerAdapter.updateList(filteredWorkers)
//        }
//
//
//        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//            override fun onQueryTextSubmit(query: String?): Boolean {
//                return false
//            }
//
//            override fun onQueryTextChange(newText: String?): Boolean {
//                val filteredCategories = categories.filter {
//                    it.name.contains(newText ?: "", ignoreCase = true)
//                }
//                categoryAdapter.updateList(filteredCategories)
//                return true
//            }
//        })
//    }
//
//}