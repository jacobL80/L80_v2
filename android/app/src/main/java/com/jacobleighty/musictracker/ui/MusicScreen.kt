package com.jacobleighty.musictracker.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jacobleighty.musictracker.data.Artist
import com.jacobleighty.musictracker.data.HistoryEntry
import java.util.Calendar
import kotlin.math.abs

// ── Colors matching the web ───────────────────────────────────────────────────

private val PageBg         = Color(0xFFFAF9F7)
private val CardBg         = Color(0xFFFFFFFF)
private val Accent         = Color(0xFFEC6F00)
private val AccentLight    = Color(0xFFFFF4E6)
private val AccentExpected = Color(0xFF1696B6)
private val ExpectedBg     = Color(0xFFF2FAFD)
private val HiatusBg       = Color(0xFFF5F5F5)
private val HiatusAccent   = Color(0xFFAAAAAA)
private val TextPrimary    = Color(0xFF1A1A1A)
private val TextSecondary  = Color(0xFF888888)
private val TextDim        = Color(0xFFBBBBBB)
private val TextDimmer     = Color(0xFFCCCCCC)
private val BorderColor    = Color(0xFFE8E8E8)

private val cardShape = RoundedCornerShape(4.dp)

private enum class Section { UPCOMING, EXPECTED, WATCHING, HIATUS }

// ── Main screen ───────────────────────────────────────────────────────────────

@Composable
fun MusicScreen(vm: MusicViewModel = viewModel(), onOpenDrawer: () -> Unit = {}) {
    val state by vm.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(PageBg)) {
        when {
            state.loading    -> CenteredText("Loading…")
            state.fetchError -> CenteredText("Could not load data.")
            else             -> MainContent(state, vm, onOpenDrawer)
        }
        if (state.showPasswordDialog) {
            PasswordDialog(onConfirm = vm::enterEditMode, onDismiss = vm::dismissPasswordDialog)
        }
        state.editingArtist?.let { artist ->
            EditArtistDialog(
                artist     = artist,
                allArtists = state.upcoming + state.expected + state.watching + state.hiatus,
                onSave     = vm::saveArtist,
                onDelete   = vm::deleteArtist,
                onDismiss  = vm::closeEdit,
            )
        }
    }
}

@Composable
private fun MainContent(state: MusicUiState, vm: MusicViewModel, onOpenDrawer: () -> Unit = {}) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var pendingScrollSection by remember { mutableStateOf<String?>(null) }

    // Compute the LazyColumn index of each section header based on what sections are present
    val sectionIndices = remember(state.upcoming.size, state.expected.size, state.watching.size, state.hiatus.size) {
        var idx = 1 // index 0 is PageHeader
        buildMap {
            if (state.upcoming.isNotEmpty()) { put("upcoming", idx); idx += 1 + state.upcoming.size }
            if (state.expected.isNotEmpty()) { put("expected", idx); idx += 1 + state.expected.size }
            if (state.watching.isNotEmpty()) { put("watching", idx); idx += 1 + state.watching.size }
            if (state.hiatus.isNotEmpty())   { put("hiatus",   idx); idx += 1 + state.hiatus.size }
        }
    }

    // Execute a pending scroll once the schedule view is visible
    LaunchedEffect(state.view) {
        if (state.view == ViewType.SCHEDULE) {
            val section = pendingScrollSection ?: return@LaunchedEffect
            pendingScrollSection = null
            delay(80)
            sectionIndices[section]?.let { listState.animateScrollToItem(it, scrollOffset = with(density) { (-24).dp.roundToPx() }) }
        }
    }

    val scrollToSection: (String) -> Unit = { section ->
        if (state.view != ViewType.SCHEDULE) {
            pendingScrollSection = section
            vm.setView(ViewType.SCHEDULE)
        } else {
            coroutineScope.launch { sectionIndices[section]?.let { listState.animateScrollToItem(it, scrollOffset = with(density) { (-24).dp.roundToPx() }) } }
        }
    }

    Scaffold(
        containerColor = PageBg,
        topBar = {
            Column {
                if (state.isEditing) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Accent)
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
                    Box(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF3E3))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    ) { Text(it, color = Accent, fontSize = 14.sp) }
                }
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = BorderColor, thickness = 1.dp)
                NavigationBar(containerColor = CardBg, tonalElevation = 0.dp) {
                    NavigationBarItem(
                        selected = false, onClick = vm::handleAddNew,
                        icon  = { Icon(Icons.Default.Add, null) },
                        label = { Text("Add", letterSpacing = 1.sp) },
                        colors = addNavColors(),
                    )
                    if (state.upcoming.isNotEmpty()) {
                        NavigationBarItem(
                            selected = false,
                            onClick  = { scrollToSection("upcoming") },
                            icon     = { Icon(Icons.Default.DateRange, null) },
                            label    = { Text("Upcoming") },
                            colors   = defaultNavColors(),
                        )
                    }
                    if (state.expected.isNotEmpty()) {
                        NavigationBarItem(
                            selected = false,
                            onClick  = { scrollToSection("expected") },
                            icon     = { Icon(Icons.Default.Schedule, null) },
                            label    = { Text("Expected") },
                            colors   = defaultNavColors(),
                        )
                    }
                    if (state.watching.isNotEmpty()) {
                        NavigationBarItem(
                            selected = false,
                            onClick  = { scrollToSection("watching") },
                            icon     = { Icon(Icons.Default.Visibility, null) },
                            label    = { Text("Watching") },
                            colors   = defaultNavColors(),
                        )
                    }
                    NavigationBarItem(
                        selected = state.view == ViewType.HISTORY,
                        onClick  = { vm.setView(if (state.view == ViewType.HISTORY) ViewType.SCHEDULE else ViewType.HISTORY) },
                        icon     = { Icon(Icons.Default.BarChart, null) },
                        label    = { Text("History") },
                        colors   = defaultNavColors(),
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item { PageHeader(state.view, onOpenDrawer) }
            if (state.view == ViewType.HISTORY) {
                item { HistorySection(state.history) }
            } else {
                if (state.upcoming.isNotEmpty()) {
                    item { SectionHeader("Upcoming", Accent) }
                    items(state.upcoming) { a ->
                        UpcomingCard(
                            artist = a, isEditing = state.isEditing,
                            onEdit = vm::openEdit, onAcquire = vm::acquireArtist,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                        )
                    }
                }
                if (state.expected.isNotEmpty()) {
                    item { SectionHeader("Expected", AccentExpected) }
                    items(state.expected) { a ->
                        ArtistRowItem(a, "~${DateUtils.getYear(a.nextRelease)}", state.isEditing, vm::openEdit, Section.EXPECTED)
                    }
                }
                if (state.watching.isNotEmpty()) {
                    item { SectionHeader("Watching", Accent) }
                    items(state.watching) { a ->
                        ArtistRowItem(a, "—", state.isEditing, vm::openEdit, Section.WATCHING)
                    }
                }
                if (state.hiatus.isNotEmpty()) {
                    item { SectionHeader("Hiatus", HiatusAccent) }
                    items(state.hiatus) { a ->
                        ArtistRowItem(a, "—", state.isEditing, vm::openEdit, Section.HIATUS)
                    }
                }
            }
        }
    }
}

// ── Page header ───────────────────────────────────────────────────────────────

@Composable
private fun PageHeader(view: ViewType, onOpenDrawer: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenDrawer, modifier = Modifier.size(28.dp).offset(x = (-4).dp)) {
                Icon(Icons.Default.Menu, "Menu", tint = Accent, modifier = Modifier.size(20.dp))
            }
            Text("MUSIC", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp,
                modifier = Modifier.padding(start = 4.dp))
        }
        Text(
            text = if (view == ViewType.HISTORY) "Release History" else "Release Schedule",
            color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp),
        )
        Box(modifier = Modifier.padding(top = 14.dp).width(48.dp).height(3.dp).background(Accent))
        HorizontalDivider(modifier = Modifier.padding(top = 14.dp), color = BorderColor)
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, color: Color) {
    Row(
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 28.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(3.dp).height(14.dp).background(color))
        Text(title.uppercase(), color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp, modifier = Modifier.padding(start = 10.dp))
    }
}

// ── Upcoming grid ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UpcomingGrid(
    artists: List<Artist>,
    isEditing: Boolean,
    onEdit: (Artist) -> Unit,
    onAcquire: (Artist) -> Unit,
) {
    FlowRow(
        modifier = Modifier.padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 2,
    ) {
        artists.forEach { a ->
            UpcomingCard(
                artist = a, isEditing = isEditing, onEdit = onEdit, onAcquire = onAcquire,
                modifier = Modifier.weight(1f).padding(bottom = 10.dp),
            )
        }
        if (artists.size % 2 != 0) Spacer(Modifier.weight(1f))
    }
}

// ── Upcoming card ─────────────────────────────────────────────────────────────

@Composable
fun UpcomingCard(
    artist: Artist,
    isEditing: Boolean,
    onEdit: (Artist) -> Unit,
    onAcquire: (Artist) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context    = LocalContext.current
    val (month, day, year) = DateUtils.parseParts(artist.nextRelease)
    val daysUntil  = DateUtils.daysUntil(artist.nextRelease)
    val imminent   = daysUntil in 0..6
    val released   = daysUntil < 0

    val cardBg      = if (imminent) AccentLight else CardBg
    val dayColor    = if (imminent) Accent else if (released) TextSecondary else TextPrimary
    val monthColor  = if (released) TextDim else Accent
    val dividerColor= if (imminent) Accent.copy(alpha = 0.3f) else BorderColor
    val borderColor = if (imminent) Accent else BorderColor

    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .clip(cardShape)
            .background(cardBg)
            .border(BorderStroke(1.5.dp, borderColor), cardShape)
            .alpha(if (released) 0.7f else 1f),
    ) {
        // Left orange accent stripe
        Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(Accent))

        Box(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .then(if (isEditing) Modifier.padding(end = 34.dp) else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Date block
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.widthIn(min = 40.dp),
                ) {
                    if (month != null) {
                        Text(month, color = monthColor, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 2.sp, lineHeight = 12.sp)
                    }
                    if (day != null) {
                        Text(day.toString(), color = dayColor, fontSize = 38.sp,
                            fontWeight = FontWeight.Bold, lineHeight = 38.sp)
                    }
                    if (year != null) {
                        Text(year.toString(), color = TextDim, fontSize = 11.sp, letterSpacing = 1.sp,
                            lineHeight = 13.sp)
                    }
                }

                // Vertical divider
                Box(modifier = Modifier.width(1.5.dp).height(48.dp).background(dividerColor))

                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (artist.url.isNotEmpty()) {
                            Text(
                                text = "${artist.name} ↗",
                                color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                                lineHeight = 20.sp,
                                modifier = Modifier.clickable {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(artist.url)))
                                },
                            )
                        } else {
                            Text(artist.name, color = TextPrimary, fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold, lineHeight = 20.sp)
                        }
                        if (artist.incompleteCollection) Text(" ●", color = Accent, fontSize = 9.sp)
                    }
                    if (artist.albumTitle.isNotEmpty()) {
                        Text(artist.albumTitle, color = TextSecondary, fontSize = 14.sp,
                            fontStyle = FontStyle.Italic, maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 1.dp))
                    }
                    if (artist.lastRelease.isNotEmpty()) {
                        Text("LAST ${DateUtils.getYear(artist.lastRelease)}", color = TextDimmer,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(top = 2.dp))
                    }
                    if (imminent) {
                        val label = when (daysUntil.toInt()) {
                            0 -> "TODAY"; 1 -> "TOMORROW"; else -> "${daysUntil} DAYS AWAY"
                        }
                        Text(label, color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }

            // Edit controls
            if (isEditing) {
                Row(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (released || imminent) {
                        OutlinedButton(
                            onClick = { onAcquire(artist) },
                            modifier = Modifier.height(26.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            border = BorderStroke(1.5.dp, Color(0xFF5A9A5A)),
                        ) {
                            Text("✓", color = Color(0xFF5A9A5A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    IconButton(onClick = { onEdit(artist) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, null, tint = TextDimmer, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

// ── Artist row ────────────────────────────────────────────────────────────────

@Composable
private fun ArtistRowItem(
    artist: Artist,
    dateDisplay: String,
    isEditing: Boolean,
    onEdit: (Artist) -> Unit,
    section: Section,
) {
    val context = LocalContext.current
    val showDate = dateDisplay != "—"
    val accentColor = when (section) {
        Section.EXPECTED -> AccentExpected; Section.HIATUS -> HiatusAccent; else -> Accent
    }
    val rowBg = when (section) {
        Section.EXPECTED -> ExpectedBg; Section.HIATUS -> HiatusBg; else -> CardBg
    }

    val yearText = if (!showDate) {
        artist.lastRelease.takeIf { it.isNotEmpty() }?.let { DateUtils.getYear(it) }
    } else {
        artist.nextRelease.takeIf { it.isNotEmpty() }?.let { DateUtils.getYear(it) }
    }
    val monthText = if (showDate && artist.nextRelease.isNotEmpty()) {
        val parts = artist.nextRelease.split("/")
        if (parts.size == 2) DateUtils.MONTHS.getOrNull((parts[0].toIntOrNull() ?: 0) - 1) else null
    } else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .height(IntrinsicSize.Min)
            .clip(cardShape)
            .background(rowBg)
            .border(BorderStroke(1.5.dp, BorderColor), cardShape),
    ) {
        // Left accent stripe
        Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(accentColor))

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 18.dp)
                .then(if (isEditing) Modifier.padding(end = 28.dp) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (yearText != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.widthIn(min = 38.dp),
                ) {
                    if (!showDate && artist.lastRelease.isNotEmpty()) {
                        Text("LAST", color = TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    } else if (monthText != null) {
                        Text(monthText, color = accentColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                    Text(yearText, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
                }
                Box(modifier = Modifier.width(1.5.dp).height(40.dp).background(BorderColor))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (artist.url.isNotEmpty()) {
                        Text("${artist.name} ↗", color = TextPrimary, fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            modifier = Modifier.clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(artist.url)))
                            })
                    } else {
                        Text(artist.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    }
                    if (artist.incompleteCollection) Text(" ●", color = Accent, fontSize = 9.sp)
                }
                val sub = buildString {
                    if (artist.albumTitle.isNotEmpty()) append(artist.albumTitle)
                    if (artist.notes.isNotEmpty()) append(if (isNotEmpty()) " (${artist.notes})" else artist.notes)
                }
                if (sub.isNotEmpty()) {
                    Text(sub, color = TextSecondary, fontSize = 13.sp, fontStyle = FontStyle.Italic,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp))
                }
                if (showDate && artist.lastRelease.isNotEmpty()) {
                    Text("LAST ${DateUtils.getYear(artist.lastRelease)}", color = TextDimmer,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        if (isEditing) {
            Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                IconButton(onClick = { onEdit(artist) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Edit, null, tint = TextDimmer, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

// ── History section ───────────────────────────────────────────────────────────

private val MONTH_SHORT = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

private fun artistColor(name: String): Color {
    var h = 0
    for (c in name) h = c.code + ((h shl 5) - h)
    val hue = ((h % 360) + 360) % 360
    return hslColor(hue.toFloat(), 0.65f, 0.52f)
}

private fun hslColor(h: Float, s: Float, l: Float): Color {
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs(h / 60f % 2f - 1f))
    val m = l - c / 2f
    val (r, g, b) = when {
        h < 60f  -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else     -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}

private fun parseRDate(s: String): Calendar? {
    if (s.isEmpty()) return null
    val p = s.split("/").mapNotNull { it.toIntOrNull() }
    val cal = Calendar.getInstance().also {
        it.set(Calendar.HOUR_OF_DAY, 0); it.set(Calendar.MINUTE, 0)
        it.set(Calendar.SECOND, 0); it.set(Calendar.MILLISECOND, 0)
    }
    return when (p.size) {
        3    -> { cal.set(p[2], p[0] - 1, p[1]); cal }
        2    -> { cal.set(p[1], p[0] - 1, 15); cal }
        1    -> { cal.set(p[0], 6, 1); cal }
        else -> null
    }
}

private fun fmtReleaseDate(s: String): String {
    val p = s.split("/").mapNotNull { it.toIntOrNull() }
    return when (p.size) {
        3    -> "${MONTH_SHORT.getOrNull(p[0] - 1)} ${p[1]}, ${p[2]}"
        2    -> "${MONTH_SHORT.getOrNull(p[0] - 1)} ${p[1]}"
        1    -> "${p[0]}"
        else -> s
    }
}

private fun fmtAcquiredAt(s: String): String {
    val p = s.split("-").mapNotNull { it.toIntOrNull() }
    if (p.size < 3) return s
    return "${MONTH_SHORT.getOrNull(p[1] - 1)} ${p[2]}, ${p[0]}"
}

private data class RichEntry(val e: HistoryEntry, val cal: Calendar)
private data class DotInfo(val e: HistoryEntry, val x: Float, val y: Float)

private data class HStats(
    val total: Int,
    val thisYear: Int,
    val artists: Int,
    val bestYear: Pair<Int, Int>?,
    val peakMonth: Pair<String, Int>?,
    val allYears: List<Pair<Int, Int>>,
    val maxYrCount: Int,
    val avgGap: Int?,
    val lastLoggedDays: Int?,
)

private fun computeHStats(entries: List<RichEntry>): HStats {
    val now      = Calendar.getInstance()
    val nowYear  = now.get(Calendar.YEAR)
    val byYear   = mutableMapOf<Int, Int>()
    val byMonth  = mutableMapOf<String, Int>()
    val artistSet = mutableSetOf<String>()
    entries.forEach { r ->
        artistSet += r.e.artistName
        val yr = r.cal.get(Calendar.YEAR)
        byYear[yr] = (byYear[yr] ?: 0) + 1
        val mk = "${MONTH_SHORT[r.cal.get(Calendar.MONTH)]} $yr"
        byMonth[mk] = (byMonth[mk] ?: 0) + 1
    }
    var gapSum = 0L; var gapCount = 0
    for (i in 1 until entries.size) {
        gapSum += (entries[i].cal.timeInMillis - entries[i - 1].cal.timeInMillis) / 86_400_000L
        gapCount++
    }
    val lastLoggedDays = entries.maxByOrNull { it.cal.timeInMillis }
        ?.e?.acquiredAt
        ?.takeIf { it.isNotEmpty() }
        ?.let { raw ->
            runCatching {
                val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val d = fmt.parse(raw.take(10)) ?: return@let null
                ((now.timeInMillis - d.time) / 86_400_000L).toInt()
            }.getOrNull()
        }
    val allYears = byYear.entries.sortedBy { it.key }.map { Pair(it.key, it.value) }
    return HStats(
        total         = entries.size,
        thisYear      = entries.count { it.cal.get(Calendar.YEAR) == nowYear },
        artists       = artistSet.size,
        bestYear      = byYear.entries.maxByOrNull { it.value }?.let { Pair(it.key, it.value) },
        peakMonth     = byMonth.entries.maxByOrNull { it.value }?.let { Pair(it.key, it.value) },
        allYears      = allYears,
        maxYrCount    = allYears.maxOfOrNull { it.second } ?: 1,
        avgGap        = if (gapCount > 0) (gapSum / gapCount).toInt() else null,
        lastLoggedDays = lastLoggedDays,
    )
}

@Composable
private fun HistorySection(history: List<HistoryEntry>) {
    val entries = remember(history) {
        history.mapNotNull { e -> parseRDate(e.releaseDate)?.let { RichEntry(e, it) } }
            .sortedBy { it.cal.timeInMillis }
    }
    if (entries.isEmpty()) { CenteredText("No history yet."); return }

    val stats    = remember(entries) { computeHStats(entries) }
    var selected by remember { mutableStateOf<DotInfo?>(null) }

    Column {
        // Stats strip
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                StatChip(stats.total.toString(), "logged")
                StatChip(stats.thisYear.toString(), "this year")
                StatChip(stats.artists.toString(), "artists")
                stats.lastLoggedDays?.let { StatChip(if (it == 0) "today" else "${it}d", "last logged") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                stats.bestYear?.let  { (yr, cnt) -> StatChip("$yr", "best year · $cnt") }
                stats.peakMonth?.let { (mk, cnt) -> StatChip(mk, "busiest month · $cnt") }
                stats.avgGap?.let    { StatChip("${it}d", "avg gap") }
            }
        }

        HorizontalDivider(color = BorderColor, modifier = Modifier.padding(horizontal = 14.dp))

        // Year bar chart
        if (stats.allYears.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                stats.allYears.forEach { (yr, count) ->
                    val barH = maxOf((count.toFloat() / stats.maxYrCount * 52).toInt(), 4).dp
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$count", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp, bottom = 4.dp)
                                .width(28.dp)
                                .height(barH)
                                .background(Accent, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        )
                        Text("$yr", color = TextDim, fontSize = 9.sp)
                    }
                }
            }
            HorizontalDivider(color = BorderColor, modifier = Modifier.padding(horizontal = 14.dp))
        }

        // Dot timeline
        Spacer(Modifier.height(8.dp))
        HistoryTimeline(entries = entries, selected = selected, onTap = { selected = it })

        // Selected dot detail card
        selected?.let { dot ->
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .clip(cardShape)
                    .background(CardBg)
                    .border(BorderStroke(1.5.dp, BorderColor), cardShape)
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(artistColor(dot.e.artistName)))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(dot.e.artistName, color = artistColor(dot.e.artistName),
                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (dot.e.albumTitle.isNotEmpty()) {
                        Text(dot.e.albumTitle, color = TextSecondary, fontSize = 13.sp,
                            fontStyle = FontStyle.Italic)
                    }
                    if (dot.e.releaseDate.isNotEmpty()) {
                        Text(fmtReleaseDate(dot.e.releaseDate), color = TextDim, fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp))
                    }
                }
                IconButton(onClick = { selected = null }, modifier = Modifier.size(36.dp)) {
                    Text("×", color = TextSecondary, fontSize = 20.sp)
                }
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String, valueColor: Color = TextPrimary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1)
        Text(label, color = TextSecondary, fontSize = 10.sp, letterSpacing = 0.3.sp, maxLines = 1)
    }
}

@Composable
private fun HistoryTimeline(
    entries: List<RichEntry>,
    selected: DotInfo?,
    onTap: (DotInfo?) -> Unit,
) {
    val density = LocalDensity.current

    val pxPerMonth = with(density) { 40.dp.toPx() }
    val dotR       = with(density) { 8.dp.toPx() }
    val timelineY  = with(density) { 130.dp.toPx() }
    val canvasH    = with(density) { 158.dp.toPx() }
    val padL       = with(density) { 16.dp.toPx() }
    val padR       = with(density) { 32.dp.toPx() }

    val nowCal = remember { Calendar.getInstance() }

    val minYear = remember(entries) {
        entries.minOfOrNull { it.cal.get(Calendar.YEAR) } ?: (nowCal.get(Calendar.YEAR) - 1)
    }
    val maxYear = remember(entries) {
        maxOf(
            entries.maxOfOrNull { it.cal.get(Calendar.YEAR) } ?: nowCal.get(Calendar.YEAR),
            nowCal.get(Calendar.YEAR),
        )
    }

    fun mToX(yr: Int, mo: Int) = padL + ((yr - minYear) * 12 + mo) * pxPerMonth + pxPerMonth / 2f

    val canvasW = padL + (maxYear - minYear + 2) * 12 * pxPerMonth + padR

    val dots = remember(entries, minYear, pxPerMonth, padL, dotR, timelineY) {
        val byKey = mutableMapOf<String, MutableList<RichEntry>>()
        entries.forEach { r ->
            val key = "${r.cal.get(Calendar.YEAR)}-${r.cal.get(Calendar.MONTH)}"
            byKey.getOrPut(key) { mutableListOf() }.add(r)
        }
        buildList {
            byKey.forEach { (key, items) ->
                val parts = key.split("-")
                val x = mToX(parts[0].toInt(), parts[1].toInt())
                items.forEachIndexed { i, r ->
                    add(DotInfo(r.e, x, timelineY - dotR - i * (dotR * 2 + 3f)))
                }
            }
        }
    }

    val yearTicks = remember(minYear, maxYear, padL, pxPerMonth) {
        (minYear..maxYear + 1).map { yr -> Pair(yr, padL + (yr - minYear) * 12 * pxPerMonth) }
    }

    val bandColor = Color(0xFFF5F3F0)
    val gridColor = Color(0xFFE8E3DE)
    val baseColor = Color(0xFFCEC9C3)

    val labelPaint = remember(density) {
        android.graphics.Paint().apply {
            textSize    = with(density) { 11.dp.toPx() }
            color       = android.graphics.Color.parseColor("#BFBAB4")
            typeface    = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }
    val nowTextPaint = remember(density) {
        android.graphics.Paint().apply {
            textSize    = with(density) { 9.dp.toPx() }
            color       = android.graphics.Color.argb(115, 236, 111, 0)
            typeface    = android.graphics.Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }

    Box(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
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
                        onTap(if (hit != null && hit.e.id == selected?.e?.id) null else hit)
                    }
                }
        ) {
            val topPad = with(density) { 10.dp.toPx() }

            // Alternating year bands
            yearTicks.forEachIndexed { i, (_, x) ->
                if (i % 2 == 1) {
                    drawRect(bandColor, Offset(x, 0f), Size(12 * pxPerMonth, timelineY), alpha = 0.6f)
                }
            }
            // Year grid lines
            yearTicks.forEach { (_, x) ->
                drawLine(gridColor, Offset(x, topPad), Offset(x, timelineY), strokeWidth = 1.5f)
            }
            // Baseline
            drawLine(baseColor, Offset(padL, timelineY), Offset(canvasW - padR, timelineY), strokeWidth = 2f)
            // Year labels
            yearTicks.forEach { (yr, x) ->
                drawContext.canvas.nativeCanvas.drawText(
                    "$yr", x + with(density) { 4.dp.toPx() },
                    canvasH - with(density) { 4.dp.toPx() }, labelPaint,
                )
            }
            // Today marker (dashed)
            val todayX = mToX(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH))
            if (todayX in padL..(canvasW - padR)) {
                drawLine(
                    color       = Accent.copy(alpha = 0.3f),
                    start       = Offset(todayX, with(density) { 16.dp.toPx() }),
                    end         = Offset(todayX, timelineY),
                    strokeWidth = 2f,
                    pathEffect  = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "NOW", todayX + with(density) { 3.dp.toPx() },
                    with(density) { 28.dp.toPx() }, nowTextPaint,
                )
            }
            // Release dots
            dots.forEach { dot ->
                val isSelected = selected?.e?.id == dot.e.id
                drawCircle(artistColor(dot.e.artistName), dotR, Offset(dot.x, dot.y))
                drawCircle(
                    color  = if (isSelected) Color(0xFF1A1A1A) else Color.White,
                    radius = dotR,
                    center = Offset(dot.x, dot.y),
                    style  = Stroke(width = if (isSelected) with(density) { 2.5.dp.toPx() } else with(density) { 2.dp.toPx() }),
                )
            }
        }
    }
}

// ── Nav helpers ───────────────────────────────────────────────────────────────

@Composable
private fun addNavColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Accent, unselectedIconColor = Accent,
    selectedTextColor = Accent, unselectedTextColor = Accent,
    indicatorColor = AccentLight,
)

@Composable
private fun defaultNavColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Accent, unselectedIconColor = Color(0xFFAAAAAA),
    selectedTextColor = Accent, unselectedTextColor = Color(0xFFAAAAAA),
    indicatorColor = AccentLight,
)

// ── Misc ──────────────────────────────────────────────────────────────────────

@Composable
private fun CenteredText(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
        Text(text, color = TextSecondary, fontSize = 18.sp)
    }
}
