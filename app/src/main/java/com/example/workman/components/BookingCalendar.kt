package com.example.workman.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workman.R
import com.example.workman.dataClass.BookingStatus
import com.example.workman.dataClass.BookingUiModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Month-grid calendar for the boss's bookings.
 *
 * Built on [java.util.Calendar] on purpose: the project targets minSdk 24 without
 * core-library desugaring, so `java.time` is unavailable, and this avoids pulling
 * in a third-party calendar dependency.
 *
 * Everything is derived from the existing [BookingUiModel] list — no new Firestore
 * query, no schema change.
 */

// ─── Status colors (mirrors StatusBadge in the boss dashboard) ─────────────────

internal fun statusColor(status: BookingStatus): Color = when (status) {
    BookingStatus.PENDING -> Color(0xFFFFA000)
    BookingStatus.ACTIVE -> Color(0xFF2196F3)
    BookingStatus.IN_PROGRESS -> Color(0xFF9C27B0)
    BookingStatus.COMPLETED -> Color(0xFF4CAF50)
    BookingStatus.CANCELLED -> Color(0xFFF44336)
}

/** A booking still awaiting action whose scheduled date has already passed. */
internal fun BookingUiModel.isOverdue(todayKey: Int): Boolean =
    (status == BookingStatus.PENDING ||
            status == BookingStatus.ACTIVE ||
            status == BookingStatus.IN_PROGRESS) && dayKey(date) < todayKey

/**
 * Collapses a [Date] to a comparable `yyyyMMdd` integer in the device timezone.
 * Cheaper and allocation-free compared to formatting/parsing strings per cell.
 */
internal fun dayKey(date: Date): Int {
    val cal = Calendar.getInstance()
    cal.time = date
    return cal.get(Calendar.YEAR) * 10000 +
            (cal.get(Calendar.MONTH) + 1) * 100 +
            cal.get(Calendar.DAY_OF_MONTH)
}

private fun todayKey(): Int = dayKey(Date())

/** Millis at 12:00 noon on the given day — safe against DST edge cases. */
private fun dayKeyToMillis(key: Int): Long {
    val cal = Calendar.getInstance()
    cal.set(key / 10000, (key / 100) % 100 - 1, key % 100, 12, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

// ─── Public entry point ────────────────────────────────────────────────────────

@Composable
fun BookingCalendarView(
    bookings: List<BookingUiModel>,
    accentColor: Color,
    accentLight: Color,
    textDark: Color,
    textMuted: Color,
    cardColor: Color,
    onBookingClick: (BookingUiModel) -> Unit,
    onCreateWorkOnDate: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { todayKey() }

    // Anchor month, expressed as a month offset from the current month so paging
    // never has to deal with year rollover manually.
    var monthOffset by remember { mutableIntStateOf(0) }
    var selectedDay by remember { mutableIntStateOf(today) }

    val bookingsByDay = remember(bookings) { bookings.groupBy { dayKey(it.date) } }

    // Month metadata for the currently displayed page.
    val monthCal = remember(monthOffset) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, monthOffset)
        }
    }
    val year = monthCal.get(Calendar.YEAR)
    val month = monthCal.get(Calendar.MONTH)
    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = monthCal.firstDayOfWeek
    // Number of blank cells before the 1st, respecting the locale's first weekday.
    val leadingBlanks = (monthCal.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7

    val monthLabel = remember(year, month) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthCal.time)
    }

    // Bookings that fall inside the displayed month.
    val monthStartKey = year * 10000 + (month + 1) * 100
    val monthEndKey = monthStartKey + 99
    val monthBookings = remember(bookings, monthStartKey) {
        bookings.filter { dayKey(it.date) in monthStartKey..monthEndKey }
    }
    val overdueCount = remember(monthBookings, today) {
        monthBookings.count { it.isOverdue(today) }
    }

    val selectedBookings = bookingsByDay[selectedDay].orEmpty()
        .sortedBy { it.date }

    Column(modifier = modifier.fillMaxSize()) {

        // ── Month navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { monthOffset-- }) {
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.calendar_prev_month),
                    tint = accentColor
                )
            }
            Text(
                text = monthLabel,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = textDark
            )
            IconButton(onClick = { monthOffset++ }) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.calendar_next_month),
                    tint = accentColor
                )
            }
        }

        // ── Summary strip: jobs this month + overdue warning + jump to today
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.calendar_jobs_this_month, monthBookings.size),
                fontSize = 12.sp,
                color = textMuted
            )
            if (overdueCount > 0) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = Color(0xFFF44336).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.calendar_overdue_count, overdueCount),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336)
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (monthOffset != 0 || selectedDay != today) {
                TextButton(onClick = {
                    monthOffset = 0
                    selectedDay = today
                }) {
                    Text(
                        stringResource(R.string.calendar_today),
                        fontSize = 12.sp,
                        color = accentColor
                    )
                }
            }
        }

        // ── Weekday header
        val weekdayLabels = remember(firstDayOfWeek) {
            val fmt = SimpleDateFormat("EEE", Locale.getDefault())
            val c = Calendar.getInstance()
            (0 until 7).map { i ->
                c.set(Calendar.DAY_OF_WEEK, ((firstDayOfWeek - 1 + i) % 7) + 1)
                fmt.format(c.time)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textMuted
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        // ── Month grid.
        // A plain Column/Row grid (not LazyVerticalGrid) so the whole calendar can
        // live inside the scrolling agenda below without nested-scroll conflicts.
        val totalCells = leadingBlanks + daysInMonth
        val rows = (totalCells + 6) / 7
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayOfMonth = cellIndex - leadingBlanks + 1
                        if (dayOfMonth in 1..daysInMonth) {
                            val key = monthStartKey + dayOfMonth
                            DayCell(
                                dayOfMonth = dayOfMonth,
                                dayBookings = bookingsByDay[key].orEmpty(),
                                isToday = key == today,
                                isSelected = key == selectedDay,
                                todayKey = today,
                                accentColor = accentColor,
                                accentLight = accentLight,
                                textDark = textDark,
                                textMuted = textMuted,
                                onClick = { selectedDay = key },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Agenda for the selected day
        val selectedLabel = remember(selectedDay) {
            SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
                .format(Date(dayKeyToMillis(selectedDay)))
        }
        Text(
            text = selectedLabel,
            modifier = Modifier.padding(horizontal = 20.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = textDark
        )
        Spacer(Modifier.height(6.dp))

        if (selectedBookings.isEmpty()) {
            // Empty day → turn dead space into a job-creation funnel.
            EmptyDayCta(
                accentColor = accentColor,
                accentLight = accentLight,
                textMuted = textMuted,
                isPast = selectedDay < today,
                onClick = { onCreateWorkOnDate(dayKeyToMillis(selectedDay)) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedBookings.size) { index ->
                    val booking = selectedBookings[index]
                    AgendaRow(
                        booking = booking,
                        isOverdue = booking.isOverdue(today),
                        textDark = textDark,
                        textMuted = textMuted,
                        cardColor = cardColor,
                        onClick = { onBookingClick(booking) }
                    )
                }
            }
        }
    }
}

// ─── Day cell ──────────────────────────────────────────────────────────────────

@Composable
private fun DayCell(
    dayOfMonth: Int,
    dayBookings: List<BookingUiModel>,
    isToday: Boolean,
    isSelected: Boolean,
    todayKey: Int,
    accentColor: Color,
    accentLight: Color,
    textDark: Color,
    textMuted: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasOverdue = dayBookings.any { it.isOverdue(todayKey) }
    // 3+ jobs in a single day is a realistic overload signal for a single boss.
    val isBusy = dayBookings.size >= 3

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isSelected -> accentColor
                    isBusy -> Color(0xFFF44336).copy(alpha = 0.10f)
                    isToday -> accentLight
                    else -> Color.Transparent
                }
            )
            .then(
                if (isToday && !isSelected) Modifier.border(
                    1.dp, accentColor, RoundedCornerShape(10.dp)
                ) else Modifier
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dayOfMonth.toString(),
                fontSize = 13.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> Color.White
                    hasOverdue -> Color(0xFFF44336)
                    else -> textDark
                }
            )
            if (dayBookings.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Cap at 3 dots so the cell never overflows; a "+" hints at more.
                    dayBookings.take(3).forEach { b ->
                        Box(
                            Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color.White else statusColor(b.status)
                                )
                        )
                    }
                    if (dayBookings.size > 3) {
                        Text(
                            "+",
                            fontSize = 7.sp,
                            color = if (isSelected) Color.White else textMuted
                        )
                    }
                }
            }
        }
    }
}

// ─── Agenda row ────────────────────────────────────────────────────────────────

@Composable
private fun AgendaRow(
    booking: BookingUiModel,
    isOverdue: Boolean,
    textDark: Color,
    textMuted: Color,
    cardColor: Color,
    onClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val color = statusColor(booking.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status stripe
            Box(
                Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    booking.serviceName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textDark,
                    maxLines = 1
                )
                Text(
                    text = booking.workerName.ifBlank {
                        stringResource(R.string.booking_waiting_worker)
                    },
                    fontSize = 12.sp,
                    color = textMuted,
                    maxLines = 1
                )
                if (isOverdue) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            stringResource(R.string.calendar_overdue_note),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFF44336)
                        )
                    }
                }
            }
            Text(
                timeFormat.format(booking.date),
                fontSize = 11.sp,
                color = textMuted
            )
        }
    }
}

// ─── Empty-day call to action ──────────────────────────────────────────────────

@Composable
private fun EmptyDayCta(
    accentColor: Color,
    accentLight: Color,
    textMuted: Color,
    isPast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.calendar_no_jobs_on_day),
            fontSize = 13.sp,
            color = textMuted
        )
        if (!isPast) {
            Spacer(Modifier.height(10.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = accentLight),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = accentColor)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.calendar_post_job_this_day),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }
    }
}

