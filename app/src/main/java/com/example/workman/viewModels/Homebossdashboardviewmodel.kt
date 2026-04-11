package com.example.workman.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workman.dataClass.WorkerUiModel
import com.google.firebase.firestore.FirebaseFirestore
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
    val selectedCategory: String = "All",
    val filteredWorkers: List<WorkerUiModel> = emptyList()
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

    // Raw fetched list — never modified after fetch
    private var allWorkers: List<WorkerUiModel> = emptyList()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        fetchWorkers()
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
                        )
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
