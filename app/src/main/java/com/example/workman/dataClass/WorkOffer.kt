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
    // True once the worker has asked the boss to leave a review for a completed job
    var reviewRequested: Boolean = false,
    var completionImages: List<String> = emptyList(),
    var completionNote: String = "",
    // Urgency tiers: URGENT, THIS_WEEK, FLEXIBLE
    var urgency: String = "THIS_WEEK",
    // Direct offer to a specific worker (favorite/repeat hire)
    var directOfferedTo: String? = null,
    // Boss info
    var bossId: String = "",
    var bossName: String = "",
    var bossPhoto: String = "",
    var bossRating: Double = 0.0,
    // Location fields for geo-based filtering
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var geohash: String = "",
    var locationName: String = "",
    // Pay / budget info
    var budgetAmount: Double = 0.0,          // 0 or less => treated as "Negotiable"
    var budgetType: String = "NEGOTIABLE",   // FIXED, HOURLY, NEGOTIABLE
    var currency: String = "Rs",
    // Preferred payment method the boss will use to pay the worker (CASH / ONLINE)
    var paymentMethod: String = "CASH",
    // Transient fields - not stored in Firestore, calculated at runtime
    @get:Exclude var distanceKm: Double = -1.0,
    @get:Exclude var createdAtMillis: Long = 0L,
    @get:Exclude var completedAtMillis: Long = 0L
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
        reviewRequested = false,
        urgency = "THIS_WEEK",
        directOfferedTo = null,
        bossId = "",
        bossName = "",
        bossPhoto = "",
        bossRating = 0.0,
        latitude = 0.0,
        longitude = 0.0,
        geohash = "",
        locationName = "",
        budgetAmount = 0.0,
        budgetType = "NEGOTIABLE",
        currency = "Rs",
        paymentMethod = "CASH",
        distanceKm = -1.0,
        createdAtMillis = 0L,
        completedAtMillis = 0L
    )
}

/**
 * Human-readable pay label for a work offer.
 * Returns "Negotiable" when no amount is set.
 */
fun WorkOffer.displayPay(): String {
    if (budgetAmount <= 0.0) return "Negotiable"
    val amount = if (budgetAmount % 1.0 == 0.0) budgetAmount.toLong().toString()
    else budgetAmount.toString()
    val suffix = when (budgetType) {
        "HOURLY" -> "/hr"
        else -> ""
    }
    return "$currency $amount$suffix"
}

/** True if the job was posted within the last hour. */
fun WorkOffer.isNew(): Boolean =
    createdAtMillis > 0L && (System.currentTimeMillis() - createdAtMillis) < 60 * 60 * 1000L

/** Short "posted X ago" label, or empty string if unknown. */
fun WorkOffer.postedAgo(): String {
    if (createdAtMillis <= 0L) return ""
    val diff = System.currentTimeMillis() - createdAtMillis
    if (diff < 0) return "Just now"
    val minutes = diff / (60 * 1000L)
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "Posted ${minutes}m ago"
        hours < 24 -> "Posted ${hours}h ago"
        days < 7 -> "Posted ${days}d ago"
        else -> "Posted ${days / 7}w ago"
    }
}

/**
 * Rough travel-time estimate based on distance (assumes ~30 km/h city driving).
 * Returns null when distance is unknown.
 */
fun WorkOffer.travelEstimate(): String? {
    if (distanceKm < 0) return null
    if (distanceKm < 1.0) return "~5 min away"
    val minutes = (distanceKm / 30.0 * 60).toInt().coerceAtLeast(1)
    return "~$minutes min drive"
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

