package com.ganjoor.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.ganjoor.android.R
import com.ganjoor.android.ui.ReadingFont

/**
 * Both are variable fonts shipped at their default weight; Android synthesises bold. Variable
 * axes would need API 26+ (minSdk is 24), and a poetry reader has no use for weight animation.
 */
val Naskh = FontFamily(Font(R.font.noto_naskh_arabic))
val Nastaliq = FontFamily(Font(R.font.noto_nastaliq_urdu))

// Noto Naskh Arabic covers Persian, Urdu and Arabic plus Latin, so it can carry the whole UI.
private val default = Typography()
val GanjoorTypography = Typography(
    displayLarge = default.displayLarge.copy(fontFamily = Naskh),
    displayMedium = default.displayMedium.copy(fontFamily = Naskh),
    displaySmall = default.displaySmall.copy(fontFamily = Naskh),
    headlineLarge = default.headlineLarge.copy(fontFamily = Naskh),
    headlineMedium = default.headlineMedium.copy(fontFamily = Naskh),
    headlineSmall = default.headlineSmall.copy(fontFamily = Naskh),
    titleLarge = default.titleLarge.copy(fontFamily = Naskh),
    titleMedium = default.titleMedium.copy(fontFamily = Naskh),
    titleSmall = default.titleSmall.copy(fontFamily = Naskh),
    bodyLarge = default.bodyLarge.copy(fontFamily = Naskh),
    bodyMedium = default.bodyMedium.copy(fontFamily = Naskh),
    bodySmall = default.bodySmall.copy(fontFamily = Naskh),
    labelLarge = default.labelLarge.copy(fontFamily = Naskh),
    labelMedium = default.labelMedium.copy(fontFamily = Naskh),
    labelSmall = default.labelSmall.copy(fontFamily = Naskh),
)

/**
 * Style for the poem text itself. Nastaliq stacks its glyphs diagonally and has very deep
 * descenders, so it needs far more leading than naskh and the legacy font padding kept on,
 * otherwise the swashes clip against the line above. Tune the two multipliers by eye if you
 * swap either font file.
 */
fun readingStyle(font: ReadingFont, sizeSp: Float): TextStyle {
    val size: TextUnit = sizeSp.sp
    val leading = if (font == ReadingFont.Nastaliq) 2.4f else 1.8f
    return TextStyle(
        fontFamily = if (font == ReadingFont.Nastaliq) Nastaliq else Naskh,
        fontSize = size,
        lineHeight = size * leading,
        platformStyle = PlatformTextStyle(includeFontPadding = true),
    )
}
