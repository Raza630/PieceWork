package com.example.workman.dataClass

data class Banner(
    val title: String = "",
    val description: String = "",
    val averagePay: String = "",
    val imageUrl: String = "",
    val category: String = "",        // Tapping banner filters by this category
    val actionUrl: String = ""         // Optional deep link
)
