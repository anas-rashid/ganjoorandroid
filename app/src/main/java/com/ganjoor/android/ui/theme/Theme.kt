package com.ganjoor.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ganjoor.android.ui.Language
import com.ganjoor.android.ui.ThemeMode

// Persian tilework: turquoise with saffron accents.
private val LightScheme = lightColorScheme(
    primary = Color(0xFF00696E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF1F6),
    onPrimaryContainer = Color(0xFF002021),
    secondary = Color(0xFF8B5000),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCBE),
    onSecondaryContainer = Color(0xFF2C1600),
    background = Color(0xFFFAFDFC),
    onBackground = Color(0xFF191C1C),
    surface = Color(0xFFFAFDFC),
    onSurface = Color(0xFF191C1C),
    surfaceVariant = Color(0xFFDAE4E4),
    onSurfaceVariant = Color(0xFF3F4949),
    surfaceBright = Color(0xFFFAFDFC),
    surfaceDim = Color(0xFFD9DDDC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F7F6),
    surfaceContainer = Color(0xFFEEF1F0),
    surfaceContainerHigh = Color(0xFFE8ECEB),
    surfaceContainerHighest = Color(0xFFE2E6E5),
    outline = Color(0xFF6F7979),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF80D4DA),
    onPrimary = Color(0xFF003739),
    primaryContainer = Color(0xFF004F53),
    onPrimaryContainer = Color(0xFF9CF1F6),
    secondary = Color(0xFFFFB873),
    onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF693C00),
    onSecondaryContainer = Color(0xFFFFDCBE),
    background = Color(0xFF191C1C),
    onBackground = Color(0xFFE0E3E2),
    surface = Color(0xFF191C1C),
    onSurface = Color(0xFFE0E3E2),
    surfaceVariant = Color(0xFF3F4949),
    onSurfaceVariant = Color(0xFFBEC8C8),
    surfaceBright = Color(0xFF3F4242),
    surfaceDim = Color(0xFF191C1C),
    surfaceContainerLowest = Color(0xFF141616),
    surfaceContainerLow = Color(0xFF212424),
    surfaceContainer = Color(0xFF252828),
    surfaceContainerHigh = Color(0xFF2F3333),
    surfaceContainerHighest = Color(0xFF3A3E3E),
    outline = Color(0xFF899393),
)

/**
 * True black, for OLED panels: a black pixel is an unlit pixel, so a night-time reading session
 * costs noticeably less battery than the regular dark theme's dark grey. Surfaces step up in
 * near-black greys so cards and sheets stay distinguishable without lighting the whole screen.
 */
private val BlackScheme = darkColorScheme(
    primary = Color(0xFF80D4DA),
    onPrimary = Color(0xFF00363A),
    primaryContainer = Color(0xFF004F53),
    onPrimaryContainer = Color(0xFF9CF1F6),
    secondary = Color(0xFFFFB873),
    onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF693C00),
    onSecondaryContainer = Color(0xFFFFDCBE),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF1C1C1C),
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF6E6E6E),
    surfaceBright = Color(0xFF262626),
    surfaceDim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF101010),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF242424),
)

// Aged paper, for long reading sessions.
private val SepiaScheme = lightColorScheme(
    primary = Color(0xFF7A4E24),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8D3B5),
    onPrimaryContainer = Color(0xFF2B1700),
    secondary = Color(0xFF6B5B3E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEADFC4),
    onSecondaryContainer = Color(0xFF231A05),
    background = Color(0xFFF6ECD9),
    onBackground = Color(0xFF3A2E1E),
    surface = Color(0xFFF1E5CE),
    onSurface = Color(0xFF3A2E1E),
    surfaceVariant = Color(0xFFE5D8BC),
    onSurfaceVariant = Color(0xFF5A4C35),
    surfaceBright = Color(0xFFF8F0E0),
    surfaceDim = Color(0xFFDBD0B8),
    surfaceContainerLowest = Color(0xFFFFFAF0),
    surfaceContainerLow = Color(0xFFF2E8D4),
    surfaceContainer = Color(0xFFECE1CA),
    surfaceContainerHigh = Color(0xFFE6DAC0),
    surfaceContainerHighest = Color(0xFFE0D3B6),
    outline = Color(0xFF8C7A5C),
)

// Warm low-light reading: dark, but without the blue cast of the regular dark theme.
private val SepiaDarkScheme = darkColorScheme(
    primary = Color(0xFFD9A96C),
    onPrimary = Color(0xFF3A2510),
    primaryContainer = Color(0xFF53391C),
    onPrimaryContainer = Color(0xFFF5DEBE),
    secondary = Color(0xFFCFBE98),
    onSecondary = Color(0xFF362C14),
    secondaryContainer = Color(0xFF4D4228),
    onSecondaryContainer = Color(0xFFEBDCB7),
    background = Color(0xFF1C1710),
    onBackground = Color(0xFFE6DAC4),
    surface = Color(0xFF241D14),
    onSurface = Color(0xFFE6DAC4),
    surfaceVariant = Color(0xFF3B3225),
    onSurfaceVariant = Color(0xFFCCBFA6),
    surfaceBright = Color(0xFF433A2D),
    surfaceDim = Color(0xFF1C1710),
    surfaceContainerLowest = Color(0xFF17120C),
    surfaceContainerLow = Color(0xFF241D14),
    surfaceContainer = Color(0xFF2A2218),
    surfaceContainerHigh = Color(0xFF352C21),
    surfaceContainerHighest = Color(0xFF40372B),
    outline = Color(0xFF968A72),
)

@Composable
fun GanjoorTheme(mode: ThemeMode, language: Language, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light, ThemeMode.Sepia -> false
        ThemeMode.Dark, ThemeMode.SepiaDark, ThemeMode.Black -> true
    }
    val scheme = when (mode) {
        ThemeMode.Sepia -> SepiaScheme
        ThemeMode.SepiaDark -> SepiaDarkScheme
        ThemeMode.Black -> BlackScheme
        else -> if (dark) DarkScheme else LightScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        // Latin interface text gets a Latin reading serif; the RTL interfaces stay on naskh.
        typography = if (language == Language.En) LibronTypography else NaskhTypography,
        content = content,
    )
}
