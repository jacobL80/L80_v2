package com.jacobleighty.musictracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.jacobleighty.musictracker.notification.WorkerScheduler
import androidx.compose.runtime.MutableState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jacobleighty.musictracker.ui.*
import kotlinx.coroutines.launch

enum class Screen {
    ALL, MUSIC, CONCERTS, RUNNING, TV_MOVIES, SETTINGS
}

class MainActivity : ComponentActivity() {

    private val screenState = mutableStateOf(Screen.ALL)

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* proceed regardless — worker fires only if permission granted */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(Constants.NOTIF_PREFS, Context.MODE_PRIVATE)
        val isDarkInitial = prefs.getBoolean(Constants.PREF_DARK_MODE, false)
        currentAppColors = if (isDarkInitial) DarkAppColors else LightAppColors

        enableEdgeToEdge(
            statusBarStyle = if (isDarkInitial)
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else
                SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        WorkerScheduler.schedule(this)

        screenState.value = screenFromIntent(intent)
        setContent {
            val isDark = remember { mutableStateOf(isDarkInitial) }

            val onToggleDark: (Boolean) -> Unit = { dark ->
                isDark.value = dark
                currentAppColors = if (dark) DarkAppColors else LightAppColors
                enableEdgeToEdge(
                    statusBarStyle = if (dark)
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                )
                prefs.edit().putBoolean(Constants.PREF_DARK_MODE, dark).apply()
            }

            val appColors = if (isDark.value) DarkAppColors else LightAppColors

            CompositionLocalProvider(LocalAppColors provides appColors) {
                MaterialTheme(
                    colorScheme = if (isDark.value)
                        darkColorScheme(
                            background   = Color(0xFF121212),
                            surface      = Color(0xFF1C1C1C),
                            primary      = Color(0xFFEC6F00),
                            onBackground = Color(0xFFE5E5E5),
                            onSurface    = Color(0xFFE5E5E5),
                        )
                    else
                        lightColorScheme(
                            background   = Color(0xFFFAF9F7),
                            surface      = Color(0xFFFFFFFF),
                            primary      = Color(0xFFEC6F00),
                            onBackground = Color(0xFF1A1A1A),
                            onSurface    = Color(0xFF1A1A1A),
                        )
                ) {
                    MainApp(screenState, isDark, onToggleDark)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        screenState.value = screenFromIntent(intent)
    }

    private fun screenFromIntent(intent: Intent?): Screen {
        val data: Uri? = intent?.data
        return when {
            data?.scheme == "l80" -> when (data.host) {
                "concerts"  -> Screen.CONCERTS
                "running"   -> Screen.RUNNING
                "tv"        -> Screen.TV_MOVIES
                "all"       -> Screen.ALL
                else        -> Screen.MUSIC
            }
            else -> Screen.ALL
        }
    }
}

@Composable
fun MainApp(
    screenState: MutableState<Screen> = remember { mutableStateOf(Screen.ALL) },
    isDark: MutableState<Boolean> = remember { mutableStateOf(false) },
    onToggleDark: (Boolean) -> Unit = {},
) {
    var currentScreen by screenState
    val drawerState   = rememberDrawerState(DrawerValue.Closed)
    val scope         = rememberCoroutineScope()
    val colors        = LocalAppColors.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavDrawerContent(
                currentScreen = currentScreen,
                onNavigate    = { screen ->
                    currentScreen = screen
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxSize().background(colors.pageBg).statusBarsPadding()) {
            when (currentScreen) {
                Screen.ALL        -> AllScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
                Screen.MUSIC      -> MusicScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
                Screen.CONCERTS   -> ConcertsScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
                Screen.RUNNING    -> RunningScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
                Screen.TV_MOVIES  -> TvMoviesScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
                Screen.SETTINGS   -> SettingsScreen(
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    isDark = isDark.value,
                    onToggleDark = onToggleDark,
                )
            }
        }
    }
}

// ── Navigation drawer ─────────────────────────────────────────────────────────

private data class NavItem(val screen: Screen, val label: String, val icon: ImageVector, val color: Color, val divider: Boolean = false)

private val NAV_ITEMS = listOf(
    NavItem(Screen.ALL,       "All",         Icons.Default.Apps,          Color(0xFFEC6F00)),
    NavItem(Screen.MUSIC,     "Music",       Icons.Default.MusicNote,     Color(0xFFEC6F00)),
    NavItem(Screen.CONCERTS,  "Concerts",    Icons.Default.Stadium,       Color(0xFF1696B6)),
    NavItem(Screen.TV_MOVIES, "TV / Movies", Icons.Default.Tv,            Color(0xFF7C3AED)),
    NavItem(Screen.RUNNING,   "Running",     Icons.Default.DirectionsRun, Color(0xFF47A025), divider = true),
    NavItem(Screen.SETTINGS,  "Settings",    Icons.Default.Settings,      Color(0xFF888888), divider = true),
)

@Composable
private fun NavDrawerContent(currentScreen: Screen, onNavigate: (Screen) -> Unit) {
    val colors = LocalAppColors.current
    ModalDrawerSheet(
        modifier = Modifier.width(260.dp),
        drawerContainerColor = colors.pageBg,
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            "L80",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp,
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = colors.border)

        NAV_ITEMS.forEach { item ->
            if (item.divider) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = colors.border)
            val selected = currentScreen == item.screen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(item.screen) }
                    .background(if (selected) item.color.copy(alpha = 0.1f) else Color.Transparent)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(item.icon, null, tint = if (selected) item.color else colors.textSecondary, modifier = Modifier.size(20.dp))
                Text(
                    item.label,
                    color = if (selected) item.color else colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
                if (selected) {
                    Spacer(Modifier.weight(1f))
                    Box(modifier = Modifier.width(3.dp).height(20.dp).background(item.color, androidx.compose.foundation.shape.RoundedCornerShape(2.dp)))
                }
            }
        }
    }
}
