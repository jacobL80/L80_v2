package com.jacobleighty.musictracker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jacobleighty.musictracker.data.Concert
import java.util.Calendar
import kotlin.math.abs

private val CPageBg      get() = currentAppColors.pageBg
private val CCardBg      get() = currentAppColors.cardBg
private val CAccent      = Color(0xFF1696B6)
private val CAccentLight get() = currentAppColors.concertAccentLight
private val CTextPrimary get() = currentAppColors.textPrimary
private val CTextSec     get() = currentAppColors.textSecondary
private val CTextDim     get() = currentAppColors.textDim
private val CTextDimmer  get() = currentAppColors.textDimmer
private val CBorder      get() = currentAppColors.border
private val cCardShape   = RoundedCornerShape(4.dp)

@Composable
fun ConcertsScreen(vm: ConcertsViewModel = viewModel(), onOpenDrawer: () -> Unit = {}) {
    val state by vm.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(CPageBg)) {
        when {
            state.loading    -> ConcertLoadingSpinner()
            state.fetchError -> CCenteredText("Could not load data.")
            else             -> CMainContent(state, vm, onOpenDrawer)
        }
        if (state.showPasswordDialog) {
            PasswordDialog(onConfirm = vm::enterEditMode, onDismiss = vm::dismissPasswordDialog)
        }
        state.editingConcert?.let { concert ->
            val bandNames = remember(state.upcoming, state.attended) {
                (state.upcoming + state.attended).mapNotNull { it.band.takeIf { b -> b.isNotEmpty() } }.distinct().sorted()
            }
            val venueNames = remember(state.upcoming, state.attended) {
                (state.upcoming + state.attended).mapNotNull { it.venue.takeIf { v -> v.isNotEmpty() } }.distinct().sorted()
            }
            val attendeeNames = remember(state.upcoming, state.attended) {
                (state.upcoming + state.attended)
                    .flatMap { parseAttendeesKt(it.attendees) }
                    .filter { it.isNotEmpty() }.distinct().sorted()
            }
            EditConcertDialog(
                concert       = concert,
                bandNames     = bandNames,
                venueNames    = venueNames,
                attendeeNames = attendeeNames,
                onSave        = vm::saveConcert,
                onDelete      = { vm.deleteConcert(concert.id) },
                onDismiss     = vm::closeEdit,
            )
        }
    }
}

@Composable
private fun CMainContent(state: ConcertsUiState, vm: ConcertsViewModel, onOpenDrawer: () -> Unit = {}) {
    Scaffold(
        containerColor = CPageBg,
        topBar = {
            Column {
                if (state.isEditing) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(CAccent)
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("EDIT MODE", color = Color.White, fontWeight = FontWeight.Bold,
                            fontSize = 11.sp, letterSpacing = 2.sp)
                        TextButton(onClick = vm::exitEditMode) {
                            Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
                state.saveError?.let {
                    Box(modifier = Modifier.fillMaxWidth().background(CAccentLight)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) { Text(it, color = CAccent, fontSize = 14.sp) }
                }
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = CBorder, thickness = 1.dp)
                NavigationBar(containerColor = CCardBg, tonalElevation = 0.dp) {
                    NavigationBarItem(
                        selected = false, onClick = vm::handleAddNew,
                        icon  = { Icon(Icons.Default.Add, null) },
                        label = { Text("Add", letterSpacing = 1.sp) },
                        colors = cAddNavColors(),
                    )
                    NavigationBarItem(
                        selected = state.view == ConcertViewType.SCHEDULE,
                        onClick  = { vm.setView(ConcertViewType.SCHEDULE) },
                        icon     = { Icon(Icons.Default.DateRange, null) },
                        label    = { Text("Schedule") },
                        colors   = cDefaultNavColors(),
                    )
                    NavigationBarItem(
                        selected = state.view == ConcertViewType.HISTORY,
                        onClick  = { vm.setView(ConcertViewType.HISTORY) },
                        icon     = { Icon(Icons.Default.BarChart, null) },
                        label    = { Text("History") },
                        colors   = cDefaultNavColors(),
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 32.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onOpenDrawer, modifier = Modifier.size(28.dp).offset(x = (-4).dp)) {
                            Icon(Icons.Default.Menu, "Menu", tint = CAccent, modifier = Modifier.size(20.dp))
                        }
                        Text("CONCERTS", color = CAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp, modifier = Modifier.padding(start = 4.dp))
                    }
                    Text(
                        text = if (state.view == ConcertViewType.HISTORY) "Concert History" else "Concert Schedule",
                        color = CTextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Box(modifier = Modifier.padding(top = 14.dp).width(48.dp).height(3.dp).background(CAccent))
                    HorizontalDivider(modifier = Modifier.padding(top = 14.dp), color = CBorder)
                }
            }

            if (state.view == ConcertViewType.HISTORY) {
                item { ConcertHistorySection(state.attended) }
            } else {
                if (state.upcoming.isNotEmpty()) {
                    item { CSectionHeader("Upcoming", CAccent) }
                    items(state.upcoming) { c ->
                        ConcertCard(c, state.isEditing, vm::openEdit, vm::markAttended)
                    }
                } else {
                    item { CCenteredText("No upcoming concerts. Tap Add to get started.") }
                }
            }
        }
    }
}

// ── Concert card (full date upcoming) ─────────────────────────────────────────

@Composable
private fun ConcertCard(
    concert: Concert,
    isEditing: Boolean,
    onEdit: (Concert) -> Unit,
    onAttend: (Concert) -> Unit,
) {
    val hasDate    = concert.date.isNotEmpty()
    val fullDate   = hasDate && DateUtils.hasFullDate(concert.date)
    val (month, day, year) = if (hasDate) DateUtils.parseParts(concert.date) else DateUtils.DateParts(null, null, null)
    val daysUntil  = if (fullDate) DateUtils.daysUntil(concert.date) else null
    val imminent   = daysUntil != null && daysUntil in 0..6
    val past       = daysUntil != null && daysUntil < 0
    val cardBg     = if (imminent) CAccentLight else CCardBg
    val borderCol  = if (imminent) CAccent else CBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .height(IntrinsicSize.Min)
            .clip(cCardShape)
            .background(cardBg)
            .border(BorderStroke(1.5.dp, borderCol), cCardShape),
    ) {
        Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(CAccent))
        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .then(if (isEditing) Modifier.padding(end = 34.dp) else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (hasDate) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(min = 40.dp)) {
                        if (month != null) Text(month, color = CAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, lineHeight = 12.sp)
                        if (day   != null) Text(day.toString(), color = CTextPrimary, fontSize = 38.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp)
                        if (year  != null) Text(year.toString(), color = if (day == null) CTextPrimary else CTextDim, fontSize = if (day == null) 22.sp else 11.sp, letterSpacing = 1.sp, lineHeight = 13.sp)
                    }
                    Box(modifier = Modifier.width(1.5.dp).height(48.dp).background(CBorder))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(concert.band, color = CTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    if (concert.tourName.isNotEmpty()) {
                        TruncatedText(concert.tourName, color = CTextSec, fontSize = 14.sp, fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(top = 1.dp))
                    }
                    if (concert.venue.isNotEmpty()) {
                        TruncatedText(concert.venue, color = CTextDim, fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp))
                    }
                    if (imminent) {
                        val label = when (daysUntil.toInt()) { 0 -> "TODAY"; 1 -> "TOMORROW"; else -> "${daysUntil} DAYS AWAY" }
                        Text(label, color = CAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
            if (isEditing) {
                Row(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (past || imminent) {
                        OutlinedButton(
                            onClick = { onAttend(concert) },
                            modifier = Modifier.height(26.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            border = BorderStroke(1.5.dp, Color(0xFF5A9A5A)),
                        ) { Text("✓", color = Color(0xFF5A9A5A), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(onClick = { onEdit(concert) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, null, tint = CTextDimmer, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

// ── History section ────────────────────────────────────────────────────────────

private data class ConcertDot(val c: Concert, val x: Float, val y: Float)

@Composable
private fun ConcertHistorySection(attended: List<Concert>) {
    if (attended.isEmpty()) { CCenteredText("No concert history yet."); return }

    var selected by remember { mutableStateOf<ConcertDot?>(null) }
    val byYear = remember(attended) {
        attended.groupBy { DateUtils.parseDate(it.date).year }
            .entries.sortedByDescending { it.key }
    }
    val totalCount  = attended.size
    val topVenuePair = remember(attended) {
        attended.filter { it.venue.isNotEmpty() }
            .groupBy { it.venue }
            .maxByOrNull { it.value.size }
            ?.let { it.key to it.value.size }
    }
    val topArtistPair = remember(attended) {
        attended.groupBy { it.band }
            .maxByOrNull { it.value.size }
            ?.let { it.key to it.value.size }
    }
    val topAttendeePair = remember(attended) {
        attended.flatMap { parseAttendeesKt(it.attendees) }
            .groupBy { it }
            .maxByOrNull { it.value.size }
            ?.let { it.key to it.value.size }
    }
    val topArtists = remember(attended) {
        attended.groupBy { it.band }
            .map { it.key to it.value.size }
            .sortedByDescending { it.second }
    }
    val topVenues = remember(attended) {
        attended.filter { it.venue.isNotEmpty() }
            .groupBy { it.venue }
            .map { it.key to it.value.size }
            .sortedByDescending { it.second }
    }
    val topAttendees = remember(attended) {
        attended.flatMap { parseAttendeesKt(it.attendees) }
            .groupBy { it }
            .map { it.key to it.value.size }
            .sortedByDescending { it.second }
    }

    Column {
        // Stats
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CStatChip(totalCount.toString(), "attended")
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    topVenuePair?.let { (venue, cnt) -> CStatChip(venue, "top venue · $cnt") }
                }
            }
            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    topArtistPair?.let { (artist, cnt) -> CStatChip(artist, "top artist · $cnt") }
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    topAttendeePair?.let { (name, cnt) -> CStatChip(name, "most with · $cnt") }
                }
            }
        }

        HorizontalDivider(color = CBorder, modifier = Modifier.padding(horizontal = 14.dp))

        // Year bar chart
        val maxInYear = byYear.maxOfOrNull { it.value.size } ?: 1
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            byYear.asReversed().forEach { (yr, concerts) ->
                val barH = maxOf((concerts.size.toFloat() / maxInYear * 52).toInt(), 4).dp
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${concerts.size}", color = CTextSec, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Box(modifier = Modifier.padding(top = 2.dp, bottom = 4.dp).width(28.dp).height(barH)
                        .background(CAccent, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)))
                    Text("$yr", color = CTextDim, fontSize = 9.sp)
                }
            }
        }

        HorizontalDivider(color = CBorder, modifier = Modifier.padding(horizontal = 14.dp))
        CHBarChart("By Artist", topArtists, CAccent)
        if (topVenues.isNotEmpty()) {
            HorizontalDivider(color = CBorder, modifier = Modifier.padding(horizontal = 14.dp))
            CHBarChart("By Venue", topVenues, CAccent)
        }
        if (topAttendees.isNotEmpty()) {
            HorizontalDivider(color = CBorder, modifier = Modifier.padding(horizontal = 14.dp))
            CHBarChart("Most With", topAttendees, CAccent)
        }
        HorizontalDivider(color = CBorder, modifier = Modifier.padding(horizontal = 14.dp))
        Spacer(Modifier.height(8.dp))

        // Dot timeline
        ConcertTimeline(attended, selected) { selected = it }

        // Selected detail card
        selected?.let { dot ->
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth()
                    .height(IntrinsicSize.Min).clip(cCardShape).background(CCardBg)
                    .border(BorderStroke(1.5.dp, CBorder), cCardShape).padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(CAccent))
                Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(dot.c.band, color = CAccent, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (dot.c.tourName.isNotEmpty()) Text(dot.c.tourName, color = CTextSec, fontSize = 13.sp, fontStyle = FontStyle.Italic)
                    if (dot.c.venue.isNotEmpty()) Text(dot.c.venue, color = CTextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    if (dot.c.date.isNotEmpty()) Text(DateUtils.formatLastDate(dot.c.date), color = CTextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                }
                IconButton(onClick = { selected = null }, modifier = Modifier.size(36.dp)) {
                    Text("×", color = CTextSec, fontSize = 20.sp)
                }
            }
        }
    }
}

@Composable
private fun ConcertTimeline(
    concerts: List<Concert>,
    selected: ConcertDot?,
    onTap: (ConcertDot?) -> Unit,
) {
    val density    = LocalDensity.current
    val pxPerMonth = with(density) { 40.dp.toPx() }
    val dotR       = with(density) { 8.dp.toPx() }
    val timelineY  = with(density) { 130.dp.toPx() }
    val canvasH    = with(density) { 158.dp.toPx() }
    val padL       = with(density) { 16.dp.toPx() }
    val padR       = with(density) { 32.dp.toPx() }
    val nowCal     = remember { Calendar.getInstance() }

    val validConcerts = remember(concerts) {
        concerts.filter { DateUtils.hasFullDate(it.date) }
            .sortedBy { DateUtils.parseDate(it.date) }
    }
    if (validConcerts.isEmpty()) return

    val minYear = remember(validConcerts) {
        validConcerts.minOfOrNull { DateUtils.parseDate(it.date).year } ?: (nowCal.get(Calendar.YEAR) - 1)
    }
    val maxYear = remember(validConcerts) {
        maxOf(
            validConcerts.maxOfOrNull { DateUtils.parseDate(it.date).year } ?: nowCal.get(Calendar.YEAR),
            nowCal.get(Calendar.YEAR),
        )
    }

    fun mToX(yr: Int, mo: Int) = padL + ((yr - minYear) * 12 + mo) * pxPerMonth + pxPerMonth / 2f

    val canvasW = padL + (maxYear - minYear + 1) * 12 * pxPerMonth + padR

    val dots = remember(validConcerts, minYear, pxPerMonth, padL) {
        val byKey = mutableMapOf<String, MutableList<Concert>>()
        validConcerts.forEach { c ->
            val d   = DateUtils.parseDate(c.date)
            val key = "${d.year}-${d.monthValue - 1}"
            byKey.getOrPut(key) { mutableListOf() }.add(c)
        }
        buildList {
            byKey.forEach { (key, items) ->
                val parts = key.split("-")
                val x = mToX(parts[0].toInt(), parts[1].toInt())
                items.forEachIndexed { i, c ->
                    add(ConcertDot(c, x, timelineY - dotR - i * (dotR * 2 + 3f)))
                }
            }
        }
    }

    val yearTicks = remember(minYear, maxYear, padL, pxPerMonth) {
        (minYear..maxYear).map { yr -> Pair(yr, padL + (yr - minYear) * 12 * pxPerMonth) }
    }

    val bandColor = currentAppColors.chartBand
    val gridColor = currentAppColors.chartGrid
    val baseColor = currentAppColors.chartBase
    val labelPaint = remember(density) {
        android.graphics.Paint().apply {
            textSize = with(density) { 11.dp.toPx() }
            color    = android.graphics.Color.parseColor("#BFBAB4")
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }

    val timelineScroll = rememberScrollState()
    LaunchedEffect(timelineScroll.maxValue) {
        if (timelineScroll.maxValue > 0) timelineScroll.scrollTo(timelineScroll.maxValue)
    }
    Box(modifier = Modifier.fillMaxWidth().horizontalScroll(timelineScroll)) {
        Canvas(
            modifier = Modifier
                .width(with(density) { canvasW.toDp() })
                .height(with(density) { canvasH.toDp() })
                .pointerInput(dots, selected) {
                    detectTapGestures { offset ->
                        val hit = dots.firstOrNull { dot ->
                            val dx = offset.x - dot.x; val dy = offset.y - dot.y
                            dx * dx + dy * dy <= (dotR * 2f) * (dotR * 2f)
                        }
                        onTap(if (hit != null && hit.c.id == selected?.c?.id) null else hit)
                    }
                }
        ) {
            yearTicks.forEachIndexed { i, (_, x) ->
                if (i % 2 == 1) drawRect(bandColor, Offset(x, 0f), Size(12 * pxPerMonth, timelineY), alpha = 0.6f)
            }
            yearTicks.forEach { (_, x) ->
                drawLine(gridColor, Offset(x, with(density) { 10.dp.toPx() }), Offset(x, timelineY), strokeWidth = 1.5f)
            }
            drawLine(baseColor, Offset(padL, timelineY), Offset(canvasW - padR, timelineY), strokeWidth = 2f)
            yearTicks.forEach { (yr, x) ->
                drawContext.canvas.nativeCanvas.drawText(
                    "$yr", x + with(density) { 4.dp.toPx() },
                    canvasH - with(density) { 4.dp.toPx() }, labelPaint,
                )
            }
            // Today marker
            val todayX = mToX(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH))
            if (todayX in padL..(canvasW - padR)) {
                drawLine(
                    color = CAccent.copy(alpha = 0.35f),
                    start = Offset(todayX, with(density) { 16.dp.toPx() }),
                    end   = Offset(todayX, timelineY),
                    strokeWidth = 2f,
                    pathEffect  = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                )
            }
            dots.forEach { dot ->
                val isSelected = selected?.c?.id == dot.c.id
                drawCircle(CAccent, dotR, Offset(dot.x, dot.y))
                drawCircle(
                    color  = if (isSelected) CTextPrimary else Color.White,
                    radius = dotR,
                    center = Offset(dot.x, dot.y),
                    style  = Stroke(width = if (isSelected) with(density) { 2.5.dp.toPx() } else with(density) { 2.dp.toPx() }),
                )
            }
        }
    }
}

// ── Edit dialog ───────────────────────────────────────────────────────────────

@Composable
private fun EditConcertDialog(
    concert: Concert,
    bandNames: List<String>,
    venueNames: List<String>,
    attendeeNames: List<String>,
    onSave: (Concert) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var band          by remember { mutableStateOf(concert.band) }
    var tourName      by remember { mutableStateOf(concert.tourName) }
    var venue         by remember { mutableStateOf(concert.venue) }
    var date          by remember { mutableStateOf(concert.date) }
    var notes         by remember { mutableStateOf(concert.notes) }
    var attended      by remember { mutableStateOf(concert.attended) }
    var attendeeChips by remember { mutableStateOf(parseAttendeesKt(concert.attendees)) }
    var attendeeInput by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    val bandSuggestions = remember(band, bandNames) {
        if (band.isBlank()) emptyList()
        else {
            val q = band.lowercase()
            bandNames.filter { it.lowercase().contains(q) && !it.equals(band, ignoreCase = true) }.take(5)
        }
    }
    val attendeeSuggestions = remember(attendeeInput, attendeeNames, attendeeChips) {
        if (attendeeInput.isBlank()) emptyList()
        else {
            val q = attendeeInput.lowercase()
            attendeeNames.filter { it.lowercase().contains(q) && !attendeeChips.contains(it) }.take(4)
        }
    }

    val venueSuggestions = remember(venue, venueNames) {
        if (venue.isBlank()) emptyList()
        else {
            val q = venue.lowercase()
            venueNames.filter { it.lowercase().contains(q) && !it.equals(venue, ignoreCase = true) }.take(5)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (concert.id == 0) "Add Concert" else "Edit Concert") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(band, { band = it }, label = { Text("Band") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
                    if (bandSuggestions.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .border(androidx.compose.foundation.BorderStroke(1.dp, CBorder), RoundedCornerShape(4.dp))
                                .background(CCardBg, RoundedCornerShape(4.dp)),
                        ) {
                            bandSuggestions.forEach { suggestion ->
                                Text(
                                    text = suggestion,
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable { band = suggestion }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    color = CTextPrimary, fontSize = 14.sp,
                                )
                                if (suggestion != bandSuggestions.last()) {
                                    HorizontalDivider(color = CBorder)
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(tourName, { tourName = it }, label = { Text("Tour name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(venue, { venue = it }, label = { Text("Venue") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
                    if (venueSuggestions.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .border(androidx.compose.foundation.BorderStroke(1.dp, CBorder), RoundedCornerShape(4.dp))
                                .background(CCardBg, RoundedCornerShape(4.dp)),
                        ) {
                            venueSuggestions.forEach { suggestion ->
                                Text(
                                    text = suggestion,
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable { venue = suggestion }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    color = CTextPrimary, fontSize = 14.sp,
                                )
                                if (suggestion != venueSuggestions.last()) {
                                    HorizontalDivider(color = CBorder)
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(date, { date = it }, label = { Text("Date (M/D/YYYY, M/D, M/YYYY, or YYYY)") },
                    isError = date.isNotBlank() && !DateUtils.isValidDate(date),
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (attendeeChips.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            attendeeChips.forEach { chip ->
                                Row(
                                    modifier = Modifier
                                        .background(CAccentLight, RoundedCornerShape(16.dp))
                                        .border(BorderStroke(1.dp, CAccent.copy(alpha = 0.3f)), RoundedCornerShape(16.dp))
                                        .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(chip, color = CAccent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    IconButton(
                                        onClick = { attendeeChips = attendeeChips - chip },
                                        modifier = Modifier.size(16.dp),
                                    ) {
                                        Icon(Icons.Default.Close, "Remove", tint = CAccent, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = attendeeInput,
                        onValueChange = { v ->
                            if (v.endsWith(",")) {
                                val name = v.dropLast(1).trim()
                                if (name.isNotEmpty() && !attendeeChips.contains(name)) attendeeChips = attendeeChips + name
                                attendeeInput = ""
                            } else {
                                attendeeInput = v
                            }
                        },
                        label = { Text("Add attendee") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            val name = attendeeInput.trim()
                            if (name.isNotEmpty() && !attendeeChips.contains(name)) attendeeChips = attendeeChips + name
                            attendeeInput = ""
                        }),
                    )
                    if (attendeeSuggestions.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .border(BorderStroke(1.dp, CBorder), RoundedCornerShape(4.dp))
                                .background(CCardBg, RoundedCornerShape(4.dp)),
                        ) {
                            attendeeSuggestions.forEach { suggestion ->
                                Text(
                                    text = suggestion,
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable {
                                            if (!attendeeChips.contains(suggestion)) attendeeChips = attendeeChips + suggestion
                                            attendeeInput = ""
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    color = CTextPrimary, fontSize = 14.sp,
                                )
                                if (suggestion != attendeeSuggestions.last()) HorizontalDivider(color = CBorder)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(attended, { attended = it })
                    Text("Attended")
                }
                if (confirmDelete) {
                    Text("Delete this concert?", color = Color.Red, fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { confirmDelete = false }) { Text("Cancel") }
                        Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Delete") }
                    }
                } else if (concert.id != 0) {
                    TextButton(onClick = { confirmDelete = true }) { Text("Delete", color = Color.Red) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(concert.copy(band = band.trim(), tourName = tourName.trim(), venue = venue.trim(), date = date.trim(), notes = notes.trim(), attended = attended, attendees = joinAttendeesKt(attendeeChips))) },
                enabled = band.isNotBlank() && DateUtils.isValidDate(date),
                colors  = ButtonDefaults.buttonColors(containerColor = CAccent),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun CSectionHeader(title: String, color: Color) {
    Row(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 28.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(3.dp).height(14.dp).background(color))
        Text(title.uppercase(), color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp, modifier = Modifier.padding(start = 10.dp))
    }
}

private const val CHART_DEFAULT_ROWS = 6

@Composable
private fun CHBarChart(title: String, items: List<Pair<String, Int>>, color: Color) {
    if (items.isEmpty()) return
    var showAll by remember { mutableStateOf(false) }
    val displayed = if (showAll || items.size <= CHART_DEFAULT_ROWS) items else items.take(CHART_DEFAULT_ROWS)
    val max = items.maxOf { it.second }
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(title.uppercase(), color = CTextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 8.dp))
        displayed.forEach { (name, count) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TruncatedText(name, color = CTextPrimary, fontSize = 12.sp,
                    modifier = Modifier.width(110.dp))
                Box(
                    modifier = Modifier.weight(1f).height(8.dp)
                        .background(currentAppColors.chartGrid, RoundedCornerShape(2.dp))
                ) {
                    Box(
                        modifier = Modifier.fillMaxHeight()
                            .fillMaxWidth(count.toFloat() / max)
                            .background(color, RoundedCornerShape(2.dp))
                    )
                }
                Text(count.toString(), color = CTextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End, modifier = Modifier.width(24.dp).padding(start = 6.dp))
            }
        }
        if (items.size > CHART_DEFAULT_ROWS) {
            TextButton(
                onClick = { showAll = !showAll },
                modifier = Modifier.padding(top = 2.dp),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
            ) {
                Text(
                    if (showAll) "Show less" else "Show all (${items.size})",
                    color = CAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun CStatChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = CTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1)
        Text(label, color = CTextSec, fontSize = 10.sp, letterSpacing = 0.3.sp, maxLines = 1)
    }
}

@Composable
private fun ConcertLoadingSpinner() {
    Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CAccent, strokeWidth = 3.dp, modifier = Modifier.size(64.dp))
            Icon(Icons.Filled.Stadium, contentDescription = null, tint = CAccent, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun CCenteredText(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
        Text(text, color = CTextSec, fontSize = 18.sp)
    }
}

@Composable
private fun cAddNavColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = CAccent, unselectedIconColor = CAccent,
    selectedTextColor = CAccent, unselectedTextColor = CAccent,
    indicatorColor = CAccentLight,
)

@Composable
private fun cDefaultNavColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = CAccent, unselectedIconColor = Color(0xFFAAAAAA),
    selectedTextColor = CAccent, unselectedTextColor = Color(0xFFAAAAAA),
    indicatorColor = CAccentLight,
)

private fun parseAttendeesKt(s: String): List<String> =
    if (s.isBlank()) emptyList() else s.split(",").map { it.trim() }.filter { it.isNotEmpty() }

private fun joinAttendeesKt(names: List<String>): String = names.joinToString(", ")
