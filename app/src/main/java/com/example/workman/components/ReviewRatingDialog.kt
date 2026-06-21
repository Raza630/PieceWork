package com.example.workman.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.workman.ui.theme.PrimaryBlue
import com.example.workman.ui.theme.TextDark
import com.example.workman.ui.theme.TextMuted
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Review & Rating Dialog — shown to boss after worker marks job COMPLETED.
 * Submits review to Firestore and updates worker's average rating.
 */
@Composable
fun ReviewRatingDialog(
    jobId: String,
    workerId: String,
    workerName: String,
    onDismiss: () -> Unit,
    onSubmitted: () -> Unit
) {
    var rating by remember { mutableFloatStateOf(0f) }
    var comment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Rate Worker",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "How was your experience with $workerName?",
                    fontSize = 13.sp,
                    color = TextMuted
                )

                Spacer(Modifier.height(20.dp))

                // Star Rating
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "Star $i",
                            tint = if (i <= rating) Color(0xFFFFD700) else Color.LightGray,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { rating = i.toFloat() }
                                .padding(2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = when (rating.toInt()) {
                        1 -> "Poor"
                        2 -> "Below Average"
                        3 -> "Average"
                        4 -> "Good"
                        5 -> "Excellent!"
                        else -> "Tap to rate"
                    },
                    fontSize = 13.sp,
                    color = if (rating > 0) PrimaryBlue else TextMuted,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(16.dp))

                // Comment field
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Write a review (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Skip")
                    }

                    Button(
                        onClick = {
                            if (rating < 1f) {
                                Toast.makeText(
                                    context,
                                    "Please select a rating",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            isSubmitting = true

                            val review = hashMapOf(
                                "jobId" to jobId,
                                "reviewerId" to (currentUser?.uid ?: ""),
                                "reviewerName" to (currentUser?.displayName ?: "Boss"),
                                "revieweeId" to workerId,
                                "rating" to rating,
                                "comment" to comment,
                                "timestamp" to FieldValue.serverTimestamp()
                            )

                            db.collection("reviews").add(review)
                                .addOnSuccessListener {
                                    // Update job status
                                    db.collection("workOffers").document(jobId)
                                        .update(
                                            mapOf(
                                                "status" to "REVIEWED",
                                                "ratingSubmitted" to true
                                            )
                                        )

                                    // Update worker's average rating
                                    updateWorkerRating(db, workerId, rating)

                                    Toast.makeText(
                                        context,
                                        "Review submitted! Thank you.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    isSubmitting = false
                                    onSubmitted()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        context,
                                        "Failed to submit review",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    isSubmitting = false
                                }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = rating > 0 && !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Submit", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun updateWorkerRating(db: FirebaseFirestore, workerId: String, newRating: Float) {
    val userRef = db.collection("users").document(workerId)
    db.runTransaction { transaction ->
        val snapshot = transaction.get(userRef)
        val currentAvg = snapshot.getDouble("averageRating") ?: 0.0
        val totalRatings = snapshot.getLong("totalRatings") ?: 0L

        val newTotal = totalRatings + 1
        val newAverage = ((currentAvg * totalRatings) + newRating) / newTotal

        transaction.update(
            userRef, mapOf(
                "averageRating" to newAverage,
                "totalRatings" to newTotal
            )
        )
    }
}

