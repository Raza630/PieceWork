package com.example.workman.utils

import android.util.Log
import com.example.workman.dataClass.WorkOffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Smart Job Matching Engine for WorkMan.
 *
 * Scores each work offer for a specific worker based on 4 signals:
 *
 *   score = (categoryMatch × 40) + (distanceScore × 30) + (historyScore × 20) + (recencyScore × 10)
 *
 * - **Category Match (40%):** Does the offer's category match the worker's skills?
 * - **Distance Score (30%):** How close is the job site to the worker?
 * - **History Score (20%):** Has the worker accepted similar jobs before?
 * - **Recency Score (10%):** How recently was the job posted?
 *
 * All sub-scores are normalized to 0..1, so the final score is 0..100.
 */
object JobMatchingEngine {

    private const val TAG = "JobMatchingEngine"

    /**
     * Holds the worker's profile data needed for matching.
     */
    data class WorkerProfile(
        val category: String = "",
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        /** Categories the worker has accepted in the past, with frequency counts. */
        val acceptedCategoryHistory: Map<String, Int> = emptyMap(),
        /** Total number of past accepted jobs. */
        val totalAcceptedJobs: Int = 0
    )

    /**
     * Result of scoring a single offer.
     */
    data class ScoredOffer(
        val offer: WorkOffer,
        /** Overall match score 0–100 */
        val matchScore: Int,
        /** Individual signal scores for debugging / UI breakdown */
        val categoryScore: Double,
        val distanceScore: Double,
        val historyScore: Double,
        val recencyScore: Double,
        /** Human-readable match reason for UI */
        val matchReason: String
    )

    // ── Weights ──────────────────────────────────────────────────────────────

    private const val WEIGHT_CATEGORY = 40.0
    private const val WEIGHT_DISTANCE = 30.0
    private const val WEIGHT_HISTORY = 20.0
    private const val WEIGHT_RECENCY = 10.0

    // Distance parameters
    /** Jobs closer than this get a perfect distance score */
    private const val IDEAL_DISTANCE_KM = 2.0

    /** Jobs farther than this get 0 distance score */
    private const val MAX_SCORING_DISTANCE_KM = 100.0

    // Recency parameters
    /** Jobs posted within this many days get a perfect recency score */
    private const val IDEAL_RECENCY_DAYS = 1L

    /** Jobs older than this get 0 recency score */
    private const val MAX_RECENCY_DAYS = 30L

    // ── Category Similarity (Dynamic from CategoryRepository) ────────────────

    /**
     * Gets the similarity between two categories from the dynamic CategoryRepository.
     * No more hardcoded pairs — managed via Firestore.
     */
    private fun getCategorySimilarity(catA: String, catB: String): Double {
        return CategoryRepository.getSimilarity(catA, catB)
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Score and rank all offers for a given worker.
     *
     * @param offers      All available work offers (already filtered by acceptance status, etc.)
     * @param profile     The current worker's profile data
     * @return            List of ScoredOffer sorted by matchScore descending (best match first)
     */
    fun scoreOffers(
        offers: List<WorkOffer>,
        profile: WorkerProfile
    ): List<ScoredOffer> {
        return offers.map { offer ->
            scoreOffer(offer, profile)
        }.sortedByDescending { it.matchScore }
    }

    /**
     * Score a single offer for a worker.
     */
    fun scoreOffer(offer: WorkOffer, profile: WorkerProfile): ScoredOffer {
        val catScore = computeCategoryScore(offer.category, profile.category)
        val distScore = computeDistanceScore(offer, profile)
        val histScore = computeHistoryScore(offer.category, profile)
        val recScore = computeRecencyScore(offer)

        val totalScore = (catScore * WEIGHT_CATEGORY) +
                (distScore * WEIGHT_DISTANCE) +
                (histScore * WEIGHT_HISTORY) +
                (recScore * WEIGHT_RECENCY)

        val reason = buildMatchReason(catScore, distScore, histScore, recScore, offer, profile)

        return ScoredOffer(
            offer = offer,
            matchScore = totalScore.toInt().coerceIn(0, 100),
            categoryScore = catScore,
            distanceScore = distScore,
            historyScore = histScore,
            recencyScore = recScore,
            matchReason = reason
        )
    }

    // ── Individual Scoring Functions ─────────────────────────────────────────

    /**
     * Category Score (0.0 – 1.0)
     * 1.0 = exact match
     * 0.3–0.7 = related category
     * 0.1 = no match (still show, just ranked lower)
     * 0.0 = one or both categories are empty
     */
    private fun computeCategoryScore(offerCategory: String, workerCategory: String): Double {
        if (offerCategory.isBlank() || workerCategory.isBlank()) {
            // If either has no category, give a neutral score (don't penalize)
            return 0.5
        }
        if (offerCategory.equals(workerCategory, ignoreCase = true)) {
            return 1.0
        }
        // Use dynamic similarity from CategoryRepository
        return getCategorySimilarity(offerCategory, workerCategory)
    }

    /**
     * Distance Score (0.0 – 1.0)
     * Uses exponential decay: closer = much better.
     */
    private fun computeDistanceScore(offer: WorkOffer, profile: WorkerProfile): Double {
        // If location data is missing, give a neutral score
        if (profile.latitude == 0.0 && profile.longitude == 0.0) return 0.5
        if (offer.latitude == 0.0 && offer.longitude == 0.0) return 0.5

        val distanceKm = if (offer.distanceKm >= 0) {
            offer.distanceKm
        } else {
            LocationHelper.calculateDistance(
                profile.latitude, profile.longitude,
                offer.latitude, offer.longitude
            )
        }

        return when {
            distanceKm <= IDEAL_DISTANCE_KM -> 1.0
            distanceKm >= MAX_SCORING_DISTANCE_KM -> 0.0
            else -> {
                // Exponential decay: score drops faster as distance increases
                val normalized = (distanceKm - IDEAL_DISTANCE_KM) /
                        (MAX_SCORING_DISTANCE_KM - IDEAL_DISTANCE_KM)
                1.0 - Math.pow(normalized, 0.7) // Concave curve: nearby jobs score much higher
            }
        }
    }

    /**
     * History Score (0.0 – 1.0)
     * Based on how many times the worker has accepted jobs in this category before.
     */
    private fun computeHistoryScore(offerCategory: String, profile: WorkerProfile): Double {
        if (offerCategory.isBlank() || profile.totalAcceptedJobs == 0) {
            return 0.5 // Neutral if no history or no category
        }

        val directCount = profile.acceptedCategoryHistory[offerCategory] ?: 0

        if (directCount > 0) {
            // Logarithmic scaling: 1 accept = 0.5, 3 = 0.75, 10+ = ~1.0
            return (0.5 + 0.5 * Math.log10(directCount.toDouble() + 1) / Math.log10(11.0))
                .coerceIn(0.0, 1.0)
        }

        // Check related categories via dynamic CategoryRepository
        var relatedScore = 0.0
        for ((cat, count) in profile.acceptedCategoryHistory) {
            val similarity = getCategorySimilarity(offerCategory, cat)
            if (similarity > 0.1) { // Only count meaningful similarity
                relatedScore =
                    maxOf(relatedScore, similarity * 0.5 * count / profile.totalAcceptedJobs)
            }
        }

        return relatedScore.coerceIn(0.0, 0.6)
    }

    /**
     * Recency Score (0.0 – 1.0)
     * Newer jobs score higher.
     */
    private fun computeRecencyScore(offer: WorkOffer): Double {
        val createdAtStr = offer.createdAt?.toString() ?: ""
        if (createdAtStr.isBlank()) return 0.5

        return try {
            // createdAt is stored as formatted string "dd/MM/yyyy HH:mm" in the ViewModel
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val createdDate = dateFormat.parse(createdAtStr) ?: return 0.5
            val daysOld = TimeUnit.MILLISECONDS.toDays(Date().time - createdDate.time)

            when {
                daysOld <= IDEAL_RECENCY_DAYS -> 1.0
                daysOld >= MAX_RECENCY_DAYS -> 0.0
                else -> 1.0 - (daysOld - IDEAL_RECENCY_DAYS).toDouble() /
                        (MAX_RECENCY_DAYS - IDEAL_RECENCY_DAYS)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse createdAt for recency: $createdAtStr")
            0.5
        }
    }

    // ── Match Reason Builder ─────────────────────────────────────────────────

    /**
     * Build a short, human-readable reason string for why this offer is recommended.
     * Shows the top contributing factor.
     */
    private fun buildMatchReason(
        catScore: Double,
        distScore: Double,
        histScore: Double,
        recScore: Double,
        offer: WorkOffer,
        profile: WorkerProfile
    ): String {
        data class Signal(val name: String, val weighted: Double, val reason: String)

        val signals = listOf(
            Signal(
                "category", catScore * WEIGHT_CATEGORY,
                if (catScore >= 0.9) "Matches your skills"
                else if (catScore >= 0.4) "Related to your skills"
                else ""
            ),
            Signal(
                "distance", distScore * WEIGHT_DISTANCE,
                if (distScore >= 0.8) "Very close to you"
                else if (distScore >= 0.5) "Within easy reach"
                else ""
            ),
            Signal(
                "history", histScore * WEIGHT_HISTORY,
                if (histScore >= 0.7) "Similar to past jobs"
                else if (histScore >= 0.4) "Based on your history"
                else ""
            ),
            Signal(
                "recency", recScore * WEIGHT_RECENCY,
                if (recScore >= 0.8) "Just posted"
                else ""
            )
        )

        // Pick the strongest signal with a non-empty reason
        val topSignal = signals
            .filter { it.reason.isNotEmpty() }
            .maxByOrNull { it.weighted }

        return topSignal?.reason ?: "Available near you"
    }

    // ── Threshold Helpers ────────────────────────────────────────────────────

    /** Offers with score >= this are shown in "Recommended for You" */
    const val RECOMMENDED_THRESHOLD = 50

    /** Offers with score >= this get a "Great Match" badge */
    const val GREAT_MATCH_THRESHOLD = 75

    /**
     * Split scored offers into recommended and other lists.
     */
    fun partitionOffers(
        scoredOffers: List<ScoredOffer>
    ): Pair<List<ScoredOffer>, List<ScoredOffer>> {
        val recommended = scoredOffers.filter { it.matchScore >= RECOMMENDED_THRESHOLD }
        val other = scoredOffers.filter { it.matchScore < RECOMMENDED_THRESHOLD }
        return recommended to other
    }
}

