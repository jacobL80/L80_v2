package com.jacobleighty.musictracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.jacobleighty.musictracker.ui.MusicScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                MusicScreen()
            }
        }
    }
}
