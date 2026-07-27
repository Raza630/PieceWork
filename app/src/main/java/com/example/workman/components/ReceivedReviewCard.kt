package com.example.workman.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Displays the review a boss left for the worker on a completed job.
 * Fetched from the `reviews` collection (matching jobId + revieweeId == worker).
 *
 * Used to give the worker visible, satisfying feedback once the client reviews
 * their work.
 */
@Composable
fun ReceivedReviewCard(
    jobId: String,
    workerId: String,
    modifier: Modifier = Modifier
) {
    var rating by remember { mutableFloatStateOf(0f) }
    var comment by remember { mutableStateOf("") }
    var reviewerName by remember { mutableStateOf("The client") }
    var found by remember { mutableStateOf(false) }

    LaunchedEffect(jobId, workerId) {
        if (workerId.isBlank()) return@LaunchedEffect
        try {
            val snap = FirebaseFirestore.getInstance().collection("reviews")
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("revieweeId", workerId)
                .limit(1)
                .get()
                .await()
            snap.documents.firstOrNull()?.let { doc ->
                rating = (doc.getDouble("rating") ?: 0.0).toFloat()
                comment = doc.getString("comment") ?: ""
                reviewerName = doc.getString("reviewerName") ?: "The client"
                found = true
            }
        } catch (_: Exception) {
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF4CAF50).copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "$reviewerName left you a review",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF2E7D32),
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            for (i in 1..5) {
                Icon(
                    imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = null,
                    tint = if (i <= rating) Color(0xFFFFC107) else Color.LightGray,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (found && rating > 0) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "%.1f".format(rating),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }
        }
        if (comment.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "\"$comment\"",
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray
            )
        }
    }
}

