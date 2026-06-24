package com.example.workman.screens

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.workman.ChatActivity
import com.example.workman.SharedPreferencesHelper
import com.example.workman.components.MapLocationView
import com.example.workman.components.QuickContactSection
import com.example.workman.components.ReviewRatingDialog
import com.example.workman.components.UrgencyBadge
import com.example.workman.dataClass.WorkOffer
import com.example.workman.ui.theme.BgColor
import com.example.workman.ui.theme.PrimaryBlue
import com.example.workman.ui.theme.SecondaryBlue
import com.example.workman.ui.theme.TextDark
import com.example.workman.ui.theme.TextMuted
import com.google.firebase.auth.FirebaseAuth
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
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

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

    fun handleAcceptJob() {
        val user = auth.currentUser ?: return

        isAccepting = true
        scope.launch {
            try {
                db.collection("workOffers").document(offerId).update(
                    mapOf(
                        "acceptedBy" to user.uid,
                        "acceptedByName" to (user.displayName ?: "Worker"),
                        "acceptedByPhoto" to (user.photoUrl?.toString() ?: ""),
                        "status" to "ASSIGNED",
                        "isAccepted" to true
                    )
                ).await()

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

                Toast.makeText(context, "Job accepted successfully!", Toast.LENGTH_SHORT).show()
                fetchOfferDetails() // Refresh local state
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to accept job: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            } finally {
                isAccepting = false
            }
        }
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

                // Boss never sees an "Accept" button on their own posting.
                if (userRole != "Hiring") {
                    BottomAcceptBar(
                        isAccepting = isAccepting,
                        isAcceptedByMe = isAcceptedByMe,
                        isAcceptedByOther = isAcceptedByOther,
                        onAccept = { if (!isJobTaken && !isAccepting) handleAcceptJob() }
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
                    Text("Offer not found", color = TextMuted)
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
                        PostedByCard(current)

                        Spacer(Modifier.height(16.dp))

                        // ── Description card
                        SectionCard(title = "Description", icon = Icons.Outlined.Info) {
                            Text(
                                text = current.description.ifBlank { "No description provided." },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextDark.copy(alpha = 0.8f),
                                lineHeight = 22.sp
                            )
                        }

                        // ── Location map card
                        if (current.latitude != 0.0 && current.longitude != 0.0) {
                            Spacer(Modifier.height(16.dp))
                            SectionCard(title = "Job Location", icon = Icons.Default.LocationOn) {
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
                                    zoom = 15.0
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
                                if (userRole == "Hiring") "Contact Worker" else "Contact Boss"
                            val contactName =
                                if (userRole == "Hiring") "Worker" else current.bossName.ifEmpty { "Boss" }

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
                                    "⭐ Rate & Review Worker",
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                        }

                        if (showReviewDialog && !current.acceptedBy.isNullOrEmpty()) {
                            ReviewRatingDialog(
                                jobId = current.id,
                                workerId = current.acceptedBy!!,
                                workerName = "Worker",
                                onDismiss = { showReviewDialog = false },
                                onSubmitted = {
                                    showReviewDialog = false
                                    fetchOfferDetails()
                                }
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
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                    text = "${pagerState.currentPage + 1}/${safeImages.size}",
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
            label = "Posted",
            value = offer.date.ifBlank { "—" },
            modifier = Modifier.weight(1f)
        )
        val statusLabel = when (offer.status) {
            "OPEN" -> "Open"
            "ASSIGNED" -> "Assigned"
            "IN_PROGRESS" -> "In Progress"
            "COMPLETED" -> "Completed"
            "REVIEWED" -> "Reviewed"
            else -> offer.status
        }
        InfoChip(
            icon = Icons.Default.CheckCircle,
            label = "Status",
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
private fun PostedByCard(offer: WorkOffer) {
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
                Text("Posted by", fontSize = 11.sp, color = TextMuted)
                Text(
                    offer.bossName.ifBlank { "WorkMan Client" },
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

// ─── Bottom Accept Bar ──────────────────────────────────────────────────────────

@Composable
private fun BottomAcceptBar(
    isAccepting: Boolean,
    isAcceptedByMe: Boolean,
    isAcceptedByOther: Boolean,
    onAccept: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 12.dp
    ) {
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Button(
                onClick = onAccept,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isAcceptedByMe && !isAcceptedByOther && !isAccepting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        isAcceptedByMe -> Color(0xFF4CAF50)
                        isAcceptedByOther -> Color(0xFFFF9800)
                        else -> PrimaryBlue
                    },
                    disabledContainerColor = when {
                        isAcceptedByMe -> Color(0xFF4CAF50)
                        isAcceptedByOther -> Color(0xFFFF9800)
                        else -> Color.Gray.copy(alpha = 0.5f)
                    }
                )
            ) {
                if (isAccepting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = when {
                            isAcceptedByMe -> "✓ Accepted by You"
                            isAcceptedByOther -> "Already Taken"
                            else -> "Accept this Job"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
