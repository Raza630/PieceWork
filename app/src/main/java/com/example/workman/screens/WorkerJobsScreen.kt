package com.example.workman.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workman.components.FeedbackData
import com.example.workman.components.FeedbackDialog
import com.example.workman.components.FeedbackType
import com.example.workman.components.PaymentSection
import com.example.workman.components.ReceivedReviewCard
import com.example.workman.dataClass.WorkOffer
import com.example.workman.ui.theme.PrimaryBlue
import com.example.workman.utils.ReviewRequestHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerJobsScreen(
    onBack: () -> Unit
) {
    var acceptedJobs by remember { mutableStateOf<List<WorkOffer>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isCompleting by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<FeedbackData?>(null) }
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val db = FirebaseFirestore.getInstance()

    // Animated professional feedback for start/complete actions.
    FeedbackDialog(data = feedback, onDismiss = { feedback = null })

    // Completion image picker
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) {
            selectedImages = it
        }

    fun fetchJobs() {
        if (userId != null) {
            isLoading = true
            db.collection("workOffers")
                .whereEqualTo("acceptedBy", userId)
                .get()
                .addOnSuccessListener { snapshot ->
                    acceptedJobs = snapshot.documents.mapNotNull {
                        it.toObject(WorkOffer::class.java)?.copy(id = it.id)
                    }
                    isLoading = false
                }
        }
    }

    LaunchedEffect(userId) { fetchJobs() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Accepted Jobs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (acceptedJobs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No jobs accepted yet", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(acceptedJobs) { job ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(job.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(job.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            Spacer(Modifier.height(8.dp))
                            Text("Date: ${job.date}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

                            Spacer(Modifier.height(12.dp))

                            if (job.status == "ASSIGNED") {
                                Button(
                                    onClick = {
                                        db.collection("workOffers").document(job.id)
                                            .update("status", "IN_PROGRESS")
                                            .addOnSuccessListener {
                                                feedback = FeedbackData(
                                                    type = FeedbackType.SUCCESS,
                                                    title = "You're on the clock! ⏱️",
                                                    message = "You've started \"${job.title}\". " +
                                                            "When you're done, add proof photos and mark it completed.",
                                                    confirmLabel = "Let's go"
                                                )
                                                fetchJobs()
                                            }
                                            .addOnFailureListener { e ->
                                                feedback = FeedbackData(
                                                    type = FeedbackType.ERROR,
                                                    title = "Couldn't start job",
                                                    message = e.localizedMessage
                                                        ?: "Please check your connection and try again.",
                                                    confirmLabel = "Try again"
                                                )
                                            }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Text("Start Working")
                                }
                            } else if (job.status == "IN_PROGRESS") {
                                Column {
                                    if (selectedImages.isNotEmpty()) {
                                        Text(
                                            "${selectedImages.size} images selected",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { imagePicker.launch("image/*") },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Add Proof Photos")
                                        }

                                        Button(
                                            onClick = {
                                                if (selectedImages.isNotEmpty()) {
                                                    isCompleting = true
                                                    // This is where Firebase Storage upload logic would go
                                                    db.collection("workOffers").document(job.id)
                                                        .update(
                                                            mapOf(
                                                                "status" to "COMPLETED",
                                                                "completionNote" to "Completed by worker"
                                                            )
                                                        ).addOnSuccessListener {
                                                            isCompleting = false
                                                            selectedImages = emptyList()
                                                            feedback = FeedbackData(
                                                                type = FeedbackType.SUCCESS,
                                                                title = "Job completed! ✅",
                                                                message = "Great work! The client has been notified " +
                                                                        "about \"${job.title}\". Your earnings will " +
                                                                        "update shortly and they can now leave you a review.",
                                                                confirmLabel = "Awesome"
                                                            )
                                                            fetchJobs()
                                                        }.addOnFailureListener { e ->
                                                            isCompleting = false
                                                            feedback = FeedbackData(
                                                                type = FeedbackType.ERROR,
                                                                title = "Couldn't complete job",
                                                                message = e.localizedMessage
                                                                    ?: "Please check your connection and try again.",
                                                                confirmLabel = "Try again"
                                                            )
                                                        }
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            enabled = selectedImages.isNotEmpty() && !isCompleting,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(
                                                    0xFF4CAF50
                                                )
                                            )
                                        ) {
                                            if (isCompleting) CircularProgressIndicator(
                                                modifier = Modifier.size(
                                                    18.dp
                                                ), color = Color.White, strokeWidth = 2.dp
                                            )
                                            else Text("Mark Completed")
                                        }
                                    }
                                }
                            } else {
                                when (job.status) {
                                    "COMPLETED" -> {
                                        // ── Payment confirmation (Phase 1 manual flow)
                                        PaymentSection(
                                            jobId = job.id,
                                            isBoss = false
                                        )
                                        Spacer(Modifier.height(12.dp))

                                        if (job.reviewRequested) {
                                            StatusNote(
                                                text = "Feedback requested — waiting for the client to review ⏳",
                                                color = Color(0xFFFF9800)
                                            )
                                        } else {
                                            Text(
                                                "Job done! Ask your client to leave a review — great reviews win you more jobs.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    ReviewRequestHelper.requestBossReview(
                                                        jobId = job.id,
                                                        bossId = job.bossId,
                                                        jobTitle = job.title
                                                    ) { success, msg ->
                                                        feedback = FeedbackData(
                                                            type = if (success) FeedbackType.SUCCESS else FeedbackType.ERROR,
                                                            title = if (success) "Feedback requested 🙌" else "Couldn't send request",
                                                            message = msg,
                                                            confirmLabel = "Done"
                                                        )
                                                        if (success) fetchJobs()
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                            ) {
                                                Text("Request feedback from client")
                                            }
                                        }
                                    }

                                    "REVIEWED" -> {
                                        PaymentSection(
                                            jobId = job.id,
                                            isBoss = false
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        ReceivedReviewCard(jobId = job.id, workerId = userId ?: "")
                                    }

                                    else -> {
                                        Text(
                                            "Status: ${job.status}",
                                            color = PrimaryBlue,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A small coloured status note (e.g. "waiting for the client"). */
@Composable
private fun StatusNote(text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color.copy(alpha = 0.10f),
                androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

