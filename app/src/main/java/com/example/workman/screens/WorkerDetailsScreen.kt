package com.example.workman.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.workman.ChatActivity
import com.example.workman.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// Boss dashboard theme colours (matching Homebossdashboardscreen)
private val WdCream = Color(0xFFFFF3E0)
private val WdOrange = Color(0xFFFF9800)
private val WdOrangeLight = Color(0xFFFFE0B2)
private val WdTextDark = Color(0xFF1A1A1A)
private val WdTextMuted = Color(0xFF888888)

data class WorkerProfileData(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val dob: String = "",
    val gender: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val isVerified: Boolean = false,
    val category: String = "",
    val yearsOfExperience: Int = 0,
    val ratePerHour: Int = 0,
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val workerLevel: String = "BRONZE",
    val completedJobs: Int = 0,
    val location: String = "",
    val portfolio: List<String> = emptyList(),
    val loaded: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDetailsScreen(
    workerId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val workerFallback = stringResource(R.string.role_worker)

    val profile by produceState(initialValue = WorkerProfileData(), workerId) {
        value = try {
            val doc = FirebaseFirestore.getInstance()
                .collection("users").document(workerId).get().await()
            WorkerProfileData(
                id = workerId,
                name = doc.getString("name")
                    ?: listOfNotNull(doc.getString("firstName"), doc.getString("lastName"))
                        .joinToString(" ").ifBlank { workerFallback },
                email = doc.getString("email") ?: "",
                dob = doc.getString("dob") ?: "",
                gender = doc.getString("gender") ?: "",
                phone = doc.getString("phone") ?: "",
                photoUrl = doc.getString("photoUrl") ?: "",
                isVerified = doc.getBoolean("isVerified") ?: false,
                category = doc.getString("category") ?: "",
                yearsOfExperience = (doc.getLong("yearsOfExperience") ?: 0L).toInt(),
                ratePerHour = (doc.getLong("ratePerHour") ?: 0L).toInt(),
                rating = doc.getDouble("averageRating") ?: doc.getDouble("rating") ?: 0.0,
                reviewCount = (doc.getLong("totalRatings")
                    ?: doc.getString("reviewCount")?.toLongOrNull() ?: 0L).toInt(),
                workerLevel = doc.getString("workerLevel") ?: "BRONZE",
                completedJobs = (doc.getLong("completedJobsCount") ?: 0L).toInt(),
                location = doc.getString("location") ?: "",
                portfolio = (doc.get("portfolioImages") as? List<*>)?.mapNotNull { it as? String }
                    ?: emptyList(),
                loaded = true
            )
        } catch (e: Exception) {
            WorkerProfileData(id = workerId, name = workerFallback, loaded = true)
        }
    }

    Scaffold(
        containerColor = WdCream,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.worker_profile),
                        color = WdTextDark,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            stringResource(R.string.cd_back),
                            tint = WdTextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (!profile.loaded) {
            Box(Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WdOrange)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(WdOrange, Color(0xFFFFB74D))),
                        RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = profile.photoUrl.ifBlank {
                            "https://ui-avatars.com/api/?name=${
                                profile.name.replace(
                                    " ",
                                    "+"
                                )
                            }&background=fff&color=FF9800&size=200"
                        },
                        contentDescription = profile.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color.White, CircleShape)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            profile.name,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (profile.isVerified) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.CheckCircle,
                                stringResource(R.string.cd_verified),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (profile.category.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.25f))
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text(
                                profile.category,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LevelBadge(profile.workerLevel)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    "⭐ ${"%.1f".format(profile.rating)}",
                    stringResource(R.string.reviews_count, profile.reviewCount),
                    Modifier.weight(1f)
                )
                StatCard(
                    "${profile.completedJobs}",
                    stringResource(R.string.jobs_done),
                    Modifier.weight(1f)
                )
                StatCard(
                    stringResource(R.string.years_short, profile.yearsOfExperience),
                    stringResource(R.string.experience),
                    Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Rate card
            if (profile.ratePerHour > 0) {
                InfoCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.hourly_rate),
                            color = WdTextMuted,
                            fontSize = 14.sp
                        )
                        Text(
                            stringResource(R.string.rate_hr, profile.ratePerHour.toString()),
                            color = WdOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            // ── Contact details card
            InfoCard {
                Text(
                    stringResource(R.string.contact_details),
                    fontWeight = FontWeight.Bold,
                    color = WdTextDark,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(12.dp))
                if (profile.phone.isNotBlank()) DetailRow(
                    Icons.Default.Phone,
                    stringResource(R.string.label_phone),
                    profile.phone
                )
                if (profile.email.isNotBlank()) DetailRow(
                    Icons.Default.Email,
                    stringResource(R.string.label_email),
                    profile.email
                )
                if (profile.location.isNotBlank()) DetailRow(
                    Icons.Default.LocationOn,
                    stringResource(R.string.label_location),
                    profile.location
                )
                if (profile.gender.isNotBlank()) DetailRow(
                    Icons.Default.Build,
                    stringResource(R.string.label_gender),
                    profile.gender
                )
            }

            // ── Portfolio
            if (profile.portfolio.isNotEmpty()) {
                Text(
                    stringResource(R.string.work_portfolio),
                    fontWeight = FontWeight.Bold,
                    color = WdTextDark,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp)
                )
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(profile.portfolio) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = stringResource(R.string.cd_portfolio),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Contact action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ContactButton(
                    stringResource(R.string.contact_call),
                    Icons.Default.Phone,
                    Color(0xFF4CAF50),
                    Modifier.weight(1f)
                ) {
                    if (profile.phone.isNotBlank()) {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_DIAL,
                                Uri.parse("tel:${profile.phone}")
                            )
                        )
                    }
                }
                ContactButton(
                    stringResource(R.string.contact_whatsapp),
                    Icons.Default.Email,
                    Color(0xFF25D366),
                    Modifier.weight(1f)
                ) {
                    if (profile.phone.isNotBlank()) {
                        val num = profile.phone.replace("+", "")
                            .let { if (it.length == 10) "91$it" else it }
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$num"))
                        )
                    }
                }
                ContactButton(
                    stringResource(R.string.contact_chat),
                    Icons.Default.Email,
                    WdOrange,
                    Modifier.weight(1f)
                ) {
                    val me = FirebaseAuth.getInstance().currentUser?.uid ?: return@ContactButton
                    val chatId = if (me < workerId) "${me}_$workerId" else "${workerId}_$me"
                    context.startActivity(
                        Intent(context, ChatActivity::class.java).putExtra("CHAT_ID", chatId)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LevelBadge(level: String) {
    val (emoji, color) = when (level.uppercase()) {
        "PLATINUM" -> "💎" to Color(0xFF00BCD4)
        "GOLD" -> "🥇" to Color(0xFFFFD700)
        "SILVER" -> "🥈" to Color(0xFFC0C0C0)
        else -> "🥉" to Color(0xFFCD7F32)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            "$emoji ${level.lowercase().replaceFirstChar { it.uppercase() }}",
            color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, color = WdTextDark, fontSize = 16.sp)
            Spacer(Modifier.height(2.dp))
            Text(label, color = WdTextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun InfoCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(WdOrangeLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = WdOrange, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = WdTextMuted, fontSize = 11.sp)
            Text(value, color = WdTextDark, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ContactButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

