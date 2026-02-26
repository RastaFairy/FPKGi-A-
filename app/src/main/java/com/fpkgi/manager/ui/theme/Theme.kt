package com.fpkgi.manager.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Paleta PS4 / FPKGi ───────────────────────────────────────────────────
val CyberBlue       = Color(0xFF00D4FF)
val NavyDark        = Color(0xFF1A1A2E)
val NavyMid         = Color(0xFF16213E)
val NavyDeep        = Color(0xFF0F3460)
val DarkBackground  = Color(0xFF0F0F1A)
val NeonGreen       = Color(0xFF00FF99)
val NeonOrange      = Color(0xFFFF9944)
val ErrorRed        = Color(0xFFFF4444)
val GoldYellow      = Color(0xFFFFD700)
val TextPrimary     = Color(0xFFE0E0E0)
val TextSecondary   = Color(0xFF888888)
val TextMuted       = Color(0xFF555577)
val SurfaceCard     = Color(0xFF16213E)
val SurfaceInput    = Color(0xFF0F3460)

private val DarkColorScheme = darkColorScheme(
    primary          = CyberBlue,
    onPrimary        = NavyDark,
    primaryContainer = NavyDeep,
    onPrimaryContainer = CyberBlue,
    secondary        = NeonGreen,
    onSecondary      = NavyDark,
    secondaryContainer = Color(0xFF0A2A0A),
    onSecondaryContainer = NeonGreen,
    tertiary         = NeonOrange,
    onTertiary       = NavyDark,
    background       = DarkBackground,
    onBackground     = TextPrimary,
    surface          = NavyMid,
    onSurface        = TextPrimary,
    surfaceVariant   = NavyDeep,
    onSurfaceVariant = TextSecondary,
    error            = ErrorRed,
    onError          = Color.White,
    outline          = TextMuted,
    outlineVariant   = Color(0xFF222244)
)

@Composable
fun FPKGiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
