package com.example.workman.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.workman.R
import com.example.workman.components.LanguageIconButton
import com.example.workman.components.LanguagePickerSheet
import com.example.workman.components.ReportDialog
import com.example.workman.dataClass.BookingStatus
import com.example.workman.dataClass.BookingUiModel
import com.example.workman.dataClass.WorkerUiModel
import com.example.workman.utils.LocationHelper
import com.example.workman.viewModels.FavoritesViewModel
import com.example.workman.viewModels.HomeBossDashboardViewModel
import com.example.workman.viewModels.WorkerListState
import java.text.SimpleDateFormat
import java.util.Locale

// ─── Color Palette ─────────────────────────────────────────────────────────────

private val Cream       = Color(0xFFFFF3E0)
private val CreamCard   = Color(0xFFFFFFFF)
private val Orange      = Color(0xFFFF9800)
private val OrangeLight = Color(0xFFFFE0B2)
private val TextDark    = Color(0xFF1A1A1A)
private val TextMuted   = Color(0xFF888888)
private val ChipBg      = Color(0xFFF5F5F5)

data class ServiceCategory(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val description: String = "Find experts for your needs",
    val startingRate: String = "₹199"
)

private val serviceCategories = listOf(
    ServiceCategory("Plumbing", Icons.Outlined.Build, Color(0xFF2196F3), "Leak repairs, pipe installs", "₹249"),
    ServiceCategory("Electrician", Icons.Outlined.Settings, Color(0xFFFFC107), "Wiring, appliance repair", "₹299"),
    ServiceCategory("Carpentry", Icons.Outlined.Home, Color(0xFF795548), "Furniture, woodwork", "₹349"),
    ServiceCategory("Cleaning", Icons.Outlined.CheckCircle, Color(0xFF4CAF50), "Deep cleaning, dusting", "₹199"),
    ServiceCategory("Painting", Icons.Outlined.Edit, Color(0xFFE91E63), "Interior & exterior painting", "₹499"),
    ServiceCategory("Masonry", Icons.Outlined.Place, Color(0xFF9E9E9E), "Brickwork, construction", "₹599"),
    ServiceCategory("Gardening", Icons.Outlined.Info, Color(0xFF8BC34A), "Lawn care, landscaping", "₹249"),
    ServiceCategory("Appliance", Icons.Outlined.Refresh, Color(0xFFFF5722), "AC, Fridge, TV repair", "₹399")
)

private val dashboardCategories: List<String>
    get() = com.example.workman.utils.CategoryRepository.getCategoriesForFilter()

// ─── Root Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeBossDashboardScreen(
    viewModel: HomeBossDashboardViewModel = viewModel(),
    onWorkerClick: (WorkerUiModel) -> Unit = {},
    onViewOffers: () -> Unit = {},
    onCreateWork: () -> Unit = {},
    /** Post a job with the date pre-filled (millis), launched from the calendar. */
    onCreateWorkOnDate: (Long) -> Unit = {},
    onNavProfile: () -> Unit = {},
    onNavChat: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    var selectedNavItem by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = Cream,
        bottomBar = {
            HomeBossBottomNav(
                selectedIndex = selectedNavItem,
                onSelect = { idx ->
                    when (idx) {
                        0 -> selectedNavItem = 0  // Home
                        1 -> selectedNavItem = 1  // Bookings (now the unified job tracker)
                        2 -> onNavChat()          // Chat
                        3 -> onNavProfile()       // Profile
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateWork,
                containerColor = Orange,
                contentColor = Color.White,
                icon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.post_a_job)
                    )
                },
                text = { Text(stringResource(R.string.post_a_job), fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Crossfade(
            targetState = selectedNavItem,
            modifier = Modifier.padding(innerPadding),
            label = "screen_fade"
        ) { page ->
            when (page) {
                0 -> HomeContent(viewModel, onWorkerClick, onNotificationClick)
                1 -> BookingContent(viewModel, onCreateWorkOnDate)
                else -> HomeContent(viewModel, onWorkerClick, onNotificationClick)
            }
        }

        // ── Rating Dialog
        val uiState by viewModel.uiState.collectAsState()
        if (uiState.showRatingDialog) {
            RatingDialog(
                booking = uiState.bookingToRate,
                onDismiss = viewModel::dismissRatingDialog,
                onSubmit = viewModel::submitRating
            )
        }
    }
}

@Composable
fun RatingDialog(
    booking: BookingUiModel?,
    onDismiss: () -> Unit,
    onSubmit: (Float, String) -> Unit
) {
    if (booking == null) return
    var rating by remember { mutableFloatStateOf(5f) }
    var review by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CreamCard,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                val context = LocalContext.current
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(booking.workerPhotoUrl.ifBlank {
                            "https://ui-avatars.com/api/?name=${
                                booking.workerName.replace(
                                    " ",
                                    "+"
                                )
                            }"
                        })
                        .crossfade(true)
                        .placeholder(R.drawable.ic_workman_logo)
                        .error(R.drawable.ic_workman_logo)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.rate_worker, booking.workerName),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    stringResource(R.string.rate_how_was_experience),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(5) { index ->
                        val starRating = index + 1
                        val isSelected = starRating <= rating
                        Icon(
                            imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = null,
                            tint = if (isSelected) Orange else TextMuted,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { rating = starRating.toFloat() }
                        )
                    }
                }
                
                OutlinedTextField(
                    value = review,
                    onValueChange = { review = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.write_review_optional),
                            color = TextMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Orange,
                        unfocusedBorderColor = OrangeLight
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(rating, review) },
                colors = ButtonDefaults.buttonColors(containerColor = Orange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.submit_review), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.skip), color = TextMuted)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    viewModel: HomeBossDashboardViewModel,
    onWorkerClick: (WorkerUiModel) -> Unit,
    onNotificationClick: () -> Unit = {},
    favoritesViewModel: FavoritesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val favState by favoritesViewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Worker currently being reported (null = dialog hidden). Reuses the shared
    // ReportDialog + the boss ViewModel's submitReport (Play-compliance flow).
    var reportWorker by remember { mutableStateOf<WorkerUiModel?>(null) }

    // Surface favorite add/remove feedback as a lightweight toast.
    LaunchedEffect(favState.message) {
        favState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            favoritesViewModel.clearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── Header Section with User Info & Location
            item {
                BossHeader(
                    name = uiState.userName,
                    photoUrl = uiState.userPhotoUrl,
                    location = uiState.userLocation,
                    onNotificationClick = onNotificationClick
                )
            }

            // ── Search bar with filter button
            item {
                DashboardSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onFilterClick = { viewModel.toggleFilterSheet() },
                    activeFilterCount = uiState.selectedCategories.size +
                            (if (uiState.searchRadiusKm != LocationHelper.DEFAULT_RADIUS_KM) 1 else 0),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                )
            }

            // ── Active filters summary (shown when filters are applied)
            if (uiState.selectedCategories.isNotEmpty() || uiState.searchRadiusKm != LocationHelper.DEFAULT_RADIUS_KM) {
                item {
                    ActiveFilterBar(
                        selectedCategories = uiState.selectedCategories,
                        radiusKm = uiState.searchRadiusKm,
                        defaultRadius = LocationHelper.DEFAULT_RADIUS_KM,
                        onClearAll = { viewModel.clearFilters() }
                    )
                }
            }

            // ── Section header with result count
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.workers),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    )
                    Text(
                        stringResource(R.string.workers_found, uiState.filteredWorkers.size),
                        fontSize = 12.sp,
                        color = Orange,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            // ── Content: Loading / Error / Empty / List
            when (val state = uiState.workerListState) {
                is WorkerListState.Loading -> {
                    item { WorkerListLoading() }
                }

                is WorkerListState.Error -> {
                    item {
                        WorkerListError(
                            message = state.message,
                            onRetry = viewModel::fetchWorkers
                        )
                    }
                }

                is WorkerListState.Success -> {
                    if (uiState.filteredWorkers.isEmpty()) {
                        item { WorkerListEmpty(query = uiState.searchQuery) }
                    } else {
                        items(uiState.filteredWorkers, key = { it.id }) { worker ->
                            WorkerCard(
                                worker = worker,
                                isFavorite = favState.favoriteIds.contains(worker.id),
                                onClick = { onWorkerClick(worker) },
                                onToggleFavorite = { favoritesViewModel.toggleFavorite(worker) },
                                onShare = { shareWorker(context, worker) },
                                onCall = { callWorker(context, worker) },
                                onReport = { reportWorker = worker },
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Filter Bottom Sheet overlay
        if (uiState.showFilterSheet) {
            FilterBottomSheet(
                selectedCategories = uiState.selectedCategories,
                selectedRadius = uiState.searchRadiusKm,
                onToggleCategory = viewModel::toggleCategoryFilter,
                onRadiusChange = viewModel::updateSearchRadius,
                onApply = viewModel::applyFilterSheet,
                onClearAll = viewModel::clearFilters,
                onDismiss = { viewModel.toggleFilterSheet() }
            )
        }

        // ── Report worker dialog (opened from a card's overflow menu)
        reportWorker?.let { w ->
            ReportDialog(
                entityId = w.id,
                entityType = "USER",
                onSubmit = { id, type, reason ->
                    viewModel.submitReport(id, type, reason)
                    reportWorker = null
                    Toast.makeText(
                        context,
                        context.getString(R.string.report_submitted_toast),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onDismiss = { reportWorker = null }
            )
        }
    }
}

// ─── Worker card action helpers ────────────────────────────────────────────────

/** Opens the system share sheet with a short text summary of the worker. */
private fun shareWorker(context: android.content.Context, worker: WorkerUiModel) {
    val text = context.getString(
        R.string.share_worker_text,
        worker.name,
        worker.category,
        String.format(Locale.getDefault(), "%.1f", worker.rating),
        worker.ratePerHour
    )
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            },
            null
        )
    )
}

/** Opens the dialer pre-filled with the worker's number, or toasts if absent. */
private fun callWorker(context: android.content.Context, worker: WorkerUiModel) {
    if (worker.phone.isBlank()) {
        Toast.makeText(
            context,
            context.getString(R.string.no_phone_available),
            Toast.LENGTH_SHORT
        ).show()
        return
    }
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${worker.phone}")))
}

// ─── Active Filter Bar ─────────────────────────────────────────────────────────

@Composable
private fun ActiveFilterBar(
    selectedCategories: Set<String>,
    radiusKm: Double,
    defaultRadius: Double,
    onClearAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (radiusKm != defaultRadius) {
                Surface(
                    color = OrangeLight,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "${radiusKm.toInt()} km",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = Orange,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            selectedCategories.take(3).forEach { cat ->
                Surface(
                    color = OrangeLight,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        cat,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        color = Orange,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (selectedCategories.size > 3) {
                Text("+${selectedCategories.size - 3}", fontSize = 11.sp, color = Orange)
            }
        }
        TextButton(onClick = onClearAll) {
            Text(stringResource(R.string.clear_all), fontSize = 12.sp, color = Orange)
        }
    }
}

// ─── Filter Bottom Sheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    selectedCategories: Set<String>,
    selectedRadius: Double,
    onToggleCategory: (String) -> Unit,
    onRadiusChange: (Double) -> Unit,
    onApply: () -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    // Semi-transparent background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onDismiss() }
    ) {
        // Sheet content
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(520.dp)
                .clickable(enabled = false) { /* consume clicks */ },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // ── Fixed top: Handle bar + Title + Distance
                Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp)) {
                    // Handle bar
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.LightGray)
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(16.dp))

                    // Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.filter_workers),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        TextButton(onClick = onClearAll) {
                            Text(stringResource(R.string.reset), color = Orange)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Distance Section
                    Text(
                        stringResource(R.string.distance),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = TextDark
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10.0, 25.0, 50.0, 100.0).forEach { radius ->
                            FilterChip(
                                selected = selectedRadius == radius,
                                onClick = { onRadiusChange(radius) },
                                label = { Text("${radius.toInt()} km", fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Orange,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Categories label
                    Text(
                        stringResource(R.string.categories),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = TextDark
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.categories_select_hint),
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // ── Scrollable categories section
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(scrollState)
                ) {
                    val allCategories =
                        com.example.workman.utils.CategoryRepository.getCategoriesForSelection()
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allCategories.forEach { category ->
                            val isSelected = selectedCategories.contains(category)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onToggleCategory(category) },
                                label = { Text(category, fontSize = 12.sp, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Orange,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // ── Fixed bottom: Apply Button (always visible)
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Button(
                        onClick = onApply,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Orange)
                    ) {
                        Text(
                            text = if (selectedCategories.isEmpty()) stringResource(R.string.apply_filters)
                            else stringResource(
                                R.string.show_results_selected,
                                selectedCategories.size
                            ),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServicesContent(
    viewModel: HomeBossDashboardViewModel,
    onCategoryClick: (String) -> Unit,
    onWorkerClick: (WorkerUiModel) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── Header
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                Text(
                    stringResource(R.string.find_services),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDark
                    )
                )
                Text(
                    stringResource(R.string.find_services_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }

        // ── Search by Service
        item {
            DashboardSearchBar(
                query = uiState.serviceSearchQuery,
                onQueryChange = viewModel::onServiceSearchQueryChange,
                placeholder = stringResource(R.string.search_service_hint),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Spacer(Modifier.height(16.dp))
        }

        // ── Popular Services (Horizontal Scroll)
        item {
            Text(
                stringResource(R.string.popular_services),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.popularServices) { worker ->
                    PopularServiceCard(worker, onWorkerClick)
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Categories Grid
        item {
            Text(
                stringResource(R.string.all_categories),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        item {
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                serviceCategories.filter {
                    it.name.contains(uiState.serviceSearchQuery, ignoreCase = true)
                }.forEach { category ->
                    CategoryGridItem(
                        category = category,
                        onClick = { onCategoryClick(category.name) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CategoryGridItem(
    category: ServiceCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(160.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CreamCard),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(category.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(category.icon, contentDescription = null, tint = category.color)
            }

            Column {
                Text(
                    category.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextDark
                )
                Text(
                    category.description,
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.starts_from, category.startingRate),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Orange
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PopularServiceCard(
    worker: WorkerUiModel,
    onClick: (WorkerUiModel) -> Unit
) {
    Card(
        onClick = { onClick(worker) },
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CreamCard),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column {
            val context = LocalContext.current
            val fallbackUrl = "https://ui-avatars.com/api/?name=${
                worker.name.replace(
                    " ",
                    "+"
                )
            }&background=FFB74D&color=fff&size=200"
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(worker.photoUrl.ifBlank { fallbackUrl })
                    .crossfade(true)
                    .placeholder(R.drawable.ic_workman_logo)
                    .error(R.drawable.ic_workman_logo)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
            Column(Modifier.padding(12.dp)) {
                Text(
                    worker.category,
                    fontSize = 10.sp,
                    color = Orange,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    worker.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Orange, modifier = Modifier.size(10.dp))
                    Text(" ${worker.rating}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun BossHeader(
    name: String,
    photoUrl: String,
    location: String,
    onNotificationClick: () -> Unit = {}
) {
    var showLanguageSheet by remember { mutableStateOf(false) }
    val unreadCount = rememberUnreadNotificationCount()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(listOf(Orange, Color(0xFFFFB74D))),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.greeting_hello, name),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(location, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Notification bell with unread badge. The boss previously had NO
                // way to reach the Notifications screen at all — job accepted /
                // completed alerts were unreachable.
                Box {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { onNotificationClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = stringResource(R.string.cd_notifications),
                            tint = Color.White
                        )
                    }
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(18.dp)
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

                // Language switcher — top-right of the header, the conventional home for
                // global app settings, so it's easy to find but never in the way of content.
                LanguageIconButton(onClick = { showLanguageSheet = true })
            }
        }
    }

    if (showLanguageSheet) {
        LanguagePickerSheet(
            onDismiss = { showLanguageSheet = false },
            accentColor = Orange
        )
    }
}

// ─── Shimmer Loading Skeletons ─────────────────────────────────────────────────

@Composable
private fun WorkerListLoading() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        repeat(4) {
            WorkerCardSkeleton()
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun WorkerCardSkeleton() {
    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue  = 0.3f,
        targetValue   = 0.7f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    Card(
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = CreamCard),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Orange.copy(alpha = shimmer))
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                repeat(3) { i ->
                    Spacer(Modifier.height(if (i == 0) 4.dp else 8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (i == 1) 0.6f else 0.85f)
                            .height(if (i == 1) 20.dp else 14.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.LightGray.copy(alpha = shimmer))
                    )
                }
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Orange.copy(alpha = shimmer))
                )
            }
        }
    }
}

// ─── Error State ───────────────────────────────────────────────────────────────

@Composable
private fun WorkerListError(message: String, onRetry: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = Orange, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.workers_load_failed),
            fontWeight = FontWeight.SemiBold,
            color = TextDark
        )
        Spacer(Modifier.height(4.dp))
        Text(message, fontSize = 12.sp, color = TextMuted)
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            colors  = ButtonDefaults.buttonColors(containerColor = Orange)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.retry))
        }
    }
}

// ─── Empty State ───────────────────────────────────────────────────────────────

@Composable
private fun WorkerListEmpty(query: String) {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = OrangeLight, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (query.isBlank()) stringResource(R.string.no_workers_available) else stringResource(
                R.string.no_results_for,
                query
            ),
            color = TextMuted
        )
    }
}

// ─── Search Bar ────────────────────────────────────────────────────────────────

@Composable
private fun DashboardSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.search_workers_hint),
    onFilterClick: (() -> Unit)? = null,
    activeFilterCount: Int = 0
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(CreamCard)
            .border(1.dp, OrangeLight, RoundedCornerShape(26.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value         = query,
            onValueChange = onQueryChange,
            modifier      = Modifier.weight(1f),
            singleLine    = true,
            textStyle     = LocalTextStyle.current.copy(color = TextDark, fontSize = 15.sp),
            decorationBox = { inner ->
                if (query.isEmpty()) Text(placeholder, color = TextMuted, fontSize = 15.sp)
                inner()
            }
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_clear),
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Orange)
                .clickable { onFilterClick?.invoke() }
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (activeFilterCount > 0) stringResource(
                        R.string.filter_count,
                        activeFilterCount
                    ) else stringResource(R.string.filter_label),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ─── Category Chips ────────────────────────────────────────────────────────────

@Composable
private fun CategoryChipRow(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { cat ->
            val isSelected = cat == selected
            val bgColor   by animateColorAsState(targetValue = if (isSelected) Orange else ChipBg, label = "chip_bg")
            val textColor by animateColorAsState(targetValue = if (isSelected) Color.White else TextDark, label = "chip_text")
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(bgColor)
                    .clickable { onSelect(cat) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    cat,
                    color      = textColor,
                    fontSize   = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─── Worker Card ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerCard(
    worker: WorkerUiModel,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onCall: () -> Unit,
    onReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        onClick   = onClick,
        modifier  = modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = CreamCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 6.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {

            // ── Photo with overlay badges
            Box(modifier = Modifier.size(100.dp)) {
                val context = LocalContext.current
                val fallbackUrl = "https://ui-avatars.com/api/?name=${
                    worker.name.replace(
                        " ",
                        "+"
                    )
                }&background=FFB74D&color=fff&size=200"
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(worker.photoUrl.ifBlank { fallbackUrl })
                        .crossfade(true)
                        .placeholder(R.drawable.ic_workman_logo)
                        .error(R.drawable.ic_workman_logo)
                        .build(),
                    contentDescription = worker.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                        .clickable { onShare() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = stringResource(R.string.cd_share),
                        tint = TextDark,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Orange, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        // Round the rating so we never render "4.0909090909".
                        val ratingText = String.format(Locale.getDefault(), "%.1f", worker.rating)
                        Text(
                            "$ratingText (${worker.reviewCount})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            // ── Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = Orange, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(worker.category, fontSize = 11.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    // ── Overflow menu: View profile / Call / Favorite / Report
                    Box {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.cd_options),
                            tint = TextMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { menuOpen = true }
                        )
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_view_profile)) },
                                onClick = { menuOpen = false; onClick() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.contact_call)) },
                                onClick = { menuOpen = false; onCall() }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            if (isFavorite) R.string.removed_from_favorites
                                            else R.string.cd_shortlist
                                        )
                                    )
                                },
                                onClick = { menuOpen = false; onToggleFavorite() }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.action_report),
                                        color = Color(0xFFE53935)
                                    )
                                },
                                onClick = { menuOpen = false; onReport() }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(worker.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.worker_experience_years, worker.yearsOfExperience),
                    fontSize = 12.sp,
                    color = TextMuted
                )
                // Distance badge
                if (worker.distanceKm >= 0) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            null,
                            tint = Orange,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            LocationHelper.formatDistance(worker.distanceKm),
                            fontSize = 11.sp,
                            color = Orange,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Orange)
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        // Guard against "₹0/hr" when no rate is set — show Negotiable.
                        Text(
                            if (worker.ratePerHour > 0)
                                stringResource(R.string.worker_rate_hourly_int, worker.ratePerHour)
                            else stringResource(R.string.budget_negotiable),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    // ── Shortlist / favorite toggle (backed by FavoritesViewModel)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isFavorite) Orange else OrangeLight)
                            .clickable { onToggleFavorite() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = stringResource(R.string.cd_shortlist),
                            tint = if (isFavorite) Color.White else Orange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Expandable FAB ────────────────────────────────────────────────────────────

@Composable
private fun HomeBossFab(
    expanded: Boolean,
    onToggle: () -> Unit,
    onViewOffers: () -> Unit,
    onCreateWork: () -> Unit
) {
    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = expanded,
            enter   = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit    = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Column(horizontalAlignment = Alignment.End) {
                FabSubItem(label = "Create Work",    icon = Icons.Default.Edit, onClick = onCreateWork)
                Spacer(Modifier.height(10.dp))
                FabSubItem(label = "View My Offers", icon = Icons.Default.List, onClick = onViewOffers)
                Spacer(Modifier.height(10.dp))
            }
        }
        FloatingActionButton(
            onClick        = onToggle,
            containerColor = Orange,
            contentColor   = Color.White,
            shape          = CircleShape
        ) {
            val rotation by animateFloatAsState(
                targetValue   = if (expanded) 45f else 0f,
                animationSpec = tween(250),
                label         = "fab_rotation"
            )
            Icon(Icons.Default.Add, contentDescription = "Toggle", modifier = Modifier.rotate(rotation))
        }
    }
}

@Composable
private fun FabSubItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(8.dp), color = Color.White, shadowElevation = 4.dp) {
            Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 13.sp)
        }
        Spacer(Modifier.width(8.dp))
        SmallFloatingActionButton(onClick = onClick, containerColor = Orange, contentColor = Color.White) {
            Icon(icon, contentDescription = label)
        }
    }
}

// ─── Bottom Navigation ─────────────────────────────────────────────────────────

@Composable
private fun BookingContent(
    viewModel: HomeBossDashboardViewModel,
    onCreateWorkOnDate: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var isCalendarView by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Tab Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(ChipBg, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tabs = listOf(
                stringResource(R.string.booking_tab_pending),
                stringResource(R.string.booking_tab_active),
                stringResource(R.string.booking_tab_history)
            )
            tabs.forEachIndexed { index, label ->
                val selected = uiState.selectedBookingTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) Orange else Color.Transparent)
                        .clickable { viewModel.onBookingTabSelected(index) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color.White else TextMuted,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // ── Sub-header with Calendar Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when(uiState.selectedBookingTab) {
                    0 -> stringResource(R.string.booking_header_pending)
                    1 -> stringResource(R.string.booking_header_active)
                    else -> stringResource(R.string.booking_header_history)
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            IconButton(onClick = { isCalendarView = !isCalendarView }) {
                Icon(
                    if (isCalendarView) Icons.Default.List else Icons.Outlined.DateRange,
                    contentDescription = stringResource(R.string.cd_toggle_view),
                    tint = Orange
                )
            }
        }

        if (isCalendarView) {
            // The calendar intentionally shows ALL bookings, not just the selected
            // tab's — the whole point is a single schedule-wide view.
            com.example.workman.components.BookingCalendarView(
                bookings = uiState.bookings,
                accentColor = Orange,
                accentLight = OrangeLight,
                textDark = TextDark,
                textMuted = TextMuted,
                cardColor = CreamCard,
                onBookingClick = { booking ->
                    // Jump to the tab that contains this booking so its actions
                    // (cancel / complete / rate) are reachable.
                    viewModel.onBookingTabSelected(
                        when (booking.status) {
                            BookingStatus.PENDING -> 0
                            BookingStatus.ACTIVE, BookingStatus.IN_PROGRESS -> 1
                            else -> 2
                        }
                    )
                    isCalendarView = false
                },
                onCreateWorkOnDate = onCreateWorkOnDate
            )
        } else {
            val filteredBookings = uiState.bookings.filter {
                when (uiState.selectedBookingTab) {
                    0 -> it.status == BookingStatus.PENDING
                    1 -> it.status == BookingStatus.ACTIVE || it.status == BookingStatus.IN_PROGRESS
                    else -> it.status == BookingStatus.COMPLETED || it.status == BookingStatus.CANCELLED
                }
            }

            if (filteredBookings.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.DateRange,
                            null,
                            modifier = Modifier.size(72.dp),
                            tint = OrangeLight
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = when (uiState.selectedBookingTab) {
                                0 -> stringResource(R.string.booking_empty_pending)
                                1 -> stringResource(R.string.booking_empty_active)
                                else -> stringResource(R.string.booking_empty_history)
                            },
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = when (uiState.selectedBookingTab) {
                                0 -> stringResource(R.string.booking_empty_pending_sub)
                                1 -> stringResource(R.string.booking_empty_active_sub)
                                else -> stringResource(R.string.booking_empty_history_sub)
                            },
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredBookings) { booking ->
                        BookingCard(
                            booking = booking,
                            onCancel = { viewModel.updateBookingStatus(booking.id, BookingStatus.CANCELLED) },
                            onComplete = {
                                viewModel.updateBookingStatus(
                                    booking.id,
                                    BookingStatus.COMPLETED
                                )
                            },
                            onRate = { viewModel.openRatingDialog(booking) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingCard(
    booking: BookingUiModel,
    onCancel: () -> Unit,
    onComplete: () -> Unit,
    onRate: () -> Unit = {}
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val hasWorker = booking.workerId.isNotBlank()
    // Completed jobs with a worker can be reviewed — but only once.
    val canRate = hasWorker &&
            booking.status == BookingStatus.COMPLETED &&
            !booking.ratingSubmitted

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (canRate) Modifier.clickable { onRate() } else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CreamCard),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasWorker) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(booking.workerPhotoUrl.ifBlank {
                                "https://ui-avatars.com/api/?name=${
                                    booking.workerName.replace(
                                        " ",
                                        "+"
                                    )
                                }"
                            })
                            .crossfade(true)
                            .placeholder(R.drawable.ic_workman_logo)
                            .error(R.drawable.ic_workman_logo)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Pending — no worker yet, show a job icon
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Orange.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Build, contentDescription = null, tint = Orange)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        booking.serviceName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1
                    )
                    Text(
                        text = if (hasWorker) booking.workerName else stringResource(R.string.booking_waiting_worker),
                        color = if (hasWorker) TextMuted else Orange,
                        fontSize = 13.sp
                    )
                }
                StatusBadge(booking.status)
            }

            Divider(Modifier.padding(vertical = 12.dp), color = ChipBg)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        stringResource(R.string.booking_agreed_rate),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    val rateText =
                        if (booking.agreedRate.any { it.isDigit() }) "₹${booking.agreedRate}" else booking.agreedRate
                    Text(
                        rateText,
                        fontWeight = FontWeight.ExtraBold,
                        color = Orange,
                        fontSize = 15.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.booking_scheduled_for),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Text(dateFormatter.format(booking.date), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
            }

            // Actions per status
            when (booking.status) {
                BookingStatus.PENDING -> {
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Text(stringResource(R.string.booking_cancel_post), fontSize = 13.sp)
                    }
                }

                BookingStatus.ACTIVE, BookingStatus.IN_PROGRESS -> {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                        ) {
                            Text(stringResource(R.string.action_cancel), fontSize = 12.sp)
                        }
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text(stringResource(R.string.mark_complete), fontSize = 12.sp)
                        }
                    }
                }

                else -> {
                    // COMPLETED / CANCELLED
                    if (booking.status == BookingStatus.COMPLETED && hasWorker) {
                        // ── Manual payment (Phase 1): Pay via UPI + Mark as Paid
                        Spacer(Modifier.height(16.dp))
                        com.example.workman.components.PaymentSection(
                            jobId = booking.jobId.ifBlank { booking.id },
                            isBoss = true
                        )

                        Spacer(Modifier.height(16.dp))
                        if (booking.ratingSubmitted) {
                            // Already reviewed — confirm it instead of re-prompting.
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    stringResource(
                                        R.string.booking_reviewed_worker,
                                        booking.workerName
                                    ),
                                    color = Color(0xFF4CAF50),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Button(
                                onClick = onRate,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Orange),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.rate_worker_button),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.booking_leave_feedback, booking.workerName),
                                color = TextMuted,
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: BookingStatus) {
    val (color, text) = when (status) {
        BookingStatus.PENDING -> Color(0xFFFFA000) to stringResource(R.string.status_pending)
        BookingStatus.ACTIVE -> Color(0xFF2196F3) to stringResource(R.string.status_active)
        BookingStatus.IN_PROGRESS -> Color(0xFF9C27B0) to stringResource(R.string.status_in_progress_badge)
        BookingStatus.COMPLETED -> Color(0xFF4CAF50) to stringResource(R.string.status_completed_badge)
        BookingStatus.CANCELLED -> Color(0xFFF44336) to stringResource(R.string.status_cancelled)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color))
            Spacer(Modifier.width(6.dp))
            Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Bottom Navigation ─────────────────────────────────────────────────────────

@Composable
private fun HomeBossBottomNav(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        Pair(Icons.Default.Home, R.string.nav_home),
        Pair(Icons.Outlined.DateRange, R.string.nav_bookings),
        Pair(Icons.Outlined.Email, R.string.nav_chat),
        Pair(Icons.Outlined.Person, R.string.nav_profile)
    )
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        items.forEachIndexed { index, (icon, labelRes) ->
            val label = stringResource(labelRes)
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick  = { onSelect(index) },
                icon     = { Icon(icon, contentDescription = label, tint = if (selectedIndex == index) Orange else TextMuted) },
                label    = { Text(label, fontSize = 11.sp, color = if (selectedIndex == index) Orange else TextMuted) },
                colors   = NavigationBarItemDefaults.colors(indicatorColor = OrangeLight)
            )
        }
    }
}
