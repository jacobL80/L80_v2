package com.jacobleighty.musictracker.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jacobleighty.musictracker.Constants
import com.jacobleighty.musictracker.notification.WorkerScheduler

private val Accent = Color(0xFFEC6F00)

private data class CategoryRow(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val prefKey: String,
    val category: String,
)

private val CATEGORIES = listOf(
    CategoryRow("Music",      Icons.Default.MusicNote,     Color(0xFFEC6F00), Constants.PREF_NOTIF_HOUR_MUSIC,    WorkerScheduler.CAT_MUSIC),
    CategoryRow("Concerts",   Icons.Default.Stadium,       Color(0xFF1696B6), Constants.PREF_NOTIF_HOUR_CONCERTS, WorkerScheduler.CAT_CONCERTS),
    CategoryRow("TV / Movies",Icons.Default.Tv,            Color(0xFF7C3AED), Constants.PREF_NOTIF_HOUR_TV,       WorkerScheduler.CAT_TV),
)

@Composable
fun SettingsScreen(
    onOpenDrawer: () -> Unit = {},
    isDark: Boolean = false,
    onToggleDark: (Boolean) -> Unit = {},
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(Constants.NOTIF_PREFS, Context.MODE_PRIVATE) }

    val hours = remember {
        mutableStateMapOf<String, Int>().apply {
            CATEGORIES.forEach { cat -> put(cat.prefKey, prefs.getInt(cat.prefKey, Constants.DEFAULT_NOTIF_HOUR)) }
        }
    }

    var pickingCategory by remember { mutableStateOf<CategoryRow?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(colors.pageBg)) {
        LazyColumn {
            item { SettingsPageHeader(onOpenDrawer, colors) }

            // ── Appearance section ─────────────────────────────────────────
            item { SettingsSectionHeader("APPEARANCE", colors) }
            item {
                SettingsToggleRow(
                    label = "Dark Mode",
                    desc  = "Use a dark color scheme",
                    icon  = Icons.Default.DarkMode,
                    checked = isDark,
                    onToggle = onToggleDark,
                    colors = colors,
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = colors.border)
            }

            // ── Notification times section ─────────────────────────────────
            item { SettingsSectionHeader("NOTIFICATION TIMES", colors) }
            item {
                Text(
                    "Choose when to receive notifications on content release dates.",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            items(CATEGORIES) { cat ->
                NotifTimeRow(
                    label = cat.label,
                    icon  = cat.icon,
                    color = cat.color,
                    hour  = hours[cat.prefKey] ?: Constants.DEFAULT_NOTIF_HOUR,
                    colors = colors,
                    onClick = { pickingCategory = cat },
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = colors.border)
            }
        }
    }

    pickingCategory?.let { cat ->
        HourPickerDialog(
            currentHour = hours[cat.prefKey] ?: Constants.DEFAULT_NOTIF_HOUR,
            colors = colors,
            onDismiss = { pickingCategory = null },
            onConfirm = { h ->
                hours[cat.prefKey] = h
                prefs.edit().putInt(cat.prefKey, h).apply()
                WorkerScheduler.rescheduleCategory(context, cat.category, h)
                pickingCategory = null
            },
        )
    }
}

// ── Page header ───────────────────────────────────────────────────────────────

@Composable
private fun SettingsPageHeader(onOpenDrawer: () -> Unit, colors: AppColors) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 32.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenDrawer, modifier = Modifier.size(28.dp).offset(x = (-4).dp)) {
                Icon(Icons.Default.Menu, "Menu", tint = Accent, modifier = Modifier.size(20.dp))
            }
            Text(
                "SETTINGS", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp, modifier = Modifier.padding(start = 4.dp),
            )
        }
        Text(
            "Settings", color = colors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp),
        )
        Box(modifier = Modifier.padding(top = 14.dp).width(48.dp).height(3.dp).background(Accent))
        HorizontalDivider(modifier = Modifier.padding(top = 14.dp), color = colors.border)
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String, colors: AppColors) {
    Row(
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 28.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(3.dp).height(14.dp).background(Accent))
        Text(
            title, color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp, modifier = Modifier.padding(start = 10.dp),
        )
    }
}

// ── Toggle row (dark mode) ────────────────────────────────────────────────────

@Composable
private fun SettingsToggleRow(
    label: String,
    desc: String,
    icon: ImageVector,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    colors: AppColors,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Accent.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = colors.textPrimary, fontSize = 16.sp)
            Text(desc, color = colors.textSecondary, fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = colors.border,
                uncheckedBorderColor = colors.border,
            ),
        )
    }
}

// ── Category row ──────────────────────────────────────────────────────────────

@Composable
private fun NotifTimeRow(
    label: String,
    icon: ImageVector,
    color: Color,
    hour: Int,
    colors: AppColors,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Text(label, color = colors.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(formatHour(hour), color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Icon(Icons.Default.ChevronRight, null, tint = colors.textDim, modifier = Modifier.size(18.dp))
    }
}

// ── Hour picker dialog ────────────────────────────────────────────────────────

@Composable
private fun HourPickerDialog(
    currentHour: Int,
    colors: AppColors,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var selected by remember { mutableStateOf(currentHour) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentHour)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBg,
        title = { Text("Notification Time", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
        text = {
            LazyColumn(state = listState, modifier = Modifier.height(300.dp)) {
                items((0..23).toList()) { h ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = h }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = h == selected,
                            onClick = { selected = h },
                            colors = RadioButtonDefaults.colors(selectedColor = Accent),
                        )
                        Text(formatHour(h), color = colors.textPrimary, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text("Set", color = Accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        },
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatHour(hour: Int): String = when {
    hour == 0  -> "12:00 AM"
    hour < 12  -> "$hour:00 AM"
    hour == 12 -> "12:00 PM"
    else       -> "${hour - 12}:00 PM"
}
