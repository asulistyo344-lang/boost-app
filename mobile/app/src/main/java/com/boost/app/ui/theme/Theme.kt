package com.boost.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Boost Color Tokens ────────────────────────────────────────────
object BoostColors {
    val Bg        = Color(0xFF080810)
    val Surface   = Color(0xFF0F0F1C)
    val Card      = Color(0xFF141426)
    val Card2     = Color(0xFF1A1A30)
    val Border    = Color(0xFF222238)
    val Border2   = Color(0xFF2D2D4A)

    val Orange    = Color(0xFFFF6B35)
    val OrangeDim = Color(0x20FF6B35)
    val Violet    = Color(0xFF7C3AED)
    val VioletDim = Color(0x207C3AED)
    val Green     = Color(0xFF10B981)
    val GreenDim  = Color(0x2010B981)
    val Gold      = Color(0xFFF59E0B)
    val GoldDim   = Color(0x20F59E0B)
    val Red       = Color(0xFFEF4444)
    val RedDim    = Color(0x20EF4444)

    val Text      = Color(0xFFF1F0FF)
    val TextMid   = Color(0xFF9090B8)
    val TextDim   = Color(0xFF4A4A70)
}

private val BoostDarkColorScheme = darkColorScheme(
    primary         = BoostColors.Orange,
    secondary       = BoostColors.Violet,
    background      = BoostColors.Bg,
    surface         = BoostColors.Surface,
    onPrimary       = BoostColors.Text,
    onSecondary     = BoostColors.Text,
    onBackground    = BoostColors.Text,
    onSurface       = BoostColors.Text,
    error           = BoostColors.Red,
    outline         = BoostColors.Border,
)

@Composable
fun BoostTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BoostDarkColorScheme,
        content = content,
    )
}
