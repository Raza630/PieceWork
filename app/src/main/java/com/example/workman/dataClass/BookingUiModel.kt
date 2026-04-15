package com.example.workman.dataClass

import java.util.Date

enum class BookingStatus {
    PENDING,
    ACTIVE,
    COMPLETED,
    CANCELLED
}

data class BookingUiModel(
    val id: String = "",
    val workerId: String = "",
    val workerName: String = "",
    val workerPhotoUrl: String = "",
    val serviceName: String = "",
    val agreedRate: String = "",
    val status: BookingStatus = BookingStatus.PENDING,
    val date: Date = Date(),
    val bossId: String = ""
)
