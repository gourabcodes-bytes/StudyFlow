package com.studyflow.logic

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Colour helpers that work on plain 0xRRGGBB numbers, so they can be tested without any UI. */

/** Ready-made accent colours shown in the colour grid. */
val AccentSwatches: List<Int> = listOf(
    0xE53935, 0xFF5722, 0xFB8C00, 0xFFB300, 0xCDDC39, 0x7CB342,
    0x43A047, 0x00897B, 0x00ACC1, 0x039BE5, 0x1E88E5, 0x3949AB,
    0x4F46E5, 0x5E35B1, 0x8E24AA, 0xD81B60, 0x6D4C41, 0x546E7A,
)

private fun red(rgb: Int) = (rgb shr 16) and 0xFF
private fun green(rgb: Int) = (rgb shr 8) and 0xFF
private fun blue(rgb: Int) = rgb and 0xFF

fun rgbOf(r: Int, g: Int, b: Int): Int =
    (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

/** Mixes two colours: t = 0 gives [from], t = 1 gives [to]. */
fun blendRgb(from: Int, to: Int, t: Float): Int {
    val k = t.coerceIn(0f, 1f)
    fun mix(a: Int, b: Int) = (a + (b - a) * k).roundToInt()
    return rgbOf(mix(red(from), red(to)), mix(green(from), green(to)), mix(blue(from), blue(to)))
}

/** Perceived brightness: 0f is black, 1f is white. */
fun brightness(rgb: Int): Float = (0.299f * red(rgb) + 0.587f * green(rgb) + 0.114f * blue(rgb)) / 255f

/** Turns the hue of a colour around the colour wheel by [degrees]. */
fun rotateHue(rgb: Int, degrees: Float): Int {
    val r = red(rgb) / 255f
    val g = green(rgb) / 255f
    val b = blue(rgb) / 255f
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC
    val l = (maxC + minC) / 2f
    val s = if (delta == 0f) 0f else delta / (1f - abs(2f * l - 1f))
    var h = when {
        delta == 0f -> 0f
        maxC == r -> 60f * (((g - b) / delta).mod(6f))
        maxC == g -> 60f * ((b - r) / delta + 2f)
        else -> 60f * ((r - g) / delta + 4f)
    }
    h = (h + degrees).mod(360f)

    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f).mod(2f) - 1f))
    val m = l - c / 2f
    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return rgbOf(((r1 + m) * 255f).roundToInt(), ((g1 + m) * 255f).roundToInt(), ((b1 + m) * 255f).roundToInt())
}

/** The five colours the theme needs, all derived from one chosen accent colour. */
class AccentRgb(
    val primary: Int,
    val onPrimary: Int,
    val container: Int,
    val onContainer: Int,
    /** A complementary colour, used for breaks in the focus timer and for gradients. */
    val secondary: Int,
)

fun accentFromSeed(seedRgb: Int, dark: Boolean): AccentRgb {
    val seed = seedRgb and 0xFFFFFF
    val partner = rotateHue(seed, 150f)
    return if (!dark) {
        AccentRgb(
            primary = seed,
            onPrimary = if (brightness(seed) > 0.62f) 0x1B1B1B else 0xFFFFFF,
            container = blendRgb(seed, 0xFFFFFF, 0.85f),
            onContainer = blendRgb(seed, 0x000000, 0.72f),
            secondary = blendRgb(partner, 0x000000, 0.10f),
        )
    } else {
        AccentRgb(
            primary = blendRgb(seed, 0xFFFFFF, 0.55f),
            onPrimary = blendRgb(seed, 0x000000, 0.80f),
            container = blendRgb(seed, 0x000000, 0.55f),
            onContainer = blendRgb(seed, 0xFFFFFF, 0.88f),
            secondary = blendRgb(partner, 0xFFFFFF, 0.45f),
        )
    }
}
