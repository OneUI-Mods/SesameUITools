package dev.ryuya.darkapps

import org.junit.Assert.*
import org.junit.Test

class SupportedAppTest {
    @Test
    fun unknownPackageCannotEnterTheModule() {
        assertNull(SupportedApp.find("com.android.settings"))
        assertEquals(SupportedApp.POVO, SupportedApp.find("com.kddi.kdla.jp"))
    }

    @Test
    fun reviewsAreIndependentForEachHost() {
        assertTrue(SupportedApp.SESAME.accepts(906, "3.0.266"))
        assertTrue(SupportedApp.POVO.accepts(864, "1.71.0-JP"))
        assertFalse(SupportedApp.SESAME.accepts(864, "1.71.0-JP"))
    }

    @Test
    fun unknownUpdatesKeepTheOriginalAppearance() {
        assertFalse(SupportedApp.POVO.accepts(865, "1.71.0-JP"))
        assertFalse(SupportedApp.POVO.accepts(864, "1.72.0-JP"))
    }
}
