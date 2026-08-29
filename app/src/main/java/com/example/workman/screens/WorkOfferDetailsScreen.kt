package com.example.workman.screens

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.workman.ChatActivity
import com.example.workman.R
import com.example.workman.SharedPreferencesHelper
import com.example.workman.components.MapLocationView
import com.example.workman.components.QuickContactSection
import com.example.workman.components.ReceivedReviewCard
import com.example.workman.components.ReviewRatingDialog
import com.example.workman.components.UrgencyBadge
import com.example.workman.dataClass.WorkOffer
import com.example.workman.ui.theme.BgColor
import com.example.workman.ui.theme.PrimaryBlue
import com.example.workman.ui.theme.SecondaryBlue
import com.example.workman.ui.theme.TextDark
import com.example.workman.ui.theme.TextMuted
import com.example.workman.utils.ReviewRequestHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "WorkOfferDetailsScreen"

/**
 * Creates a deterministic chat ID from two user IDs.
 * Always produces the same ID regardless of parameter order.
 */
private fun createChatId(userId1: String, userId2: String): String {
    return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkOfferDetailsScreen(
    offerId: String,
    onBack: () -> Unit
) {
    var offer by remember { mutableStateOf<WorkOffer?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isAccepting by remember { mutableStateOf(false) }
    var isStartingWork by remember { mutableStateOf(false) }
    var isCancelling by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<com.example.workman.components.FeedbackData?>(null) }
    var fullScreenImage by remember { mutableStateOf<String?>(null) }
    var resolvedBossName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // Professional animated feedback for accepting a job.
    com.example.workman.components.FeedbackDialog(
        data = feedback,
        onDismiss = { feedback = null }
    )

    // Full-screen viewer for completion "After" photos.
    fullScreenImage?.let { url ->
        FullScreenImageDialog(url = url, onDismiss = { fullScreenImage = null })
    }

    fun fetchOfferDetails() {
        scope.launch {
            try {
                if (offerId.isNotEmpty()) {
                    val doc = db.collection("workOffers").document(offerId).get().await()
                    offer = doc.toObject(WorkOffer::class.java)?.copy(id = doc.id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load work offer $offerId", e)
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(offerId) {
        fetchOfferDetails()
    }

    // Resolve the real boss name from their profile so the "Posted by" card is
    // correct even for older jobs that stored a placeholder like "User".
    LaunchedEffect(offer?.bossId) {
        val bossId = offer?.bossId
        if (bossId.isNullOrBlank()) return@LaunchedEffect
        try {
            val doc = db.collection("users").document(bossId).get().await()
            val storedName = doc.getString("name")?.trim().orEmpty()
            val composed = listOfNotNull(
                doc.getString("firstName")?.trim(),
                doc.getString("lastName")?.trim()
            ).filter { it.isNotBlank() }.joinToString(" ")
            resolvedBossName = when {
                storedName.isNotBlank() -> storedName
                composed.isNotBlank() -> composed
                else -> ""
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not resolve boss name: ${e.message}")
        }
    }

    fun handleAcceptJob() {
        val user = auth.currentUser ?: return

        isAccepting = true
        scope.launch {
            try {
                val docRef = db.collection("workOffers").document(offerId)

                // Atomically claim the job so two workers can't accept the same
                // offer. Refuses if it was already taken or is no longer OPEN
                // (e.g. cancelled/completed by the boss in the meantime).
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    val existingAcceptedBy = snapshot.getString("acceptedBy")
                    val currentStatus = snapshot.getString("status") ?: "OPEN"

                    if (!existingAcceptedBy.isNullOrBlank() && existingAcceptedBy != user.uid) {
                        throw IllegalStateException("This job has already been taken by another worker.")
                    }
                    if (currentStatus != "OPEN") {
                        throw IllegalStateException("This job is no longer available.")
                    }

                    transaction.update(
                        docRef,
                        mapOf(
                            "acceptedBy" to user.uid,
                            "acceptedByName" to (user.displayName ?: "Worker"),
                            "acceptedByPhoto" to (user.photoUrl?.toString() ?: ""),
                            "status" to "ASSIGNED",
                            "isAccepted" to true
                        )
                    )
                    null
                }.await()

                // Best-effort: move the linked booking to ACTIVE so the boss's
                // Bookings tab updates immediately (works without Cloud Functions).
                try {
                    db.collection("bookings").document(offerId).update(
                        mapOf(
                            "workerId" to user.uid,
                            "workerName" to (user.displayName ?: "Worker"),
                            "workerPhotoUrl" to (user.photoUrl?.toString() ?: ""),
                            "status" to "ACTIVE"
                        )
                    ).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not update booking to ACTIVE: ${e.message}")
                }

                feedback = com.example.workman.components.FeedbackData(
                    type = com.example.workman.components.FeedbackType.SUCCESS,
                    title = context.getString(R.string.job_got_title),
                    message = context.getString(
                        R.string.job_got_message,
                        offer?.title ?: context.getString(R.string.job_this_job)
                    ),
                    confirmLabel = context.getString(R.string.great)
                )
                fetchOfferDetails() // Refresh local state
            } catch (e: Exception) {
                feedback = com.example.workman.components.FeedbackData(
                    type = com.example.workman.components.FeedbackType.ERROR,
                    title = context.getString(R.string.job_accept_failed_title),
                    message = e.localizedMessage
                        ?: context.getString(R.string.job_accept_failed_msg),
                    confirmLabel = context.getString(R.string.try_again)
                )
            } finally {
                isAccepting = false
            }
        }
    }

    fun handleStartWork() {
        val user = auth.currentUser ?: return

        isStartingWork = true
        scope.launch {
            try {
                val docRef = db.collection("workOffers").document(offerId)

                // Only the assigned worker may start the job, and only while it
                // is still ASSIGNED. Guards against stale UI / double taps.
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    val acceptedBy = snapshot.getString("acceptedBy")
                    val currentStatus = snapshot.getString("status") ?: "OPEN"

                    if (acceptedBy != user.uid) {
                        throw IllegalStateException("You are not assigned to this job.")
                    }
                    if (currentStatus != "ASSIGNED") {
                        throw IllegalStateException("This job can no longer be started.")
                    }

                    transaction.update(docRef, mapOf("status" to "IN_PROGRESS"))
                    null
                }.await()

                // Best-effort: keep the linked booking in sync for the boss.
                try {
                    db.collection("bookings").document(offerId)
                        .update("status", "IN_PROGRESS").await()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not update booking to IN_PROGRESS: ${e.message}")
                }

                feedback = com.example.workman.components.FeedbackData(
                    type = com.example.workman.components.FeedbackType.SUCCESS,
                    title = context.getString(R.string.work_started_title),
                    message = context.getString(
                        R.string.work_started_msg,
                        offer?.title ?: context.getString(R.string.job_this_job)
                    ),
                    confirmLabel = context.getString(R.string.lets_go)
                )
                fetchOfferDetails()
            } catch (e: Exception) {
                feedback = com.example.workman.components.FeedbackData(
                    type = com.example.workman.components.FeedbackType.ERROR,
                    title = context.getString(R.string.start_work_failed_title),
                    message = e.localizedMessage
                        ?: context.getString(R.string.check_connection),
                    confirmLabel = context.getString(R.string.try_again)
                )
            } finally {
                isStartingWork = false
            }
        }
    }

    fun handleCancelJob() {
        val user = auth.currentUser ?: return

        isCancelling = true
        scope.launch {
            try {
                val docRef = db.collection("workOffers").document(offerId)

                // Release the assignment and reopen the job for other workers.
                // Only the assigned worker may cancel, and only before it is
                // completed/reviewed.
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    val acceptedBy = snapshot.getString("acceptedBy")
                    val currentStatus = snapshot.getString("status") ?: "OPEN"

                    if (acceptedBy != user.uid) {
                        throw IllegalStateException("You are not assigned to this job.")
                    }
                    if (currentStatus != "ASSIGNED" && currentStatus != "IN_PROGRESS") {
                        throw IllegalStateException("This job can no longer be cancelled.")
                    }

                    transaction.update(
                        docRef,
                        mapOf(
                            "acceptedBy" to FieldValue.delete(),
                            "acceptedByName" to FieldValue.delete(),
                            "acceptedByPhoto" to FieldValue.delete(),
                            "status" to "OPEN",
                            "isAccepted" to false
                        )
                    )
                    null
                }.await()

                // Best-effort: return the linked booking to PENDING for the boss.
                try {
                    db.collection("bookings").document(offerId).update(
                        mapOf(
                            "workerId" to FieldValue.delete(),
                            "workerName" to FieldValue.delete(),
                            "workerPhotoUrl" to FieldValue.delete(),
                            "status" to "PENDING"
                        )
                    ).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not reset booking after cancel: ${e.message}")
                }

                feedback = com.example.workman.components.FeedbackData(
                    type = com.example.workman.components.FeedbackType.SUCCESS,
                    title = context.getString(R.string.job_cancelled_title),
                    message = context.getString(
                        R.string.job_cancelled_msg,
                        offer?.title ?: context.getString(R.string.job_this_job)
                    ),
                    confirmLabel = context.getString(R.string.done)
                )
                fetchOfferDetails()
            } catch (e: Exception) {
                feedback = com.example.workman.components.FeedbackData(
                    type = com.example.workman.components.FeedbackType.ERROR,
                    title = context.getString(R.string.cancel_job_failed_title),
                    message = e.localizedMessage
                        ?: context.getString(R.string.check_connection),
                    confirmLabel = context.getString(R.string.try_again)
                )
            } finally {
                isCancelling = false
            }
        }
    }

    // Confirmation before releasing an accepted job.
    if (showCancelDialog) {
        CancelJobDialog(
            jobTitle = offer?.title ?: "",
            onConfirm = {
                showCancelDialog = false
                handleCancelJob()
            },
            onDismiss = { showCancelDialog = false }
        )
    }

    Scaffold(
        containerColor = BgColor,
        bottomBar = {
            val current = offer
            if (!isLoading && current != null) {
                val currentUserId = auth.currentUser?.uid ?: ""
                val isJobTaken = current.isAccepted || !current.acceptedBy.isNullOrEmpty()
                val isAcceptedByMe = isJobTaken && current.acceptedBy == currentUserId
                val isAcceptedByOther = isJobTaken && !isAcceptedByMe
                val userRole = SharedPreferencesHelper(context).getUserChoice()

                // Boss never sees worker action buttons on their own posting.
                if (userRole != "Hiring") {
                    BottomWorkerActionBar(
                        status = current.status,
                        isAcceptedByMe = isAcceptedByMe,
                        isAcceptedByOther = isAcceptedByOther,
                        isAccepting = isAccepting,
                        isStartingWork = isStartingWork,
                        isCancelling = isCancelling,
                        onAccept = { if (!isJobTaken && !isAccepting) handleAcceptJob() },
                        onStartWork = { if (!isStartingWork) handleStartWork() },
                        onCancel = { if (!isCancelling) showCancelDialog = true }
                    )
                }
            }
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            }

            offer == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.offer_not_found), color = TextMuted)
                }
            }

            else -> {
                val current = offer!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = padding.calculateBottomPadding())
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Image gallery / carousel with overlay back button
                    ImageGalleryHeader(images = current.images, onBack = onBack)

                    // ── Main content pulled up to overlap the gallery
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-22).dp)
                            .background(
                                BgColor,
                                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                    ) {
                        // Title + category/urgency chips
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (current.category.isNotBlank()) {
                                Surface(
                                    color = SecondaryBlue,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = current.category,
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 4.dp
                                        ),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue
                                    )
                                }
                            }
                            UrgencyBadge(urgency = current.urgency)
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = current.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )

                        Spacer(Modifier.height(16.dp))

                        // ── Info chips: date + status
                        InfoChipsRow(current)

                        Spacer(Modifier.height(16.dp))

                        // ── Posted by card
                        PostedByCard(current, resolvedBossName)

                        Spacer(Modifier.height(16.dp))

                        // ── Description card
                        SectionCard(
                            title = stringResource(R.string.section_description),
                            icon = Icons.Outlined.Info
                        ) {
                            Text(
                                text = current.description.ifBlank { stringResource(R.string.no_description) },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextDark.copy(alpha = 0.8f),
                                lineHeight = 22.sp
                            )
                        }

                        // ── Completed Work proof — "After" photos + note submitted by the
                        // worker on completion. Shown to the boss (to review before rating)
                        // and to the worker (to see their own submission).
                        val userRoleForCompletion = SharedPreferencesHelper(context).getUserChoice()
                        val hasCompletionProof = current.completionImages.isNotEmpty() ||
                                current.completionNote.isNotBlank()
                        if (hasCompletionProof &&
                            (current.status == "COMPLETED" || current.status == "REVIEWED")
                        ) {
                            Spacer(Modifier.height(16.dp))
                            WorkCompletionSection(
                                offer = current,
                                isBoss = userRoleForCompletion == "Hiring",
                                onImageClick = { fullScreenImage = it }
                            )
                        }

                        // ── Location map card
                        if (current.latitude != 0.0 && current.longitude != 0.0) {
                            Spacer(Modifier.height(16.dp))
                            SectionCard(
                                title = stringResource(R.string.section_job_location),
                                icon = Icons.Default.LocationOn
                            ) {
                                if (current.locationName.isNotBlank()) {
                                    Text(
                                        text = current.locationName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextMuted
                                    )
                                    Spacer(Modifier.height(12.dp))
                                }
                                MapLocationView(
                                    latitude = current.latitude,
                                    longitude = current.longitude,
                                    locationName = current.locationName,
                                    zoom = 15.0,
                                    showDirections = true
                                )
                            }
                        }

                        // ── Quick Contact Section — shown after job is accepted
                        if (!current.acceptedBy.isNullOrEmpty()) {
                            Spacer(Modifier.height(16.dp))

                            val userRole = SharedPreferencesHelper(context).getUserChoice()
                            val contactUserId = if (userRole == "Hiring") {
                                current.acceptedBy!! // Boss contacts the worker
                            } else {
                                current.bossId // Worker contacts the boss
                            }
                            val contactLabel =
                                if (userRole == "Hiring") stringResource(R.string.contact_worker) else stringResource(
                                    R.string.contact_boss
                                )
                            val contactName =
                                if (userRole == "Hiring") stringResource(R.string.role_worker)
                                else resolvedBossName.ifBlank {
                                    current.bossName.ifBlank { stringResource(R.string.role_boss) }
                                }

                            QuickContactSection(
                                userId = contactUserId,
                                userName = contactName,
                                label = contactLabel,
                                onChatClick = {
                                    val chatId = createChatId(current.bossId, current.acceptedBy!!)
                                    val intent = Intent(context, ChatActivity::class.java).apply {
                                        putExtra("CHAT_ID", chatId)
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }

                        // ── Review button for boss when job is COMPLETED
                        var showReviewDialog by remember { mutableStateOf(false) }
                        val userRole = SharedPreferencesHelper(context).getUserChoice()

                        if (current.status == "COMPLETED" && userRole == "Hiring" && !current.ratingSubmitted) {
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { showReviewDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFFFFD700
                                    )
                                )
                            ) {
                                Text(
                                    stringResource(R.string.rate_review_worker),
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                        }

                        if (showReviewDialog && !current.acceptedBy.isNullOrEmpty()) {
                            ReviewRatingDialog(
                                jobId = current.id,
                                workerId = current.acceptedBy!!,
                                workerName = stringResource(R.string.role_worker),
                                onDismiss = { showReviewDialog = false },
                                onSubmitted = {
                                    showReviewDialog = false
                                    fetchOfferDetails()
                                }
                            )
                        }

                        // ── Worker: seek feedback from the client on a completed job
                        val myUid = auth.currentUser?.uid
                        val isMyCompletedJob =
                            userRole != "Hiring" && current.acceptedBy == myUid

                        if (isMyCompletedJob && current.status == "COMPLETED") {
                            Spacer(Modifier.height(16.dp))
                            if (current.reviewRequested) {
                                Surface(
                                    color = Color(0xFFFF9800).copy(alpha = 0.10f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(R.string.feedback_requested_note),
                                        modifier = Modifier.padding(14.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFB26A00),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.job_complete_review_prompt),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        ReviewRequestHelper.requestBossReview(
                                            jobId = current.id,
                                            bossId = current.bossId,
                                            jobTitle = current.title
                                        ) { success, msg ->
                                            feedback = com.example.workman.components.FeedbackData(
                                                type = if (success) com.example.workman.components.FeedbackType.SUCCESS
                                                else com.example.workman.components.FeedbackType.ERROR,
                                                title = if (success) context.getString(R.string.feedback_requested_title) else context.getString(
                                                    R.string.feedback_request_failed_title
                                                ),
                                                message = msg,
                                                confirmLabel = context.getString(R.string.done)
                                            )
                                            if (success) fetchOfferDetails()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Text(
                                        stringResource(R.string.request_feedback),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // ── Worker: show the review the client left
                        if (isMyCompletedJob && current.status == "REVIEWED") {
                            Spacer(Modifier.height(16.dp))
                            ReceivedReviewCard(
                                jobId = current.id,
                                workerId = myUid ?: ""
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// ─── Image Gallery Header ───────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageGalleryHeader(
    images: List<String>,
    onBack: () -> Unit
) {
    val safeImages = if (images.isEmpty()) listOf("https://via.placeholder.com/400") else images
    val pagerState = rememberPagerState(pageCount = { safeImages.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        HorizontalPager(state = pagerState) { page ->
            AsyncImage(
                model = safeImages[page],
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Dark gradient at top for back-button visibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)
                    )
                )
        )

        // Back button
        Box(
            modifier = Modifier
                .padding(start = 16.dp, top = 40.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = Color.White
            )
        }

        // Image counter chip (top-right)
        if (safeImages.size > 1) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 44.dp, end = 16.dp),
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.image_counter,
                        pagerState.currentPage + 1,
                        safeImages.size
                    ),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Page indicator dots (bottom center, above the overlapping card)
        if (safeImages.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(safeImages.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(7.dp)
                            .width(if (selected) 20.dp else 7.dp)
                            .clip(CircleShape)
                            .background(if (selected) Color.White else Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

// ─── Info Chips Row ─────────────────────────────────────────────────────────────

@Composable
private fun InfoChipsRow(offer: WorkOffer) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoChip(
            icon = Icons.Default.DateRange,
            label = stringResource(R.string.info_posted),
            value = offer.date.ifBlank { stringResource(R.string.dash) },
            modifier = Modifier.weight(1f)
        )
        val statusLabel = when (offer.status) {
            "OPEN" -> stringResource(R.string.status_open)
            "ASSIGNED" -> stringResource(R.string.status_assigned)
            "IN_PROGRESS" -> stringResource(R.string.status_in_progress_badge)
            "COMPLETED" -> stringResource(R.string.status_completed_badge)
            "REVIEWED" -> stringResource(R.string.status_reviewed)
            else -> offer.status
        }
        InfoChip(
            icon = Icons.Default.CheckCircle,
            label = stringResource(R.string.info_status),
            value = statusLabel,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SecondaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, fontSize = 11.sp, color = TextMuted)
                Text(
                    value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }
        }
    }
}

// ─── Posted By Card ─────────────────────────────────────────────────────────────

@Composable
private fun PostedByCard(offer: WorkOffer, resolvedName: String = "") {
    // Prefer the freshly-resolved profile name; fall back to the stored
    // bossName, ignoring known placeholders ("User"/"WorkMan Client").
    val storedName = offer.bossName.takeIf {
        it.isNotBlank() && !it.equals("User", ignoreCase = true) &&
                !it.equals("WorkMan Client", ignoreCase = true)
    }
    val displayName = resolvedName.ifBlank { storedName ?: stringResource(R.string.workman_client) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.posted_by), fontSize = 11.sp, color = TextMuted)
                Text(
                    displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }
        }
    }
}

// ─── Generic Section Card ───────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

// ─── Bottom Worker Action Bar ───────────────────────────────────────────────────

/**
 * Status-aware action bar for the worker:
 *  • OPEN            → Accept this Job
 *  • ASSIGNED (mine) → Start Work  +  Cancel Job
 *  • IN_PROGRESS     → Work in Progress (info)  +  Cancel Job
 *  • taken by other  → Already Taken (disabled)
 *  • completed/etc.  → status pill (no actions)
 */
@Composable
private fun BottomWorkerActionBar(
    status: String,
    isAcceptedByMe: Boolean,
    isAcceptedByOther: Boolean,
    isAccepting: Boolean,
    isStartingWork: Boolean,
    isCancelling: Boolean,
    onAccept: () -> Unit,
    onStartWork: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(color = Color.White, shadowElevation = 12.dp) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            when {
                // ── Assigned to me and not yet started → Start Work + Cancel
                isAcceptedByMe && status == "ASSIGNED" -> {
                    PrimaryBar(
                        text = stringResource(R.string.start_work),
                        loading = isStartingWork,
                        enabled = !isStartingWork && !isCancelling,
                        containerColor = Color(0xFF4CAF50),
                        onClick = onStartWork
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedCancelButton(
                        loading = isCancelling,
                        enabled = !isStartingWork && !isCancelling,
                        onClick = onCancel
                    )
                }

                // ── In progress → info state + Cancel
                isAcceptedByMe && status == "IN_PROGRESS" -> {
                    StatusPill(
                        text = stringResource(R.string.work_in_progress_state),
                        color = Color(0xFF2196F3)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.complete_from_my_jobs),
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedCancelButton(
                        loading = isCancelling,
                        enabled = !isCancelling,
                        onClick = onCancel
                    )
                }

                // ── Completed / reviewed by me → status pill, no actions
                isAcceptedByMe && (status == "COMPLETED" || status == "REVIEWED") -> {
                    StatusPill(
                        text = stringResource(R.string.accept_state_accepted),
                        color = Color(0xFF4CAF50)
                    )
                }

                // ── Taken by another worker → disabled
                isAcceptedByOther -> {
                    PrimaryBar(
                        text = stringResource(R.string.accept_state_taken),
                        loading = false,
                        enabled = false,
                        containerColor = Color(0xFFFF9800),
                        onClick = {}
                    )
                }

                // ── Open → Accept
                else -> {
                    PrimaryBar(
                        text = stringResource(R.string.accept_this_job),
                        loading = isAccepting,
                        enabled = !isAccepting,
                        containerColor = PrimaryBlue,
                        onClick = onAccept
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryBar(
    text: String,
    loading: Boolean,
    enabled: Boolean,
    containerColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor.copy(alpha = 0.6f)
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        }
    }
}

@Composable
private fun OutlinedCancelButton(
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val red = Color(0xFFE53935)
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(16.dp),
        enabled = enabled,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            red.copy(alpha = if (enabled) 1f else 0.4f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = red)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = red,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                stringResource(R.string.cancel_job),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = color)
        }
    }
}

// ─── Cancel Job Confirmation Dialog ─────────────────────────────────────────────

@Composable
private fun CancelJobDialog(
    jobTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = Color(0xFFE53935)
            )
        },
        title = {
            Text(
                stringResource(R.string.cancel_job_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(stringResource(R.string.cancel_job_message, jobTitle))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Text(stringResource(R.string.cancel_job_confirm), color = Color.White)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_job_dismiss), color = TextMuted)
            }
        }
    )
}

// ─── Work Completion Proof (After photos + note) ─────────────────────────────────

@Composable
private fun WorkCompletionSection(
    offer: WorkOffer,
    isBoss: Boolean,
    onImageClick: (String) -> Unit
) {
    SectionCard(
        title = stringResource(R.string.section_completed_work),
        icon = Icons.Default.CheckCircle
    ) {
        Text(
            text = if (isBoss)
                stringResource(R.string.completed_work_boss_desc)
            else
                stringResource(R.string.completed_work_worker_desc),
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )

        if (offer.completionNote.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Surface(
                color = SecondaryBlue.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.completion_note_quote, offer.completionNote),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark
                )
            }
        }

        if (offer.completionImages.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                offer.completionImages.forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = stringResource(R.string.cd_completion_photo),
                        modifier = Modifier
                            .size(128.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick(url) },
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.tap_photo_fullscreen),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

// ─── Full-screen Image Viewer ────────────────────────────────────────────────────

@Composable
private fun FullScreenImageDialog(
    url: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = url,
                contentDescription = stringResource(R.string.cd_completion_photo),
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 16.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_close),
                    tint = Color.White
                )
            }
        }
    }
}


