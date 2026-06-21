package com.example.workman.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workman.dataClass.WorkerLevel

/**
 * Displays a worker's level badge (Bronze/Silver/Gold/Platinum).
 *
 * Usage:
 * ```
 * WorkerLevelBadge(completedJobs = 25) // Shows "🥇 Gold"
 * ```
 */
@Composable
fun WorkerLevelBadge(
    completedJobs: Int,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val level = WorkerLevel.fromJobCount(completedJobs)
    val colorHex = WorkerLevel.getColorHex(level)
    val badgeColor = Color(colorHex)

    Surface(
        modifier = modifier,
        color = badgeColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = WorkerLevel.getEmoji(level),
                fontSize = 12.sp
            )
            if (showLabel) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = level.lowercase().replaceFirstChar { it.uppercase() },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }
    }
}

/**
 * Displays urgency badge for a work offer.
 */
@Composable
fun UrgencyBadge(
    urgency: String,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (urgency) {
        "URGENT" -> Color(0xFFE53935) to "🔴 Urgent"
        "THIS_WEEK" -> Color(0xFFFF9800) to "🟡 This Week"
        "FLEXIBLE" -> Color(0xFF4CAF50) to "🟢 Flexible"
        else -> Color(0xFFFF9800) to "🟡 This Week"
    }

    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

