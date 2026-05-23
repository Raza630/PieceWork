package com.example.workman.viewModels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workman.dataClass.Banner
import com.example.workman.dataClass.WorkOffer
import com.example.workman.utils.LocationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
    val nearbyOfferCount: Int = 0
)

class HomeWorkerDashboardViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var allOffers: List<WorkOffer> = emptyList()

    // Current user's location
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0

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

                _uiState.update { it.copy(
                    userName = doc.getString("name") ?: "Worker",
                    userLocation = doc.getString("location") ?: "Not set",
                    isLocationAvailable = lat != 0.0 && lng != 0.0
                ) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load user data", e)
            }
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
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredOffers = applyFilters(allOffers, query, state.searchRadiusKm)
            )
        }
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

        _uiState.update {
            it.copy(
                offerListState = WorkOfferListState.Success(allOffers),
                filteredOffers = filtered,
                isRefreshing = false,
                nearbyOfferCount = filtered.size
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
                    offer.description.contains(query, ignoreCase = true)

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
            .sortedBy { it.distanceKm.let { d -> if (d < 0) Double.MAX_VALUE else d } } // Nearest first
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
                            "status" to "accepted",
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
                        offerListState = WorkOfferListState.Success(allOffers),
                        filteredOffers = applyFilters(
                            allOffers,
                            state.searchQuery,
                            state.searchRadiusKm
                        ),
                        acceptingOfferIds = state.acceptingOfferIds - workOffer.id
                    )
                }
                onResult(true, "Work accepted successfully!")
            } catch (e: Exception) {
                _uiState.update { it.copy(acceptingOfferIds = it.acceptingOfferIds - workOffer.id) }
                onResult(false, "Error: ${e.message}")
            }
        }
    }
}
