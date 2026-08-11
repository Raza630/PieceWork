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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workman.R
import com.example.workman.components.FeedbackData
import com.example.workman.components.FeedbackDialog
import com.example.workman.components.FeedbackType
import com.example.workman.components.PaymentSection
import com.example.workman.components.ReceivedReviewCard
import com.example.workman.dataClass.WorkOffer
import com.example.workman.ui.theme.PrimaryBlue
import com.example.workman.utils.CloudinaryUploader
import com.example.workman.utils.ReviewRequestHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
                title = { Text(stringResource(R.string.my_accepted_jobs)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
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
                Text(stringResource(R.string.no_jobs_accepted), color = Color.Gray)
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
                            Text(
                                stringResource(R.string.job_date, job.date),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )

                            Spacer(Modifier.height(12.dp))

                            if (job.status == "ASSIGNED") {
                                Button(
                                    onClick = {
                                        db.collection("workOffers").document(job.id)
                                            .update("status", "IN_PROGRESS")
                                            .addOnSuccessListener {
                                                feedback = FeedbackData(
                                                    type = FeedbackType.SUCCESS,
                                                    title = context.getString(R.string.job_started_title),
                                                    message = context.getString(
                                                        R.string.job_started_msg,
                                                        job.title
                                                    ),
                                                    confirmLabel = context.getString(R.string.lets_go)
                                                )
                                                fetchJobs()
                                            }
                                            .addOnFailureListener { e ->
                                                feedback = FeedbackData(
                                                    type = FeedbackType.ERROR,
                                                    title = context.getString(R.string.job_start_failed_title),
                                                    message = e.localizedMessage
                                                        ?: context.getString(R.string.check_connection),
                                                    confirmLabel = context.getString(R.string.try_again)
                                                )
                                            }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Text(stringResource(R.string.start_working))
                                }
                            } else if (job.status == "IN_PROGRESS") {
                                Column {
                                    if (selectedImages.isNotEmpty()) {
                                        Text(
                                            stringResource(
                                                R.string.images_selected,
                                                selectedImages.size
                                            ),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { imagePicker.launch("image/*") },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(stringResource(R.string.add_proof_photos))
                                        }

                                        Button(
                                            onClick = {
                                                if (selectedImages.isNotEmpty()) {
                                                    isCompleting = true
                                                    val imagesToUpload = selectedImages
                                                    // Upload "After" proof photos to Cloudinary,
                                                    // then persist them so the boss can review the work.
                                                    scope.launch {
                                                        val uploadedUrls =
                                                            CloudinaryUploader.uploadImages(
                                                                context = context,
                                                                uris = imagesToUpload,
                                                                folder = "completions"
                                                            )

                                                        if (uploadedUrls.isEmpty()) {
                                                            isCompleting = false
                                                            feedback = FeedbackData(
                                                                type = FeedbackType.ERROR,
                                                                title = context.getString(R.string.photos_upload_failed_title),
                                                                message = context.getString(R.string.photos_upload_failed_msg),
                                                                confirmLabel = context.getString(R.string.try_again)
                                                            )
                                                            return@launch
                                                        }

                                                        db.collection("workOffers").document(job.id)
                                                            .update(
                                                                mapOf(
                                                                    "status" to "COMPLETED",
                                                                    "completionImages" to uploadedUrls,
                                                                    "completionNote" to "Completed by worker",
                                                                    "completedAt" to FieldValue.serverTimestamp()
                                                                )
                                                            ).addOnSuccessListener {
                                                                isCompleting = false
                                                                selectedImages = emptyList()
                                                                feedback = FeedbackData(
                                                                    type = FeedbackType.SUCCESS,
                                                                    title = context.getString(R.string.job_completed_title),
                                                                    message = context.getString(
                                                                        R.string.job_completed_msg,
                                                                        job.title
                                                                    ),
                                                                    confirmLabel = context.getString(
                                                                        R.string.awesome
                                                                    )
                                                                )
                                                                fetchJobs()
                                                            }.addOnFailureListener { e ->
                                                                isCompleting = false
                                                                feedback = FeedbackData(
                                                                    type = FeedbackType.ERROR,
                                                                    title = context.getString(R.string.job_complete_failed_title),
                                                                    message = e.localizedMessage
                                                                        ?: context.getString(R.string.check_connection),
                                                                    confirmLabel = context.getString(
                                                                        R.string.try_again
                                                                    )
                                                                )
                                                            }
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
                                            else Text(stringResource(R.string.mark_completed))
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
                                                text = stringResource(R.string.feedback_waiting_note),
                                                color = Color(0xFFFF9800)
                                            )
                                        } else {
                                            Text(
                                                stringResource(R.string.job_done_review_prompt),
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
                                                            title = if (success) context.getString(R.string.feedback_requested_title) else context.getString(
                                                                R.string.feedback_request_failed_title
                                                            ),
                                                            message = msg,
                                                            confirmLabel = context.getString(R.string.done)
                                                        )
                                                        if (success) fetchJobs()
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                            ) {
                                                Text(stringResource(R.string.request_feedback))
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
                                            stringResource(R.string.job_status, job.status),
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

