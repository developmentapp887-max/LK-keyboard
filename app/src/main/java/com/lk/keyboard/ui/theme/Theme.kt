package com.lk.keyboard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LKDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ObsidianBlack,
    primaryContainer = Color(0xFF0A2E3D),
    onPrimaryContainer = NeonCyanSoft,
    secondary = ElectricPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2A1647),
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = CobaltBlue,
    onTertiary = Color.White,
    background = ObsidianBlack,
    onBackground = TextPrimaryDark,
    surface = MidnightDeck,
    onSurface = TextPrimaryDark,
    surfaceVariant = ElevatedSlate,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF283554),
    outlineVariant = Color(0xFF1C243C),
    error = ErrorCoral,
    onError = ObsidianBlack,
)

@Composable
fun LKKeyboardTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LKDarkColorScheme,
        typography = LKTypography,
        content = content,
    )
}
