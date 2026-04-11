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
import com.example.workman.dataClass.WorkerUiModel
import com.example.workman.viewModels.HomeBossDashboardViewModel
import com.example.workman.viewModels.WorkerListState

// ─── Color Palette ─────────────────────────────────────────────────────────────

private val Cream       = Color(0xFFFFF3E0)
private val CreamCard   = Color(0xFFFFFFFF)
private val Orange      = Color(0xFFFF9800)
private val OrangeLight = Color(0xFFFFE0B2)
private val TextDark    = Color(0xFF1A1A1A)
private val TextMuted   = Color(0xFF888888)
private val ChipBg      = Color(0xFFF5F5F5)

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
    val uiState by viewModel.uiState.collectAsState()
    var fabExpanded     by remember { mutableStateOf(false) }
    var selectedNavItem by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = Cream,
        bottomBar = {
            HomeBossBottomNav(
                selectedIndex = selectedNavItem,
                onSelect = { idx ->
                    selectedNavItem = idx
                    when (idx) {
                        3 -> onNavChat()
                        4 -> onNavProfile()
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
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
    modifier: Modifier = Modifier
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
                if (query.isEmpty()) Text("Search workers or category...", color = TextMuted, fontSize = 15.sp)
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
