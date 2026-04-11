package com.example.workman.dataClass

data class WorkerUiModel(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val yearsOfExperience: Int = 0,
    val rating: Double = 0.0,
    val reviewCount: String = "0",
    val ratePerHour: Int = 0,
    val photoUrl: String = ""
)
