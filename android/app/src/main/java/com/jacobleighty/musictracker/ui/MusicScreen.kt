package com.jacobleighty.musictracker.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
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
fun MusicScreen(vm: MusicViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(PageBg)) {
        when {
            state.loading    -> CenteredText("Loading…")
            state.fetchError -> CenteredText("Could not load data.")
            else             -> MainContent(state, vm)
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
private fun MainContent(state: MusicUiState, vm: MusicViewModel) {
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
            item { PageHeader(state.view) }
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
private fun PageHeader(view: ViewType) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text("MUSIC", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
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

@Composable
private fun HistorySection(history: List<HistoryEntry>) {
    if (history.isEmpty()) { CenteredText("No history yet."); return }
    Column(modifier = Modifier.padding(horizontal = 14.dp)) {
        history.forEach { entry ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .height(IntrinsicSize.Min).clip(cardShape).background(CardBg)
                    .border(BorderStroke(1.5.dp, BorderColor), cardShape),
            ) {
                Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(Accent))
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(entry.artistName, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    if (entry.albumTitle.isNotEmpty()) {
                        Text(entry.albumTitle, color = TextSecondary, fontSize = 13.sp, fontStyle = FontStyle.Italic)
                    }
                    Text(entry.acquiredAt, color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                }
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
