package com.example.workman.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workman.dataClass.FavoriteWorker
import com.example.workman.dataClass.WorkerUiModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class FavoritesUiState(
    val favorites: List<FavoriteWorker> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),  // Quick lookup for UI
    val isLoading: Boolean = false,
    val message: String? = null
)

/**
 * ViewModel for managing favorite workers (repeat hire feature).
 * Boss can add/remove favorites and directly offer them jobs.
 */
class FavoritesViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "FavoritesVM"
    }

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val snapshot = db.collection("users")
                    .document(userId)
                    .collection("favorites")
                    .get()
                    .await()

                val favorites = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(FavoriteWorker::class.java)?.copy(workerId = doc.id)
                }

                _uiState.update {
                    it.copy(
                        favorites = favorites,
                        favoriteIds = favorites.map { f -> f.workerId }.toSet(),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load favorites", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Add a worker to boss's favorites.
     */
    fun addFavorite(worker: WorkerUiModel) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val favorite = FavoriteWorker(
                    workerId = worker.id,
                    workerName = worker.name,
                    workerPhotoUrl = worker.photoUrl,
                    workerCategory = worker.category,
                    workerRating = worker.rating,
                    addedAt = Timestamp.now()
                )

                db.collection("users")
                    .document(userId)
                    .collection("favorites")
                    .document(worker.id)
                    .set(favorite)
                    .await()

                _uiState.update {
                    it.copy(
                        favorites = it.favorites + favorite,
                        favoriteIds = it.favoriteIds + worker.id,
                        message = "${worker.name} added to favorites"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add favorite", e)
                _uiState.update { it.copy(message = "Failed to add favorite") }
            }
        }
    }

    /**
     * Remove a worker from boss's favorites.
     */
    fun removeFavorite(workerId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("favorites")
                    .document(workerId)
                    .delete()
                    .await()

                _uiState.update {
                    it.copy(
                        favorites = it.favorites.filter { f -> f.workerId != workerId },
                        favoriteIds = it.favoriteIds - workerId,
                        message = "Removed from favorites"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove favorite", e)
            }
        }
    }

    /**
     * Check if a worker is in favorites.
     */
    fun isFavorite(workerId: String): Boolean {
        return _uiState.value.favoriteIds.contains(workerId)
    }

    /**
     * Toggle favorite status.
     */
    fun toggleFavorite(worker: WorkerUiModel) {
        if (isFavorite(worker.id)) {
            removeFavorite(worker.id)
        } else {
            addFavorite(worker)
        }
    }

    /**
     * Create a direct job offer to a favorite worker.
     * Creates a WorkOffer with directOfferedTo = workerId, so only that worker sees it.
     */
    fun createDirectOffer(
        workerId: String,
        title: String,
        description: String,
        category: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        val userName = auth.currentUser?.displayName ?: "Boss"

        viewModelScope.launch {
            try {
                val jobId = db.collection("workOffers").document().id
                val workData = hashMapOf(
                    "jobId" to jobId,
                    "title" to title,
                    "description" to description,
                    "category" to category,
                    "bossId" to userId,
                    "bossName" to userName,
                    "status" to "OPEN",
                    "isAccepted" to false,
                    "directOfferedTo" to workerId,
                    "urgency" to "THIS_WEEK",
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                db.collection("workOffers").document(jobId).set(workData).await()

                // Update hire count in favorites
                db.collection("users")
                    .document(userId)
                    .collection("favorites")
                    .document(workerId)
                    .update(
                        mapOf(
                            "lastHiredAt" to Timestamp.now(),
                            "hireCount" to com.google.firebase.firestore.FieldValue.increment(1)
                        )
                    )

                onResult(true, "Direct offer sent successfully!")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create direct offer", e)
                onResult(false, "Failed to send offer: ${e.message}")
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

