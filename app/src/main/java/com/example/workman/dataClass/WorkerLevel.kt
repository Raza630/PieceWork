package com.example.workman.dataClass

/**
 * Worker gamification levels based on completed jobs.
 * Displayed as badges on worker cards and profiles.
 */
object WorkerLevel {

    const val BRONZE = "BRONZE"
    const val SILVER = "SILVER"
    const val GOLD = "GOLD"
    const val PLATINUM = "PLATINUM"

    /**
     * Determine the worker's level based on completed job count.
     */
    fun fromJobCount(completedJobs: Int): String = when {
        completedJobs >= 50 -> PLATINUM
        completedJobs >= 21 -> GOLD
        completedJobs >= 6 -> SILVER
        else -> BRONZE
    }

    /**
     * Get display name with emoji for a level.
     */
    fun getDisplayName(level: String): String = when (level) {
        PLATINUM -> "⭐ Platinum"
        GOLD -> "🥇 Gold"
        SILVER -> "🥈 Silver"
        BRONZE -> "🥉 Bronze"
        else -> "🥉 Bronze"
    }

    /**
     * Get just the emoji for a level (for compact display).
     */
    fun getEmoji(level: String): String = when (level) {
        PLATINUM -> "⭐"
        GOLD -> "🥇"
        SILVER -> "🥈"
        BRONZE -> "🥉"
        else -> "🥉"
    }

    /**
     * Get the color hex for a level badge.
     */
    fun getColorHex(level: String): Long = when (level) {
        PLATINUM -> 0xFF9C27B0  // Purple
        GOLD -> 0xFFFFD700      // Gold
        SILVER -> 0xFFC0C0C0    // Silver
        BRONZE -> 0xFFCD7F32    // Bronze
        else -> 0xFFCD7F32
    }

    /**
     * Get jobs needed for next level.
     */
    fun jobsToNextLevel(completedJobs: Int): Pair<String, Int>? = when {
        completedJobs >= 50 -> null // Already max
        completedJobs >= 21 -> PLATINUM to (50 - completedJobs)
        completedJobs >= 6 -> GOLD to (21 - completedJobs)
        else -> SILVER to (6 - completedJobs)
    }
}

