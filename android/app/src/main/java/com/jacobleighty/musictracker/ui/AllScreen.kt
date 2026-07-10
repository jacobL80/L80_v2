package com.jacobleighty.musictracker.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jacobleighty.musictracker.Screen
import com.jacobleighty.musictracker.data.AllItem
import java.time.LocalDate
import java.time.YearMonth

private val AAccent    = Color(0xFFEC6F00)
private val aCardShape = RoundedCornerShape(4.dp)

private val TYPE_SCREENS = mapOf(
    "music"   to Screen.MUSIC,
    "concert" to Screen.CONCERTS,
    "tv"      to Screen.TV_MOVIES,
)

private val TYPE_COLORS = mapOf(
    "music"   to Color(0xFFEC6F00),
    "concert" to Color(0xFF1696B6),
    "tv"      to Color(0xFF7C3AED),
)
private val TYPE_LABELS = mapOf(
    "music"   to "Music",
    "concert" to "Concert",
    "tv"      to "TV / Movie",
)
private val TYPE_ICONS: Map<String, ImageVector> = mapOf(
    "music"   to Icons.Default.MusicNote,
    "concert" to Icons.Default.Stadium,
    "tv"      to Icons.Default.Tv,
)

@Composable
fun AllScreen(vm: AllViewModel = viewModel(), onOpenDrawer: () -> Unit = {}, onNavigate: (Screen) -> Unit = {}) {
    val colors = LocalAppColors.current
    val state by vm.uiState.collectAsState()

    LaunchedEffect(Unit) { vm.loadData() }

    Box(modifier = Modifier.fillMaxSize().background(colors.pageBg)) {
        when {
            state.loading    -> AllLoadingSpinner()
            state.fetchError -> ACenteredText("Could not load data.")
            else             -> AMainContent(state, onOpenDrawer, onNavigate)
        }
    }
}

@Composable
private fun AMainContent(state: AllUiState, onOpenDrawer: () -> Unit, onNavigate: (Screen) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { APageHeader(onOpenDrawer, state.upcoming, onNavigate) }

        val allItems = state.upcoming + state.past
        if (allItems.isNotEmpty()) {
            item { ReleaseCalendarSection(allItems) }
        }

        if (state.upcoming.isEmpty() && state.past.isEmpty()) {
            item {
                val colors = LocalAppColors.current
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Text("No upcoming dates yet.", color = colors.textSecondary, fontSize = 16.sp)
                }
            }
        }

        if (state.upcoming.isNotEmpty()) {
            item { ASectionHeader("Upcoming") }
            items(state.upcoming) { item -> AllItemCard(item) }
        }

        if (state.past.isNotEmpty()) {
            item { ASectionHeader("Recently Past", dim = true) }
            items(state.past.takeLast(10).reversed()) { item -> AllItemCard(item, dimmed = true) }
        }
    }
}

@Composable
private fun APageHeader(onOpenDrawer: () -> Unit, upcoming: List<AllItem>, onNavigate: (Screen) -> Unit) {
    val colors = LocalAppColors.current
    val counts = upcoming.groupBy { it.type }.mapValues { it.value.size }

    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 32.dp, end = 20.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenDrawer, modifier = Modifier.size(28.dp).offset(x = (-4).dp)) {
                Icon(Icons.Default.Menu, "Menu", tint = AAccent, modifier = Modifier.size(20.dp))
            }
            Text("ALL", color = AAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp, modifier = Modifier.padding(start = 4.dp))
        }
        Row(
            modifier = Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Upcoming", color = colors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            if (counts.isNotEmpty()) {
                listOf("music", "concert", "tv").forEach { type ->
                    val count  = counts[type] ?: return@forEach
                    val color  = TYPE_COLORS[type] ?: AAccent
                    val icon   = TYPE_ICONS[type] ?: Icons.Default.Apps
                    val screen = TYPE_SCREENS[type]
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .then(if (screen != null) Modifier.clickable { onNavigate(screen) } else Modifier)
                            .border(BorderStroke(1.5.dp, color), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(icon, contentDescription = type, tint = color, modifier = Modifier.size(13.dp))
                        Text("$count", color = color, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
        }
        Box(modifier = Modifier.padding(top = 14.dp).width(48.dp).height(3.dp).background(AAccent))
        HorizontalDivider(modifier = Modifier.padding(top = 14.dp), color = colors.border)
    }
}

@Composable
private fun ASectionHeader(title: String, dim: Boolean = false, topPadding: androidx.compose.ui.unit.Dp = 24.dp) {
    val colors = LocalAppColors.current
    val color = if (dim) colors.textDim else colors.textPrimary
    Row(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = topPadding, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(3.dp).height(14.dp).background(if (dim) colors.textDim else AAccent))
        Text(title.uppercase(), color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun AllItemCard(item: AllItem, dimmed: Boolean = false) {
    val colors     = LocalAppColors.current
    val context    = LocalContext.current
    val color      = TYPE_COLORS[item.type] ?: AAccent
    val icon       = TYPE_ICONS[item.type] ?: Icons.Default.Apps
    val (month, day, year) = DateUtils.parseParts(item.date)
    val daysUntil  = DateUtils.daysUntil(item.date)
    val imminent   = daysUntil in 0..6
    val cardBg     = if (imminent) color.copy(alpha = 0.08f) else colors.cardBg
    val borderCol  = if (imminent) color else colors.border

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .height(IntrinsicSize.Min)
            .clip(aCardShape)
            .background(cardBg)
            .border(BorderStroke(1.5.dp, borderCol), aCardShape)
            .alpha(if (dimmed) 0.6f else 1f),
    ) {
        Box(modifier = Modifier.width(3.dp).fillMaxHeight().background(color))

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Date block
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(min = 40.dp),
            ) {
                if (month != null) Text(month, color = color, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp, lineHeight = 12.sp)
                if (day != null) Text(day.toString(), color = colors.textPrimary, fontSize = 38.sp,
                    fontWeight = FontWeight.Bold, lineHeight = 38.sp)
                if (year != null) Text(year.toString(), color = colors.textDim, fontSize = 11.sp,
                    letterSpacing = 1.sp, lineHeight = 13.sp)
            }

            // Vertical divider
            Box(modifier = Modifier.width(1.5.dp).height(48.dp).background(colors.border))

            // Category icon
            Icon(
                imageVector = icon,
                contentDescription = TYPE_LABELS[item.type],
                tint = color,
                modifier = Modifier.size(22.dp),
            )

            // Info
            Column(modifier = Modifier.weight(1f)) {
                if (item.url.isNotEmpty()) {
                    TruncatedText(
                        text = "${item.title} ↗",
                        color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp, maxLines = 2,
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                        },
                    )
                } else if (item.type == "tv" && item.showType.isNotEmpty()) {
                    val tc = when (item.showType) { "Movie" -> Color(0xFF0EA5E9); "Anime" -> Color(0xFFF59E0B); else -> Color(0xFF7C3AED) }
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)) { append(item.title) }
                        append("  ")
                        withStyle(SpanStyle(color = tc, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)) { append(item.showType) }
                    }, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                } else {
                    TruncatedText(item.title, color = colors.textPrimary, fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, maxLines = 2)
                }

                if (item.subtitle.isNotEmpty()) {
                    TruncatedText(item.subtitle, color = colors.textSecondary, fontSize = 13.sp,
                        fontStyle = FontStyle.Italic, modifier = Modifier.padding(top = 2.dp))
                }

                if (imminent) {
                    val label = when (daysUntil.toInt()) {
                        0 -> "TODAY"; 1 -> "TOMORROW"; else -> "${daysUntil} DAYS AWAY"
                    }
                    Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
}

@Composable
private fun ReleaseCalendarSection(items: List<AllItem>) {
    val grouped = mutableMapOf<Pair<Int, Int>, MutableList<AllItem>>()
    items.filter { it.date.trim().split("/").size == 3 }.forEach { item ->
        val d   = DateUtils.parseDate(item.date)
        val key = Pair(d.year, d.monthValue - 1) // 0-indexed month
        grouped.getOrPut(key) { mutableListOf() }.add(item)
    }
    val months = grouped.entries.sortedWith(compareBy({ it.key.first }, { it.key.second }))

    Column {
        ASectionHeader("Calendar", topPadding = 6.dp)
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            months.forEach { (key, monthItems) ->
                MonthCalendarCard(year = key.first, month = key.second, monthItems = monthItems)
            }
        }
    }
}

@Composable
private fun MonthCalendarCard(year: Int, month: Int, monthItems: List<AllItem>) {
    val colors     = LocalAppColors.current
    val monthNames = listOf("JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC")
    val dayLabels  = listOf("S","M","T","W","T","F","S")

    val ym          = YearMonth.of(year, month + 1)
    val firstDow    = ym.atDay(1).dayOfWeek.value % 7 // ISO Mon=1..Sun=7 → Sun=0
    val daysInMonth = ym.lengthOfMonth()

    val byDay = mutableMapOf<Int, MutableList<AllItem>>()
    monthItems.forEach { item ->
        val d = DateUtils.parseDate(item.date)
        byDay.getOrPut(d.dayOfMonth) { mutableListOf() }.add(item)
    }

    val today = LocalDate.now()
    val isCurrentMonth = year == today.year && month + 1 == today.monthValue

    val cells = mutableListOf<Int?>()
    repeat(firstDow) { cells.add(null) }
    (1..daysInMonth).forEach { cells.add(it) }
    while (cells.size < 42) cells.add(null)

    Card(
        modifier = Modifier.width(210.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.5.dp, colors.border),
        colors = CardDefaults.cardColors(containerColor = colors.cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
            Text(
                text = "${monthNames[month]} $year",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                dayLabels.forEach { label ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(label, fontSize = 9.sp, color = colors.textDim, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            if (day != null) {
                                val events = byDay[day]
                                val isToday = isCurrentMonth && day == today.dayOfMonth
                                var menuOpen by remember { mutableStateOf(false) }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = if (events != null) Modifier.clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                    ) { menuOpen = true } else Modifier,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .then(if (isToday) Modifier.border(1.5.dp, AAccent, RoundedCornerShape(3.dp)) else Modifier),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            fontSize = 10.sp,
                                            color = if (isToday) AAccent else if (events != null) colors.textPrimary else colors.textDim,
                                            fontWeight = if (isToday || events != null) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                    }
                                    if (events != null) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            modifier = Modifier.padding(top = 2.dp),
                                        ) {
                                            events.take(3).forEach { item ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(TYPE_COLORS[item.type] ?: AAccent),
                                                )
                                            }
                                        }
                                        DropdownMenu(
                                            expanded = menuOpen,
                                            onDismissRequest = { menuOpen = false },
                                        ) {
                                            events.forEachIndexed { idx, item ->
                                                val color = TYPE_COLORS[item.type] ?: AAccent
                                                Column(
                                                    modifier = Modifier
                                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                                        .widthIn(max = 220.dp),
                                                ) {
                                                    Text(item.title, fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold, color = colors.textPrimary, maxLines = 2)
                                                    if (item.subtitle.isNotEmpty()) {
                                                        Text(item.subtitle, fontSize = 12.sp,
                                                            color = colors.textSecondary, fontStyle = FontStyle.Italic,
                                                            modifier = Modifier.padding(top = 2.dp), maxLines = 2)
                                                    }
                                                    Text(
                                                        text = TYPE_LABELS[item.type] ?: "",
                                                        fontSize = 11.sp, color = color,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(top = 4.dp),
                                                    )
                                                }
                                                if (idx < events.lastIndex) HorizontalDivider(color = colors.border)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun AllLoadingSpinner() {
    val colors = LocalAppColors.current
    Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = colors.textSecondary, strokeWidth = 3.dp, modifier = Modifier.size(64.dp))
            Icon(Icons.Filled.Apps, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun ACenteredText(text: String) {
    val colors = LocalAppColors.current
    Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
        Text(text, color = colors.textSecondary, fontSize = 18.sp)
    }
}
