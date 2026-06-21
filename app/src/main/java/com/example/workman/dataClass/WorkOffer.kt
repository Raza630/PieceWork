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
    var category: String = "",  // Job category for smart matching
    var status: String = "OPEN", // OPEN, ASSIGNED, IN_PROGRESS, COMPLETED, REVIEWED
    var ratingSubmitted: Boolean = false,
    var completionImages: List<String> = emptyList(),
    var completionNote: String = "",
    // Urgency tiers: URGENT, THIS_WEEK, FLEXIBLE
    var urgency: String = "THIS_WEEK",
    // Direct offer to a specific worker (favorite/repeat hire)
    var directOfferedTo: String? = null,
    // Boss info
    var bossId: String = "",
    var bossName: String = "",
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
        category = "",
        status = "OPEN",
        ratingSubmitted = false,
        urgency = "THIS_WEEK",
        directOfferedTo = null,
        bossId = "",
        bossName = "",
        latitude = 0.0,
        longitude = 0.0,
        geohash = "",
        locationName = "",
        distanceKm = -1.0
    )
}

/**
 * Urgency levels for work offers.
 */
object UrgencyLevel {
    const val URGENT = "URGENT"        // Need someone within 2 hours
    const val THIS_WEEK = "THIS_WEEK"  // Normal priority
    const val FLEXIBLE = "FLEXIBLE"    // No rush

    fun getDisplayName(urgency: String): String = when (urgency) {
        URGENT -> "🔴 Urgent"
        THIS_WEEK -> "🟡 This Week"
        FLEXIBLE -> "🟢 Flexible"
        else -> "🟡 This Week"
    }

    fun getAll(): List<String> = listOf(URGENT, THIS_WEEK, FLEXIBLE)
}

