package com.example.workman

import android.Manifest
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.workman.adaptes.BannerAdapter
import com.example.workman.adaptes.WorkOfferAdapter
import com.example.workman.dataClass.Banner
import com.example.workman.dataClass.WorkOffer
import com.example.workman.databinding.ActivityMainWorkerDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class HomeWorkerDashboardActivity : BaseBottomNavigationActivity() {

    private lateinit var binding: ActivityMainWorkerDashboardBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    
    private lateinit var workOfferAdapter: WorkOfferAdapter
    private val workOffersList = mutableListOf<WorkOffer>()
    private val bannerList = mutableListOf<Banner>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainWorkerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupUI()
        loadUserData()
        fetchBanners()
        loadWorkOffers()
        requestNotificationPermission()
    }

    private fun setupUI() {
        setupBottomNavigation(binding.bottomNavigation)
        updateBottomNavigationSelection(binding.bottomNavigation, R.id.nav_home)

        // Setup Banners
        binding.bannerRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        
        // Setup Work Offers (using kriyaScrollView for now as per your XML)
        workOfferAdapter = WorkOfferAdapter(this, workOffersList, db)
        binding.kriyaScrollView.layoutManager = LinearLayoutManager(this)
        binding.kriyaScrollView.adapter = workOfferAdapter
        
        // Header Text
        binding.kriyaHeader.text = "Available Work Offers"
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
            val name = doc.getString("name") ?: "Worker"
            binding.greetingText.text = "Hello, $name!"
            
            // Location could be dynamic if you have it in Firestore
            val location = doc.getString("location") ?: "Bangalore"
            binding.locationText.text = location
        }
    }

    private fun fetchBanners() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val snapshot = db.collection("banners").get().await()
                val fetchedBanners = snapshot.toObjects(Banner::class.java)
                withContext(Dispatchers.Main) {
                    bannerList.clear()
                    bannerList.addAll(fetchedBanners)
                    binding.bannerRecyclerView.adapter = BannerAdapter(bannerList)
                }
            } catch (e: Exception) {
                Log.e("WorkerDashboard", "Banner Error: ${e.message}")
            }
        }
    }

    private fun loadWorkOffers() {
        // We only want to see offers that are NOT yet accepted or were accepted by ME
        val currentUserId = auth.currentUser?.uid ?: ""
        
        db.collection("workOffers")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("WorkerDashboard", "Firestore Error: ${e.message}")
                    return@addSnapshotListener
                }

                workOffersList.clear()
                snapshots?.forEach { doc ->
                    val acceptedBy = doc.getString("acceptedBy")
                    
                    // Business Logic: Show if it's open OR if I am the one who accepted it
                    if (acceptedBy == null || acceptedBy == currentUserId) {
                        val workOffer = WorkOffer(
                            title = doc.getString("title") ?: "Untitled",
                            description = doc.getString("description") ?: "",
                            date = doc.getString("date") ?: "",
                            createdAt = doc.getTimestamp("createdAt")?.toDate()?.let {
                                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                            } ?: "",
                            images = doc.get("images") as? List<String> ?: emptyList(),
                            id = doc.id,
                            acceptedBy = acceptedBy,
                            isAccepted = doc.getBoolean("isAccepted") ?: false
                        )
                        workOffersList.add(workOffer)
                    }
                }
                workOfferAdapter.notifyDataSetChanged()
            }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateBottomNavigationSelection(binding.bottomNavigation, R.id.nav_home)
    }

    companion object {
        const val REQUEST_NOTIFICATION_PERMISSION = 101
    }
}
