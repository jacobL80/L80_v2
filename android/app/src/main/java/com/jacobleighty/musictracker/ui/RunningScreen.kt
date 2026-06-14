package com.jacobleighty.musictracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jacobleighty.musictracker.data.RunningDayEntry
import com.jacobleighty.musictracker.data.RunningWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val RPageBg      = Color(0xFFFAF9F7)
private val RCardBg      = Color(0xFFFFFFFF)
private val RAccent      = Color(0xFF47A025)
private val RAccentLight = Color(0xFFEDF7E8)
private val RTextPrimary = Color(0xFF1A1A1A)
private val RTextSec     = Color(0xFF888888)
private val RTextDim     = Color(0xFFBBBBBB)
private val RBorder      = Color(0xFFE8E8E8)
private val rCardShape   = RoundedCornerShape(4.dp)

private val DAY_COLORS = listOf(
    Color(0xFF4A6FA5), // Mon
    Color(0xFF47A025), // Tue
    Color(0xFF8A3ABA), // Wed
    Color(0xFFEC6F00), // Thu
    Color(0xFFD63030), // Fri
    Color(0xFF1696B6), // Sat
    Color(0xFFB8A000), // Sun
)
private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private fun parsePaceSeconds(text: String): Int? {
    if (text.isBlank()) return null
    val parts = text.trim().split(":")
    if (parts.size != 2) return null
    val minutes = parts[0].toIntOrNull() ?: return null
    val seconds = parts[1].toIntOrNull() ?: return null
    if (minutes < 0 || seconds < 0 || seconds >= 60) return null
    val total = minutes * 60 + seconds
    return if (total > 0) total else null
}

private fun formatPace(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

private fun calculateStreaks(weeks: List<RunningWeek>): Pair<Int, Int> {
    if (weeks.isEmpty()) return 0 to 0
    val sorted = weeks.sortedBy { it.weekStart }
    var maxStreak = 1
    var streak = 1
    for (i in 1 until sorted.size) {
        val prev = LocalDate.parse(sorted[i - 1].weekStart)
        val curr = LocalDate.parse(sorted[i].weekStart)
        if (ChronoUnit.DAYS.between(prev, curr) == 7L) {
            streak++
            if (streak > maxStreak) maxStreak = streak
        } else {
            streak = 1
        }
    }
    val lastWeekStart = LocalDate.parse(sorted.last().weekStart)
    val daysSinceLast = ChronoUnit.DAYS.between(lastWeekStart, LocalDate.now())
    val currentStreak = if (daysSinceLast <= 13) streak else 0
    return maxStreak to currentStreak
}

@Composable
fun RunningScreen(vm: RunningViewModel = viewModel(), onOpenDrawer: () -> Unit = {}) {
    val state by vm.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(RPageBg)) {
        when {
            state.loading    -> RLoadingSpinner()
            state.fetchError -> RCenteredText("Could not load data.")
            else             -> RMainContent(state, vm, onOpenDrawer)
        }
        if (state.showPasswordDialog) {
            PasswordDialog(onConfirm = vm::enterEditMode, onDismiss = vm::dismissPasswordDialog)
        }
        if (state.showAddModal) {
            AddRunEntryDialog(
                onSave    = { miles, date, pace -> vm.addEntry(miles, date, pace) },
                onDismiss = vm::closeAddModal,
                saveError = state.saveError,
            )
        }
        if (state.showEditModal && state.editingEntry != null) {
            EditRunEntryDialog(
                entry     = state.editingEntry,
                onSave    = { miles, date, pace -> vm.updateEntry(state.editingEntry.id, miles, date, pace) },
                onDismiss = vm::closeEditModal,
                saveError = state.editError,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RMainContent(state: RunningUiState, vm: RunningViewModel, onOpenDrawer: () -> Unit = {}) {
    val chartWeeks = remember(state.allWeeks, state.selectedYear) {
        val base = if (state.selectedYear != null) state.allWeeks.filter { it.year == state.selectedYear } else state.allWeeks
        base.sortedBy { it.weekStart }
    }
    val tableWeeks = state.filteredWeeks

    val yearTotal = tableWeeks.sumOf { it.total.toDouble() }.toFloat()
    val weekCount = tableWeeks.size

    Scaffold(
        containerColor = RPageBg,
        topBar = {
            if (state.isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(RAccent).statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("EDIT MODE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp)
                    TextButton(onClick = vm::exitEditMode) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = RBorder, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RCardBg)
                        .navigationBarsPadding()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RNavItem(
                        icon    = Icons.Default.Add,
                        label   = "Log Run",
                        selected = false,
                        tint    = RAccent,
                        modifier = Modifier.weight(1f),
                        onClick = vm::handleAddNew,
                    )
                    Box(modifier = Modifier.width(1.dp).height(28.dp).background(RBorder))
                    RNavItem(
                        icon    = Icons.Default.List,
                        label   = "Log",
                        selected = state.activeTab == RunningTab.LOG,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.setTab(RunningTab.LOG) },
                    )
                    RNavItem(
                        icon    = Icons.Default.ShowChart,
                        label   = "Stats",
                        selected = state.activeTab == RunningTab.STATS,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.setTab(RunningTab.STATS) },
                    )
                }
            }
        },
    ) { padding ->
        if (state.activeTab == RunningTab.STATS) {
            RStatsContent(weeks = state.allWeeks, modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 32.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onOpenDrawer, modifier = Modifier.size(28.dp).offset(x = (-4).dp)) {
                                Icon(Icons.Default.Menu, "Menu", tint = RAccent, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                "RUNNING", color = RAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp, modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                        Text(
                            text = "Weekly Log",
                            color = RTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Box(modifier = Modifier.padding(top = 14.dp).width(48.dp).height(3.dp).background(RAccent))
                        HorizontalDivider(modifier = Modifier.padding(top = 14.dp), color = RBorder)
                    }
                }

                stickyHeader {
                    Column(modifier = Modifier.fillMaxWidth().background(RCardBg)) {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
                            RunningBarChart(weeks = chartWeeks)
                        }
                        HorizontalDivider(color = RBorder)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                YearChip("All", state.selectedYear == null) { vm.setYear(null) }
                                state.availableYears.forEach { yr ->
                                    YearChip("$yr", state.selectedYear == yr) { vm.setYear(yr) }
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("%.2f mi".format(yearTotal), color = RTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Text("$weekCount weeks", color = RTextSec, fontSize = 12.sp)
                            }
                        }
                        HorizontalDivider(color = RBorder)
                    }
                }

                items(tableWeeks) { week ->
                    WeekRow(
                        week      = week,
                        isEditing = state.isEditing,
                        expanded  = state.expandedWeekStart == week.weekStart,
                        onToggle  = { vm.toggleExpanded(week.weekStart) },
                        onDelete  = { id -> vm.deleteEntry(id) },
                        onEdit    = { entry -> vm.startEditEntry(entry) },
                    )
                }
            }
        }
    }
}

// ── Stats view ────────────────────────────────────────────────────────────────

@Composable
private fun RStatsContent(weeks: List<RunningWeek>, modifier: Modifier = Modifier) {
    val allEntries   = remember(weeks) { weeks.flatMap { it.entries } }
    val totalMiles   = remember(weeks) { weeks.sumOf { it.total.toDouble() }.toFloat() }
    val totalRuns    = allEntries.size
    val avgWeekly    = if (weeks.isEmpty()) 0f else totalMiles / weeks.size
    val bestWeek     = remember(weeks) { weeks.maxByOrNull { it.total } }

    val today = remember { LocalDate.now() }
    val yearMiles = remember(weeks, today) {
        weeks.filter { it.year == today.year }.sumOf { it.total.toDouble() }.toFloat()
    }
    val monthMiles = remember(allEntries, today) {
        allEntries.filter {
            runCatching {
                val d = LocalDate.parse(it.date)
                d.year == today.year && d.monthValue == today.monthValue
            }.getOrDefault(false)
        }.sumOf { it.miles.toDouble() }.toFloat()
    }

    val (longestStreak, currentStreak) = remember(weeks) { calculateStreaks(weeks) }

    val favDay = remember(weeks) {
        listOf("Mon" to weeks.sumOf { it.mon.toDouble() },
               "Tue" to weeks.sumOf { it.tue.toDouble() },
               "Wed" to weeks.sumOf { it.wed.toDouble() },
               "Thu" to weeks.sumOf { it.thu.toDouble() },
               "Fri" to weeks.sumOf { it.fri.toDouble() },
               "Sat" to weeks.sumOf { it.sat.toDouble() },
               "Sun" to weeks.sumOf { it.sun.toDouble() })
            .filter { it.second > 0 }.maxByOrNull { it.second }?.first
    }

    val avgRunDist    = if (totalRuns > 0) totalMiles / totalRuns else 0f
    val longestRun    = remember(allEntries) { allEntries.maxOfOrNull { it.miles } }
    val daysSinceLast = remember(allEntries, today) {
        allEntries.maxByOrNull { it.date }?.date?.let {
            runCatching { ChronoUnit.DAYS.between(LocalDate.parse(it), today).toInt() }.getOrNull()
        }
    }
    val bestMonth     = remember(allEntries) {
        allEntries.groupBy { it.date.take(7) }
            .mapValues { (_, entries) -> entries.sumOf { it.miles.toDouble() }.toFloat() }
            .maxByOrNull { it.value }
            ?.let { (ym, miles) ->
                val parts = ym.split("-")
                val monthNames = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
                val label = runCatching { "${monthNames[parts[1].toInt() - 1]} ${parts[0]}" }.getOrDefault(ym)
                Pair(label, miles)
            }
    }

    val pacedEntries = remember(allEntries) { allEntries.filter { it.paceSeconds != null } }
    val avgPaceSecs  = if (pacedEntries.isEmpty()) null else
        pacedEntries.sumOf { it.paceSeconds!! } / pacedEntries.size
    val bestPaceSecs = pacedEntries.minOfOrNull { it.paceSeconds!! }

    LazyColumn(
        modifier       = modifier,
        contentPadding = PaddingValues(horizontal = 14.dp, bottom = 24.dp),
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 24.dp)) {
                Text("RUNNING", color = RAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                Text("Statistics", color = RTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp))
                Box(modifier = Modifier.padding(top = 14.dp).width(48.dp).height(3.dp).background(RAccent))
            }
        }

        item {
            RStatSection("Overview")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RStatCard("Total Miles", "%.2f".format(totalMiles), accent = true, modifier = Modifier.weight(1f))
                RStatCard("Total Runs", "$totalRuns", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RStatCard("Weeks Logged", "${weeks.size}", modifier = Modifier.weight(1f))
                RStatCard("Avg / Week", "%.2f mi".format(avgWeekly), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RStatCard("Avg Run", "%.2f mi".format(avgRunDist), modifier = Modifier.weight(1f))
                RStatCard("Longest Run", longestRun?.let { "%.2f mi".format(it) } ?: "—", modifier = Modifier.weight(1f))
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            RStatSection("Recent")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RStatCard("This Year", "%.2f mi".format(yearMiles), modifier = Modifier.weight(1f))
                RStatCard("This Month", "%.2f mi".format(monthMiles), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RStatCard("Current Streak", if (currentStreak > 0) "$currentStreak wks" else "—", modifier = Modifier.weight(1f))
                RStatCard("Longest Streak", if (longestStreak > 0) "$longestStreak wks" else "—", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            RStatCard(
                label    = "Days Since Last Run",
                value    = daysSinceLast?.let { if (it == 0) "Today" else "$it days" } ?: "—",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Spacer(Modifier.height(16.dp))
            RStatSection("Records")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RStatCard("Best Week", bestWeek?.let { "%.2f mi".format(it.total) } ?: "—",
                    subtitle = bestWeek?.weekStart, modifier = Modifier.weight(1f))
                RStatCard("Best Month", bestMonth?.let { "%.2f mi".format(it.second) } ?: "—",
                    subtitle = bestMonth?.first, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            RStatCard("Favorite Day", favDay ?: "—", modifier = Modifier.fillMaxWidth())
        }

        item {
            Spacer(Modifier.height(16.dp))
            RStatSection("Day Breakdown")
            Spacer(Modifier.height(8.dp))
            RDayFrequencyChart(weeks = weeks)
        }

        if (pacedEntries.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                RStatSection("Pace")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RStatCard("Avg Pace", avgPaceSecs?.let { "${formatPace(it)}/mi" } ?: "—", modifier = Modifier.weight(1f))
                    RStatCard("Best Pace", bestPaceSecs?.let { "${formatPace(it)}/mi" } ?: "—", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                RStatCard("Pace Logged", "${pacedEntries.size} of $totalRuns runs", modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun RStatSection(title: String) {
    Text(
        title.uppercase(),
        color = RTextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun RStatCard(
    label: String,
    value: String,
    subtitle: String? = null,
    accent: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(rCardShape)
            .background(if (accent) RAccentLight else RCardBg)
            .border(androidx.compose.foundation.BorderStroke(1.dp, if (accent) RAccent.copy(alpha = 0.3f) else RBorder), rCardShape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(value, color = if (accent) RAccent else RTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        if (subtitle != null) {
            Text(subtitle, color = RTextDim, fontSize = 10.sp, modifier = Modifier.padding(top = 1.dp))
        }
        Text(label, color = RTextSec, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

// ── Day frequency chart ───────────────────────────────────────────────────────

@Composable
private fun RDayFrequencyChart(weeks: List<RunningWeek>) {
    val dayCounts = remember(weeks) {
        val counts = IntArray(7)
        weeks.flatMap { it.entries }.forEach { entry ->
            runCatching {
                val dow = LocalDate.parse(entry.date).dayOfWeek.value - 1 // Mon=0, Sun=6
                counts[dow]++
            }
        }
        counts.toList()
    }
    val maxCount = (dayCounts.maxOrNull() ?: 0).coerceAtLeast(1)

    Row(
        modifier = Modifier.fillMaxWidth().height(140.dp).padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        dayCounts.zip(DAY_COLORS).zip(DAY_LABELS).forEach { (pair, label) ->
            val (count, color) = pair
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(18.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (count > 0) {
                        Text("$count", fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold, lineHeight = 9.sp)
                    }
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFF2F0ED)),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(count.toFloat() / maxCount)
                                .background(color),
                        )
                    }
                }
                Text(
                    label.substring(0, 1),
                    fontSize = 9.sp,
                    color = if (count > 0) color else RTextDim,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

// ── Nav item ──────────────────────────────────────────────────────────────────

@Composable
private fun RNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = if (selected) RAccent else RTextSec,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, color = tint, fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 2.dp))
    }
}

// ── Bar chart ─────────────────────────────────────────────────────────────────

@Composable
private fun RunningBarChart(weeks: List<RunningWeek>) {
    if (weeks.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
            Text("No data yet", color = RTextDim, fontSize = 13.sp)
        }
        return
    }

    val density    = LocalDensity.current
    val barW       = with(density) { 20.dp.toPx() }
    val barGap     = with(density) { 4.dp.toPx() }
    val maxBarH    = with(density) { 120.dp.toPx() }
    val padL       = with(density) { 4.dp.toPx() }
    val padR       = with(density) { 24.dp.toPx() }
    val chartH     = with(density) { 140.dp.toPx() }
    val labelAreaH = with(density) { 24.dp.toPx() }
    val yAxisW     = with(density) { 32.dp.toPx() }
    val totalH     = chartH + labelAreaH

    val maxTotal  = weeks.maxOfOrNull { it.total } ?: 1f
    val canvasW   = padL + weeks.size * (barW + barGap) + padR

    val tickInterval = when {
        maxTotal > 60 -> 20f
        maxTotal > 30 -> 10f
        maxTotal > 15 -> 5f
        else          -> 2f
    }
    val ticks = generateSequence(tickInterval) { it + tickInterval }
        .takeWhile { it <= maxTotal * 1.05f }
        .toList()

    val scrollState = rememberScrollState()

    LaunchedEffect(weeks.size) {
        if (weeks.isNotEmpty()) scrollState.scrollTo(scrollState.maxValue)
    }

    var hovered by remember { mutableStateOf<Int?>(null) }
    val hoveredWeek = hovered?.let { weeks.getOrNull(it) }

    val yearLabelPaint = remember(density) {
        android.graphics.Paint().apply {
            textSize    = with(density) { 9.dp.toPx() }
            color       = android.graphics.Color.parseColor("#BFBAB4")
            typeface    = android.graphics.Typeface.DEFAULT_BOLD
            textAlign   = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    val milesLabelPaint = remember(density) {
        android.graphics.Paint().apply {
            textSize    = with(density) { 8.dp.toPx() }
            color       = android.graphics.Color.parseColor("#888888")
            textAlign   = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    val yAxisPaint = remember(density) {
        android.graphics.Paint().apply {
            textSize    = with(density) { 9.dp.toPx() }
            color       = android.graphics.Color.parseColor("#BBBBBB")
            textAlign   = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
        }
    }

    Column {
        hoveredWeek?.let { week ->
            Row(
                modifier = Modifier.fillMaxWidth().background(RAccentLight)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(week.weekStart, color = RAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("%.2f mi".format(week.total), color = RTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(week.mon, week.tue, week.wed, week.thu, week.fri, week.sat, week.sun)
                        .zip(DAY_LABELS).zip(DAY_COLORS).forEach { (pair, color) ->
                            val (miles, label) = pair
                            if (miles > 0f) Text("${label[0]}:${"%.2f".format(miles)}", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .width(with(density) { yAxisW.toDp() })
                    .height(with(density) { totalH.toDp() })
            ) {
                ticks.forEach { tickVal ->
                    val y = chartH - (tickVal / maxTotal) * maxBarH
                    drawContext.canvas.nativeCanvas.drawText(
                        "${tickVal.toInt()}",
                        yAxisW - with(density) { 3.dp.toPx() },
                        y + with(density) { 3.dp.toPx() },
                        yAxisPaint,
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).horizontalScroll(scrollState)) {
                Canvas(
                    modifier = Modifier
                        .width(with(density) { canvasW.toDp() })
                        .height(with(density) { totalH.toDp() })
                        .pointerInput(weeks) {
                            detectTapGestures { offset ->
                                val idx = ((offset.x - padL) / (barW + barGap)).toInt()
                                hovered = if (idx in weeks.indices && hovered == idx) null else idx.takeIf { it in weeks.indices }
                            }
                        }
                ) {
                    ticks.forEach { tickVal ->
                        val y = chartH - (tickVal / maxTotal) * maxBarH
                        drawLine(
                            color       = Color(0xFFEEEEEE),
                            start       = Offset(0f, y),
                            end         = Offset(canvasW, y),
                            strokeWidth = 1f,
                            pathEffect  = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
                        )
                    }

                    weeks.forEachIndexed { i, week ->
                        val x    = padL + i * (barW + barGap)
                        val days = listOf(week.mon, week.tue, week.wed, week.thu, week.fri, week.sat, week.sun)
                        var stackY = chartH

                        days.zip(DAY_COLORS).forEach { (miles, color) ->
                            if (miles > 0f) {
                                val h = (miles / maxTotal) * maxBarH
                                stackY -= h
                                drawRect(color.copy(alpha = if (hovered == i) 1f else 0.75f), Offset(x, stackY), Size(barW, h))
                            }
                        }

                        if (i == 0 || week.weekStart.substring(0, 4) != weeks[i - 1].weekStart.substring(0, 4)) {
                            drawContext.canvas.nativeCanvas.drawText(
                                week.weekStart.substring(0, 4),
                                x + barW / 2f, totalH - with(density) { 4.dp.toPx() }, yearLabelPaint,
                            )
                        }
                        if (hovered == i && week.total > 0f) {
                            val topY = chartH - (week.total / maxTotal) * maxBarH
                            drawContext.canvas.nativeCanvas.drawText(
                                "%.2f".format(week.total), x + barW / 2f,
                                topY - with(density) { 4.dp.toPx() }, milesLabelPaint,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Week row ──────────────────────────────────────────────────────────────────

@Composable
private fun WeekRow(
    week: RunningWeek,
    isEditing: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDelete: (Int) -> Unit,
    onEdit: (RunningDayEntry) -> Unit = {},
) {
    val dayMiles = listOf(week.mon, week.tue, week.wed, week.thu, week.fri, week.sat, week.sun)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(rCardShape).background(RCardBg).border(androidx.compose.foundation.BorderStroke(1.dp, RBorder), rCardShape),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .then(if (isEditing) Modifier.clickable { onToggle() } else Modifier)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                dayMiles.zip(DAY_COLORS).zip(DAY_LABELS).forEach { (pair, label) ->
                    val (miles, color) = pair
                    if (miles > 0f) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label.substring(0, 1), color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("%.2f".format(miles), color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(week.weekStart, color = RTextSec, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("%.2f mi".format(week.total), color = RTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                if (isEditing) {
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, tint = RTextDim, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (isEditing && expanded) {
            HorizontalDivider(color = RBorder)
            week.entries.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(entry.date, color = RTextSec, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("%.2f mi".format(entry.miles), color = RTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            if (entry.paceSeconds != null) {
                                Text("${formatPace(entry.paceSeconds)}/mi", color = RTextSec, fontSize = 12.sp)
                            }
                        }
                    }
                    IconButton(onClick = { onEdit(entry) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null, tint = RAccent, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { onDelete(entry.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, tint = Color(0xFFCC4444), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ── Add entry dialog ──────────────────────────────────────────────────────────

@Composable
private fun AddRunEntryDialog(
    onSave: (Float, LocalDate, Int?) -> Unit,
    onDismiss: () -> Unit,
    saveError: String?,
) {
    var milesText by remember { mutableStateOf("") }
    var dateText  by remember { mutableStateOf(LocalDate.now().toString()) }
    var paceText  by remember { mutableStateOf("") }
    val miles       = milesText.toFloatOrNull()
    val date        = runCatching { LocalDate.parse(dateText) }.getOrNull()
    val paceSeconds = parsePaceSeconds(paceText)
    val paceInvalid = paceText.isNotBlank() && paceSeconds == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Run") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = milesText, onValueChange = { milesText = it },
                    label = { Text("Miles") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = dateText, onValueChange = { dateText = it },
                    label = { Text("Date (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = paceText, onValueChange = { paceText = it },
                    label = { Text("Avg Pace — MM:SS/mi (optional)") },
                    placeholder = { Text("e.g. 8:30") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    isError = paceInvalid,
                )
                if (paceInvalid) {
                    Text("Enter pace as M:SS (e.g. 8:30)", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }
                saveError?.let { Text(it, color = Color.Red, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick  = { if (miles != null && date != null) onSave(miles, date, paceSeconds) },
                enabled  = miles != null && miles > 0f && date != null,
                colors   = ButtonDefaults.buttonColors(containerColor = RAccent),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ── Edit entry dialog ─────────────────────────────────────────────────────────

@Composable
private fun EditRunEntryDialog(
    entry: RunningDayEntry,
    onSave: (Float, LocalDate, Int?) -> Unit,
    onDismiss: () -> Unit,
    saveError: String?,
) {
    var milesText by remember { mutableStateOf("%.2f".format(entry.miles)) }
    var dateText  by remember { mutableStateOf(entry.date) }
    var paceText  by remember { mutableStateOf(if (entry.paceSeconds != null) formatPace(entry.paceSeconds) else "") }
    val miles       = milesText.toFloatOrNull()
    val date        = runCatching { LocalDate.parse(dateText) }.getOrNull()
    val paceSeconds = parsePaceSeconds(paceText)
    val paceInvalid = paceText.isNotBlank() && paceSeconds == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Run") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = milesText, onValueChange = { milesText = it },
                    label = { Text("Miles") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = dateText, onValueChange = { dateText = it },
                    label = { Text("Date (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = paceText, onValueChange = { paceText = it },
                    label = { Text("Avg Pace — MM:SS/mi (optional)") },
                    placeholder = { Text("e.g. 8:30") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    isError = paceInvalid,
                )
                if (paceInvalid) {
                    Text("Enter pace as M:SS (e.g. 8:30)", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }
                saveError?.let { Text(it, color = Color.Red, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick  = { if (miles != null && date != null) onSave(miles, date, paceSeconds) },
                enabled  = miles != null && miles > 0f && date != null && !paceInvalid,
                colors   = ButtonDefaults.buttonColors(containerColor = RAccent),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ── Year chip ─────────────────────────────────────────────────────────────────

@Composable
private fun YearChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) RAccent else RCardBg)
            .border(androidx.compose.foundation.BorderStroke(1.dp, if (selected) RAccent else RBorder), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) Color.White else RTextSec, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

// ── Misc ──────────────────────────────────────────────────────────────────────

@Composable private fun RLoadingSpinner() {
    val runningGreen = Color(0xFF47A025)
    Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = runningGreen, strokeWidth = 3.dp, modifier = Modifier.size(64.dp))
            Icon(Icons.Filled.DirectionsRun, contentDescription = null, tint = runningGreen, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable private fun RCenteredText(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
        Text(text, color = RTextSec, fontSize = 18.sp)
    }
}
