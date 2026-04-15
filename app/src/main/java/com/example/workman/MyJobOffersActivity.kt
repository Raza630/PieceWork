package com.example.workman

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.workman.adaptes.WorkOfferAdapter
import com.example.workman.dataClass.WorkOffer
import com.example.workman.databinding.ActivityMyJobOffersBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Displays all job offers created by the current user (Boss/Hiring role).
 * Features:
 * - Real-time updates via Firestore listener
 * - Swipe to refresh
 * - Empty/Loading/Error states
 * - Toolbar with back navigation
 */
class MyJobOffersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyJobOffersBinding

    // Firebase
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    // Listener registration for cleanup
    private var jobOffersListener: ListenerRegistration? = null

    // Adapter
    private lateinit var workOfferAdapter: WorkOfferAdapter
    private val jobOffersList = mutableListOf<WorkOffer>()

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyJobOffersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        setupEmptyStateAction()

        loadJobOffers()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "My Job Offers"
        }
    }

    private fun setupRecyclerView() {
        workOfferAdapter = WorkOfferAdapter(this, jobOffersList, db)
        binding.recyclerViewJobs.apply {
            layoutManager = LinearLayoutManager(this@MyJobOffersActivity)
            adapter = workOfferAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.apply {
            setColorSchemeResources(R.color.orange_primary)
            setOnRefreshListener {
                loadJobOffers()
            }
        }
    }

    private fun setupEmptyStateAction() {
        binding.btnCreateFirstJob.setOnClickListener {
            startActivity(Intent(this, CreateWorkActivity::class.java))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadJobOffers() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showError("Please sign in to view your jobs")
            return
        }

        showLoading()

        // Remove previous listener if exists
        jobOffersListener?.remove()

        // Query without orderBy to avoid requiring composite index
        // We'll sort client-side instead
        jobOffersListener = db.collection("workOffers")
            .whereEqualTo("bossId", currentUser.uid)
            .addSnapshotListener { snapshots, error ->
                binding.swipeRefreshLayout.isRefreshing = false

                if (error != null) {
                    Log.e(TAG, "Failed to load job offers", error)

                    // Check if it's an index error and provide helpful message
                    val errorMessage = if (error.message?.contains("index") == true) {
                        "Database index required. Please contact support or try again later."
                    } else {
                        "Failed to load job offers: ${error.localizedMessage}"
                    }
                    showError(errorMessage)
                    return@addSnapshotListener
                }

                if (snapshots == null || snapshots.isEmpty) {
                    showEmpty()
                    return@addSnapshotListener
                }

                val newList = snapshots.documents.mapNotNull { document ->
                    try {
                        val createdAtTimestamp = document.getTimestamp("createdAt")
                        WorkOffer(
                            id = document.id,
                            title = document.getString("title") ?: "Untitled",
                            description = document.getString("description") ?: "",
                            date = document.getString("date") ?: "",
                            createdAt = createdAtTimestamp?.toDate()?.let {
                                dateFormatter.format(it)
                            } ?: "",
                            images = (document.get("images") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            acceptedBy = document.getString("acceptedBy"),
                            isAccepted = document.getBoolean("isAccepted") ?: false
                        ) to createdAtTimestamp?.seconds // Keep timestamp for sorting
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing document ${document.id}", e)
                        null
                    }
                }
                // Sort by createdAt descending (newest first) - client-side sorting
                .sortedByDescending { it.second }
                .map { it.first }

                jobOffersList.clear()
                jobOffersList.addAll(newList)
                workOfferAdapter.updateList(newList)

                showContent()
                Log.d(TAG, "Loaded ${newList.size} job offers")
            }
    }

    // region View State Management

    private fun showLoading() {
        binding.apply {
            progressBar.visibility = View.VISIBLE
            recyclerViewJobs.visibility = View.GONE
            emptyStateLayout.visibility = View.GONE
            errorStateLayout.visibility = View.GONE
        }
    }

    private fun showContent() {
        binding.apply {
            progressBar.visibility = View.GONE
            recyclerViewJobs.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            errorStateLayout.visibility = View.GONE
        }
    }

    private fun showEmpty() {
        binding.apply {
            progressBar.visibility = View.GONE
            recyclerViewJobs.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
            errorStateLayout.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        binding.apply {
            progressBar.visibility = View.GONE
            recyclerViewJobs.visibility = View.GONE
            emptyStateLayout.visibility = View.GONE
            errorStateLayout.visibility = View.VISIBLE
            tvErrorMessage.text = message
            btnRetry.setOnClickListener { loadJobOffers() }
        }
    }

    // endregion

    override fun onDestroy() {
        super.onDestroy()
        // Clean up listener to prevent memory leaks
        jobOffersListener?.remove()
        jobOffersListener = null
    }

    companion object {
        private const val TAG = "MyJobOffersActivity"
    }
}

