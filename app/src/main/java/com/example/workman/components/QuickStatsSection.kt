package com.example.workman.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workman.dataClass.WorkerLevel
import com.example.workman.ui.theme.CardBg
import com.example.workman.ui.theme.PrimaryBlue
import com.example.workman.ui.theme.TextDark
import com.example.workman.ui.theme.TextMuted
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Quick Stats Dashboard for Profile screen.
 * Shows job completion stats, rating, level, and progress to next level.
 */
@Composable
fun QuickStatsSection(
    modifier: Modifier = Modifier
) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var completedJobs by remember { mutableStateOf(0) }
    var totalJobsPosted by remember { mutableStateOf(0) }
    var averageRating by remember { mutableStateOf(0.0) }
    var totalRatings by remember { mutableStateOf(0L) }
    var userRole by remember { mutableStateOf("") }
    var isLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId == null) return@LaunchedEffect
        val db = FirebaseFirestore.getInstance()

        try {
            // Get user data
            val userDoc = db.collection("users").document(userId).get().await()
            userRole = userDoc.getString("role") ?: ""
            averageRating = userDoc.getDouble("averageRating") ?: 0.0
            totalRatings = userDoc.getLong("totalRatings") ?: 0L

            if (userRole == "Worker") {
                // Count completed jobs for worker
                val jobsSnapshot = db.collection("workOffers")
                    .whereEqualTo("acceptedBy", userId)
                    .whereEqualTo("status", "COMPLETED")
                    .get().await()
                completedJobs = jobsSnapshot.size()
            } else {
                // Count jobs posted for boss
                val postedSnapshot = db.collection("workOffers")
                    .whereEqualTo("bossId", userId)
                    .get().await()
                totalJobsPosted = postedSnapshot.size()

                val completedSnapshot = db.collection("workOffers")
                    .whereEqualTo("bossId", userId)
                    .whereEqualTo("status", "COMPLETED")
                    .get().await()
                completedJobs = completedSnapshot.size()
            }

            isLoaded = true
        } catch (_: Exception) {
            isLoaded = true
        }
    }

    if (!isLoaded) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Quick Stats",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Spacer(Modifier.height(16.dp))

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (userRole == "Worker") {
                    StatItem(
                        value = completedJobs.toString(),
                        label = "Jobs Done",
                        icon = Icons.Default.Check,
                        iconColor = Color(0xFF4CAF50)
                    )
                    StatItem(
                        value = if (averageRating > 0) "%.1f".format(averageRating) else "N/A",
                        label = "Avg Rating",
                        icon = Icons.Default.Star,
                        iconColor = Color(0xFFFFD700)
                    )
                    StatItem(
                        value = totalRatings.toString(),
                        label = "Reviews",
                        icon = Icons.Default.Star,
                        iconColor = PrimaryBlue
                    )
                } else {
                    StatItem(
                        value = totalJobsPosted.toString(),
                        label = "Jobs Posted",
                        icon = Icons.Default.Check,
                        iconColor = PrimaryBlue
                    )
                    StatItem(
                        value = completedJobs.toString(),
                        label = "Completed",
                        icon = Icons.Default.Check,
                        iconColor = Color(0xFF4CAF50)
                    )
                    StatItem(
                        value = if (averageRating > 0) "%.1f".format(averageRating) else "N/A",
                        label = "Avg Rating",
                        icon = Icons.Default.Star,
                        iconColor = Color(0xFFFFD700)
                    )
                }
            }

            // Worker level progress (only for workers)
            if (userRole == "Worker") {
                Spacer(Modifier.height(16.dp))
                WorkerLevelProgress(completedJobs = completedJobs)
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: ImageVector,
    iconColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}

@Composable
private fun WorkerLevelProgress(completedJobs: Int) {
    val level = WorkerLevel.fromJobCount(completedJobs)
    val nextLevelInfo = WorkerLevel.jobsToNextLevel(completedJobs)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WorkerLevelBadge(completedJobs = completedJobs)

        Spacer(Modifier.width(12.dp))

        if (nextLevelInfo != null) {
            val (nextLevel, jobsNeeded) = nextLevelInfo
            val maxForLevel = when (level) {
                WorkerLevel.BRONZE -> 6
                WorkerLevel.SILVER -> 15  // 21-6
                WorkerLevel.GOLD -> 29    // 50-21
                else -> 1
            }
            val progress = 1f - (jobsNeeded.toFloat() / maxForLevel)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$jobsNeeded more jobs to ${
                        nextLevel.lowercase().replaceFirstChar { it.uppercase() }
                    }",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(WorkerLevel.getColorHex(nextLevel)),
                    trackColor = Color.LightGray.copy(alpha = 0.3f)
                )
            }
        } else {
            Text(
                text = "Max level reached! 🎉",
                fontSize = 12.sp,
                color = Color(0xFF9C27B0),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

