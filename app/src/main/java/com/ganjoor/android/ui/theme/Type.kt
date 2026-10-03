package com.ganjoor.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.ganjoor.android.R
import com.ganjoor.android.ui.ReadingFont

/**
 * Both files are variable fonts with a `wght` axis, registered at four weights so a reader can
 * thicken the text — thin naskh strokes wash out on a lit screen, especially in the dark themes.
 *
 * Real axis interpolation needs API 26+; on 24 and 25 the variation settings are ignored and
 * Android falls back to synthesising the heavier weights, which still darkens the text.
 */
@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(weight, FontStyle.Normal),
)

private val weights =
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

val Naskh = FontFamily(weights.map { variable(R.font.noto_naskh_arabic, it) })
val Nastaliq = FontFamily(weights.map { variable(R.font.noto_nastaliq_urdu, it) })

/**
 * The English UI only. Libron is a reading serif (OFL, github.com/nicoverbruggen/libron) and
 * reads far better in Latin than the Latin side of an Arabic font. It never touches the poems:
 * those are always naskh or nastaliq, whatever language the interface is in.
 */
val Libron = FontFamily(
    Font(R.font.libron_regular, FontWeight.Normal),
    Font(R.font.libron_bold, FontWeight.Bold),
)

// Noto Naskh Arabic covers Persian, Urdu and Arabic plus Latin, so it carries the RTL interfaces.
private val default = Typography()

private fun typographyOf(family: FontFamily) = Typography(
    displayLarge = default.displayLarge.copy(fontFamily = family),
    displayMedium = default.displayMedium.copy(fontFamily = family),
    displaySmall = default.displaySmall.copy(fontFamily = family),
    headlineLarge = default.headlineLarge.copy(fontFamily = family),
    headlineMedium = default.headlineMedium.copy(fontFamily = family),
    headlineSmall = default.headlineSmall.copy(fontFamily = family),
    titleLarge = default.titleLarge.copy(fontFamily = family),
    titleMedium = default.titleMedium.copy(fontFamily = family),
    titleSmall = default.titleSmall.copy(fontFamily = family),
    bodyLarge = default.bodyLarge.copy(fontFamily = family),
    bodyMedium = default.bodyMedium.copy(fontFamily = family),
    bodySmall = default.bodySmall.copy(fontFamily = family),
    labelLarge = default.labelLarge.copy(fontFamily = family),
    labelMedium = default.labelMedium.copy(fontFamily = family),
    labelSmall = default.labelSmall.copy(fontFamily = family),
)

val NaskhTypography = typographyOf(Naskh)
val LibronTypography = typographyOf(Libron)

/**
 * Style for the poem text itself. Nastaliq stacks its glyphs diagonally and has very deep
 * descenders, so it needs far more leading than naskh and the legacy font padding kept on,
 * otherwise the swashes clip against the line above. Tune the two multipliers by eye if you
 * swap either font file.
 */
fun readingStyle(font: ReadingFont, sizeSp: Float, weight: FontWeight): TextStyle {
    val size: TextUnit = sizeSp.sp
    val leading = if (font == ReadingFont.Nastaliq) 2.4f else 1.8f
    return TextStyle(
        fontFamily = if (font == ReadingFont.Nastaliq) Nastaliq else Naskh,
        fontWeight = weight,
        fontSize = size,
        lineHeight = size * leading,
        platformStyle = PlatformTextStyle(includeFontPadding = true),
    )
}
