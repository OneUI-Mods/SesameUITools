package dev.ryuya.darkapps

import kotlin.jvm.JvmStatic

/* Stored values are explicit so new UI choices cannot change existing preferences. */
object ThemeChoice {
    const val SYSTEM: String = "system"
    const val DARK: String = "dark"
    const val LIGHT: String = "light"

    @JvmStatic
    fun valid(mode: String?): String? {
        return if (ThemeChoice.DARK == mode || ThemeChoice.LIGHT == mode) mode
        else ThemeChoice.SYSTEM
    }

    @JvmStatic
    fun dark(enabled: Boolean, mode: String?, systemDark: Boolean): Boolean {
        return enabled && (ThemeChoice.DARK == mode || (ThemeChoice.LIGHT != mode && systemDark))
    }
}
