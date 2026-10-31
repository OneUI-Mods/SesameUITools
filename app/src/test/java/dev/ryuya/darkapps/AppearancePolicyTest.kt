package dev.ryuya.darkapps

import org.junit.Assert.*
import org.junit.Test

class AppearancePolicyTest {
    @Test
    fun preservesDeviceStateColors() {
        for (color in intArrayOf(-0xd7514f, -0x33b5bc, -0xbc5fb9, -0xcc9901)) {
            assertEquals(color, DarkPalette.foreground(color))
            assertEquals(color, DarkPalette.background(color))
        }
    }

    @Test
    fun artworkKeepsStateColorsAndHasDarkSurround() {
        assertEquals(DarkPalette.SURFACE, DarkPalette.artwork(-0x1))
        assertEquals(-0x33b5bc, DarkPalette.artwork(-0x33b5bc))
        assertEquals(-0xd7514f, DarkPalette.artwork(-0xd7514f))
        assertEquals(0, DarkPalette.artwork(0))
    }

    @Test
    fun keepsTransparency() {
        assertEquals(0x00000000, DarkPalette.background(0x00000000))
        assertEquals(0x00000000, DarkPalette.foreground(0x00000000))
        assertEquals(0x66, DarkPalette.foreground(0x66000000) ushr 24)
    }

    @Test
    fun createsContrastWithoutRepeatedInversion() {
        assertEquals(DarkPalette.BACKGROUND, DarkPalette.background(-0x1))
        assertEquals(DarkPalette.TEXT, DarkPalette.foreground(-0x1000000))
        assertEquals(DarkPalette.BACKGROUND, DarkPalette.background(DarkPalette.BACKGROUND))
        assertEquals(DarkPalette.TEXT, DarkPalette.foreground(DarkPalette.TEXT))
        assertEquals(DarkPalette.SECONDARY, DarkPalette.foreground(DarkPalette.SECONDARY))
    }

    @Test
    fun povoCardsStaySeparateAndIconsAreWhite() {
        val card = DarkPalette.card(-0xa0806)
        assertNotEquals(DarkPalette.BACKGROUND, card)
        assertEquals(card, DarkPalette.card(card))
        assertEquals(-0x1, DarkPalette.icon(-0xcccccd))
        assertEquals(0x66ffffff, DarkPalette.icon(0x66333333))
        assertEquals(-0x1a00, DarkPalette.card(-0x1a00))
        assertEquals(-0x1a00, DarkPalette.icon(-0x1a00))
    }

    @Test
    fun systemModeFollowsDeviceAndSwitchWins() {
        assertTrue(ThemeChoice.dark(true, ThemeChoice.SYSTEM, true))
        assertFalse(ThemeChoice.dark(true, ThemeChoice.SYSTEM, false))
        assertTrue(ThemeChoice.dark(true, ThemeChoice.DARK, false))
        assertFalse(ThemeChoice.dark(true, ThemeChoice.LIGHT, true))
        assertFalse(ThemeChoice.dark(false, ThemeChoice.DARK, true))
    }

    @Test
    fun crashWindowExpiresAndRequiresThreeCrashes() {
        val a = CrashWindow.record(1000, 0, 0)
        assertFalse(a!!.blocked())
        val b = CrashWindow.record(2000, a.start, a.count)
        assertFalse(b!!.blocked())
        val c = CrashWindow.record(3000, b.start, b.count)
        assertTrue(c!!.blocked())
        assertEquals(1, CrashWindow.record(130001, c.start, c.count)!!.count)
        assertEquals(1, CrashWindow.record(100, c.start, c.count)!!.count)
    }
}
