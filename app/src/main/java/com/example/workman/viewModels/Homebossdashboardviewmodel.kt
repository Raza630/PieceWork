package com.example.workman.viewModels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workman.dataClass.BookingStatus
import com.example.workman.dataClass.BookingUiModel
import com.example.workman.dataClass.Report
import com.example.workman.dataClass.Review
import com.example.workman.dataClass.WorkerUiModel
import com.example.workman.utils.LocationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ─── UI State ──────────────────────────────────────────────────────────────────

sealed class WorkerListState {
    object Loading : WorkerListState()
    data class Success(val workers: List<WorkerUiModel>) : WorkerListState()
    data class Error(val message: String) : WorkerListState()
}

data class DashboardUiState(
    val workerListState: WorkerListState = WorkerListState.Loading,
    val searchQuery: String = "",
    val serviceSearchQuery: String = "",
    val selectedCategory: String = "All",
    val selectedCategories: Set<String> = emptySet(), // Multi-select filter
    val filteredWorkers: List<WorkerUiModel> = emptyList(),
    val popularServices: List<WorkerUiModel> = emptyList(),
    val bookings: List<BookingUiModel> = emptyList(),
    val selectedBookingTab: Int = 0, // 0: Pending, 1: Active, 2: History
    val bookingToRate: BookingUiModel? = null,
    val showRatingDialog: Boolean = false,
    val userName: String = "Boss",
    val userLocation: String = "Detecting location...",
    // Location-based filtering
    val searchRadiusKm: Double = LocationHelper.DEFAULT_RADIUS_KM,
    val isLocationAvailable: Boolean = false,
    val nearbyWorkerCount: Int = 0,
    val showReportDialog: Boolean = false,
    val entityToReport: String? = null, // userId or jobId
    val showFilterSheet: Boolean = false
)

// ─── Firestore Worker Document Model ──────────────────────────────────────────

data class WorkerDocument(
    val name: String = "",
    val category: String = "",
    val skills: String = "",
    val yearsOfExperience: Int = 0,
    val rating: Double = 0.0,
    val reviewCount: String = "0",
    val ratePerHour: Int = 0,
    val photoUrl: String = "",
    val role: String = "",
    val isVerified: Boolean = false,
    // Location fields
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val geohash: String = "",
    val location: String = ""
)

// ─── ViewModel ─────────────────────────────────────────────────────────────────

class HomeBossDashboardViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var bookingsListener: ListenerRegistration? = null

    // Raw fetched list — never modified after fetch
    private var allWorkers: List<WorkerUiModel> = emptyList()

    // Current user's location
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "BossDashboardVM"
    }

    init {
        loadUserData()
        fetchWorkers()
        observeBookings()
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

                _uiState.update { it.copy(
                    userName = doc.getString("name") ?: "Boss",
                    userLocation = doc.getString("location") ?: "Not set",
                    isLocationAvailable = lat != 0.0 && lng != 0.0
                ) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load user data", e)
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

                _uiState.update {
                    it.copy(
                        userLocation = locationResult.locationName,
                        isLocationAvailable = true
                    )
                }

                // Re-apply location filter now that we have coordinates
                refilterWorkers()
            } else {
                _uiState.update { it.copy(userLocation = "Location unavailable") }
            }
        }
    }

    /**
     * Change the search radius and re-filter workers.
     */
    fun updateSearchRadius(radiusKm: Double) {
        val clamped = radiusKm.coerceIn(LocationHelper.MIN_RADIUS_KM, LocationHelper.MAX_RADIUS_KM)
        _uiState.update { it.copy(searchRadiusKm = clamped) }
        refilterWorkers()
    }

    override fun onCleared() {
        super.onCleared()
        bookingsListener?.remove()
    }

    // ── Real-time Bookings ──────────────────────────────────────────────────

    private fun observeBookings() {
        val currentUserId = auth.currentUser?.uid ?: return
        
        bookingsListener = db.collection("bookings")
            .whereEqualTo("bossId", currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                val bookingList = snapshot?.documents?.mapNotNull { doc ->
                    val statusStr = doc.getString("status") ?: "PENDING"
                    BookingUiModel(
                        id = doc.id,
                        jobId = doc.getString("jobId") ?: "",
                        workerId = doc.getString("workerId") ?: "",
                        workerName = doc.getString("workerName") ?: "Worker",
                        workerPhotoUrl = doc.getString("workerPhotoUrl") ?: "",
                        serviceName = doc.getString("serviceName") ?: "Service",
                        agreedRate = doc.getString("agreedRate") ?: "0",
                        status = BookingStatus.valueOf(statusStr),
                        date = doc.getDate("date") ?: java.util.Date(),
                        bossId = doc.getString("bossId") ?: ""
                    )
                } ?: emptyList()
                
                _uiState.update { it.copy(bookings = bookingList) }
            }
    }

    fun onBookingTabSelected(index: Int) {
        _uiState.update { it.copy(selectedBookingTab = index) }
    }

    fun updateBookingStatus(bookingId: String, newStatus: BookingStatus) {
        viewModelScope.launch {
            try {
                db.collection("bookings").document(bookingId)
                    .update("status", newStatus.name)
                    .await()

                // Keep the linked Work Offer in sync
                val booking = _uiState.value.bookings.find { it.id == bookingId }
                val linkedJobId = booking?.jobId?.ifBlank { bookingId } ?: bookingId
                val updates: Map<String, Any?>? = when (newStatus) {
                    BookingStatus.IN_PROGRESS -> mapOf("status" to "IN_PROGRESS")
                    BookingStatus.COMPLETED -> mapOf("status" to "COMPLETED")
                    BookingStatus.CANCELLED -> mapOf(
                        // Mark cancelled so it leaves the worker feed and active lists
                        "status" to "CANCELLED",
                        "acceptedBy" to null,
                        "isAccepted" to false
                    )

                    else -> null
                }
                if (updates != null) {
                    db.collection("workOffers").document(linkedJobId)
                        .update(updates)
                        .addOnFailureListener { Log.w(TAG, "Failed to sync work offer status", it) }
                }

                if (newStatus == BookingStatus.COMPLETED) {
                    _uiState.update { it.copy(bookingToRate = booking, showRatingDialog = true) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update booking status", e)
            }
        }
    }

    fun submitRating(rating: Float, reviewComment: String) {
        val booking = _uiState.value.bookingToRate ?: return
        val bossId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                val reviewId = db.collection("reviews").document().id
                val review = Review(
                    reviewId = reviewId,
                    jobId = booking.id,
                    reviewerId = bossId,
                    reviewerName = _uiState.value.userName,
                    revieweeId = booking.workerId,
                    rating = rating,
                    comment = reviewComment,
                    timestamp = FieldValue.serverTimestamp()
                )

                db.collection("reviews").document(reviewId).set(review).await()

                // Update worker's total rating in their profile
                updateWorkerRating(booking.workerId, rating)
                
                _uiState.update { it.copy(showRatingDialog = false, bookingToRate = null) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to submit rating", e)
            }
        }
    }

    private suspend fun updateWorkerRating(workerId: String, newRating: Float) {
        try {
            val workerRef = db.collection("users").document(workerId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(workerRef)
                val currentTotal = snapshot.getLong("totalRatings") ?: 0L
                val currentAvg = snapshot.getDouble("averageRating") ?: 0.0

                val newTotal = currentTotal + 1
                val newAvg = ((currentAvg * currentTotal) + newRating) / newTotal

                transaction.update(
                    workerRef, mapOf(
                        "totalRatings" to newTotal,
                        "averageRating" to newAvg,
                        "rating" to newAvg // Sync with old field if used
                    )
                )
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update worker rating profile", e)
        }
    }

    fun submitReport(entityId: String, type: String, reason: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val reportId = db.collection("reports").document().id
                val report = Report(
                    reportId = reportId,
                    reporterId = userId,
                    reportedEntityId = entityId,
                    reportType = type,
                    reason = reason,
                    timestamp = com.google.firebase.Timestamp.now()
                )
                db.collection("reports").document(reportId).set(report).await()
                _uiState.update { it.copy(showReportDialog = false, entityToReport = null) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to submit report", e)
            }
        }
    }

    fun openReportDialog(entityId: String) {
        _uiState.update { it.copy(showReportDialog = true, entityToReport = entityId) }
    }

    fun closeReportDialog() {
        _uiState.update { it.copy(showReportDialog = false, entityToReport = null) }
    }

    fun dismissRatingDialog() {
        _uiState.update { it.copy(showRatingDialog = false, bookingToRate = null) }
    }

    // ── Firestore Fetch ────────────────────────────────────────────────────────

    fun fetchWorkers() {
        viewModelScope.launch {
            _uiState.update { it.copy(workerListState = WorkerListState.Loading) }

            try {
                val snapshot = db.collection("users")
                    .whereEqualTo("role", "Worker")
                    .get()
                    .await()

                allWorkers = snapshot.documents.mapNotNull { doc ->
                    val raw = doc.toObject(WorkerDocument::class.java) ?: return@mapNotNull null
                    val workerLat = raw.latitude
                    val workerLng = raw.longitude

                    // Calculate distance if we have both locations
                    val distance = if (userLatitude != 0.0 && userLongitude != 0.0 &&
                        workerLat != 0.0 && workerLng != 0.0
                    ) {
                        LocationHelper.calculateDistance(
                            userLatitude, userLongitude,
                            workerLat, workerLng
                        )
                    } else -1.0

                    WorkerUiModel(
                        id               = doc.id,
                        name             = raw.name,
                        category         = raw.category,
                        yearsOfExperience = raw.yearsOfExperience,
                        rating           = raw.rating,
                        reviewCount      = raw.reviewCount,
                        ratePerHour      = raw.ratePerHour,
                        photoUrl = raw.photoUrl,
                        isVerified = raw.isVerified,
                        latitude = workerLat,
                        longitude = workerLng,
                        locationName = raw.location,
                        distanceKm = distance,
                        workerLevel = doc.getString("workerLevel") ?: "BRONZE",
                        completedJobsCount = doc.getLong("completedJobsCount")?.toInt() ?: 0,
                        phone = doc.getString("phone") ?: ""
                    )
                }

                refilterWorkers()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(workerListState = WorkerListState.Error(e.message ?: "Unknown error"))
                }
            }
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery     = query,
                filteredWorkers = applyFilters(
                    allWorkers,
                    query,
                    state.selectedCategory,
                    state.searchRadiusKm
                )
            )
        }
    }

    fun onServiceSearchQueryChange(query: String) {
        _uiState.update { it.copy(serviceSearchQuery = query) }
    }

    // ── Category Filter ───────────────────────────────────────────────────────

    fun onCategorySelected(category: String) {
        _uiState.update { state ->
            val newCategory = if (state.selectedCategory == category) "All" else category
            state.copy(
                selectedCategory = newCategory,
                filteredWorkers = applyFilters(
                    allWorkers,
                    state.searchQuery,
                    newCategory,
                    state.searchRadiusKm,
                    state.selectedCategories
                )
            )
        }
    }

    /**
     * Toggle a category in multi-select filter.
     */
    fun toggleCategoryFilter(category: String) {
        _uiState.update { state ->
            val newSet = if (state.selectedCategories.contains(category)) {
                state.selectedCategories - category
            } else {
                state.selectedCategories + category
            }
            state.copy(selectedCategories = newSet)
        }
    }

    /**
     * Apply the filter selections from the bottom sheet.
     */
    fun applyFilterSheet() {
        _uiState.update { it.copy(showFilterSheet = false) }
        refilterWorkers()
    }

    /**
     * Clear all filter selections.
     */
    fun clearFilters() {
        _uiState.update {
            it.copy(
                selectedCategories = emptySet(),
                searchRadiusKm = LocationHelper.DEFAULT_RADIUS_KM,
                selectedCategory = "All"
            )
        }
        refilterWorkers()
    }

    fun toggleFilterSheet() {
        _uiState.update { it.copy(showFilterSheet = !it.showFilterSheet) }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun refilterWorkers() {
        // Recalculate distances if location just became available
        if (userLatitude != 0.0 && userLongitude != 0.0) {
            allWorkers = allWorkers.map { worker ->
                if (worker.latitude != 0.0 && worker.longitude != 0.0) {
                    val dist = LocationHelper.calculateDistance(
                        userLatitude, userLongitude,
                        worker.latitude, worker.longitude
                    )
                    worker.copy(distanceKm = dist)
                } else worker
            }
        }

        val state = _uiState.value
        val filtered = applyFilters(
            allWorkers,
            state.searchQuery,
            state.selectedCategory,
            state.searchRadiusKm,
            state.selectedCategories
        )

        _uiState.update {
            it.copy(
                workerListState = WorkerListState.Success(allWorkers),
                filteredWorkers = filtered,
                popularServices = allWorkers
                    .filter { w -> w.distanceKm in 0.0..state.searchRadiusKm || w.distanceKm < 0 }
                    .sortedByDescending { w -> w.rating }
                    .take(5),
                nearbyWorkerCount = filtered.size
            )
        }
    }

    private fun applyFilters(
        workers: List<WorkerUiModel>,
        query: String,
        category: String,
        radiusKm: Double,
        selectedCategories: Set<String> = emptySet()
    ): List<WorkerUiModel> {
        return workers.filter { worker ->
            val matchesSearch = query.isBlank() ||
                    worker.name.contains(query, ignoreCase = true) ||
                    worker.category.contains(query, ignoreCase = true)

            val matchesCategory = if (selectedCategories.isNotEmpty()) {
                // Multi-select: match any of the selected categories
                selectedCategories.any { selectedCat ->
                    worker.category.trim().equals(selectedCat.trim(), ignoreCase = true) ||
                            worker.category.contains(selectedCat, ignoreCase = true)
                }
            } else if (category == "All") {
                true
            } else {
                worker.category.trim().equals(category.trim(), ignoreCase = true) ||
                worker.category.contains(category, ignoreCase = true)
            }

            // Location filter: show workers within radius, OR if no location data available
            val matchesLocation = if (userLatitude == 0.0 || userLongitude == 0.0) {
                true // No user location → show all
            } else if (worker.latitude == 0.0 && worker.longitude == 0.0) {
                true // Worker hasn't set location → still show them (graceful fallback)
            } else {
                worker.distanceKm <= radiusKm
            }

            matchesSearch && matchesCategory && matchesLocation
        }
            .sortedBy { it.distanceKm.let { d -> if (d < 0) Double.MAX_VALUE else d } } // Nearest first
    }
}
