package com.jacobleighty.musictracker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jacobleighty.musictracker.data.TvShow
import java.util.Calendar

private val TPageBg      = Color(0xFFFAF9F7)
private val TCardBg      = Color(0xFFFFFFFF)
private val TAccent      = Color(0xFF7C3AED)
private val TAccentLight = Color(0xFFF3EEFF)
private val TExpectedBg  = Color(0xFFF5F0FF)
private val TTextPrimary = Color(0xFF1A1A1A)
private val TTextSec     = Color(0xFF888888)
private val TTextDim     = Color(0xFFBBBBBB)
private val TTextDimmer  = Color(0xFFCCCCCC)
private val TBorder      = Color(0xFFE8E8E8)
private val tCardShape   = RoundedCornerShape(4.dp)

@Composable
fun TvMoviesScreen(vm: TvMoviesViewModel = viewModel(), onOpenDrawer: () -> Unit = {}) {
    val state by vm.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(TPageBg)) {
        when {
            state.loading    -> TvLoadingSpinner()
            state.fetchError -> TCenteredText("Could not load data.")
            else             -> TMainContent(state, vm, onOpenDrawer)
        }
        if (state.showPasswordDialog) {
            PasswordDialog(onConfirm = vm::enterEditMode, onDismiss = vm::dismissPasswordDialog)
        }
        val serviceNames = remember(state) {
            (state.upcoming + state.expected + state.watchlist + state.watched)
                .map { it.service }.filter { it.isNotEmpty() }.distinct().sorted()
        }
        state.editingShow?.let { show ->
            EditShowDialog(
                show         = show,
                serviceNames = serviceNames,
                onSave       = vm::saveShow,
                onDelete     = { vm.deleteShow(show.id) },
                onDismiss    = vm::closeEdit,
            )
        }
    }
}

@Composable
private fun TMainContent(state: TvMoviesUiState, vm: TvMoviesViewModel, onOpenDrawer: () -> Unit = {}) {
    Scaffold(
        containerColor = TPageBg,
        topBar = {
            Column {
                if (state.isEditing) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(TAccent)
                            .statusBarsPadding()
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
                state.saveError?.let {
                    Box(modifier = Modifier.fillMaxWidth().background(TAccentLight).padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(it, color = TAccent, fontSize = 14.sp)
                    }
                }
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = TBorder, thickness = 1.dp)
                NavigationBar(containerColor = TCardBg, tonalElevation = 0.dp) {
                    NavigationBarItem(
                        selected = false, onClick = vm::handleAddNew,
                        icon  = { Icon(Icons.Default.Add, null) },
                        label = { Text("Add", letterSpacing = 1.sp) },
                        colors = tAddNavColors(),
                    )
                    NavigationBarItem(
                        selected = state.view == TvViewType.SCHEDULE,
                        onClick  = { vm.setView(TvViewType.SCHEDULE) },
                        icon     = { Icon(Icons.Default.DateRange, null) },
                        label    = { Text("Schedule") },
                        colors   = tDefaultNavColors(),
                    )
                    NavigationBarItem(
                        selected = state.view == TvViewType.HISTORY,
                        onClick  = { vm.setView(TvViewType.HISTORY) },
                        icon     = { Icon(Icons.Default.BarChart, null) }, label = { Text("History") },
                        colors   = tDefaultNavColors(),
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 32.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onOpenDrawer, modifier = Modifier.size(28.dp).offset(x = (-4).dp)) {
                            Icon(Icons.Default.Menu, "Menu", tint = TAccent, modifier = Modifier.size(20.dp))
                        }
                        Text("TV / MOVIES", color = TAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp, modifier = Modifier.padding(start = 4.dp))
                    }
                    Text(
                        text = if (state.view == TvViewType.HISTORY) "Watch History" else "Watch Schedule",
                        color = TTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Box(modifier = Modifier.padding(top = 14.dp).width(48.dp).height(3.dp).background(TAccent))
                    HorizontalDivider(modifier = Modifier.padding(top = 14.dp), color = TBorder)
                }
            }

            if (state.view == TvViewType.HISTORY) {
                item { TvHistorySection(state.watched) }
            } else {
                if (state.toWatch.isNotEmpty()) {
                    item { TSectionHeader("To Watch", Color(0xFF16A34A)) }
                    items(state.toWatch) { show -> TvCard(show, state.isEditing, vm::openEdit, vm::handleMarkWatched, watchable = true) }
                }
                if (state.upcoming.isNotEmpty()) {
                    item { TSectionHeader("Upcoming", TAccent) }
                    items(state.upcoming) { show -> TvCard(show, state.isEditing, vm::openEdit, vm::markWatched) }
                }
                if (state.expected.isNotEmpty()) {
                    item { TSectionHeader("Expected", TAccent) }
                    items(state.expected) { show -> TvRow(show, DateUtils.getYear(show.date), state.isEditing, vm::openEdit) }
                }
                if (state.watchlist.isNotEmpty()) {
                    item { TSectionHeader("Watchlist", TTextDim) }
                    items(state.watchlist) { show -> TvRow(show, "—", state.isEditing, vm::openEdit) }
                }
                if (state.toWatch.isEmpty() && state.upcoming.isEmpty() && state.expected.isEmpty() && state.watchlist.isEmpty()) {
                    item { TCenteredText("No shows yet. Tap Add to get started.") }
                }
            }
        }
    }
}

@Composable
private fun TvCard(show: TvShow, isEditing: Boolean, onEdit: (TvShow) -> Unit, onWatch: (TvShow) -> Unit, watchable: Boolean = false) {
    val (month, day, year) = DateUtils.parseParts(show.date)
    val daysUntil = DateUtils.daysUntil(show.date)
    val imminent  = !watchable && daysUntil in 0..6
    val past      = daysUntil < 0
    val toWatchGreen = Color(0xFF16A34A)
    val cardBg    = when { watchable -> Color(0xFFF0FDF4); imminent -> TAccentLight; else -> TCardBg }
    val borderCol = when { watchable -> Color(0xFFBBF7D0); imminent -> TAccent; else -> TBorder }
    val accentBar = if (watchable) toWatchGreen else TAccent

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
            .height(IntrinsicSize.Min).clip(tCardShape).background(cardBg)
            .border(BorderStroke(1.5.dp, borderCol), tCardShape),
    ) {
        Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(accentBar))
        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    .then(if (isEditing || watchable) Modifier.padding(end = 34.dp) else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 40.dp)) {
                    if (month != null) Text(month, color = accentBar, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, lineHeight = 12.sp)
                    if (day   != null) Text(day.toString(), color = if (watchable) toWatchGreen else TTextPrimary, fontSize = 38.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp)
                    if (year  != null) Text(year.toString(), color = TTextDim, fontSize = 11.sp, letterSpacing = 1.sp, lineHeight = 13.sp)
                }
                Box(modifier = Modifier.width(1.5.dp).height(48.dp).background(TBorder))
                Column(modifier = Modifier.weight(1f)) {
                    Text(show.programName, color = TTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    if (show.service.isNotEmpty()) TruncatedText(show.service, color = TTextSec, fontSize = 14.sp, fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(top = 1.dp))
                    if (show.type.isNotEmpty()) {
                        val tc = when (show.type) { "Movie" -> Color(0xFF0EA5E9); "Anime" -> Color(0xFFF59E0B); else -> TAccent }
                        Text(show.type, color = tc, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    if (imminent) {
                        val label = when (daysUntil.toInt()) { 0 -> "TODAY"; 1 -> "TOMORROW"; else -> "${daysUntil} DAYS AWAY" }
                        Text(label, color = TAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
            if (watchable || isEditing) {
                Row(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (watchable || past || imminent) {
                        val watchColor = if (watchable) toWatchGreen else Color(0xFF5A9A5A)
                        OutlinedButton(
                            onClick = { onWatch(show) }, modifier = Modifier.height(26.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            border = BorderStroke(1.5.dp, watchColor),
                        ) { Text("✓", color = watchColor, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        if (isEditing) Spacer(Modifier.width(4.dp))
                    }
                    if (isEditing) {
                        IconButton(onClick = { onEdit(show) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, null, tint = TTextDimmer, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvRow(show: TvShow, dateDisplay: String, isEditing: Boolean, onEdit: (TvShow) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
            .height(IntrinsicSize.Min).clip(tCardShape).background(TExpectedBg)
            .border(BorderStroke(1.5.dp, TBorder), tCardShape),
    ) {
        Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(TAccent))
        Row(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 18.dp)
                .then(if (isEditing) Modifier.padding(end = 28.dp) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (dateDisplay != "—") {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 38.dp)) {
                    Text(dateDisplay, color = TTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
                }
                Box(modifier = Modifier.width(1.5.dp).height(40.dp).background(TBorder))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(show.programName, color = TTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                if (show.service.isNotEmpty()) {
                    TruncatedText(show.service, color = TTextSec, fontSize = 13.sp, fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(top = 3.dp))
                }
                if (show.type.isNotEmpty()) {
                    val tc = when (show.type) { "Movie" -> Color(0xFF0EA5E9); "Anime" -> Color(0xFFF59E0B); else -> TAccent }
                    Text(show.type, color = tc, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
        if (isEditing) {
            Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                IconButton(onClick = { onEdit(show) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Edit, null, tint = TTextDimmer, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

// ── History section ────────────────────────────────────────────────────────────

private data class TvDot(val show: TvShow, val x: Float, val y: Float)

@Composable
private fun TvHistorySection(watched: List<TvShow>) {
    if (watched.isEmpty()) { TCenteredText("No watch history yet."); return }

    var selected     by remember { mutableStateOf<TvDot?>(null) }
    var selectedYear by remember { mutableStateOf<Int?>(null) }

    val byYear = remember(watched) {
        watched.filter { DateUtils.hasFullDate(it.date) }
            .groupBy { DateUtils.parseDate(it.date).year }
            .entries.sortedByDescending { it.key }
    }
    val years = remember(watched) {
        watched.filter { it.date.isNotEmpty() }
            .mapNotNull { DateUtils.getYear(it.date).toIntOrNull() }
            .distinct().sortedDescending()
    }
    val topServicePair = remember(watched) {
        watched.filter { it.service.isNotEmpty() }
            .groupBy { it.service }.maxByOrNull { it.value.size }?.let { it.key to it.value.size }
    }
    val subset = remember(watched, selectedYear) {
        if (selectedYear == null) watched
        else watched.filter { it.date.isNotEmpty() && DateUtils.getYear(it.date).toIntOrNull() == selectedYear }
    }
    val typeCounts = remember(subset) {
        mapOf("TV" to subset.count { it.type == "TV" },
              "Movie" to subset.count { it.type == "Movie" },
              "Anime" to subset.count { it.type == "Anime" })
    }
    val typeColors = mapOf("TV" to TAccent, "Movie" to Color(0xFF0EA5E9), "Anime" to Color(0xFFF59E0B))

    Column {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            TStatChip(watched.size.toString(), "watched")
            topServicePair?.let { (svc, cnt) -> TStatChip(svc, "top service · $cnt") }
        }
        HorizontalDivider(color = TBorder, modifier = Modifier.padding(horizontal = 14.dp))

        val maxInYear = byYear.maxOfOrNull { it.value.size } ?: 1
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom,
        ) {
            byYear.asReversed().forEach { (yr, shows) ->
                val barH = maxOf((shows.size.toFloat() / maxInYear * 52).toInt(), 4).dp
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${shows.size}", color = TTextSec, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Box(modifier = Modifier.padding(top = 2.dp, bottom = 4.dp).width(28.dp).height(barH)
                        .background(TAccent, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)))
                    Text("$yr", color = TTextDim, fontSize = 9.sp)
                }
            }
        }

        HorizontalDivider(color = TBorder, modifier = Modifier.padding(horizontal = 14.dp))

        // Type breakdown with year filter
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            Text("BY TYPE", color = TTextDimmer, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(
                    selected = selectedYear == null,
                    onClick  = { selectedYear = null },
                    label    = { Text("All", fontSize = 11.sp) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TAccentLight, selectedLabelColor = TAccent),
                )
                years.forEach { yr ->
                    FilterChip(
                        selected = selectedYear == yr,
                        onClick  = { selectedYear = if (selectedYear == yr) null else yr },
                        label    = { Text("$yr", fontSize = 11.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TAccentLight, selectedLabelColor = TAccent),
                    )
                }
            }
            val subTotal = subset.size
            listOf("TV", "Movie", "Anime").forEach { t ->
                val count = typeCounts[t] ?: 0
                val frac  = if (subTotal > 0) count.toFloat() / subTotal else 0f
                val color = typeColors[t] ?: TAccent
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(t, color = TTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(56.dp))
                    Box(modifier = Modifier.weight(1f).height(9.dp)
                        .background(Color(0xFFF0EEEB), RoundedCornerShape(2.dp))) {
                        if (frac > 0f) Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(frac)
                            .background(color, RoundedCornerShape(2.dp)))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("${(frac * 100).toInt()}% ($count)", color = TTextSec, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.width(72.dp))
                }
            }
        }

        HorizontalDivider(color = TBorder, modifier = Modifier.padding(horizontal = 14.dp))
        Spacer(Modifier.height(8.dp))
        TvTimeline(watched.filter { DateUtils.hasFullDate(it.date) }, selected) { selected = it }

        selected?.let { dot ->
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth()
                    .height(IntrinsicSize.Min).clip(tCardShape).background(TCardBg)
                    .border(BorderStroke(1.5.dp, TBorder), tCardShape).padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(TAccent))
                Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(dot.show.programName, color = TAccent, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (dot.show.service.isNotEmpty()) Text(dot.show.service, color = TTextSec, fontSize = 13.sp, fontStyle = FontStyle.Italic)
                    if (dot.show.date.isNotEmpty()) Text(DateUtils.formatLastDate(dot.show.date), color = TTextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                }
                IconButton(onClick = { selected = null }, modifier = Modifier.size(36.dp)) { Text("×", color = TTextSec, fontSize = 20.sp) }
            }
        }
    }
}

@Composable
private fun TvTimeline(shows: List<TvShow>, selected: TvDot?, onTap: (TvDot?) -> Unit) {
    val density    = LocalDensity.current
    val pxPerMonth = with(density) { 40.dp.toPx() }
    val dotR       = with(density) { 8.dp.toPx() }
    val timelineY  = with(density) { 130.dp.toPx() }
    val canvasH    = with(density) { 158.dp.toPx() }
    val padL       = with(density) { 16.dp.toPx() }
    val padR       = with(density) { 32.dp.toPx() }
    val nowCal     = remember { Calendar.getInstance() }

    val sorted = remember(shows) { shows.sortedBy { DateUtils.parseDate(it.date) } }
    val minYear = remember(sorted) { sorted.minOfOrNull { DateUtils.parseDate(it.date).year } ?: nowCal.get(Calendar.YEAR) }
    val maxYear = remember(sorted) { maxOf(sorted.maxOfOrNull { DateUtils.parseDate(it.date).year } ?: nowCal.get(Calendar.YEAR), nowCal.get(Calendar.YEAR)) }

    fun mToX(yr: Int, mo: Int) = padL + ((yr - minYear) * 12 + mo) * pxPerMonth + pxPerMonth / 2f
    val canvasW = padL + (maxYear - minYear + 2) * 12 * pxPerMonth + padR

    val dots = remember(sorted, minYear, pxPerMonth) {
        val byKey = mutableMapOf<String, MutableList<TvShow>>()
        sorted.forEach { s ->
            val d   = DateUtils.parseDate(s.date)
            val key = "${d.year}-${d.monthValue - 1}"
            byKey.getOrPut(key) { mutableListOf() }.add(s)
        }
        buildList {
            byKey.forEach { (key, items) ->
                val parts = key.split("-")
                val x = mToX(parts[0].toInt(), parts[1].toInt())
                items.forEachIndexed { i, s -> add(TvDot(s, x, timelineY - dotR - i * (dotR * 2 + 3f))) }
            }
        }
    }
    val yearTicks = remember(minYear, maxYear) { (minYear..maxYear + 1).map { yr -> Pair(yr, padL + (yr - minYear) * 12 * pxPerMonth) } }

    val bandColor = Color(0xFFF5F3F0); val gridColor = Color(0xFFE8E3DE); val baseColor = Color(0xFFCEC9C3)
    val labelPaint = remember(density) {
        android.graphics.Paint().apply {
            textSize = with(density) { 11.dp.toPx() }; color = android.graphics.Color.parseColor("#BFBAB4")
            typeface = android.graphics.Typeface.DEFAULT_BOLD; isAntiAlias = true
        }
    }

    val scrollState = rememberScrollState()
    LaunchedEffect(minYear) {
        val targetX = (padL + (nowCal.get(Calendar.YEAR) - minYear) * 12 * pxPerMonth - pxPerMonth * 6).toInt().coerceAtLeast(0)
        scrollState.scrollTo(targetX)
    }

    Box(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
        Canvas(
            modifier = Modifier.width(with(density) { canvasW.toDp() }).height(with(density) { canvasH.toDp() })
                .pointerInput(dots, selected) {
                    detectTapGestures { offset ->
                        val hit = dots.firstOrNull { dot ->
                            val dx = offset.x - dot.x; val dy = offset.y - dot.y
                            dx * dx + dy * dy <= (dotR * 2f) * (dotR * 2f)
                        }
                        onTap(if (hit != null && hit.show.id == selected?.show?.id) null else hit)
                    }
                }
        ) {
            yearTicks.forEachIndexed { i, (_, x) ->
                if (i % 2 == 1) drawRect(bandColor, Offset(x, 0f), Size(12 * pxPerMonth, timelineY), alpha = 0.6f)
            }
            yearTicks.forEach { (_, x) -> drawLine(gridColor, Offset(x, with(density) { 10.dp.toPx() }), Offset(x, timelineY), strokeWidth = 1.5f) }
            drawLine(baseColor, Offset(padL, timelineY), Offset(canvasW - padR, timelineY), strokeWidth = 2f)
            yearTicks.forEach { (yr, x) ->
                drawContext.canvas.nativeCanvas.drawText("$yr", x + with(density) { 4.dp.toPx() }, canvasH - with(density) { 4.dp.toPx() }, labelPaint)
            }
            val todayX = mToX(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH))
            if (todayX in padL..(canvasW - padR)) {
                drawLine(TAccent.copy(alpha = 0.35f), Offset(todayX, with(density) { 16.dp.toPx() }), Offset(todayX, timelineY),
                    strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            }
            dots.forEach { dot ->
                val isSelected = selected?.show?.id == dot.show.id
                drawCircle(TAccent, dotR, Offset(dot.x, dot.y))
                drawCircle(
                    color = if (isSelected) Color(0xFF1A1A1A) else Color.White, radius = dotR, center = Offset(dot.x, dot.y),
                    style = Stroke(width = if (isSelected) with(density) { 2.5.dp.toPx() } else with(density) { 2.dp.toPx() }),
                )
            }
        }
    }
}

// ── Edit dialog ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditShowDialog(show: TvShow, serviceNames: List<String> = emptyList(), onSave: (TvShow) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var programName   by remember { mutableStateOf(show.programName) }
    var service       by remember { mutableStateOf(show.service) }
    var serviceSug    by remember { mutableStateOf<List<String>>(emptyList()) }
    var date          by remember { mutableStateOf(show.date) }
    var notes         by remember { mutableStateOf(show.notes) }
    var showType      by remember { mutableStateOf(show.type) }
    var watched       by remember { mutableStateOf(show.watched) }
    var confirmDelete by remember { mutableStateOf(false) }
    var typeExpanded  by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (show.id == 0) "Add Show" else "Edit Show") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(programName, { programName = it }, label = { Text("Program name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
                OutlinedTextField(
                    value = service,
                    onValueChange = {
                        service = it
                        serviceSug = if (it.isBlank()) emptyList()
                            else serviceNames.filter { n -> n.contains(it, ignoreCase = true) && !n.equals(it, ignoreCase = true) }.take(5)
                    },
                    label = { Text("Where to watch") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                if (serviceSug.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()
                        .border(BorderStroke(1.dp, TBorder), RoundedCornerShape(4.dp))) {
                        serviceSug.forEach { sug ->
                            TextButton(
                                onClick = { service = sug; serviceSug = emptyList() },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            ) { Text(sug, color = TAccent, modifier = Modifier.fillMaxWidth()) }
                        }
                    }
                }
                OutlinedTextField(date, { date = it }, label = { Text("Date (M/D/YYYY or M/YYYY or YYYY)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = showType.ifEmpty { "— Select type —" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        listOf("", "TV", "Movie", "Anime").forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(if (opt.isEmpty()) "— None —" else opt) },
                                onClick = { showType = opt; typeExpanded = false },
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(watched, { watched = it })
                    Text("Watched")
                }
                if (confirmDelete) {
                    Text("Delete this show?", color = Color.Red, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { confirmDelete = false }) { Text("Cancel") }
                        Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Delete") }
                    }
                } else if (show.id != 0) {
                    TextButton(onClick = { confirmDelete = true }) { Text("Delete", color = Color.Red) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(show.copy(programName = programName.trim(), service = service.trim(), date = date.trim(), notes = notes.trim(), type = showType.trim(), watched = watched)) },
                enabled = programName.isNotBlank(),
                colors  = ButtonDefaults.buttonColors(containerColor = TAccent),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable private fun TSectionHeader(title: String, color: Color) {
    Row(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 28.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(3.dp).height(14.dp).background(color))
        Text(title.uppercase(), color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable private fun TStatChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1)
        Text(label, color = TTextSec, fontSize = 10.sp, letterSpacing = 0.3.sp, maxLines = 1)
    }
}

@Composable private fun TvLoadingSpinner() {
    Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TAccent, strokeWidth = 3.dp, modifier = Modifier.size(64.dp))
            Icon(Icons.Filled.Tv, contentDescription = null, tint = TAccent, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable private fun TCenteredText(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
        Text(text, color = TTextSec, fontSize = 18.sp)
    }
}

@Composable private fun tAddNavColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = TAccent, unselectedIconColor = TAccent,
    selectedTextColor = TAccent, unselectedTextColor = TAccent, indicatorColor = TAccentLight)

@Composable private fun tDefaultNavColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = TAccent, unselectedIconColor = Color(0xFFAAAAAA),
    selectedTextColor = TAccent, unselectedTextColor = Color(0xFFAAAAAA), indicatorColor = TAccentLight)
