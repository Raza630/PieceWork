package com.example.workman.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.workman.dataClass.BookingStatus
import com.example.workman.dataClass.BookingUiModel
import com.example.workman.dataClass.WorkerUiModel
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

private val dashboardCategories = listOf(
    "All",
    "Professionals",
    "Associate Professionals",
    "Clerks",
    "Service Workers",
    "Skilled Agricultural",
    "Craft & Trades",
    "Machine Operators",
    "Elementary"
)

// ─── Root Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeBossDashboardScreen(
    viewModel: HomeBossDashboardViewModel = viewModel(),
    onWorkerClick: (WorkerUiModel) -> Unit = {},
    onViewOffers: () -> Unit = {},
    onCreateWork: () -> Unit = {},
    onNavProfile: () -> Unit = {},
    onNavChat: () -> Unit = {}
) {
    var fabExpanded     by remember { mutableStateOf(false) }
    var selectedNavItem by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = Cream,
        bottomBar = {
            HomeBossBottomNav(
                selectedIndex = selectedNavItem,
                onSelect = { idx ->
                    if (idx < 3) {
                        selectedNavItem = idx
                    } else {
                        when (idx) {
                            3 -> onNavChat()
                            4 -> onNavProfile()
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            HomeBossFab(
                expanded     = fabExpanded,
                onToggle     = { fabExpanded = !fabExpanded },
                onViewOffers = { fabExpanded = false; onViewOffers() },
                onCreateWork = { fabExpanded = false; onCreateWork() }
            )
        }
    ) { innerPadding ->
        Crossfade(
            targetState = selectedNavItem,
            modifier = Modifier.padding(innerPadding),
            label = "screen_fade"
        ) { page ->
            when (page) {
                0 -> HomeContent(viewModel, onWorkerClick)
                1 -> ServicesContent(
                    viewModel = viewModel,
                    onCategoryClick = { category ->
                        viewModel.onCategorySelected(category)
                        selectedNavItem = 0 // Navigate back to home/list view
                    },
                    onWorkerClick = onWorkerClick
                )
                2 -> BookingContent(viewModel)
                else -> HomeContent(viewModel, onWorkerClick)
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
                AsyncImage(
                    model = booking.workerPhotoUrl.ifBlank { "https://ui-avatars.com/api/?name=${booking.workerName.replace(" ", "+")}" },
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Rate ${booking.workerName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    "How was your experience?",
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
                    placeholder = { Text("Write a review (optional)", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
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
                Text("Submit Review", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip", color = TextMuted)
            }
        }
    )
}

@Composable
private fun HomeContent(
    viewModel: HomeBossDashboardViewModel,
    onWorkerClick: (WorkerUiModel) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── Search bar
        item {
            DashboardSearchBar(
                query         = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                modifier      = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
            )
        }

        // ── Category chips
        item {
            CategoryChipRow(
                categories = dashboardCategories,
                selected   = uiState.selectedCategory,
                onSelect   = viewModel::onCategorySelected
            )
            Spacer(Modifier.height(16.dp))
        }

        // ── Section header
        item {
            Text(
                text     = "Workers",
                style    = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color      = TextDark
                ),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(8.dp))
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
                            worker   = worker,
                            onClick  = { onWorkerClick(worker) },
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 6.dp)
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
                    "Find Services",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDark
                    )
                )
                Text(
                    "Select a category to find experts",
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
                placeholder = "Search for plumbing, electrical...",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Spacer(Modifier.height(16.dp))
        }

        // ── Popular Services (Horizontal Scroll)
        item {
            Text(
                "Popular Services",
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
                "All Categories",
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
                    "Starts from ${category.startingRate}",
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
            AsyncImage(
                model = worker.photoUrl.ifBlank {
                    "https://ui-avatars.com/api/?name=${worker.name.replace(" ", "+")}&background=FFB74D&color=fff&size=200"
                },
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
        modifier            = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = Orange, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("Failed to load workers", fontWeight = FontWeight.SemiBold, color = TextDark)
        Spacer(Modifier.height(4.dp))
        Text(message, fontSize = 12.sp, color = TextMuted)
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            colors  = ButtonDefaults.buttonColors(containerColor = Orange)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Retry")
        }
    }
}

// ─── Empty State ───────────────────────────────────────────────────────────────

@Composable
private fun WorkerListEmpty(query: String) {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = OrangeLight, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            text  = if (query.isBlank()) "No workers available" else "No results for \"$query\"",
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
    placeholder: String = "Search workers or category..."
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
                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Orange)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Filter", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                AsyncImage(
                    model              = worker.photoUrl.ifBlank {
                        "https://ui-avatars.com/api/?name=${worker.name.replace(" ", "+")}&background=FFB74D&color=fff&size=200"
                    },
                    contentDescription = worker.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = TextDark, modifier = Modifier.size(14.dp))
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
                        Text("${worker.rating} (${worker.reviewCount})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextDark)
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
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text(worker.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.height(2.dp))
                Text("${worker.yearsOfExperience} years of experience", fontSize = 12.sp, color = TextMuted)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Orange)
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text("₹${worker.ratePerHour}/hrs", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(OrangeLight)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Shortlist", tint = Orange, modifier = Modifier.size(18.dp))
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
private fun BookingContent(viewModel: HomeBossDashboardViewModel) {
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
            val tabs = listOf("Pending", "Active", "History")
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
                    0 -> "Pending Requests"
                    1 -> "Ongoing Jobs"
                    else -> "Past Bookings"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            IconButton(onClick = { isCalendarView = !isCalendarView }) {
                Icon(
                    if (isCalendarView) Icons.Default.List else Icons.Outlined.DateRange,
                    contentDescription = "Toggle View",
                    tint = Orange
                )
            }
        }

        if (isCalendarView) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = OrangeLight
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Booking Calendar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        "View and manage your schedule in a monthly view",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 48.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { isCalendarView = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Orange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Back to List View")
                    }
                }
            }
        } else {
            val filteredBookings = uiState.bookings.filter {
                when (uiState.selectedBookingTab) {
                    0 -> it.status == BookingStatus.PENDING
                    1 -> it.status == BookingStatus.ACTIVE
                    else -> it.status == BookingStatus.COMPLETED || it.status == BookingStatus.CANCELLED
                }
            }

            if (filteredBookings.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.DateRange, null, modifier = Modifier.size(64.dp), tint = OrangeLight)
                        Text("No bookings found", color = TextMuted)
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
                            onComplete = { viewModel.updateBookingStatus(booking.id, BookingStatus.COMPLETED) }
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
    onComplete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CreamCard),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = booking.workerPhotoUrl.ifBlank { "https://ui-avatars.com/api/?name=${booking.workerName.replace(" ", "+")}" },
                    contentDescription = null,
                    modifier = Modifier.size(50.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(booking.workerName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(booking.serviceName, color = TextMuted, fontSize = 13.sp)
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
                    Text("Agreed Rate", color = TextMuted, fontSize = 11.sp)
                    Text("₹${booking.agreedRate}", fontWeight = FontWeight.ExtraBold, color = Orange, fontSize = 15.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Scheduled For", color = TextMuted, fontSize = 11.sp)
                    Text(dateFormatter.format(booking.date), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
            }

            if (booking.status == BookingStatus.PENDING || booking.status == BookingStatus.ACTIVE) {
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Cancel", fontSize = 12.sp)
                    }
                    if (booking.status == BookingStatus.ACTIVE) {
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Complete", fontSize = 12.sp)
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
        BookingStatus.PENDING -> Color(0xFFFFA000) to "Pending"
        BookingStatus.ACTIVE -> Color(0xFF2196F3) to "Active"
        BookingStatus.COMPLETED -> Color(0xFF4CAF50) to "Completed"
        BookingStatus.CANCELLED -> Color(0xFFF44336) to "Cancelled"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Bottom Navigation ─────────────────────────────────────────────────────────

@Composable
private fun HomeBossBottomNav(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        Pair(Icons.Default.Home,       "Home"),
        Pair(Icons.Outlined.Build,     "Services"),
        Pair(Icons.Outlined.DateRange, "Booking"),
        Pair(Icons.Outlined.Email,     "Chat"),
        Pair(Icons.Outlined.Person,    "Profile")
    )
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        items.forEachIndexed { index, (icon, label) ->
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
