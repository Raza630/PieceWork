package com.example.workman.dataClass

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class WorkOffer(
    var title: String = "",
    var description: String = "",
    var date: String = "",
    var createdAt: Any? = null,
    var images: List<String> = emptyList(),
    var id: String = "",
    var acceptedBy: String? = null,
    var isAccepted: Boolean = false,
    var status: String = "OPEN", // OPEN, ASSIGNED, IN_PROGRESS, COMPLETED, REVIEWED
    var ratingSubmitted: Boolean = false,
    var completionImages: List<String> = emptyList(),
    var completionNote: String = "",
    // Location fields for geo-based filtering
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var geohash: String = "",
    var locationName: String = "",
    // Transient field - not stored in Firestore, calculated at runtime
    @get:Exclude var distanceKm: Double = -1.0
) {
    constructor() : this(
        title = "",
        description = "",
        date = "",
        createdAt = null,
        images = emptyList(),
        id = "",
        acceptedBy = null,
        isAccepted = false,
        status = "OPEN",
        ratingSubmitted = false,
        latitude = 0.0,
        longitude = 0.0,
        geohash = "",
        locationName = "",
        distanceKm = -1.0
    )
}
