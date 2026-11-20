package com.example.newtv.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import com.example.newtv.R
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Shapes
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

/** "Cable box, circa 1997" design tokens. */
object NostalgiaColors {
    /** Deep CRT black-blue; bottom of the screen gradient and window background. */
    val Background = Color(0xFF070A14)
    /** Slightly lifted blue at the top of the screen gradient. */
    val BackgroundTop = Color(0xFF111B3D)
    val Surface = Color(0xFF111833)
    val SurfaceRaised = Color(0xFF1A2447)
    /** Phosphor green used for OSD text (channel numbers, clocks, headers). */
    val OsdGreen = Color(0xFF7CFF6B)
    /** Classic guide-listing cell blue. */
    val GuideBlue = Color(0xFF1B2A6B)
    val GuideBlueDeep = Color(0xFF121D4D)
    val GuideLine = Color(0xFF3A4C9E)
    /** THE focus colour: bright white. No channel accent is white or yellow. */
    val Focus = Color(0xFFFFFFFF)
    /** Soft ice-blue halo around focused elements. */
    val FocusGlow = Color(0xFFA8DCFF)
    /** Text/icon colour on top of a focused (white) container. */
    val OnFocus = Color(0xFF0A0F24)
    val Error = Color(0xFFFF6B6B)
    val TextPrimary = Color(0xFFE8EEFF)
    val TextMuted = Color(0xFF9AA6C8)
    val OnAccent = Color(0xFF070A14)
}

/** Channel branding palette. Never white/yellow so focus stays unmistakable. */
val ChannelAccents: List<Color> = listOf(
    Color(0xFFFF4FD8),
    Color(0xFF33F6FF),
    Color(0xFF8CFF5E),
    Color(0xFFFF9F1C),
    Color(0xFFB388FF)
)

object Spacing {
    /** TV overscan safe area. */
    val SafeHorizontal: Dp = 48.dp
    val SafeVertical: Dp = 27.dp
    val XXS: Dp = 2.dp
    val XS: Dp = 4.dp
    val S: Dp = 8.dp
    val M: Dp = 12.dp
    val L: Dp = 16.dp
    val XL: Dp = 24.dp
    val XXL: Dp = 32.dp
}

object NostalgiaShapes {
    val Small = RoundedCornerShape(4.dp)
    val Medium = RoundedCornerShape(8.dp)
    val Large = RoundedCornerShape(14.dp)
    /** Rounded corners of the simulated CRT glass. */
    val ScreenCorner: Dp = 18.dp
}

object NostalgiaFonts {
    /** OSD / display face: VT323 (SIL OFL 1.1, see third_party/fonts/VT323-OFL.txt). */
    val Osd: FontFamily = FontFamily(Font(R.font.vt323_regular, FontWeight.Normal))
    val Body: FontFamily = FontFamily.SansSerif
}

/** Focus ring metrics shared by every focusable component. */
object NostalgiaFocus {
    val BorderWidth: Dp = 3.dp
    val GlowElevation: Dp = 14.dp
    const val Scale: Float = 1.06f
    val GlowColor: Color = NostalgiaColors.FocusGlow.copy(alpha = 0.55f)
}

/** Convenience accessor bundle, e.g. `NostalgiaTokens.colors.OsdGreen`. */
object NostalgiaTokens {
    val colors = NostalgiaColors
    val spacing = Spacing
    val shapes = NostalgiaShapes
    val fonts = NostalgiaFonts
    val focus = NostalgiaFocus
    val accents = ChannelAccents
}

/** OSD text style: phosphor-green glow, tracked out. */
fun osdTextStyle(
    size: Int = 28,
    color: Color = NostalgiaColors.OsdGreen,
    glow: Boolean = true
): TextStyle = TextStyle(
    fontFamily = NostalgiaFonts.Osd,
    fontWeight = FontWeight.Bold,
    fontSize = size.sp,
    letterSpacing = 1.sp,
    color = color,
    shadow = if (glow && color.isSpecified) Shadow(color.copy(alpha = 0.55f), Offset.Zero, 8f) else null
)

val LocalReducedMotion = staticCompositionLocalOf { false }

private val NostalgiaColorScheme: ColorScheme = darkColorScheme(
    primary = ChannelAccents[0],
    onPrimary = NostalgiaColors.OnAccent,
    secondary = ChannelAccents[1],
    onSecondary = NostalgiaColors.OnAccent,
    tertiary = NostalgiaColors.OsdGreen,
    onTertiary = NostalgiaColors.OnAccent,
    background = NostalgiaColors.Background,
    onBackground = NostalgiaColors.TextPrimary,
    surface = NostalgiaColors.Surface,
    onSurface = NostalgiaColors.TextPrimary,
    surfaceVariant = NostalgiaColors.SurfaceRaised,
    onSurfaceVariant = NostalgiaColors.TextMuted,
    border = NostalgiaColors.GuideLine,
    error = NostalgiaColors.Error,
    onError = NostalgiaColors.OnAccent
)

private fun body(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = NostalgiaFonts.Body, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp
)

private fun osd(size: Int, line: Int) = TextStyle(
    fontFamily = NostalgiaFonts.Osd, fontWeight = FontWeight.Bold,
    fontSize = size.sp, lineHeight = line.sp, letterSpacing = 1.sp
)

private val NostalgiaTypography = Typography(
    displayLarge = osd(72, 76),
    displayMedium = osd(56, 60),
    displaySmall = osd(44, 48),
    headlineLarge = osd(40, 44),
    headlineMedium = osd(34, 38),
    headlineSmall = osd(28, 32),
    titleLarge = body(26, 32, FontWeight.Bold),
    titleMedium = body(21, 27, FontWeight.SemiBold),
    titleSmall = body(18, 24, FontWeight.SemiBold),
    bodyLarge = body(20, 28),
    bodyMedium = body(18, 24),
    bodySmall = body(16, 22),
    labelLarge = osd(22, 26),
    labelMedium = osd(18, 22),
    labelSmall = osd(16, 20)
)

private val NostalgiaMaterialShapes = Shapes(
    extraSmall = NostalgiaShapes.Small,
    small = NostalgiaShapes.Small,
    medium = NostalgiaShapes.Medium,
    large = NostalgiaShapes.Large,
    extraLarge = NostalgiaShapes.Large
)

@Composable
fun NewTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NostalgiaColorScheme,
        shapes = NostalgiaMaterialShapes,
        typography = NostalgiaTypography,
        content = content
    )
}
