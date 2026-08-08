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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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

/**
 * Sort options for the work-offer list.
 */
enum class OfferSortOption(val label: String) {
    BEST_MATCH("Best Match"),
    NEAREST("Nearest"),
    HIGHEST_PAY("Highest Pay"),
    MOST_RECENT("Most Recent"),
    MOST_URGENT("Most Urgent")
}

/**
 * Aggregated earnings & motivation data for the worker dashboard header.
 */
data class EarningsSummary(
    val weeklyEarnings: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val completedThisWeek: Int = 0,
    val completedTotal: Int = 0,
    val pendingPayout: Int = 0,
    val weeklyGoal: Double = 0.0,
    val workerLevel: String = "BRONZE",
    val currency: String = "Rs"
)

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
    val offerScores: Map<String, JobMatchingEngine.ScoredOffer> = emptyMap(),
    // Earnings & motivation
    val earnings: EarningsSummary = EarningsSummary(),
    // Discovery controls
    val sortOption: OfferSortOption = OfferSortOption.BEST_MATCH,
    val selectedCategory: String = "All",
    val availableCategories: List<String> = listOf("All"),
    // Saved / bookmarked jobs
    val savedOfferIds: Set<String> = emptySet(),
    // Active work management
    val activeJobs: List<WorkOffer> = emptyList(),
    // Onboarding / empty-state intelligence
    val workerCategory: String = "",
    val suggestedRadius: Double? = null,
    val suggestedRadiusCount: Int = 0
)

class HomeWorkerDashboardViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    private var allOffers: List<WorkOffer> = emptyList()

    /** Confirmed/pending payment records where this user is the worker. */
    private var myPayments: List<com.example.workman.dataClass.PaymentRecord> = emptyList()

    /** Live Firestore listener so accepted/removed jobs update in real time. */
    private var offersListener: ListenerRegistration? = null

    /** Live listener on this worker's payment records. */
    private var paymentsListener: ListenerRegistration? = null

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
        observePayments()
    }

    /**
     * Live listener on this worker's payment records so earnings update the
     * instant a boss marks a job paid / the worker confirms receipt.
     */
    private fun observePayments() {
        val userId = auth.currentUser?.uid ?: return
        paymentsListener = com.example.workman.utils.PaymentRepository.listenForUser(
            field = "workerId",
            userId = userId
        ) { records ->
            myPayments = records
            _uiState.update { it.copy(earnings = computeEarnings(it.earnings)) }
        }
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

                @Suppress("UNCHECKED_CAST")
                val savedIds = (doc.get("savedOffers") as? List<String>)?.toSet() ?: emptySet()
                val weeklyGoal = doc.getDouble("weeklyGoal") ?: 0.0
                val workerLevel = doc.getString("workerLevel") ?: "BRONZE"

                _uiState.update { it.copy(
                    userName = doc.getString("name") ?: "Worker",
                    userLocation = doc.getString("location") ?: "Not set",
                    isLocationAvailable = lat != 0.0 && lng != 0.0,
                    savedOfferIds = savedIds,
                    workerCategory = workerCategory,
                    earnings = it.earnings.copy(
                        weeklyGoal = weeklyGoal,
                        workerLevel = workerLevel
                    )
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

    /**
     * Fetches the latest work offers and keeps them live via a Firestore
     * snapshot listener, so a job accepted by another worker (or removed)
     * updates on this screen in real time instead of failing on tap.
     *
     * @param showLoading when true the list switches to the skeleton/loading state.
     *        Pass false for background refreshes (e.g. on resume) to avoid flicker.
     */
    fun fetchWorkOffers(showLoading: Boolean = true) {
        val currentUserId = auth.currentUser?.uid ?: return

        _uiState.update {
            it.copy(
                offerListState = if (showLoading) WorkOfferListState.Loading else it.offerListState,
                isRefreshing = true
            )
        }

        // Remove any previous listener before re-registering
        offersListener?.remove()

        // NOTE: We intentionally do NOT use .orderBy("createdAt") here.
        // Firestore's orderBy silently EXCLUDES any document that is missing
        // the field. Fetching all and sorting client-side guarantees every
        // offer is considered.
        offersListener = db.collection("workOffers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update {
                        it.copy(
                            offerListState = WorkOfferListState.Error(
                                error.message ?: "Unknown error"
                            ),
                            isRefreshing = false
                        )
                    }
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                allOffers = documentsToOffers(snapshot.documents, currentUserId)
                refilterOffers()
            }
    }

    /**
     * Convert raw Firestore documents into visible [WorkOffer]s for this worker,
     * applying visibility rules and computing distance.
     */
    private fun documentsToOffers(
        docs: List<com.google.firebase.firestore.DocumentSnapshot>,
        currentUserId: String
    ): List<WorkOffer> {
        // Sort newest-first client-side. Docs missing createdAt are treated as
        // newest so manually-created test jobs still appear.
        val sortedDocs = docs.sortedByDescending { doc ->
            doc.getTimestamp("createdAt")?.toDate()?.time ?: Long.MAX_VALUE
        }

        return sortedDocs.mapNotNull { doc ->
            val acceptedBy = doc.getString("acceptedBy")
            val directOfferedTo = doc.getString("directOfferedTo")
            val status = doc.getString("status") ?: "OPEN"

            // Hide jobs that are no longer available (cancelled or completed),
            // unless the current worker is the one who accepted it.
            val isClosed =
                status == "CANCELLED" || status == "COMPLETED" || status == "REVIEWED"

            val isVisibleToMe = if (acceptedBy != null) {
                acceptedBy == currentUserId
            } else {
                !isClosed && (directOfferedTo == null || directOfferedTo == currentUserId)
            }

            if (isVisibleToMe) {
                val offerLat = doc.getDouble("latitude") ?: 0.0
                val offerLng = doc.getDouble("longitude") ?: 0.0

                val distance = if (userLatitude != 0.0 && userLongitude != 0.0 &&
                    offerLat != 0.0 && offerLng != 0.0
                ) {
                    LocationHelper.calculateDistance(
                        userLatitude, userLongitude,
                        offerLat, offerLng
                    )
                } else -1.0

                val createdTs = doc.getTimestamp("createdAt")?.toDate()
                val completedTs = doc.getTimestamp("completedAt")?.toDate()

                WorkOffer(
                    title = doc.getString("title") ?: "Untitled",
                    description = doc.getString("description") ?: "",
                    date = doc.getString("date") ?: "",
                    createdAt = createdTs?.let {
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                    } ?: "",
                    images = doc.get("images") as? List<String> ?: emptyList(),
                    id = doc.id,
                    acceptedBy = acceptedBy,
                    isAccepted = doc.getBoolean("isAccepted") ?: false,
                    status = status,
                    category = doc.getString("category") ?: "",
                    urgency = doc.getString("urgency") ?: "THIS_WEEK",
                    directOfferedTo = directOfferedTo,
                    bossId = doc.getString("bossId") ?: "",
                    bossName = doc.getString("bossName") ?: "",
                    bossPhoto = doc.getString("bossPhoto") ?: "",
                    bossRating = doc.getDouble("bossRating") ?: 0.0,
                    latitude = offerLat,
                    longitude = offerLng,
                    geohash = doc.getString("geohash") ?: "",
                    locationName = doc.getString("locationName") ?: "",
                    budgetAmount = doc.getDouble("budgetAmount") ?: 0.0,
                    budgetType = doc.getString("budgetType") ?: "NEGOTIABLE",
                    currency = doc.getString("currency") ?: "Rs",
                    distanceKm = distance,
                    createdAtMillis = createdTs?.time ?: 0L,
                    completedAtMillis = completedTs?.time ?: 0L
                )
            } else null
        }
    }

    override fun onCleared() {
        super.onCleared()
        offersListener?.remove()
        paymentsListener?.remove()
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

    /**
     * Change how the offer list is sorted.
     */
    fun updateSortOption(option: OfferSortOption) {
        _uiState.update { it.copy(sortOption = option) }
        refilterOffers()
    }

    /**
     * Filter offers by a specific category ("All" clears the filter).
     */
    fun updateCategoryFilter(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
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
        val filtered = applyFilters(
            allOffers,
            state.searchQuery,
            state.searchRadiusKm,
            state.selectedCategory
        )

        Log.d(
            TAG,
            "Radius filter → viewerLoc=($userLatitude,$userLongitude) " +
                    "radius=${state.searchRadiusKm}km " +
                    "total=${allOffers.size} passed=${filtered.size} " +
                    "distances=${allOffers.map { it.distanceKm }}"
        )

        // Score and partition offers using the matching engine
        val scoredOffers = JobMatchingEngine.scoreOffers(filtered, workerProfile)
        val (recommended, other) = JobMatchingEngine.partitionOffers(scoredOffers)

        // Apply the user-selected sort within each section
        val sortedRecommended = sortScoredOffers(recommended, state.sortOption)
        val sortedOther = sortScoredOffers(other, state.sortOption)

        // Build score lookup map
        val scoreMap = scoredOffers.associateBy { it.offer.id }

        // For the flat filteredOffers list, use sorted order (recommended first, then other)
        val sortedOffers = (sortedRecommended + sortedOther).map { it.offer }

        // Active jobs = accepted by me and still in progress
        val myId = auth.currentUser?.uid
        val activeJobs = if (myId != null) {
            allOffers.filter {
                it.acceptedBy == myId && (it.status == "ASSIGNED" || it.status == "IN_PROGRESS")
            }
        } else emptyList()

        // Radius suggestion when the current radius yields no results
        val (suggestedRadius, suggestedCount) = computeRadiusSuggestion(
            state.searchQuery, state.selectedCategory, state.searchRadiusKm, filtered.size
        )

        _uiState.update {
            it.copy(
                offerListState = WorkOfferListState.Success(allOffers),
                filteredOffers = sortedOffers,
                isRefreshing = false,
                nearbyOfferCount = filtered.size,
                recommendedOffers = sortedRecommended,
                otherOffers = sortedOther,
                offerScores = scoreMap,
                earnings = computeEarnings(it.earnings),
                availableCategories = deriveAvailableCategories(),
                activeJobs = activeJobs,
                suggestedRadius = suggestedRadius,
                suggestedRadiusCount = suggestedCount
            )
        }
    }

    /**
     * If the current radius yields no jobs, find the smallest larger radius
     * that surfaces at least one job (ignoring the current radius cap).
     */
    private fun computeRadiusSuggestion(
        query: String,
        category: String,
        currentRadius: Double,
        currentCount: Int
    ): Pair<Double?, Int> {
        if (currentCount > 0) return null to 0
        if (userLatitude == 0.0 || userLongitude == 0.0) return null to 0

        val tiers = listOf(25.0, 50.0, 100.0).filter { it > currentRadius }
        for (r in tiers) {
            val count = applyFilters(allOffers, query, r, category).size
            if (count > 0) return r to count
        }
        return null to 0
    }

    /**
     * Sort a scored-offer list according to the selected [OfferSortOption].
     * Unknown values (e.g. distance == -1) are pushed to the bottom.
     */
    private fun sortScoredOffers(
        offers: List<JobMatchingEngine.ScoredOffer>,
        option: OfferSortOption
    ): List<JobMatchingEngine.ScoredOffer> = when (option) {
        OfferSortOption.BEST_MATCH ->
            offers.sortedByDescending { it.matchScore }

        OfferSortOption.NEAREST ->
            offers.sortedBy { if (it.offer.distanceKm < 0) Double.MAX_VALUE else it.offer.distanceKm }

        OfferSortOption.HIGHEST_PAY ->
            offers.sortedByDescending { it.offer.budgetAmount }

        OfferSortOption.MOST_RECENT ->
            offers.sortedByDescending { parseCreatedAtMillis(it.offer) }

        OfferSortOption.MOST_URGENT ->
            offers.sortedBy { urgencyRank(it.offer.urgency) }
    }

    private fun urgencyRank(urgency: String): Int = when (urgency) {
        "URGENT" -> 0
        "THIS_WEEK" -> 1
        "FLEXIBLE" -> 2
        else -> 1
    }

    private fun parseCreatedAtMillis(offer: WorkOffer): Long {
        val str = offer.createdAt?.toString().orEmpty()
        if (str.isBlank()) return 0L
        return try {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse(str)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Compute earnings & progress from CONFIRMED PAYMENTS (not job budgets).
     *
     * Phase 1 payment model: a job only counts toward earnings once BOTH the boss
     * marked it paid AND the worker confirmed receipt (status == PAID_OUT in the
     * `payments` collection). This makes "earned" mean *money actually received*
     * rather than "amount the boss advertised".
     *
     * [pendingPayout] now means "completed jobs still awaiting payment
     * confirmation" — i.e. work done but money not yet settled.
     */
    private fun computeEarnings(base: EarningsSummary): EarningsSummary {
        val userId = auth.currentUser?.uid ?: return base
        val mine = allOffers.filter { it.acceptedBy == userId }
        val completed = mine.filter { it.status == "COMPLETED" || it.status == "REVIEWED" }

        // Only fully-confirmed payments count as real money.
        val paidRecords = myPayments.filter { it.isPaidOut }
        val paidJobIds = paidRecords.map { it.jobId }.toSet()

        val weekAgo = System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(7)
        val paidThisWeek = paidRecords.filter { record ->
            val settledAt = if (record.paidAtMillis > 0L) {
                record.paidAtMillis
            } else {
                // Legacy/missing timestamp — fall back to the job's completion date
                val offer = mine.firstOrNull { it.id == record.jobId }
                offer?.completedAtMillis?.takeIf { it > 0L } ?: 0L
            }
            settledAt >= weekAgo
        }

        // Work that's finished but the money isn't settled yet.
        val awaitingPayment = completed.count { it.id !in paidJobIds }

        val currency = paidRecords.firstOrNull { it.currency.isNotBlank() }?.currency
            ?: mine.firstOrNull { it.currency.isNotBlank() }?.currency
            ?: base.currency

        return base.copy(
            weeklyEarnings = paidThisWeek.sumOf { it.amount },
            totalEarnings = paidRecords.sumOf { it.amount },
            completedThisWeek = paidThisWeek.size,
            completedTotal = completed.size,
            pendingPayout = awaitingPayment,
            currency = currency
        )
    }

    /**
     * Categories that actually appear in the currently-loaded offers, for quick chips.
     */
    private fun deriveAvailableCategories(): List<String> {
        val cats = allOffers
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        return listOf("All") + cats
    }

    private fun applyFilters(
        offers: List<WorkOffer>,
        query: String,
        radiusKm: Double,
        category: String
    ): List<WorkOffer> {
        return offers.filter { offer ->
            val matchesSearch = query.isBlank() ||
                    offer.title.contains(query, ignoreCase = true) ||
                    offer.description.contains(query, ignoreCase = true) ||
                    offer.category.contains(query, ignoreCase = true)

            val matchesCategory = category == "All" ||
                    offer.category.equals(category, ignoreCase = true)

            // Location filter: show offers within radius, OR if no location data available
            val matchesLocation = if (userLatitude == 0.0 || userLongitude == 0.0) {
                true // No user location → show all
            } else if (offer.latitude == 0.0 && offer.longitude == 0.0) {
                true // Offer doesn't have location → still show (backward compatible)
            } else {
                offer.distanceKm <= radiusKm
            }

            matchesSearch && matchesCategory && matchesLocation
        }
    }

    /**
     * Toggle whether an offer is saved/bookmarked by the current worker.
     * Persists to the user document's `savedOffers` array.
     */
    fun toggleSaveOffer(offerId: String) {
        val userId = auth.currentUser?.uid ?: return
        val currentlySaved = _uiState.value.savedOfferIds.contains(offerId)

        // Optimistic UI update
        _uiState.update {
            val updated = if (currentlySaved) it.savedOfferIds - offerId
            else it.savedOfferIds + offerId
            it.copy(savedOfferIds = updated)
        }

        viewModelScope.launch {
            try {
                val op = if (currentlySaved) FieldValue.arrayRemove(offerId)
                else FieldValue.arrayUnion(offerId)
                db.collection("users").document(userId)
                    .update("savedOffers", op)
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle saved offer", e)
                // Roll back on failure
                _uiState.update {
                    val reverted = if (currentlySaved) it.savedOfferIds + offerId
                    else it.savedOfferIds - offerId
                    it.copy(savedOfferIds = reverted)
                }
            }
        }
    }

    /**
     * Create a job alert / saved search so the worker gets notified about
     * matching jobs. Stored in the `jobAlerts` collection.
     */
    fun createJobAlert(category: String, radiusKm: Double, onResult: (Boolean, String) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val alertId = db.collection("jobAlerts").document().id
                val data = mapOf(
                    "id" to alertId,
                    "userId" to userId,
                    "category" to category,
                    "radiusKm" to radiusKm,
                    "active" to true,
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                db.collection("jobAlerts").document(alertId).set(data).await()
                val label = if (category == "All") "all jobs" else category
                onResult(true, "Alert created for $label within ${radiusKm.toInt()} km")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create job alert", e)
                onResult(false, "Couldn't create alert: ${e.message}")
            }
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
                        it.copy(acceptedBy = userId, isAccepted = true, status = "ASSIGNED")
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
                        "completionNote" to note,
                        // Timestamp the completion so weekly earnings are accurate.
                        "completedAt" to FieldValue.serverTimestamp()
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
