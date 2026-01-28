package com.example.newtv.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val NostalgiaColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFFF4FD8),
    onPrimary = Color(0xFF0B0B14),
    secondary = Color(0xFF33F6FF),
    onSecondary = Color(0xFF0B0B14),
    tertiary = Color(0xFFFFE66D),
    onTertiary = Color(0xFF0B0B14),
    background = Color(0xFF0B0B14),
    onBackground = Color(0xFFFDFCFB),
    surface = Color(0xFF1A1A2E),
    onSurface = Color(0xFFFDFCFB),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF0B0B14)
)

@Composable
fun NewTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NostalgiaColorScheme,
        content = content
    )
}
