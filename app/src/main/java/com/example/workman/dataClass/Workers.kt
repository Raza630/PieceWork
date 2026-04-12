package com.example.workman.dataClass

import java.io.Serializable

data class Workers (val name: String, val imageResId: Int, val category: String, val starRating: Float): Serializable
data class Worker(
    val id: String = "",
    val name: String = "",
    val specialty: String = "",
    val rating: Double = 0.0,
    val phone: String = "",
    val bio: String = "",
    val profileImageUrl: String = "",
    val hourlyRate: String = "$0/hr"
)