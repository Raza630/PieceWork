package com.example.workman.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workman.dataClass.BookingStatus
import com.example.workman.dataClass.BookingUiModel
import com.example.workman.dataClass.WorkerUiModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.*
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
    val filteredWorkers: List<WorkerUiModel> = emptyList(),
    val popularServices: List<WorkerUiModel> = emptyList(),
    val bookings: List<BookingUiModel> = emptyList(),
    val selectedBookingTab: Int = 0, // 0: Pending, 1: Active, 2: History
    val bookingToRate: BookingUiModel? = null,
    val showRatingDialog: Boolean = false
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
    val role: String = ""
)

// ─── ViewModel ─────────────────────────────────────────────────────────────────

class HomeBossDashboardViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var bookingsListener: ListenerRegistration? = null

    // Raw fetched list — never modified after fetch
    private var allWorkers: List<WorkerUiModel> = emptyList()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        fetchWorkers()
        observeBookings()
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
                
                if (newStatus == BookingStatus.COMPLETED) {
                    val booking = _uiState.value.bookings.find { it.id == bookingId }
                    _uiState.update { it.copy(bookingToRate = booking, showRatingDialog = true) }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun submitRating(rating: Float, review: String) {
        val booking = _uiState.value.bookingToRate ?: return
        viewModelScope.launch {
            try {
                // 1. Add review to 'reviews' collection
                val reviewData = hashMapOf(
                    "workerId" to booking.workerId,
                    "bossId" to booking.bossId,
                    "bookingId" to booking.id,
                    "rating" to rating,
                    "review" to review,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )
                db.collection("reviews").add(reviewData).await()

                // 2. Update worker's average rating (simplified logic for now)
                // In a real app, you'd use a Cloud Function or a transaction to update avg rating
                
                _uiState.update { it.copy(showRatingDialog = false, bookingToRate = null) }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun dismissRatingDialog() {
        _uiState.update { it.copy(showRatingDialog = false, bookingToRate = null) }
    }

    // ── Firestore Fetch ────────────────────────────────────────────────────────

    fun fetchWorkers() {
        viewModelScope.launch {
            _uiState.update { it.copy(workerListState = WorkerListState.Loading) }

            try {
                // Fetch all workers. We will filter by role in code to be safer if needed,
                // but your Firestore query uses role.
                val snapshot = db.collection("users")
                    .whereEqualTo("role", "Worker")
                    .get()
                    .await()

                allWorkers = snapshot.documents.mapNotNull { doc ->
                    val raw = doc.toObject(WorkerDocument::class.java) ?: return@mapNotNull null
                    WorkerUiModel(
                        id               = doc.id,
                        name             = raw.name,
                        category         = raw.category,
                        yearsOfExperience = raw.yearsOfExperience,
                        rating           = raw.rating,
                        reviewCount      = raw.reviewCount,
                        ratePerHour      = raw.ratePerHour,
                        photoUrl         = raw.photoUrl
                    )
                }

                _uiState.update { state ->
                    state.copy(
                        workerListState  = WorkerListState.Success(allWorkers),
                        filteredWorkers  = applyFilters(
                            workers  = allWorkers,
                            query    = state.searchQuery,
                            category = state.selectedCategory
                        ),
                        popularServices = allWorkers.sortedByDescending { it.rating }.take(5)
                    )
                }

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
                filteredWorkers = applyFilters(allWorkers, query, state.selectedCategory)
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
                filteredWorkers  = applyFilters(allWorkers, state.searchQuery, newCategory)
            )
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun applyFilters(
        workers: List<WorkerUiModel>,
        query: String,
        category: String
    ): List<WorkerUiModel> {
        return workers.filter { worker ->
            val matchesSearch = query.isBlank() ||
                    worker.name.contains(query, ignoreCase = true) ||
                    worker.category.contains(query, ignoreCase = true)

            // Make category matching more robust by using trim and partial matching if needed,
            // or exact match for tabs.
            val matchesCategory = if (category == "All") {
                true
            } else {
                // Check if worker's category contains the selected category name (e.g. "Professionals" in "Professionals")
                // Using trim to handle any stray spaces in Firestore data.
                worker.category.trim().equals(category.trim(), ignoreCase = true) ||
                worker.category.contains(category, ignoreCase = true)
            }

            matchesSearch && matchesCategory
        }
    }
}
