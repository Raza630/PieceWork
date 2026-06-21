package com.example.workman.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.workman.components.UrgencyBadge
import com.example.workman.dataClass.Banner
import com.example.workman.dataClass.WorkOffer
import com.example.workman.utils.JobMatchingEngine
import com.example.workman.utils.LocationHelper
import com.example.workman.viewModels.HomeWorkerDashboardViewModel
import com.example.workman.viewModels.WorkOfferListState

// ─── Color Palette ─────────────────────────────────────────────────────────────

private val BgColor      = Color(0xFFF0F2F8)
private val CardBg       = Color(0xFFFFFFFF)
private val PrimaryBlue  = Color(0xFF5B61F4)
private val SecondaryBlue = Color(0xFFE8E9FF)
private val TextDark     = Color(0xFF1A1C3D)
private val TextMuted    = Color(0xFF8E92B2)
private val GradientStart = Color(0xFF5B61F4)
private val GradientEnd   = Color(0xFF3F46D0)
private val MatchGold = Color(0xFFFF9800)
private val MatchGreen = Color(0xFF4CAF50)

// ─── Root Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeWorkerDashboardScreen(
    viewModel: HomeWorkerDashboardViewModel = viewModel(),
    onOfferClick: (WorkOffer) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onNavProfile: () -> Unit = {},
    onNavChat: () -> Unit = {},
    onNavHome: () -> Unit = {},
    onNavJobs: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedNavItem by remember { mutableStateOf(0) }
    val context = LocalContext.current

    Scaffold(
        containerColor = BgColor,
        bottomBar = {
            WorkerBottomNav(
                selectedIndex = selectedNavItem,
                onSelect = { idx ->
                    selectedNavItem = idx
                    when (idx) {
                        0 -> onNavHome()
                        1 -> onNavJobs()
                        2 -> onNavChat()
                        3 -> onNavProfile()
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // ── Header Section
                item {
                    WorkerHeader(
                        name = uiState.userName,
                        location = uiState.userLocation,
                        onNotificationClick = onNotificationClick
                    )
                }

                // ── Search Bar
                item {
                    WorkerSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                    )
                }

                // ── Banners / Promotions
                if (uiState.banners.isNotEmpty()) {
                    item {
                        BannerRow(
                            banners = uiState.banners,
                            onBannerClick = { banner ->
                                // Filter offers by banner category
                                if (banner.category.isNotBlank()) {
                                    viewModel.onSearchQueryChange(banner.category)
                                }
                            }
                        )
                    }
                }

                // ── Location Radius Chips (always visible)
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(10.0, 25.0, 50.0, 100.0).forEach { radius ->
                                FilterChip(
                                    selected = uiState.searchRadiusKm == radius,
                                    onClick = { viewModel.updateSearchRadius(radius) },
                                    label = { Text("${radius.toInt()} km", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue,
                                        selectedLabelColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                        }
                        if (!uiState.isLocationAvailable) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Enable location to filter by distance",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        } else {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${uiState.nearbyOfferCount} jobs within ${uiState.searchRadiusKm.toInt()} km",
                                fontSize = 11.sp,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // ── Work Offers List
                when (val state = uiState.offerListState) {
                    is WorkOfferListState.Loading -> {
                        items(3) { WorkOfferSkeleton() }
                    }
                    is WorkOfferListState.Error -> {
                        item {
                            WorkerErrorState(
                                message = state.message,
                                onRetry = viewModel::fetchWorkOffers
                            )
                        }
                    }
                    is WorkOfferListState.Success -> {
                        // ── "Recommended for You" section
                        if (uiState.recommendedOffers.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Recommended for You",
                                    subtitle = "${uiState.recommendedOffers.size} best matches",
                                    icon = Icons.Default.Star,
                                    iconTint = MatchGold
                                )
                            }

                            items(
                                uiState.recommendedOffers,
                                key = { "rec_${it.offer.id}" }
                            ) { scoredOffer ->
                                WorkOfferCard(
                                    offer = scoredOffer.offer,
                                    scoredOffer = scoredOffer,
                                    isAccepting = uiState.acceptingOfferIds.contains(scoredOffer.offer.id),
                                    onAccept = {
                                        viewModel.acceptWork(scoredOffer.offer) { _, msg ->
                                            android.widget.Toast.makeText(
                                                context,
                                                msg,
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onClick = { onOfferClick(scoredOffer.offer) },
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                            }
                        }

                        // ── "Other Available Work" section
                        if (uiState.otherOffers.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = if (uiState.recommendedOffers.isNotEmpty()) "Other Available Work" else "Available Work",
                                    subtitle = "${uiState.nearbyOfferCount} jobs within ${uiState.searchRadiusKm.toInt()} km",
                                    icon = null,
                                    iconTint = PrimaryBlue
                                )
                            }

                            items(
                                uiState.otherOffers,
                                key = { "other_${it.offer.id}" }
                            ) { scoredOffer ->
                                WorkOfferCard(
                                    offer = scoredOffer.offer,
                                    scoredOffer = scoredOffer,
                                    isAccepting = uiState.acceptingOfferIds.contains(scoredOffer.offer.id),
                                    onAccept = {
                                        viewModel.acceptWork(scoredOffer.offer) { _, msg ->
                                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onClick = { onOfferClick(scoredOffer.offer) },
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                            }
                        }

                        // ── Empty state
                        if (uiState.recommendedOffers.isEmpty() && uiState.otherOffers.isEmpty()) {
                            item { WorkerEmptyState(query = uiState.searchQuery) }
                        }
                    }
                }
            }
        }
    }
}

// ─── Section Header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    iconTint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
            }
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}

// ─── Match Badge ───────────────────────────────────────────────────────────────

@Composable
private fun MatchBadge(scoredOffer: JobMatchingEngine.ScoredOffer) {
    val score = scoredOffer.matchScore
    val isGreatMatch = score >= JobMatchingEngine.GREAT_MATCH_THRESHOLD

    val badgeColor = if (isGreatMatch) MatchGreen else MatchGold
    val badgeBg = badgeColor.copy(alpha = 0.1f)

    Surface(
        color = badgeBg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isGreatMatch) {
                Icon(
                    Icons.Default.Star,
                    null,
                    tint = badgeColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = "${score}% match",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }
    }

    if (scoredOffer.matchReason.isNotEmpty()) {
        Spacer(Modifier.width(6.dp))
        Text(
            text = scoredOffer.matchReason,
            fontSize = 10.sp,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── Components ────────────────────────────────────────────────────────────────

@Composable
private fun WorkerHeader(
    name: String,
    location: String,
    onNotificationClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                brush = Brush.verticalGradient(listOf(GradientStart, GradientEnd)),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .padding(horizontal = 20.dp, vertical = 40.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        "Hello, $name!",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(location, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { onNotificationClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
            }
        }
    }
}

@Composable
private fun WorkerSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = TextMuted)
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = TextDark, fontSize = 16.sp),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Search for work opportunities...", color = TextMuted)
                    inner()
                }
            )
        }
    }
}

@Composable
private fun BannerRow(banners: List<Banner>, onBannerClick: (Banner) -> Unit = {}) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(banners) { banner ->
            Card(
                modifier = Modifier
                    .width(280.dp)
                    .height(140.dp)
                    .clickable { onBannerClick(banner) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(banner.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(banner.description, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, maxLines = 2)
                        Spacer(Modifier.weight(1f))
                        if (banner.averagePay.isNotBlank()) {
                            Text(
                                "Pay: ${banner.averagePay}",
                                color = Color.Yellow,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        if (banner.category.isNotBlank()) {
                            Text(
                                "Tap to explore →",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }
                    if (banner.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = banner.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkOfferCard(
    offer: WorkOffer,
    scoredOffer: JobMatchingEngine.ScoredOffer? = null,
    isAccepting: Boolean,
    onAccept: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAcceptedByMe = offer.acceptedBy != null

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Match badge row at the top
            if (scoredOffer != null && scoredOffer.matchScore >= JobMatchingEngine.RECOMMENDED_THRESHOLD) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    MatchBadge(scoredOffer)
                }
            }

            Row(verticalAlignment = Alignment.Top) {
                // Work Thumbnail
                val placeholder = "https://ui-avatars.com/api/?name=${offer.title.replace(" ", "+")}&background=5B61F4&color=fff"
                AsyncImage(
                    model = if (offer.images.isNotEmpty()) offer.images[0] else placeholder,
                    contentDescription = null,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        offer.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        offer.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Category chip
                    if (offer.category.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = SecondaryBlue,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = offer.category,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryBlue
                                )
                            }
                            if (offer.urgency == "URGENT") {
                                UrgencyBadge(urgency = offer.urgency)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Divider(color = BgColor)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.DateRange,
                            null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            offer.date,
                            fontSize = 12.sp,
                            color = TextDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // Distance badge
                    if (offer.distanceKm >= 0) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                LocationHelper.formatDistance(offer.distanceKm),
                                fontSize = 11.sp,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Button(
                    onClick = { if (!isAcceptedByMe && !isAccepting) onAccept() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAcceptedByMe) Color(0xFF4CAF50) else PrimaryBlue
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    enabled = !isAcceptedByMe && !isAccepting
                ) {
                    if (isAccepting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            if (isAcceptedByMe) "Accepted" else "Accept Work",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkOfferSkeleton() {
    val shimmerAlpha by rememberInfiniteTransition().animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Box(Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray.copy(shimmerAlpha)))
            Spacer(Modifier.width(16.dp))
            Column {
                Box(Modifier
                    .width(150.dp)
                    .height(20.dp)
                    .background(Color.LightGray.copy(shimmerAlpha)))
                Spacer(Modifier.height(8.dp))
                Box(Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .background(Color.LightGray.copy(shimmerAlpha)))
                Spacer(Modifier.height(4.dp))
                Box(Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
                    .background(Color.LightGray.copy(shimmerAlpha)))
            }
        }
    }
}

@Composable
private fun WorkerEmptyState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Search, null, modifier = Modifier.size(64.dp), tint = SecondaryBlue)
        Spacer(Modifier.height(16.dp))
        Text(
            if (query.isEmpty()) "No work offers available right now." else "No matches found for '$query'",
            textAlign = TextAlign.Center,
            color = TextMuted
        )
    }
}

@Composable
private fun WorkerErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Warning, null, modifier = Modifier.size(48.dp), tint = Color.Red.copy(alpha = 0.6f))
        Spacer(Modifier.height(16.dp))
        Text("Oops! Something went wrong", fontWeight = FontWeight.Bold, color = TextDark)
        Text(message, color = TextMuted, textAlign = TextAlign.Center, fontSize = 12.sp)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
            Text("Retry")
        }
    }
}

@Composable
private fun WorkerBottomNav(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        Triple(Icons.Default.Home, Icons.Outlined.Home, "Home"),
        Triple(Icons.Filled.List, Icons.Outlined.List, "Jobs"),
        Triple(Icons.Filled.Email, Icons.Outlined.Email, "Chat"),
        Triple(Icons.Filled.Person, Icons.Outlined.Person, "Profile")
    )
    NavigationBar(containerColor = CardBg, tonalElevation = 8.dp) {
        items.forEachIndexed { index, (filled, outlined, label) ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                icon = {
                    Icon(
                        if (selectedIndex == index) filled else outlined,
                        contentDescription = label,
                        tint = if (selectedIndex == index) PrimaryBlue else TextMuted
                    )
                },
                label = {
                    Text(
                        label,
                        color = if (selectedIndex == index) PrimaryBlue else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(indicatorColor = SecondaryBlue)
            )
        }
    }
}
