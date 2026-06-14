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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jacobleighty.musictracker.data.AllItem

private val APageBg      = Color(0xFFFAF9F7)
private val ACardBg      = Color(0xFFFFFFFF)
private val AAccent      = Color(0xFFEC6F00)
private val ABorder      = Color(0xFFE8E8E8)
private val ATextPrimary = Color(0xFF1A1A1A)
private val ATextSec     = Color(0xFF888888)
private val ATextDim     = Color(0xFFBBBBBB)
private val aCardShape   = RoundedCornerShape(4.dp)

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
fun AllScreen(vm: AllViewModel = viewModel(), onOpenDrawer: () -> Unit = {}) {
    val state by vm.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(APageBg)) {
        when {
            state.loading    -> AllLoadingSpinner()
            state.fetchError -> ACenteredText("Could not load data.")
            else             -> AMainContent(state, onOpenDrawer)
        }
    }
}

@Composable
private fun AMainContent(state: AllUiState, onOpenDrawer: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { APageHeader(onOpenDrawer, state.upcoming) }

        if (state.upcoming.isEmpty() && state.past.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Text("No upcoming dates yet.", color = ATextSec, fontSize = 16.sp)
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
private fun APageHeader(onOpenDrawer: () -> Unit, upcoming: List<AllItem>) {
    val counts = upcoming.groupBy { it.type }.mapValues { it.value.size }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenDrawer, modifier = Modifier.size(28.dp).offset(x = (-4).dp)) {
                Icon(Icons.Default.Menu, "Menu", tint = AAccent, modifier = Modifier.size(20.dp))
            }
            Text("ALL", color = AAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp, modifier = Modifier.padding(start = 4.dp))
        }
        Text("Upcoming", color = ATextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp))
        Box(modifier = Modifier.padding(top = 14.dp).width(48.dp).height(3.dp).background(AAccent))
        HorizontalDivider(modifier = Modifier.padding(top = 14.dp), color = ABorder)

        if (counts.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("music", "concert", "tv").forEach { type ->
                    val count = counts[type] ?: return@forEach
                    val color = TYPE_COLORS[type] ?: AAccent
                    val label = TYPE_LABELS[type] ?: type
                    val plural = if (count > 1) when (type) { "music" -> "" else -> "s" } else ""
                    Box(
                        modifier = Modifier
                            .border(BorderStroke(1.5.dp, color), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    ) {
                        Text("$count $label$plural", color = color, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ASectionHeader(title: String, dim: Boolean = false) {
    val color = if (dim) Color(0xFFAAAAAA) else ATextPrimary
    Row(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(3.dp).height(14.dp).background(if (dim) Color(0xFFAAAAAA) else AAccent))
        Text(title.uppercase(), color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun AllItemCard(item: AllItem, dimmed: Boolean = false) {
    val context    = LocalContext.current
    val color      = TYPE_COLORS[item.type] ?: AAccent
    val icon       = TYPE_ICONS[item.type] ?: Icons.Default.Apps
    val (month, day, year) = DateUtils.parseParts(item.date)
    val daysUntil  = DateUtils.daysUntil(item.date)
    val imminent   = daysUntil in 0..6
    val cardBg     = if (imminent) color.copy(alpha = 0.08f) else ACardBg
    val borderCol  = if (imminent) color else ABorder

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
                if (day != null) Text(day.toString(), color = ATextPrimary, fontSize = 38.sp,
                    fontWeight = FontWeight.Bold, lineHeight = 38.sp)
                if (year != null) Text(year.toString(), color = ATextDim, fontSize = 11.sp,
                    letterSpacing = 1.sp, lineHeight = 13.sp)
            }

            // Vertical divider
            Box(modifier = Modifier.width(1.5.dp).height(48.dp).background(ABorder))

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
                        color = ATextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp, maxLines = 2,
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                        },
                    )
                } else {
                    TruncatedText(item.title, color = ATextPrimary, fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, maxLines = 2)
                }

                if (item.subtitle.isNotEmpty()) {
                    TruncatedText(item.subtitle, color = ATextSec, fontSize = 13.sp,
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
private fun AllLoadingSpinner() {
    Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ATextSec, strokeWidth = 3.dp, modifier = Modifier.size(64.dp))
            Icon(Icons.Filled.Apps, contentDescription = null, tint = ATextSec, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun ACenteredText(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
        Text(text, color = ATextSec, fontSize = 18.sp)
    }
}
