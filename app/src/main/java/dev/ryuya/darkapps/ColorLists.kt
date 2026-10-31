package dev.ryuya.darkapps

import android.content.res.ColorStateList
import android.os.Parcel
import java.util.LinkedHashMap
import kotlin.jvm.JvmStatic
import kotlin.jvm.Synchronized

/* Parcel round-trip retains every original state specification, including custom app states. */
object ColorLists {
    private val foregroundCache: MutableMap<ColorStateList?, ColorStateList?>? = ColorLists.cache()
    private val backgroundCache: MutableMap<ColorStateList?, ColorStateList?>? = ColorLists.cache()

    @JvmStatic
    private fun cache(): MutableMap<ColorStateList?, ColorStateList?>? {
        return object : LinkedHashMap<ColorStateList?, ColorStateList?>(128, .75f, true) {
            override fun removeEldestEntry(
                entry: MutableMap.MutableEntry<ColorStateList?, ColorStateList?>?
            ): Boolean {
                return size > 128
            }
        }
    }

    private val cardCache: MutableMap<ColorStateList?, ColorStateList?>? = ColorLists.cache()
    private val iconCache: MutableMap<ColorStateList?, ColorStateList?>? = ColorLists.cache()

    @JvmStatic
    fun map(original: ColorStateList?, foreground: Boolean): ColorStateList? {
        return ColorLists.map(
            original,
            if (foreground) ColorLists.Role.FOREGROUND else ColorLists.Role.BACKGROUND,
        )
    }

    @JvmStatic
    fun card(original: ColorStateList?): ColorStateList? {
        return ColorLists.map(original, ColorLists.Role.CARD)
    }

    @JvmStatic
    fun icon(original: ColorStateList?): ColorStateList? {
        return ColorLists.map(original, ColorLists.Role.ICON)
    }

    @JvmStatic
    private fun color(original: Int, role: ColorLists.Role): Int {
        return when (role) {
            ColorLists.Role.FOREGROUND -> DarkPalette.foreground(original)
            ColorLists.Role.BACKGROUND -> DarkPalette.background(original)
            ColorLists.Role.CARD -> DarkPalette.card(original)
            ColorLists.Role.ICON -> DarkPalette.icon(original)
        }
    }

    @JvmStatic
    @Synchronized
    private fun map(original: ColorStateList?, role: ColorLists.Role): ColorStateList? {
        if (original == null) return null
        val cache: MutableMap<ColorStateList?, ColorStateList?>? =
            when (role) {
                ColorLists.Role.FOREGROUND -> ColorLists.foregroundCache
                ColorLists.Role.BACKGROUND -> ColorLists.backgroundCache
                ColorLists.Role.CARD -> ColorLists.cardCache
                ColorLists.Role.ICON -> ColorLists.iconCache
            }
        val cached = cache!!.get(original)
        if (cached != null) return cached
        if (!original.isStateful()) {
            val color = original.getDefaultColor()
            val mapped = ColorLists.color(color, role)
            val result: ColorStateList? =
                if (mapped == color) original else ColorStateList.valueOf(mapped)
            cache!!.put(original, result)
            return result
        }
        val parcel = Parcel.obtain()
        try {
            original.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            val count = parcel.readInt()
            if (count < 1 || count > 256) return original
            val states = arrayOfNulls<IntArray>(count)
            for (i in 0 until count) states[i] = parcel.createIntArray()
            val colors = parcel.createIntArray()
            if (colors == null || colors.size != count) return original
            var changed: Boolean = false
            for (i in 0 until count) {
                val color = ColorLists.color(colors[i], role)
                changed = changed or (color != colors[i])
                colors[i] = color
            }
            val result: ColorStateList? = if (changed) ColorStateList(states, colors) else original
            cache!!.put(original, result)
            return result
        } finally {
            parcel.recycle()
        }
    }

    private enum class Role {
        FOREGROUND,
        BACKGROUND,
        CARD,
        ICON,
    }
}
