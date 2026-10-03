package com.example.saturdayalarm.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B4BC4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E1FF),
    onPrimaryContainer = Color(0xFF1B0E61),
    secondary = Color(0xFF52604F),
    secondaryContainer = Color(0xFFD5E8CF),
    background = Color(0xFFF8F7FC),
    surface = Color(0xFFF8F7FC),
    surfaceVariant = Color(0xFFE9E5F0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC8BEFF),
    onPrimary = Color(0xFF302477),
    primaryContainer = Color(0xFF45399A),
    onPrimaryContainer = Color(0xFFE7E1FF),
    secondary = Color(0xFFBACBB4),
    secondaryContainer = Color(0xFF3B4939),
    background = Color(0xFF121218),
    surface = Color(0xFF121218),
    surfaceVariant = Color(0xFF48454F),
)

@Composable
fun SaturdayAlarmTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
