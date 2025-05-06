package com.example.ecommerceproject

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE), // Vibrant purple
    secondary = Color(0xFF03DAC6), // Teal
    tertiary = Color(0xFFFF7597), // Pink
    background = Color(0xFFF5F5F5), // Light grey
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC), // Light purple
    secondary = Color(0xFF03DAC6), // Teal
    tertiary = Color(0xFFFF7597), // Pink
    background = Color(0xFF121212), // Dark grey
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun ECommerceProjectTheme(
    darkTheme: Boolean = false, // Simplified: use light theme for Gen Z vibe
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = MaterialTheme.typography, // Use default Material 3 typography
        content = content
    )
}