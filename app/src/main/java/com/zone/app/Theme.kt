package com.zone.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object ZoneColors {
    val background = Color(0xFFF5F5F7)
    val backgroundDark = Color(0xFF000000)
    val card = Color(0xFFFFFFFF)
    val cardDark = Color(0xFF1C1C1E)
    val cardSoft = Color(0xFFEFEFF4)
    val cardSoftDark = Color(0xFF2C2C2E)
    val text = Color(0xFF1C1C1E)
    val textDark = Color(0xFFF2F2F7)
    val secondary = Color(0xFF6C6C70)
    val secondaryDark = Color(0xFF98989D)
    val separator = Color(0xFFE5E5EA)
    val separatorDark = Color(0xFF38383A)
    val blue = Color(0xFF007AFF)
    val green = Color(0xFF34C759)
    val orange = Color(0xFFFF9500)
    val red = Color(0xFFFF3B30)
    val purple = Color(0xFFAF52DE)
    val teal = Color(0xFF5AC8FA)
    val yellow = Color(0xFFFFCC00)
}

private val zoneTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
)

@Composable
fun ZoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = ZoneColors.blue,
            onPrimary = Color.White,
            primaryContainer = ZoneColors.cardSoftDark,
            onPrimaryContainer = ZoneColors.textDark,
            secondary = ZoneColors.teal,
            background = ZoneColors.backgroundDark,
            onBackground = ZoneColors.textDark,
            surface = ZoneColors.cardDark,
            onSurface = ZoneColors.textDark,
            surfaceVariant = ZoneColors.cardSoftDark,
            onSurfaceVariant = ZoneColors.secondaryDark,
            outline = ZoneColors.separatorDark,
            error = ZoneColors.red
        )
    } else {
        lightColorScheme(
            primary = ZoneColors.blue,
            onPrimary = Color.White,
            primaryContainer = ZoneColors.cardSoft,
            onPrimaryContainer = ZoneColors.text,
            secondary = ZoneColors.teal,
            background = ZoneColors.background,
            onBackground = ZoneColors.text,
            surface = ZoneColors.card,
            onSurface = ZoneColors.text,
            surfaceVariant = ZoneColors.cardSoft,
            onSurfaceVariant = ZoneColors.secondary,
            outline = ZoneColors.separator,
            error = ZoneColors.red
        )
    }
    MaterialTheme(
        colorScheme = colors,
        typography = zoneTypography,
        content = content
    )
}

@Composable
fun resolvedDark(): Boolean {
    val system = isSystemInDarkTheme()
    return when (Zone.settings.value.theme) {
        "light" -> false
        "dark" -> true
        else -> system
    }
}