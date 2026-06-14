package com.jacobleighty.musictracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
    ALL, MUSIC, CONCERTS, RUNNING, TV_MOVIES
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialScreen = screenFromIntent(intent)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    background   = Color(0xFFFAF9F7),
                    surface      = Color(0xFFFFFFFF),
                    primary      = Color(0xFFEC6F00),
                    onBackground = Color(0xFF1A1A1A),
                    onSurface    = Color(0xFF1A1A1A),
                )
            ) {
                MainApp(initialScreen)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
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
fun MainApp(initialScreen: Screen = Screen.MUSIC) {
    var currentScreen by remember { mutableStateOf(initialScreen) }
    val drawerState   = rememberDrawerState(DrawerValue.Closed)
    val scope         = rememberCoroutineScope()

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
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                Screen.ALL        -> AllScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
                Screen.MUSIC      -> MusicScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
                Screen.CONCERTS   -> ConcertsScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
                Screen.RUNNING    -> RunningScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
                Screen.TV_MOVIES  -> TvMoviesScreen(onOpenDrawer = { scope.launch { drawerState.open() } })
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
)

@Composable
private fun NavDrawerContent(currentScreen: Screen, onNavigate: (Screen) -> Unit) {
    ModalDrawerSheet(
        modifier = Modifier.width(260.dp),
        drawerContainerColor = Color(0xFFFAF9F7),
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            "L80",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            color = Color(0xFF1A1A1A), fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp,
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), color = Color(0xFFE8E8E8))

        NAV_ITEMS.forEach { item ->
            if (item.divider) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = Color(0xFFE8E8E8))
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
                Icon(item.icon, null, tint = if (selected) item.color else Color(0xFF888888), modifier = Modifier.size(20.dp))
                Text(
                    item.label,
                    color = if (selected) item.color else Color(0xFF1A1A1A),
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
