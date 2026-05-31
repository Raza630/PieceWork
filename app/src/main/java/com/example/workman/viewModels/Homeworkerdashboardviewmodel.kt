package com.example.workman.viewModels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workman.dataClass.Banner
import com.example.workman.dataClass.WorkOffer
import com.example.workman.utils.JobMatchingEngine
import com.example.workman.utils.LocationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

sealed class WorkOfferListState {
    object Loading : WorkOfferListState()
    data class Success(val offers: List<WorkOffer>) : WorkOfferListState()
    data class Error(val message: String) : WorkOfferListState()
}

data class WorkerDashboardUiState(
    val offerListState: WorkOfferListState = WorkOfferListState.Loading,
    val banners: List<Banner> = emptyList(),
    val searchQuery: String = "",
    val userName: String = "Worker",
    val userLocation: String = "Detecting location...",
    val filteredOffers: List<WorkOffer> = emptyList(),
    val isRefreshing: Boolean = false,
    val acceptingOfferIds: Set<String> = emptySet(),
    // Location-based filtering
    val searchRadiusKm: Double = LocationHelper.DEFAULT_RADIUS_KM,
    val isLocationAvailable: Boolean = false,
    val nearbyOfferCount: Int = 0,
    val isCompleting: Boolean = false,
    // Smart matching
    val recommendedOffers: List<JobMatchingEngine.ScoredOffer> = emptyList(),
    val otherOffers: List<JobMatchingEngine.ScoredOffer> = emptyList(),
    /** Map of offerId → ScoredOffer for quick lookup in UI */
    val offerScores: Map<String, JobMatchingEngine.ScoredOffer> = emptyMap()
)

class HomeWorkerDashboardViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    private var allOffers: List<WorkOffer> = emptyList()

    // Current user's location
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0

    // Worker profile for matching
    private var workerProfile = JobMatchingEngine.WorkerProfile()

    private val _uiState = MutableStateFlow(WorkerDashboardUiState())
    val uiState: StateFlow<WorkerDashboardUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "WorkerDashboardVM"
    }

    init {
        loadUserData()
        fetchBanners()
        fetchWorkOffers()
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(user.uid).get().await()
                val lat = doc.getDouble("latitude") ?: 0.0
                val lng = doc.getDouble("longitude") ?: 0.0
                userLatitude = lat
                userLongitude = lng

                val workerCategory = doc.getString("category") ?: ""

                _uiState.update { it.copy(
                    userName = doc.getString("name") ?: "Worker",
                    userLocation = doc.getString("location") ?: "Not set",
                    isLocationAvailable = lat != 0.0 && lng != 0.0
                ) }

                // Build worker profile for matching
                workerProfile = workerProfile.copy(
                    category = workerCategory,
                    latitude = lat,
                    longitude = lng
                )

                // Load acceptance history for smart matching
                loadAcceptanceHistory(user.uid)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load user data", e)
            }
        }
    }

    /**
     * Load the worker's past accepted jobs to build category history for matching.
     */
    private suspend fun loadAcceptanceHistory(userId: String) {
        try {
            val snapshot = db.collection("workOffers")
                .whereEqualTo("acceptedBy", userId)
                .get()
                .await()

            val categoryCount = mutableMapOf<String, Int>()
            for (doc in snapshot.documents) {
                val cat = doc.getString("category") ?: ""
                if (cat.isNotBlank()) {
                    categoryCount[cat] = (categoryCount[cat] ?: 0) + 1
                }
            }

            workerProfile = workerProfile.copy(
                acceptedCategoryHistory = categoryCount,
                totalAcceptedJobs = snapshot.size()
            )

            Log.d(
                TAG,
                "Loaded acceptance history: ${snapshot.size()} jobs, categories: $categoryCount"
            )

            // Re-score offers if they're already loaded
            if (allOffers.isNotEmpty()) {
                refilterOffers()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load acceptance history", e)
        }
    }

    fun fetchBanners() {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("banners").get().await()
                val banners = snapshot.toObjects(Banner::class.java)
                _uiState.update { it.copy(banners = banners) }
            } catch (e: Exception) {
                // Ignore banner errors
            }
        }
    }

    fun fetchWorkOffers() {
        val currentUserId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(offerListState = WorkOfferListState.Loading, isRefreshing = true) }
            try {
                val snapshot = db.collection("workOffers")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                allOffers = snapshot.documents.mapNotNull { doc ->
                    val acceptedBy = doc.getString("acceptedBy")
                    // Show if open OR accepted by me
                    if (acceptedBy == null || acceptedBy == currentUserId) {
                        val offerLat = doc.getDouble("latitude") ?: 0.0
                        val offerLng = doc.getDouble("longitude") ?: 0.0

                        // Calculate distance if both locations available
                        val distance = if (userLatitude != 0.0 && userLongitude != 0.0 &&
                            offerLat != 0.0 && offerLng != 0.0
                        ) {
                            LocationHelper.calculateDistance(
                                userLatitude, userLongitude,
                                offerLat, offerLng
                            )
                        } else -1.0

                        WorkOffer(
                            title = doc.getString("title") ?: "Untitled",
                            description = doc.getString("description") ?: "",
                            date = doc.getString("date") ?: "",
                            createdAt = doc.getTimestamp("createdAt")?.toDate()?.let {
                                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                            } ?: "",
                            images = doc.get("images") as? List<String> ?: emptyList(),
                            id = doc.id,
                            acceptedBy = acceptedBy,
                            isAccepted = doc.getBoolean("isAccepted") ?: false,
                            category = doc.getString("category") ?: "",
                            latitude = offerLat,
                            longitude = offerLng,
                            geohash = doc.getString("geohash") ?: "",
                            locationName = doc.getString("locationName") ?: "",
                            distanceKm = distance
                        )
                    } else null
                }

                refilterOffers()

            } catch (e: Exception) {
                _uiState.update { it.copy(
                    offerListState = WorkOfferListState.Error(e.message ?: "Unknown error"),
                    isRefreshing = false
                ) }
            }
        }
    }

    fun updateLocation(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(userLocation = "Detecting...") }

            val locationResult = LocationHelper.syncLocationToFirebase(context)
            if (locationResult != null) {
                userLatitude = locationResult.latitude
                userLongitude = locationResult.longitude

                workerProfile = workerProfile.copy(
                    latitude = locationResult.latitude,
                    longitude = locationResult.longitude
                )

                _uiState.update {
                    it.copy(
                        userLocation = locationResult.locationName,
                        isLocationAvailable = true
                    )
                }

                // Re-apply location filter with new coordinates
                refilterOffers()
            } else {
                _uiState.update { it.copy(userLocation = "Location unavailable") }
            }
        }
    }

    /**
     * Change the search radius and re-filter offers.
     */
    fun updateSearchRadius(radiusKm: Double) {
        val clamped = radiusKm.coerceIn(LocationHelper.MIN_RADIUS_KM, LocationHelper.MAX_RADIUS_KM)
        _uiState.update { it.copy(searchRadiusKm = clamped) }
        refilterOffers()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        refilterOffers()
    }

    private fun refilterOffers() {
        // Recalculate distances if location just became available
        if (userLatitude != 0.0 && userLongitude != 0.0) {
            allOffers = allOffers.map { offer ->
                if (offer.latitude != 0.0 && offer.longitude != 0.0) {
                    val dist = LocationHelper.calculateDistance(
                        userLatitude, userLongitude,
                        offer.latitude, offer.longitude
                    )
                    offer.copy(distanceKm = dist)
                } else offer
            }
        }

        val state = _uiState.value
        val filtered = applyFilters(allOffers, state.searchQuery, state.searchRadiusKm)

        // Score and partition offers using the matching engine
        val scoredOffers = JobMatchingEngine.scoreOffers(filtered, workerProfile)
        val (recommended, other) = JobMatchingEngine.partitionOffers(scoredOffers)

        // Build score lookup map
        val scoreMap = scoredOffers.associateBy { it.offer.id }

        // For the flat filteredOffers list, use scored order (recommended first, then other)
        val sortedOffers = (recommended + other).map { it.offer }

        _uiState.update {
            it.copy(
                offerListState = WorkOfferListState.Success(allOffers),
                filteredOffers = sortedOffers,
                isRefreshing = false,
                nearbyOfferCount = filtered.size,
                recommendedOffers = recommended,
                otherOffers = other,
                offerScores = scoreMap
            )
        }
    }

    private fun applyFilters(
        offers: List<WorkOffer>,
        query: String,
        radiusKm: Double
    ): List<WorkOffer> {
        return offers.filter { offer ->
            val matchesSearch = query.isBlank() ||
                    offer.title.contains(query, ignoreCase = true) ||
                    offer.description.contains(query, ignoreCase = true) ||
                    offer.category.contains(query, ignoreCase = true)

            // Location filter: show offers within radius, OR if no location data available
            val matchesLocation = if (userLatitude == 0.0 || userLongitude == 0.0) {
                true // No user location → show all
            } else if (offer.latitude == 0.0 && offer.longitude == 0.0) {
                true // Offer doesn't have location → still show (backward compatible)
            } else {
                offer.distanceKm <= radiusKm
            }

            matchesSearch && matchesLocation
        }
    }

    fun submitReport(entityId: String, type: String, reason: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val reportId = db.collection("reports").document().id
                val report = com.example.workman.dataClass.Report(
                    reportId = reportId,
                    reporterId = userId,
                    reportedEntityId = entityId,
                    reportType = type,
                    reason = reason
                )
                db.collection("reports").document(reportId).set(report).await()
            } catch (e: Exception) {
                Log.e(TAG, "Report failed", e)
            }
        }
    }

    fun acceptWork(workOffer: WorkOffer, onResult: (Boolean, String) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val userName = auth.currentUser?.displayName ?: "Worker"
        val userPhoto = auth.currentUser?.photoUrl?.toString() ?: ""

        viewModelScope.launch {
            _uiState.update { it.copy(acceptingOfferIds = it.acceptingOfferIds + workOffer.id) }
            try {
                db.collection("workOffers")
                    .document(workOffer.id)
                    .update(
                        mapOf(
                            "acceptedBy" to userId,
                            "acceptedByName" to userName,
                            "acceptedByPhoto" to userPhoto,
                            "status" to "ASSIGNED",
                            "isAccepted" to true
                        )
                    )
                    .await()

                // Update local list instead of full refresh
                allOffers = allOffers.map {
                    if (it.id == workOffer.id) {
                        it.copy(acceptedBy = userId, isAccepted = true)
                    } else it
                }

                _uiState.update { state ->
                    state.copy(
                        acceptingOfferIds = state.acceptingOfferIds - workOffer.id
                    )
                }
                refilterOffers()
                onResult(true, "Work accepted successfully!")
            } catch (e: Exception) {
                _uiState.update { it.copy(acceptingOfferIds = it.acceptingOfferIds - workOffer.id) }
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    fun completeWork(
        workOffer: WorkOffer,
        images: List<Uri>,
        note: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (images.isEmpty()) {
            onResult(false, "At least one 'After' photo is required.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCompleting = true) }
            try {
                val imageUrls = mutableListOf<String>()
                images.forEach { uri ->
                    val ref =
                        storage.reference.child("completion_images/${workOffer.id}/${System.currentTimeMillis()}")
                    ref.putFile(uri).await()
                    imageUrls.add(ref.downloadUrl.await().toString())
                }

                db.collection("workOffers").document(workOffer.id).update(
                    mapOf(
                        "status" to "COMPLETED",
                        "completionImages" to imageUrls,
                        "completionNote" to note
                    )
                ).await()

                onResult(true, "Job completed successfully!")
                fetchWorkOffers() // Refresh list
            } catch (e: Exception) {
                onResult(false, "Failed to complete job: ${e.message}")
            } finally {
                _uiState.update { it.copy(isCompleting = false) }
            }
        }
    }
}
