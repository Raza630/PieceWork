package com.example.workman.screens

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
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
import com.example.workman.dataClass.WorkOffer
import com.example.workman.ui.theme.BgColor
import com.example.workman.ui.theme.PrimaryBlue
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
        topBar = {
            TopAppBar(
                title = { Text("Job Details", color = TextDark, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (offer == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Offer not found", color = TextMuted)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Image Header
                AsyncImage(
                    model = offer!!.images.firstOrNull() ?: "https://via.placeholder.com/400",
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)),
                    contentScale = ContentScale.Crop
                )

                Column(modifier = Modifier.padding(24.dp)) {
                    Surface(
                        color = PrimaryBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Work Opportunity",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = offer!!.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        if (offer!!.locationName.isNotEmpty()) {
                            Text(text = offer!!.locationName, color = TextMuted, fontSize = 14.sp)
                        } else {
                            Text(
                                text = "Location not specified",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(text = "Posted on: ${offer!!.date}", color = TextMuted, fontSize = 13.sp)

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = offer!!.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextDark.copy(alpha = 0.8f),
                        lineHeight = 24.sp
                    )

                    // Map showing job location
                    if (offer!!.latitude != 0.0 && offer!!.longitude != 0.0) {
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Job Location",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        MapLocationView(
                            latitude = offer!!.latitude,
                            longitude = offer!!.longitude,
                            locationName = offer!!.locationName,
                            zoom = 15.0
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Determine acceptance state: check both isAccepted flag AND acceptedBy field
                    val currentUserId = auth.currentUser?.uid ?: ""
                    val isJobTaken = offer!!.isAccepted || !offer!!.acceptedBy.isNullOrEmpty()
                    val isAcceptedByMe = isJobTaken && offer!!.acceptedBy == currentUserId
                    val isAcceptedByOther = isJobTaken && !isAcceptedByMe

                    Button(
                        onClick = { if (!isJobTaken && !isAccepting) handleAcceptJob() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isJobTaken && !isAccepting,
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

                    // Quick Contact Section — shown after job is accepted
                    if (isJobTaken && !offer!!.acceptedBy.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))

                        val userRole = SharedPreferencesHelper(context).getUserChoice()
                        val contactUserId = if (userRole == "Hiring") {
                            offer!!.acceptedBy!! // Boss contacts the worker
                        } else {
                            offer!!.bossId // Worker contacts the boss
                        }
                        val contactLabel =
                            if (userRole == "Hiring") "Contact Worker" else "Contact Boss"
                        val contactName =
                            if (userRole == "Hiring") "Worker" else offer!!.bossName.ifEmpty { "Boss" }

                        QuickContactSection(
                            userId = contactUserId,
                            userName = contactName,
                            label = contactLabel,
                            onChatClick = {
                                // Create or open chat between boss and worker
                                val chatId = createChatId(offer!!.bossId, offer!!.acceptedBy!!)
                                val intent = Intent(context, ChatActivity::class.java).apply {
                                    putExtra("CHAT_ID", chatId)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }

                    // Review button for boss when job is COMPLETED
                    var showReviewDialog by remember { mutableStateOf(false) }
                    val userRole = SharedPreferencesHelper(context).getUserChoice()

                    if (offer!!.status == "COMPLETED" && userRole == "Hiring" && !offer!!.ratingSubmitted) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showReviewDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                        ) {
                            Text(
                                "⭐ Rate & Review Worker",
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                    }

                    if (showReviewDialog && !offer!!.acceptedBy.isNullOrEmpty()) {
                        ReviewRatingDialog(
                            jobId = offer!!.id,
                            workerId = offer!!.acceptedBy!!,
                            workerName = "Worker",
                            onDismiss = { showReviewDialog = false },
                            onSubmitted = {
                                showReviewDialog = false
                                fetchOfferDetails() // Refresh
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
