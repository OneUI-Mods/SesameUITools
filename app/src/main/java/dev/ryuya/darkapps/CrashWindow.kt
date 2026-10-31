package dev.ryuya.darkapps

import kotlin.jvm.JvmRecord
import kotlin.jvm.JvmStatic

/* Repeated confirmed host crashes, rather than ordinary force-stops, trigger the guard. */
object CrashWindow {
    @JvmStatic
    fun record(now: Long, start: Long, count: Int): CrashWindow.State? {
        return if (now < start || now - start > 120000 || count == 0) CrashWindow.State(now, 1)
        else CrashWindow.State(start, count + 1)
    }

    @JvmRecord
    data class State(val start: Long, val count: Int) {
        fun blocked(): Boolean {
            return count >= 3
        }

        override fun toString(): String = "State[start=$start, count=$count]"
    }
}
