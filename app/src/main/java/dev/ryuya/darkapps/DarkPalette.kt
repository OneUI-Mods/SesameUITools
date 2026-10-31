package dev.ryuya.darkapps

import kotlin.jvm.JvmStatic
import kotlin.math.max
import kotlin.math.min

/* Role-aware neutral conversion. Colored state indicators and transparent pixels are preserved. */
object DarkPalette {
    const val BACKGROUND: Int = -0xebe6e4
    const val SURFACE: Int = -0xdfd7d4
    const val CARD: Int = -0xd6cac5
    const val CARD_OUTLINE: Int = -0xa6948b
    const val ICON: Int = -0x1
    const val TEXT: Int = -0x1a1211
    const val SECONDARY: Int = -0x4b3d38

    @JvmStatic
    fun neutral(color: Int): Boolean {
        val r: Int = (color ushr 16) and 255
        val g: Int = (color ushr 8) and 255
        val b: Int = color and 255
        return max(r, max(g, b)) - min(r, min(g, b)) <= 20
    }

    @JvmStatic
    fun foreground(color: Int): Int {
        if ((color ushr 24) == 0 || !DarkPalette.neutral(color)) return color
        val level: Int = color and 255
        return DarkPalette.alpha(
            color,
            if (level < 90 || level > 220) DarkPalette.TEXT else DarkPalette.SECONDARY,
        )
    }

    @JvmStatic
    fun background(color: Int): Int {
        if ((color ushr 24) == 0 || !DarkPalette.neutral(color)) return color
        val level: Int = color and 255
        if (level >= 242) return DarkPalette.alpha(color, DarkPalette.BACKGROUND)
        if (level >= 205) return DarkPalette.alpha(color, DarkPalette.SURFACE)
        if (level >= 160) return DarkPalette.alpha(color, -0xb9ada7)
        return color
    }

    @JvmStatic
    fun card(color: Int): Int {
        if ((color ushr 24) != 0 && DarkPalette.neutral(color) && (color and 255) >= 205)
            return DarkPalette.alpha(color, DarkPalette.CARD)
        return DarkPalette.background(color)
    }

    @JvmStatic
    fun icon(color: Int): Int {
        return if ((color ushr 24) == 0 || !DarkPalette.neutral(color)) color
        else DarkPalette.alpha(color, DarkPalette.ICON)
    }

    @JvmStatic
    fun artwork(color: Int): Int {
        if ((color ushr 24) == 0 || !DarkPalette.neutral(color)) return color
        val level: Int = color and 255
        if (level >= 245) return DarkPalette.alpha(color, DarkPalette.SURFACE)
        if (level >= 180) return DarkPalette.alpha(color, -0xa99c96)
        return DarkPalette.alpha(color, DarkPalette.TEXT)
    }

    @JvmStatic
    private fun alpha(original: Int, replacement: Int): Int {
        return (original and -0x1000000) or (replacement and 0x00ffffff)
    }
}
