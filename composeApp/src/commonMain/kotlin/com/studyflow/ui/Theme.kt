package com.studyflow.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The colours that change when the user picks another accent. */
class AccentColors(
    val primary: Color,
    val onPrimary: Color,
    val container: Color,
    val onContainer: Color,
    /** A complementary colour, used for breaks in the focus timer. */
    val secondary: Color,
)

class AccentPalette(
    val name: String,
    /** The dot shown in the colour picker. */
    val swatch: Color,
    val light: AccentColors,
    val dark: AccentColors,
)

val AccentPalettes = listOf(
    AccentPalette(
        name = "Indigo",
        swatch = Color(0xFF4F46E5),
        light = AccentColors(Color(0xFF4F46E5), Color.White, Color(0xFFE0E7FF), Color(0xFF1E1B4B), Color(0xFF0D9488)),
        dark = AccentColors(Color(0xFFA5B4FC), Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFFE0E7FF), Color(0xFF5EEAD4)),
    ),
    AccentPalette(
        name = "Teal",
        swatch = Color(0xFF0D9488),
        light = AccentColors(Color(0xFF0D9488), Color.White, Color(0xFFCCFBF1), Color(0xFF042F2E), Color(0xFF4F46E5)),
        dark = AccentColors(Color(0xFF5EEAD4), Color(0xFF042F2E), Color(0xFF115E59), Color(0xFFCCFBF1), Color(0xFFA5B4FC)),
    ),
    AccentPalette(
        name = "Rose",
        swatch = Color(0xFFE11D48),
        light = AccentColors(Color(0xFFE11D48), Color.White, Color(0xFFFFE4E6), Color(0xFF4C0519), Color(0xFFD97706)),
        dark = AccentColors(Color(0xFFFDA4AF), Color(0xFF4C0519), Color(0xFF881337), Color(0xFFFFE4E6), Color(0xFFFCD34D)),
    ),
    AccentPalette(
        name = "Orange",
        swatch = Color(0xFFEA580C),
        light = AccentColors(Color(0xFFEA580C), Color.White, Color(0xFFFFEDD5), Color(0xFF431407), Color(0xFF0D9488)),
        dark = AccentColors(Color(0xFFFDBA74), Color(0xFF431407), Color(0xFF9A3412), Color(0xFFFFEDD5), Color(0xFF5EEAD4)),
    ),
    AccentPalette(
        name = "Green",
        swatch = Color(0xFF16A34A),
        light = AccentColors(Color(0xFF16A34A), Color.White, Color(0xFFDCFCE7), Color(0xFF052E16), Color(0xFF4F46E5)),
        dark = AccentColors(Color(0xFF86EFAC), Color(0xFF052E16), Color(0xFF166534), Color(0xFFDCFCE7), Color(0xFFA5B4FC)),
    ),
)

private fun buildScheme(a: AccentColors, dark: Boolean): ColorScheme =
    if (!dark) {
        lightColorScheme(
            primary = a.primary,
            onPrimary = a.onPrimary,
            primaryContainer = a.container,
            onPrimaryContainer = a.onContainer,
            secondary = a.secondary,
            onSecondary = Color.White,
            // chips and the selected navigation item follow the accent as well
            secondaryContainer = a.container,
            onSecondaryContainer = a.onContainer,
            background = Color(0xFFF4F6FB),
            onBackground = Color(0xFF0F172A),
            surface = Color(0xFFF4F6FB),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFE6EAF2),
            onSurfaceVariant = Color(0xFF55627A),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF8FAFC),
            surfaceContainer = Color.White,
            surfaceContainerHigh = Color.White,
            surfaceContainerHighest = Color(0xFFEEF1F7),
            outline = Color(0xFF8A94A8),
            outlineVariant = Color(0xFFD5DAE5),
            error = Color(0xFFDC2626),
        )
    } else {
        darkColorScheme(
            primary = a.primary,
            onPrimary = a.onPrimary,
            primaryContainer = a.container,
            onPrimaryContainer = a.onContainer,
            secondary = a.secondary,
            onSecondary = Color(0xFF0B1020),
            secondaryContainer = a.container,
            onSecondaryContainer = a.onContainer,
            background = Color(0xFF0B1020),
            onBackground = Color(0xFFE2E8F0),
            surface = Color(0xFF0B1020),
            onSurface = Color(0xFFE2E8F0),
            surfaceVariant = Color(0xFF1E293B),
            onSurfaceVariant = Color(0xFF9AA7BD),
            surfaceContainerLowest = Color(0xFF080C18),
            surfaceContainerLow = Color(0xFF0F1526),
            surfaceContainer = Color(0xFF121A2E),
            surfaceContainerHigh = Color(0xFF172038),
            surfaceContainerHighest = Color(0xFF1E2842),
            outline = Color(0xFF64748B),
            outlineVariant = Color(0xFF2A3550),
            error = Color(0xFFF87171),
        )
    }

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private fun appTypography(font: FontFamily?): Typography {
    val base = Typography()
    if (font == null) return base
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = font, letterSpacing = (-0.5).sp),
        displayMedium = base.displayMedium.copy(fontFamily = font, letterSpacing = (-0.5).sp),
        displaySmall = base.displaySmall.copy(fontFamily = font),
        headlineLarge = base.headlineLarge.copy(fontFamily = font, letterSpacing = (-0.3).sp),
        headlineMedium = base.headlineMedium.copy(fontFamily = font, letterSpacing = (-0.3).sp),
        headlineSmall = base.headlineSmall.copy(fontFamily = font),
        titleLarge = base.titleLarge.copy(fontFamily = font),
        titleMedium = base.titleMedium.copy(fontFamily = font),
        titleSmall = base.titleSmall.copy(fontFamily = font),
        bodyLarge = base.bodyLarge.copy(fontFamily = font),
        bodyMedium = base.bodyMedium.copy(fontFamily = font),
        bodySmall = base.bodySmall.copy(fontFamily = font),
        labelLarge = base.labelLarge.copy(fontFamily = font),
        labelMedium = base.labelMedium.copy(fontFamily = font),
        labelSmall = base.labelSmall.copy(fontFamily = font),
    )
}

/** Colours a class can be tagged with in the timetable. */
val ClassPalette = listOf(
    Color(0xFF6366F1),
    Color(0xFF14B8A6),
    Color(0xFFF59E0B),
    Color(0xFFEC4899),
    Color(0xFF3B82F6),
    Color(0xFF84CC16),
)

/**
 * @param themeMode 0 = follow the phone, 1 = light, 2 = dark
 * @param accentIndex position in [AccentPalettes]
 */
@Composable
fun StudyFlowTheme(
    themeMode: Int = 0,
    accentIndex: Int = 0,
    fontFamily: FontFamily? = null,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val accent = AccentPalettes[accentIndex.coerceIn(0, AccentPalettes.lastIndex)]
    val scheme = remember(accent, dark) { buildScheme(if (dark) accent.dark else accent.light, dark) }
    val typography = remember(fontFamily) { appTypography(fontFamily) }

    // keeps the status bar icons readable when the in-app theme differs from the phone's
    SystemBarsEffect(darkTheme = dark)

    MaterialTheme(
        colorScheme = scheme,
        typography = typography,
        shapes = AppShapes,
        content = content,
    )
}
