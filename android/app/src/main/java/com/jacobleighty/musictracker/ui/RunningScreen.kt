package com.jacobleighty.musictracker.ui

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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jacobleighty.musictracker.data.RunningWeek
import java.time.LocalDate
import kotlin.math.roundToInt

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
                onSave    = { miles, date -> vm.addEntry(miles, date) },
                onDismiss = vm::closeAddModal,
                saveError = state.saveError,
            )
        }
    }
}

@Composable
private fun RMainContent(state: RunningUiState, vm: RunningViewModel, onOpenDrawer: () -> Unit = {}) {
    // Weeks shown in chart must be ascending (oldest first)
    val chartWeeks = remember(state.allWeeks) { state.allWeeks.sortedBy { it.weekStart } }
    val tableWeeks = state.filteredWeeks   // already descending from API

    val yearTotal    = tableWeeks.sumOf { it.total.toDouble() }.toFloat()
    val weekCount    = tableWeeks.size

    Column(modifier = Modifier.fillMaxSize()) {
        // Edit mode banner
        if (state.isEditing) {
            Row(
                modifier = Modifier.fillMaxWidth().background(RAccent).statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("EDIT MODE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp)
                TextButton(onClick = vm::exitEditMode) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // Sticky chart
        Box(modifier = Modifier.fillMaxWidth().background(RCardBg).padding(vertical = 8.dp)) {
            RunningBarChart(weeks = chartWeeks)
        }
        HorizontalDivider(color = RBorder)

        // Summary + year filter bar
        Row(
            modifier = Modifier.fillMaxWidth().background(RCardBg)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = onOpenDrawer, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Menu, "Menu", tint = RAccent, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text("%.1f mi".format(yearTotal), color = RTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("$weekCount weeks", color = RTextSec, fontSize = 12.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                YearChip("All", state.selectedYear == null) { vm.setYear(null) }
                state.availableYears.forEach { yr ->
                    YearChip("$yr", state.selectedYear == yr) { vm.setYear(yr) }
                }
            }
        }
        HorizontalDivider(color = RBorder)

        // Add button row
        Row(modifier = Modifier.fillMaxWidth().background(RCardBg).padding(horizontal = 16.dp, vertical = 6.dp)) {
            TextButton(onClick = vm::handleAddNew, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Icon(Icons.Default.Add, null, tint = RAccent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Log run", color = RAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        HorizontalDivider(color = RBorder)

        // Weekly table
        val listState = rememberLazyListState()
        LazyColumn(state = listState, modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(tableWeeks) { week ->
                WeekRow(
                    week      = week,
                    isEditing = state.isEditing,
                    expanded  = state.expandedWeekStart == week.weekStart,
                    onToggle  = { vm.toggleExpanded(week.weekStart) },
                    onDelete  = { id -> vm.deleteEntry(id) },
                )
            }
        }
    }
}

// ── Bar chart ─────────────────────────────────────────────────────────────────

@Composable
private fun RunningBarChart(weeks: List<RunningWeek>) {
    if (weeks.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            Text("No data yet", color = RTextDim, fontSize = 13.sp)
        }
        return
    }

    val density      = LocalDensity.current
    val barW         = with(density) { 20.dp.toPx() }
    val barGap       = with(density) { 4.dp.toPx() }
    val maxBarH      = with(density) { 60.dp.toPx() }
    val padL         = with(density) { 8.dp.toPx() }
    val padR         = with(density) { 24.dp.toPx() }
    val chartH       = with(density) { 80.dp.toPx() }
    val labelAreaH   = with(density) { 16.dp.toPx() }
    val totalH       = chartH + labelAreaH

    val maxTotal     = weeks.maxOfOrNull { it.total } ?: 1f
    val canvasW      = padL + weeks.size * (barW + barGap) + padR

    val scrollState  = rememberScrollState()

    // Auto-scroll to end (most recent) on first composition
    LaunchedEffect(weeks.size) {
        if (weeks.isNotEmpty()) scrollState.scrollTo(scrollState.maxValue)
    }

    // Tooltip state: week index
    var hovered by remember { mutableStateOf<Int?>(null) }
    val hoveredWeek = hovered?.let { weeks.getOrNull(it) }

    val labelPaint = remember(density) {
        android.graphics.Paint().apply {
            textSize = with(density) { 9.dp.toPx() }
            color    = android.graphics.Color.parseColor("#BFBAB4")
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    val milesLabelPaint = remember(density) {
        android.graphics.Paint().apply {
            textSize = with(density) { 8.dp.toPx() }
            color    = android.graphics.Color.parseColor("#888888")
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    Column {
        // Tooltip
        hoveredWeek?.let { week ->
            Row(
                modifier = Modifier.fillMaxWidth().background(RAccentLight)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(week.weekStart, color = RAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("%.1f mi".format(week.total), color = RTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(week.mon, week.tue, week.wed, week.thu, week.fri, week.sat, week.sun)
                        .zip(DAY_LABELS).zip(DAY_COLORS).forEach { (pair, color) ->
                            val (miles, label) = pair
                            if (miles > 0f) Text("${label[0]}:${"%.1f".format(miles)}", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
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

                    // Year label at Jan of each year
                    if (i == 0 || week.weekStart.substring(0, 4) != weeks[i - 1].weekStart.substring(0, 4)) {
                        drawContext.canvas.nativeCanvas.drawText(
                            week.weekStart.substring(0, 4),
                            x + barW / 2f, totalH - with(density) { 2.dp.toPx() }, labelPaint,
                        )
                    }
                    // Miles label on hovered bar
                    if (hovered == i && week.total > 0f) {
                        val topY = chartH - (week.total / maxTotal) * maxBarH
                        drawContext.canvas.nativeCanvas.drawText(
                            "%.0f".format(week.total), x + barW / 2f,
                            topY - with(density) { 2.dp.toPx() }, milesLabelPaint,
                        )
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
) {
    val dayMiles = listOf(week.mon, week.tue, week.wed, week.thu, week.fri, week.sat, week.sun)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(rCardShape).background(RCardBg).border(androidx.compose.foundation.BorderStroke(1.dp, RBorder), rCardShape),
    ) {
        // Week header row
        Row(
            modifier = Modifier.fillMaxWidth()
                .then(if (isEditing) Modifier.clickable { onToggle() } else Modifier)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(week.weekStart, color = RTextSec, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("%.1f mi".format(week.total), color = RTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            // Day dots
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                dayMiles.zip(DAY_COLORS).zip(DAY_LABELS).forEach { (pair, label) ->
                    val (miles, color) = pair
                    if (miles > 0f) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label.substring(0, 1), color = color, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("%.0f".format(miles), color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (isEditing) {
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, tint = RTextDim, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Expanded entries (edit mode only)
        if (isEditing && expanded) {
            HorizontalDivider(color = RBorder)
            week.entries.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(entry.date, color = RTextSec, fontSize = 12.sp)
                        Text("%.1f mi".format(entry.miles), color = RTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
    onSave: (Float, LocalDate) -> Unit,
    onDismiss: () -> Unit,
    saveError: String?,
) {
    var milesText by remember { mutableStateOf("") }
    var dateText  by remember { mutableStateOf(LocalDate.now().toString()) }
    val miles     = milesText.toFloatOrNull()
    val date      = runCatching { LocalDate.parse(dateText) }.getOrNull()

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
                saveError?.let { Text(it, color = Color.Red, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick  = { if (miles != null && date != null) onSave(miles, date) },
                enabled  = miles != null && miles > 0f && date != null,
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
