package dev.ryuya.darkapps

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.SharedPreferences
import android.os.Process

/* Tracks actual crash evidence only. The host is never killed or restarted by this guard. */
@SuppressLint("ApplySharedPref") // Guard records must survive an immediate host crash.
class CrashGuard(context: Context, reset: Long) {
    private val prefs: SharedPreferences
    private var blocked: Boolean
    private var armedAt: Long = 0

    init {
        prefs = context.getSharedPreferences("dark-apps.guard", 0)
        if (reset != prefs.getLong("reset", 0))
            prefs.edit().clear().putLong("reset", reset).commit()
        blocked = prefs.getBoolean("blocked", false)
        val pid = prefs.getInt("pid", -1)
        val operation = prefs.getLong("operation", 0)
        val now = System.currentTimeMillis()
        if (pid > 0 && pid != Process.myPid() && now >= operation && now - operation < 120000) {
            for (exit in
                context
                    .getSystemService<ActivityManager?>(ActivityManager::class.java)
                    .getHistoricalProcessExitReasons(context.getPackageName(), pid, 4)) {
                if (
                    exit.getTimestamp() < operation ||
                        exit.getTimestamp() <= prefs.getLong("consumed", 0)
                )
                    continue
                val reason = exit.getReason()
                if (
                    reason == ApplicationExitInfo.REASON_CRASH ||
                        reason == ApplicationExitInfo.REASON_CRASH_NATIVE
                ) {
                    val state =
                        CrashWindow.record(now, prefs.getLong("start", 0), prefs.getInt("count", 0))
                    blocked = blocked or state!!.blocked()
                    prefs
                        .edit()
                        .putLong("start", state.start)
                        .putInt("count", state.count)
                        .putLong("consumed", exit.getTimestamp())
                        .putBoolean("blocked", blocked)
                        .commit()
                }
                break
            }
        }
    }

    fun allowed(): Boolean {
        return !blocked
    }

    fun arm() {
        val now = System.currentTimeMillis()
        if (armedAt != 0L && now >= armedAt && now - armedAt < 30000) return
        if (!prefs.edit().putInt("pid", Process.myPid()).putLong("operation", now).commit()) {
            blocked = true
            throw IllegalStateException("Unable to store crash guard state")
        }
        armedAt = now
    }
}
