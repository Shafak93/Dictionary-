package com.example.ui.theme

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

// Bespoke Premium "Sleek Interface" Color Palette (Material 3 Dark/Light Violet & Charcoal)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),       // Sleek Lavender Accent
    onPrimary = Color(0xFF381E72),     // Dark Purple Text on Primary
    secondary = Color(0xFF4F378B),     // Rich Deep Purple Container
    onSecondary = Color(0xFFE6E1E5),   // High Contrast Light Gray / Purple Text
    tertiary = Color(0xFF10B981),      // Security Emerald Accent
    background = Color(0xFF1C1B1F),    // Mysterious Obsidian Canvas
    onBackground = Color(0xFFE6E1E5),  // Sleek Muted Gray text
    surface = Color(0xFF25232A),       // Sophisticated Dark Cocoa-slate Surface
    onSurface = Color(0xFFE6E1E5),     // Surface content text
    outline = Color(0xFF49454F)        // Thin Charcoal border outline
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),       // Solid Premium Purple
    onPrimary = Color.White,
    secondary = Color(0xFF625B71),     // Purple Slate Gray
    onSecondary = Color.White,
    tertiary = Color(0xFF7D5260),      // Dusty Mute Rose
    background = Color(0xFFFEF7FF),    // Warm White Canvas
    onBackground = Color(0xFF1D1B20),  // Deep Obsidian Text
    surface = Color(0xFFF3EDF7),       // Soft Purple-gray Surface
    onSurface = Color(0xFF1D1B20),     // Obsidian text on surface
    outline = Color(0xFFCAD4EA)        // Light outline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic wallpaper coloring (dynamicColor = false) to enforce our pristine bespoke Slate branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
