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
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PropertyName
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

@IgnoreExtraProperties
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
    @get:PropertyName("isVerified") @set:PropertyName("isVerified")
    var verified: Boolean = false,
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
    private var offersSyncListener: ListenerRegistration? = null

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
        observeWorkOffersForBookingSync()
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
        offersSyncListener?.remove()
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
                        bossId = doc.getString("bossId") ?: "",
                        paymentStatus = doc.getString("paymentStatus") ?: "UNPAID",
                        paymentMethod = doc.getString("paymentMethod") ?: "CASH",
                        ratingSubmitted = doc.getBoolean("ratingSubmitted") ?: false
                    )
                } ?: emptyList()
                
                _uiState.update { it.copy(bookings = bookingList) }
            }
    }

    fun onBookingTabSelected(index: Int) {
        _uiState.update { it.copy(selectedBookingTab = index) }
    }

    /**
     * Real-time listener on the boss's own work offers that keeps each linked
     * booking in sync. The moment a job gains an `acceptedBy` (or its status
     * moves to IN_PROGRESS / COMPLETED), the matching booking is promoted to
     * the right state — so the Bookings tabs stay correct even without the
     * Cloud Function (works for in-app accepts AND manual Firestore edits).
     */
    private fun observeWorkOffersForBookingSync() {
        val currentUserId = auth.currentUser?.uid ?: return

        offersSyncListener = db.collection("workOffers")
            .whereEqualTo("bossId", currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                viewModelScope.launch {
                    for (doc in snapshot.documents) {
                        val acceptedBy = doc.getString("acceptedBy")
                        val offerStatus = doc.getString("status") ?: "OPEN"

                        // Nothing to sync until a worker has actually claimed it.
                        if (acceptedBy.isNullOrBlank() ||
                            offerStatus == "OPEN" || offerStatus == "CANCELLED"
                        ) {
                            continue
                        }

                        val jobId = doc.id
                        try {
                            val bookingRef = db.collection("bookings").document(jobId)
                            val bookingSnap = bookingRef.get().await()
                            if (!bookingSnap.exists()) continue

                            val bookingStatus = bookingSnap.getString("status") ?: "PENDING"
                            val target: String? = when (offerStatus) {
                                "ASSIGNED" ->
                                    if (bookingStatus == "PENDING") "ACTIVE" else null

                                "IN_PROGRESS" ->
                                    if (bookingStatus == "PENDING" ||
                                        bookingStatus == "ACTIVE"
                                    ) "IN_PROGRESS" else null

                                "COMPLETED", "REVIEWED" ->
                                    if (bookingStatus != "COMPLETED") "COMPLETED" else null

                                else -> null
                            }

                            if (target != null) {
                                bookingRef.update(
                                    mapOf(
                                        "workerId" to acceptedBy,
                                        "workerName" to (doc.getString("acceptedByName")
                                            ?: "Worker"),
                                        "workerPhotoUrl" to (doc.getString("acceptedByPhoto")
                                            ?: ""),
                                        "status" to target
                                    )
                                ).await()
                                Log.d(TAG, "Booking $jobId synced to $target")
                            }
                        } catch (ex: Exception) {
                            Log.w(TAG, "Booking sync failed for $jobId: ${ex.message}")
                        }
                    }
                }
            }
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
                    BookingStatus.COMPLETED -> mapOf(
                        "status" to "COMPLETED",
                        // Timestamp completion so the worker's weekly earnings are accurate
                        "completedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
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
        val booking = _uiState.value.bookingToRate ?: run {
            Log.w(TAG, "submitRating aborted: no bookingToRate in state")
            return
        }
        val bossId = auth.currentUser?.uid ?: run {
            Log.w(TAG, "submitRating aborted: no authenticated user")
            return
        }

        Log.d(
            TAG,
            "submitRating START → bookingId=${booking.id} jobId=${booking.jobId} " +
                    "workerId=${booking.workerId} rating=$rating"
        )

        if (booking.workerId.isBlank()) {
            Log.e(TAG, "submitRating: workerId is blank — cannot rate. Booking=${booking.id}")
        }

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
                Log.d(TAG, "submitRating: review document saved OK → reviewId=$reviewId")

                // Update worker's total rating in their profile
                val profileUpdated = updateWorkerRating(booking.workerId, rating)

                // Mark this job as rated so the "Rate Worker" CTA disappears.
                // Written to BOTH the booking (drives the Bookings tab) and the
                // work offer (drives WorkOfferDetailsScreen) so the UI agrees
                // everywhere. Best-effort: a failure here must not lose the review.
                val linkedJobId = booking.jobId.ifBlank { booking.id }
                try {
                    db.collection("bookings").document(booking.id)
                        .update("ratingSubmitted", true).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not flag booking as rated: ${e.message}")
                }
                try {
                    db.collection("workOffers").document(linkedJobId)
                        .update(
                            mapOf(
                                "ratingSubmitted" to true,
                                "status" to "REVIEWED"
                            )
                        ).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not flag work offer as rated: ${e.message}")
                }

                if (profileUpdated) {
                    Log.i(
                        TAG,
                        "submitRating SUCCESS ✓ Review saved AND worker profile rating updated " +
                                "(workerId=${booking.workerId})"
                    )
                } else {
                    Log.w(
                        TAG,
                        "submitRating PARTIAL ⚠ Review saved but worker profile rating NOT updated " +
                                "(likely Firestore rules not deployed). workerId=${booking.workerId}"
                    )
                }

                _uiState.update { state ->
                    state.copy(
                        showRatingDialog = false,
                        bookingToRate = null,
                        // Optimistically flip the flag so the Rate CTA disappears
                        // immediately, before the Firestore listener echoes back.
                        bookings = state.bookings.map { b ->
                            if (b.id == booking.id) b.copy(ratingSubmitted = true) else b
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "submitRating FAILED ✗ Could not save review for booking=${booking.id}",
                    e
                )
            }
        }
    }

    /**
     * Updates the worker's aggregate rating fields in their profile.
     * @return true if the transaction committed, false on any failure.
     */
    private suspend fun updateWorkerRating(workerId: String, newRating: Float): Boolean {
        return try {
            val workerRef = db.collection("users").document(workerId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(workerRef)
                val currentTotal = snapshot.getLong("totalRatings") ?: 0L
                val currentAvg = snapshot.getDouble("averageRating") ?: 0.0

                val newTotal = currentTotal + 1
                val newAvg = ((currentAvg * currentTotal) + newRating) / newTotal

                Log.d(
                    TAG,
                    "updateWorkerRating: workerId=$workerId " +
                            "oldAvg=$currentAvg oldTotal=$currentTotal → newAvg=$newAvg newTotal=$newTotal"
                )

                transaction.update(
                    workerRef, mapOf(
                        "totalRatings" to newTotal,
                        "averageRating" to newAvg,
                        "rating" to newAvg // Sync with old field if used
                    )
                )
            }.await()
            Log.d(TAG, "updateWorkerRating: transaction committed OK for workerId=$workerId")
            true
        } catch (e: Exception) {
            Log.e(
                TAG,
                "updateWorkerRating: FAILED for workerId=$workerId — ${e.message}. " +
                        "If this is PERMISSION_DENIED, deploy firestore.rules.",
                e
            )
            false
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

    /**
     * Open the rating dialog for a given (usually completed) booking so the boss
     * can leave feedback for the worker at any time from the History tab.
     */
    fun openRatingDialog(booking: BookingUiModel) {
        if (booking.workerId.isBlank()) return
        // Guard against double-reviewing the same job.
        if (booking.ratingSubmitted) return
        _uiState.update { it.copy(bookingToRate = booking, showRatingDialog = true) }
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
                        rating = doc.getDouble("averageRating") ?: raw.rating,
                        reviewCount = (doc.getLong("totalRatings")?.toString())
                            ?: raw.reviewCount,
                        ratePerHour      = raw.ratePerHour,
                        photoUrl = raw.photoUrl,
                        isVerified = raw.verified,
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

        Log.d(
            TAG,
            "Radius filter → viewerLoc=($userLatitude,$userLongitude) " +
                    "radius=${state.searchRadiusKm}km " +
                    "total=${allWorkers.size} passed=${filtered.size} " +
                    "distances=${allWorkers.map { it.distanceKm }}"
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
