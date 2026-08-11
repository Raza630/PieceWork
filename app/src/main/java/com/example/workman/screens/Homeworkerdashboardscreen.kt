package com.example.workman.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.workman.R
import com.example.workman.components.LanguageIconButton
import com.example.workman.components.LanguagePickerSheet
import com.example.workman.components.ReportDialog
import com.example.workman.components.UrgencyBadge
import com.example.workman.dataClass.Banner
import com.example.workman.dataClass.WorkOffer
import com.example.workman.dataClass.WorkerLevel
import com.example.workman.dataClass.displayPay
import com.example.workman.dataClass.isNew
import com.example.workman.dataClass.postedAgo
import com.example.workman.dataClass.travelEstimate
import com.example.workman.utils.JobMatchingEngine
import com.example.workman.utils.LocationHelper
import com.example.workman.viewModels.EarningsSummary
import com.example.workman.viewModels.HomeWorkerDashboardViewModel
import com.example.workman.viewModels.OfferSortOption
import com.example.workman.viewModels.WorkOfferListState
import com.example.workman.viewModels.WorkerDashboardUiState

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
    val context = LocalContext.current

    // Professional in-app feedback (animated success/error dialog) for key actions.
    var feedback by remember { mutableStateOf<com.example.workman.components.FeedbackData?>(null) }
    var pendingJobsNav by remember { mutableStateOf(false) }

    com.example.workman.components.FeedbackDialog(
        data = feedback,
        onDismiss = { feedback = null },
        onConfirm = {
            if (pendingJobsNav) {
                pendingJobsNav = false
                onNavJobs()
            }
        },
        secondaryLabel = if (pendingJobsNav) "Stay here" else null,
        onSecondary = { pendingJobsNav = false }
    )

    HomeWorkerDashboardContent(
        uiState = uiState,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        updateSearchRadius = viewModel::updateSearchRadius,
        updateSortOption = viewModel::updateSortOption,
        updateCategoryFilter = viewModel::updateCategoryFilter,
        toggleSaveOffer = viewModel::toggleSaveOffer,
        onCreateAlert = {
            viewModel.createJobAlert(
                uiState.selectedCategory,
                uiState.searchRadiusKm
            ) { success, msg ->
                feedback = com.example.workman.components.FeedbackData(
                    type = if (success) com.example.workman.components.FeedbackType.SUCCESS
                    else com.example.workman.components.FeedbackType.ERROR,
                    title = if (success) "Alert created" else "Couldn't create alert",
                    message = msg,
                    confirmLabel = "Got it"
                )
            }
        },
        fetchWorkOffers = viewModel::fetchWorkOffers,
        acceptWork = { offer ->
            viewModel.acceptWork(offer) { success, msg ->
                if (success) {
                    pendingJobsNav = true
                    feedback = com.example.workman.components.FeedbackData(
                        type = com.example.workman.components.FeedbackType.SUCCESS,
                        title = "You got the job! 🎉",
                        message = "\"${offer.title}\" is now assigned to you. " +
                                "Head to My Jobs to start working and track your progress.",
                        confirmLabel = "Go to My Jobs"
                    )
                } else {
                    feedback = com.example.workman.components.FeedbackData(
                        type = com.example.workman.components.FeedbackType.ERROR,
                        title = "Couldn't accept job",
                        message = msg.ifBlank { "Something went wrong. Please try again." },
                        confirmLabel = "Try again"
                    )
                }
            }
        },
        submitReport = { entityId, type, reason ->
            viewModel.submitReport(entityId, type, reason)
            feedback = com.example.workman.components.FeedbackData(
                type = com.example.workman.components.FeedbackType.SUCCESS,
                title = "Report submitted",
                message = "Thanks for helping keep WorkMan safe. Our team will review this shortly.",
                confirmLabel = "Done"
            )
        },
        onEnableLocation = { viewModel.updateLocation(context) },
        onOfferClick = onOfferClick,
        onNotificationClick = onNotificationClick,
        onNavProfile = onNavProfile,
        onNavChat = onNavChat,
        onNavHome = onNavHome,
        onNavJobs = onNavJobs
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeWorkerDashboardContent(
    uiState: WorkerDashboardUiState,
    onSearchQueryChange: (String) -> Unit = {},
    updateSearchRadius: (Double) -> Unit = {},
    updateSortOption: (OfferSortOption) -> Unit = {},
    updateCategoryFilter: (String) -> Unit = {},
    toggleSaveOffer: (String) -> Unit = {},
    onCreateAlert: () -> Unit = {},
    fetchWorkOffers: () -> Unit = {},
    acceptWork: (WorkOffer) -> Unit = {},
    submitReport: (String, String, String) -> Unit = { _, _, _ -> },
    onEnableLocation: () -> Unit = {},
    onOfferClick: (WorkOffer) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onNavProfile: () -> Unit = {},
    onNavChat: () -> Unit = {},
    onNavHome: () -> Unit = {},
    onNavJobs: () -> Unit = {}
) {
    var selectedNavItem by remember { mutableStateOf(0) }
    var reportOffer by remember { mutableStateOf<WorkOffer?>(null) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Number of non-default filters currently applied (for the badge on the Filters button)
    val activeFilterCount = listOf(
        uiState.selectedCategory != "All",
        uiState.sortOption != OfferSortOption.BEST_MATCH
    ).count { it }


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
                        photoUrl = uiState.userPhotoUrl,
                        location = uiState.userLocation,
                        onNotificationClick = onNotificationClick
                    )
                }

                // ── Earnings & Motivation
                item {
                    EarningsMotivationCard(
                        earnings = uiState.earnings,
                        modifier = Modifier.padding(
                            start = 20.dp, end = 20.dp, top = 16.dp
                        )
                    )
                }

                // ── Your Active Jobs
                if (uiState.activeJobs.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = stringResource(R.string.active_jobs_title),
                            subtitle = stringResource(
                                R.string.active_jobs_subtitle,
                                uiState.activeJobs.size
                            ),
                            icon = Icons.Default.CheckCircle,
                            iconTint = MatchGreen
                        )
                    }
                    item {
                        ActiveJobsRow(
                            jobs = uiState.activeJobs,
                            onJobClick = onOfferClick
                        )
                    }
                }

                // ── Search Bar + Filters button
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WorkerSearchBar(
                            query = uiState.searchQuery,
                            onQueryChange = onSearchQueryChange,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(12.dp))
                        FilterButton(
                            activeCount = activeFilterCount,
                            onClick = { showFilterSheet = true }
                        )
                    }
                }

                // ── Results summary line
                item {
                    ResultsSummary(
                        count = uiState.nearbyOfferCount,
                        radiusKm = uiState.searchRadiusKm,
                        isLocationAvailable = uiState.isLocationAvailable,
                        selectedCategory = uiState.selectedCategory,
                        sortOption = uiState.sortOption,
                        onEnableLocation = onEnableLocation,
                        onOpenFilters = { showFilterSheet = true }
                    )
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
                                onRetry = fetchWorkOffers
                            )
                        }
                    }
                    is WorkOfferListState.Success -> {
                        // ── "Recommended for You" section
                        if (uiState.recommendedOffers.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = stringResource(R.string.recommended_title),
                                    subtitle = stringResource(
                                        R.string.recommended_subtitle,
                                        uiState.recommendedOffers.size
                                    ),
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
                                    isSaved = uiState.savedOfferIds.contains(scoredOffer.offer.id),
                                    onToggleSave = { toggleSaveOffer(scoredOffer.offer.id) },
                                    onAccept = { acceptWork(scoredOffer.offer) },
                                    onClick = { onOfferClick(scoredOffer.offer) },
                                    onLongPress = { reportOffer = scoredOffer.offer },
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                            }
                        }

                        // ── "Other Available Work" section
                        if (uiState.otherOffers.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = if (uiState.recommendedOffers.isNotEmpty()) stringResource(
                                        R.string.other_available_work
                                    ) else stringResource(R.string.available_work),
                                    subtitle = stringResource(
                                        R.string.jobs_within_km,
                                        uiState.nearbyOfferCount,
                                        uiState.searchRadiusKm.toInt()
                                    ),
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
                                    isSaved = uiState.savedOfferIds.contains(scoredOffer.offer.id),
                                    onToggleSave = { toggleSaveOffer(scoredOffer.offer.id) },
                                    onAccept = { acceptWork(scoredOffer.offer) },
                                    onClick = { onOfferClick(scoredOffer.offer) },
                                    onLongPress = { reportOffer = scoredOffer.offer },
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                            }
                        }

                        // ── Empty state
                        if (uiState.recommendedOffers.isEmpty() && uiState.otherOffers.isEmpty()) {
                            item {
                                WorkerEmptyState(
                                    query = uiState.searchQuery,
                                    hasSkills = uiState.workerCategory.isNotBlank(),
                                    suggestedRadius = uiState.suggestedRadius,
                                    suggestedCount = uiState.suggestedRadiusCount,
                                    onExpandRadius = { r -> updateSearchRadius(r) },
                                    onCompleteProfile = onNavProfile
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Report job dialog (triggered by long-press on a card)
    reportOffer?.let { offer ->
        ReportDialog(
            entityId = offer.id,
            entityType = "JOB",
            onSubmit = { id, type, reason ->
                submitReport(id, type, reason)
                reportOffer = null
            },
            onDismiss = { reportOffer = null }
        )
    }

    // Filter bottom sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = CardBg
        ) {
            FilterSheetContent(
                selectedSort = uiState.sortOption,
                onSortSelected = updateSortOption,
                radiusKm = uiState.searchRadiusKm,
                onRadiusSelected = updateSearchRadius,
                categories = uiState.availableCategories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = updateCategoryFilter,
                isLocationAvailable = uiState.isLocationAvailable,
                onEnableLocation = onEnableLocation,
                onCreateAlert = onCreateAlert,
                resultCount = uiState.nearbyOfferCount,
                onReset = {
                    updateSortOption(OfferSortOption.BEST_MATCH)
                    updateCategoryFilter("All")
                },
                onClose = { showFilterSheet = false }
            )
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
                text = stringResource(R.string.match_percent, score),
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

// ─── Match Breakdown ("Why recommended") ─────────────────────────────────────────

@Composable
private fun MatchBreakdown(scoredOffer: JobMatchingEngine.ScoredOffer) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BgColor)
            .padding(12.dp)
    ) {
        Text(
            stringResource(R.string.why_recommend_title),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(Modifier.height(8.dp))
        BreakdownRow(stringResource(R.string.breakdown_skill_match), scoredOffer.categoryScore)
        BreakdownRow(stringResource(R.string.breakdown_distance), scoredOffer.distanceScore)
        BreakdownRow(stringResource(R.string.breakdown_history), scoredOffer.historyScore)
        BreakdownRow(stringResource(R.string.breakdown_freshness), scoredOffer.recencyScore)
    }
}

@Composable
private fun BreakdownRow(label: String, score: Double) {
    val pct = (score * 100).toInt().coerceIn(0, 100)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = TextMuted, modifier = Modifier.width(90.dp))
        LinearProgressIndicator(
            progress = pct / 100f,
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (pct >= 70) MatchGreen else PrimaryBlue,
            trackColor = Color.White
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "$pct%",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextDark,
            modifier = Modifier.width(34.dp)
        )
    }
}

// ─── Earnings & Motivation ────────────────────────────────────────────────────

@Composable
private fun EarningsMotivationCard(
    earnings: EarningsSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.earnings_this_week),
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${earnings.currency} ${earnings.weeklyEarnings.toLong()}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = MatchGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.earnings_done, earnings.completedThisWeek),
                            fontSize = 12.sp,
                            color = TextDark,
                            fontWeight = FontWeight.Medium
                        )
                        if (earnings.pendingPayout > 0) {
                            Spacer(Modifier.width(12.dp))
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MatchGold)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.earnings_pending, earnings.pendingPayout),
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                // Weekly goal ring (only when a goal is set)
                if (earnings.weeklyGoal > 0) {
                    WeeklyGoalRing(
                        current = earnings.weeklyEarnings,
                        goal = earnings.weeklyGoal
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            LevelProgressBar(completedJobs = earnings.completedTotal)
        }
    }
}

@Composable
private fun WeeklyGoalRing(current: Double, goal: Double) {
    val fraction = if (goal > 0) (current / goal).coerceIn(0.0, 1.0).toFloat() else 0f
    Box(
        modifier = Modifier.size(76.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            // Track
            drawArc(
                color = SecondaryBlue,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            // Progress
            drawArc(
                color = PrimaryBlue,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            "${(fraction * 100).toInt()}%",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue
        )
    }
}

@Composable
private fun LevelProgressBar(completedJobs: Int) {
    val currentLevel = WorkerLevel.fromJobCount(completedJobs)
    val nextInfo = WorkerLevel.jobsToNextLevel(completedJobs)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            WorkerLevel.getDisplayName(currentLevel),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(Modifier.weight(1f))
        Text(
            if (nextInfo != null)
                stringResource(
                    R.string.level_more_to_next,
                    nextInfo.second,
                    WorkerLevel.getDisplayName(nextInfo.first)
                )
            else stringResource(R.string.level_max_reached),
            fontSize = 11.sp,
            color = TextMuted
        )
    }
    Spacer(Modifier.height(6.dp))

    // Progress within the current tier
    val (tierStart, tierEnd) = when (currentLevel) {
        WorkerLevel.BRONZE -> 0 to 6
        WorkerLevel.SILVER -> 6 to 21
        WorkerLevel.GOLD -> 21 to 50
        else -> 50 to 50
    }
    val progress = if (tierEnd > tierStart)
        ((completedJobs - tierStart).toFloat() / (tierEnd - tierStart)).coerceIn(0f, 1f)
    else 1f

    LinearProgressIndicator(
        progress = progress,
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = PrimaryBlue,
        trackColor = SecondaryBlue
    )
}

// ─── Filters (button, summary, bottom sheet) ──────────────────────────────────

@Composable
private fun FilterButton(activeCount: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PrimaryBlue)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.List,
            contentDescription = stringResource(R.string.cd_filters),
            tint = Color.White
        )
        if (activeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MatchGold),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    activeCount.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ResultsSummary(
    count: Int,
    radiusKm: Double,
    isLocationAvailable: Boolean,
    selectedCategory: String,
    sortOption: OfferSortOption,
    onEnableLocation: () -> Unit,
    onOpenFilters: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        if (!isLocationAvailable) {
            Button(
                onClick = onEnableLocation,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.enable_location),
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (isLocationAvailable) stringResource(
                    R.string.jobs_within_km,
                    count,
                    radiusKm.toInt()
                ) else stringResource(R.string.jobs_count, count),
                fontSize = 12.sp,
                color = PrimaryBlue,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = sortOption.label,
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.clickable { onOpenFilters() }
            )
        }

        // Active category chip (quick removal via opening filters)
        if (selectedCategory != "All") {
            Spacer(Modifier.height(8.dp))
            Surface(
                color = SecondaryBlue,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.clickable { onOpenFilters() }
            ) {
                Text(
                    stringResource(R.string.category_prefix, selectedCategory),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheetContent(
    selectedSort: OfferSortOption,
    onSortSelected: (OfferSortOption) -> Unit,
    radiusKm: Double,
    onRadiusSelected: (Double) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    isLocationAvailable: Boolean,
    onEnableLocation: () -> Unit,
    onCreateAlert: () -> Unit,
    resultCount: Int,
    onReset: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.cd_filters),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextDark
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onReset) {
                Text(stringResource(R.string.reset), color = PrimaryBlue)
            }
        }

        // Distance
        FilterSectionLabel(stringResource(R.string.filter_distance))
        if (!isLocationAvailable) {
            Text(
                stringResource(R.string.filter_location_hint),
                fontSize = 12.sp,
                color = TextMuted
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onEnableLocation,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.enable_location))
            }
        } else {
            FilterChipsRow(
                options = listOf(10.0, 25.0, 50.0, 100.0),
                isSelected = { it == radiusKm },
                label = { "${it.toInt()} km" },
                onSelect = onRadiusSelected
            )
        }

        // Sort
        FilterSectionLabel(stringResource(R.string.filter_sort_by))
        FilterChipsRow(
            options = OfferSortOption.entries.toList(),
            isSelected = { it == selectedSort },
            label = { it.label },
            onSelect = onSortSelected
        )

        // Category
        if (categories.size > 1) {
            FilterSectionLabel(stringResource(R.string.filter_category))
            FilterChipsRow(
                options = categories,
                isSelected = { it == selectedCategory },
                label = { it },
                onSelect = onCategorySelected
            )
        }

        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = onCreateAlert,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.Default.Notifications,
                null,
                tint = PrimaryBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.create_job_alert), color = PrimaryBlue)
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Text(
                stringResource(R.string.show_results, resultCount),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun FilterSectionLabel(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = TextDark
    )
    Spacer(Modifier.height(10.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> FilterChipsRow(
    options: List<T>,
    isSelected: (T) -> Boolean,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { option ->
            FilterChip(
                selected = isSelected(option),
                onClick = { onSelect(option) },
                label = { Text(label(option), fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryBlue,
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

// ─── Components ────────────────────────────────────────────────────────────────

@Composable
private fun NewBadge() {
    Surface(color = MatchGreen, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = stringResource(R.string.badge_new),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun BossPreview(name: String, photo: String, rating: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val placeholder =
            "https://ui-avatars.com/api/?name=${
                name.replace(
                    " ",
                    "+"
                )
            }&background=E8E9FF&color=5B61F4"
        AsyncImage(
            model = photo.ifBlank { placeholder },
            contentDescription = stringResource(R.string.cd_poster),
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (rating > 0.0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        null,
                        tint = MatchGold,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        String.format("%.1f", rating),
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            } else {
                Text(stringResource(R.string.poster_role), fontSize = 10.sp, color = TextMuted)
            }
        }
    }
}

@Composable
private fun ActiveJobsRow(
    jobs: List<WorkOffer>,
    onJobClick: (WorkOffer) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(jobs, key = { "active_${it.id}" }) { job ->
            Card(
                modifier = Modifier.width(260.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MatchGreen.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                stringResource(R.string.status_in_progress),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MatchGreen
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            job.displayPay(),
                            fontSize = 12.sp,
                            color = MatchGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        job.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (job.bossName.isNotBlank()) {
                        Text(
                            stringResource(R.string.for_boss, job.bossName),
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { onJobClick(job) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MatchGreen),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.mark_complete),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerHeader(
    name: String,
    photoUrl: String,
    location: String,
    onNotificationClick: () -> Unit
) {
    var showLanguageSheet by remember { mutableStateOf(false) }
    val unreadCount = rememberUnreadNotificationCount()

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
                    if (photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = stringResource(R.string.cd_profile_photo),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        stringResource(R.string.greeting_hello, name),
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

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Notification icon with badge — positioned at the top for maximum visibility.
                Box {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable { onNotificationClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = stringResource(R.string.cd_notifications),
                            tint = Color.White
                        )
                    }

                    // Unread badge — only shown when there's actually something to
                    // read. It previously rendered unconditionally, so the user always
                    // saw a "you have notifications" dot even with an empty inbox.
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(18.dp)
                                .border(2.dp, GradientStart, CircleShape)
                                .padding(1.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Language switcher — placed below notifications to reduce horizontal clutter.
                LanguageIconButton(onClick = { showLanguageSheet = true })
            }
        }
    }

    if (showLanguageSheet) {
        LanguagePickerSheet(
            onDismiss = { showLanguageSheet = false },
            accentColor = PrimaryBlue
        )
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
                    if (query.isEmpty()) Text(
                        stringResource(R.string.worker_search_hint),
                        color = TextMuted
                    )
                    inner()
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun WorkOfferCard(
    offer: WorkOffer,
    scoredOffer: JobMatchingEngine.ScoredOffer? = null,
    isAccepting: Boolean,
    isSaved: Boolean = false,
    onToggleSave: () -> Unit = {},
    onAccept: () -> Unit,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isAcceptedByMe = offer.acceptedBy != null
    val isCompleted = offer.status == "COMPLETED" || offer.status == "REVIEWED"
    var showBreakdown by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Match badge row at the top (tappable to reveal breakdown)
            if (scoredOffer != null && scoredOffer.matchScore >= JobMatchingEngine.RECOMMENDED_THRESHOLD) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBreakdown = !showBreakdown }
                        .padding(bottom = 8.dp)
                ) {
                    MatchBadge(scoredOffer)
                    Spacer(Modifier.weight(1f))
                    Icon(
                        if (showBreakdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.cd_why_recommended),
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                AnimatedVisibility(visible = showBreakdown) {
                    MatchBreakdown(scoredOffer)
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
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            offer.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isSaved) stringResource(R.string.cd_remove_saved) else stringResource(
                                R.string.cd_save_job
                            ),
                            tint = if (isSaved) Color(0xFFE53935) else TextMuted,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { onToggleSave() }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        offer.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Category chip + New badge
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (offer.isNew()) {
                            NewBadge()
                        }
                        if (offer.category.isNotBlank()) {
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
                        }
                        if (offer.urgency == "URGENT") {
                            UrgencyBadge(urgency = offer.urgency)
                        }
                    }
                    // Scarcity signal: "Posted X ago"
                    val postedAgo = offer.postedAgo()
                    if (postedAgo.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(postedAgo, fontSize = 10.sp, color = TextMuted)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Divider(color = BgColor)
            Spacer(Modifier.height(12.dp))

            // Boss preview
            if (offer.bossName.isNotBlank()) {
                BossPreview(
                    name = offer.bossName,
                    photo = offer.bossPhoto,
                    rating = offer.bossRating
                )
                Spacer(Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    // Pay / budget
                    Text(
                        offer.displayPay(),
                        fontSize = 15.sp,
                        color = MatchGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
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
                            val travel = offer.travelEstimate()
                            if (travel != null) {
                                Text(
                                    " · $travel",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { if (!isAcceptedByMe && !isAccepting && !isCompleted) onAccept() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isCompleted -> Color(0xFF4CAF50)
                            isAcceptedByMe -> Color(0xFF4CAF50)
                            else -> PrimaryBlue
                        },
                        disabledContainerColor = when {
                            isCompleted -> Color(0xFF4CAF50)
                            isAcceptedByMe -> Color(0xFF4CAF50)
                            else -> Color.Gray.copy(alpha = 0.5f)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    enabled = !isAcceptedByMe && !isAccepting && !isCompleted
                ) {
                    if (isAccepting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            when {
                                isCompleted -> stringResource(R.string.status_completed)
                                isAcceptedByMe -> stringResource(R.string.status_accepted)
                                else -> stringResource(R.string.accept_work)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
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
private fun WorkerEmptyState(
    query: String,
    hasSkills: Boolean = true,
    suggestedRadius: Double? = null,
    suggestedCount: Int = 0,
    onExpandRadius: (Double) -> Unit = {},
    onCompleteProfile: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Search, null, modifier = Modifier.size(64.dp), tint = SecondaryBlue)
        Spacer(Modifier.height(16.dp))
        Text(
            if (query.isEmpty()) stringResource(R.string.empty_no_offers) else stringResource(
                R.string.empty_no_matches,
                query
            ),
            textAlign = TextAlign.Center,
            color = TextMuted
        )

        // Expand-radius CTA
        if (suggestedRadius != null && suggestedCount > 0) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onExpandRadius(suggestedRadius) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(
                        R.string.empty_try_radius,
                        suggestedRadius.toInt(),
                        suggestedCount
                    ),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Profile-completeness nudge
        if (!hasSkills) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCompleteProfile() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SecondaryBlue)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, null, tint = PrimaryBlue)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.empty_add_skills_title),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            stringResource(R.string.empty_add_skills_subtitle),
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = PrimaryBlue)
                }
            }
        }
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
        Text(
            stringResource(R.string.error_generic_title),
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Text(message, color = TextMuted, textAlign = TextAlign.Center, fontSize = 12.sp)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun WorkerBottomNav(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        Triple(Icons.Default.Home, Icons.Outlined.Home, R.string.nav_home),
        Triple(Icons.Filled.List, Icons.Outlined.List, R.string.nav_jobs),
        Triple(Icons.Filled.Email, Icons.Outlined.Email, R.string.nav_chat),
        Triple(Icons.Filled.Person, Icons.Outlined.Person, R.string.nav_profile)
    )
    NavigationBar(containerColor = CardBg, tonalElevation = 8.dp) {
        items.forEachIndexed { index, (filled, outlined, labelRes) ->
            val label = stringResource(labelRes)
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

@Preview(showBackground = true)
@Composable
fun HomeWorkerDashboardPreview() {
    val mockBanners = listOf(
        Banner("Plumbing Special", "Get more jobs in plumbing", "50-100/hr", "", "Plumbing"),
        Banner("Electrician Pro", "High demand for electricians", "80-150/hr", "", "Electrical")
    )

    val mockOffer1 = WorkOffer(
        id = "1",
        title = "Fix Kitchen Sink",
        description = "Need a plumber to fix a leaking sink in the kitchen.",
        category = "Plumbing",
        date = "Today, 2 PM",
        locationName = "Central Park, NY",
        distanceKm = 1.2,
        urgency = "URGENT",
        budgetAmount = 1500.0,
        budgetType = "FIXED",
        bossName = "Ali Raza",
        bossRating = 4.7,
        createdAtMillis = System.currentTimeMillis() - 10 * 60 * 1000L
    )

    val mockOffer2 = WorkOffer(
        id = "2",
        title = "Garden Maintenance",
        description = "Looking for someone to mow the lawn and trim hedges.",
        category = "Gardening",
        date = "Tomorrow, 10 AM",
        locationName = "Brooklyn, NY",
        distanceKm = 5.5,
        urgency = "THIS_WEEK",
        budgetAmount = 800.0,
        budgetType = "HOURLY",
        bossName = "Sara Khan",
        bossRating = 4.2,
        createdAtMillis = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L
    )

    val activeJob = WorkOffer(
        id = "3",
        title = "Paint Living Room",
        category = "Painter",
        status = "ASSIGNED",
        acceptedBy = "me",
        budgetAmount = 5000.0,
        budgetType = "FIXED",
        bossName = "Bilal Ahmed"
    )

    val scoredOffer1 = JobMatchingEngine.ScoredOffer(
        offer = mockOffer1,
        matchScore = 95,
        categoryScore = 1.0,
        distanceScore = 0.9,
        historyScore = 0.8,
        recencyScore = 1.0,
        matchReason = "Perfect skill match"
    )

    val scoredOffer2 = JobMatchingEngine.ScoredOffer(
        offer = mockOffer2,
        matchScore = 60,
        categoryScore = 0.5,
        distanceScore = 0.7,
        historyScore = 0.4,
        recencyScore = 0.8,
        matchReason = "Near you"
    )

    val uiState = WorkerDashboardUiState(
        userName = "John Doe",
        userLocation = "New York, NY",
        banners = mockBanners,
        offerListState = WorkOfferListState.Success(emptyList()),
        recommendedOffers = listOf(scoredOffer1),
        otherOffers = listOf(scoredOffer2),
        nearbyOfferCount = 2,
        isLocationAvailable = true,
        availableCategories = listOf("All", "Plumbing", "Gardening"),
        selectedCategory = "All",
        activeJobs = listOf(activeJob),
        earnings = EarningsSummary(
            weeklyEarnings = 4200.0,
            totalEarnings = 18000.0,
            completedThisWeek = 3,
            completedTotal = 12,
            pendingPayout = 2,
            weeklyGoal = 6000.0,
            workerLevel = "SILVER",
            currency = "Rs"
        )
    )

    HomeWorkerDashboardContent(uiState = uiState)
}
