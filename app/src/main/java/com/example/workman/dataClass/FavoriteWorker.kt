package com.example.workman.dataClass

import com.google.firebase.Timestamp

/**
 * Represents a boss's favorite worker for repeat hiring.
 * Stored in Firestore: users/{bossId}/favorites/{workerId}
 */
data class FavoriteWorker(
    val workerId: String = "",
    val workerName: String = "",
    val workerPhotoUrl: String = "",
    val workerCategory: String = "",
    val workerRating: Double = 0.0,
    val addedAt: Timestamp = Timestamp.now(),
    val lastHiredAt: Timestamp? = null,
    val hireCount: Int = 0,
    val note: String = ""  // Boss's private note about this worker
)

