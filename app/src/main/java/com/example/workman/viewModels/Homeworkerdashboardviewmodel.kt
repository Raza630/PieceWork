package com.example.workman.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.location.Geocoder
import com.example.workman.dataClass.Banner
import com.example.workman.dataClass.WorkOffer
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

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
    val acceptingOfferIds: Set<String> = emptySet()
)

class HomeWorkerDashboardViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var allOffers: List<WorkOffer> = emptyList()

    private val _uiState = MutableStateFlow(WorkerDashboardUiState())
    val uiState: StateFlow<WorkerDashboardUiState> = _uiState.asStateFlow()

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
                _uiState.update { it.copy(
                    userName = doc.getString("name") ?: "Worker",
                    userLocation = doc.getString("location") ?: "Not set"
                ) }
            } catch (e: Exception) {
                // Ignore user data errors
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
                // In a real app, we might use a listener, but for now we fetch once
                // or use a Flow-based listener if needed.
                val snapshot = db.collection("workOffers")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                allOffers = snapshot.documents.mapNotNull { doc ->
                    val acceptedBy = doc.getString("acceptedBy")
                    // Show if open OR accepted by me
                    if (acceptedBy == null || acceptedBy == currentUserId) {
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
                            isAccepted = doc.getBoolean("isAccepted") ?: false
                        )
                    } else null
                }

                _uiState.update { state ->
                    state.copy(
                        offerListState = WorkOfferListState.Success(allOffers),
                        filteredOffers = applyFilters(allOffers, state.searchQuery),
                        isRefreshing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    offerListState = WorkOfferListState.Error(e.message ?: "Unknown error"),
                    isRefreshing = false
                ) }
            }
        }
    }

    fun updateLocation(context: Context) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            _uiState.update { it.copy(userLocation = "Permission denied") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(userLocation = "Detecting...") }
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    // Use a more robust check for SDK version if needed, but for now we'll stick to basic
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val area = address.subLocality ?: address.locality ?: address.subAdminArea ?: "Unknown Area"
                        val city = address.locality ?: ""
                        val locationString = if (city.isNotEmpty() && area != city) "$area, $city" else area
                        
                        _uiState.update { it.copy(userLocation = locationString) }
                        
                        // Sync with Firebase
                        auth.currentUser?.uid?.let { uid ->
                            db.collection("users").document(uid).update("location", locationString)
                        }
                    } else {
                        _uiState.update { it.copy(userLocation = "Location found, but area unknown") }
                    }
                } else {
                    _uiState.update { it.copy(userLocation = "Location unavailable") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(userLocation = "Error getting location") }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredOffers = applyFilters(allOffers, query)
            )
        }
    }

    private fun applyFilters(offers: List<WorkOffer>, query: String): List<WorkOffer> {
        if (query.isBlank()) return offers
        return offers.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true)
        }
    }

    fun acceptWork(workOffer: WorkOffer, onResult: (Boolean, String) -> Unit) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(acceptingOfferIds = it.acceptingOfferIds + workOffer.id) }
            try {
                db.collection("workOffers")
                    .document(workOffer.id)
                    .update(
                        mapOf(
                            "acceptedBy" to userId,
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
                        filteredOffers = applyFilters(allOffers, state.searchQuery),
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
