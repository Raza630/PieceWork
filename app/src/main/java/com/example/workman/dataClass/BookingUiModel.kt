package com.example.workman.dataClass

import java.util.Date

enum class BookingStatus {
    PENDING,
    ACTIVE,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

data class BookingUiModel(
    val id: String = "",
    val jobId: String = "",
    val workerId: String = "",
    val workerName: String = "",
    val workerPhotoUrl: String = "",
    val serviceName: String = "",
    val agreedRate: String = "",
    val status: BookingStatus = BookingStatus.PENDING,
    val date: Date = Date(),
    val bossId: String = "",
    // Manual payment tracking (Phase 1): UNPAID / BOSS_MARKED_PAID / WORKER_CONFIRMED / PAID_OUT
    val paymentStatus: String = "UNPAID",
    val paymentMethod: String = "CASH",
    // True once the boss has left a review for this job (hides the Rate button)
    val ratingSubmitted: Boolean = false
)
