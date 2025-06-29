package com.example.workman.dataClass

import com.google.firebase.firestore.FieldValue


data class WorkOffer(
    val title: String,
    val description: String,
    val date: String,
    var createdAt: Any? = FieldValue.serverTimestamp(),
    val images: List<String> = emptyList(),
    val id: String = "",
    val acceptedBy: String? = null,
    val isAccepted: Boolean
)


