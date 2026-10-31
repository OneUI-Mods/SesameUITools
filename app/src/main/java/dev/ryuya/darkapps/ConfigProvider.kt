package dev.ryuya.darkapps

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Process
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/* Per-app appearance settings. Runtime writes are limited to that app's installed UID. */
class ConfigProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        return true
    }

    private fun checkCaller(app: SupportedApp) {
        val uid = Binder.getCallingUid()
        if (uid == Process.myUid()) return
        try {
            if (
                uid == getContext()!!.getPackageManager().getApplicationInfo(app.packageName, 0).uid
            )
                return
        } catch (ignored: NameNotFoundException) {}
        throw SecurityException("Installed host UID required")
    }

    override fun query(
        uri: Uri,
        projection: Array<String?>?,
        selection: String?,
        args: Array<String?>?,
        order: String?,
    ): Cursor {
        val app = SupportedApp.fromUri(uri)
        checkCaller(app!!)
        val p = app!!.settings(getContext()!!)
        val out = MatrixCursor(arrayOf<String>("enabled", "mode", "reset"))
        out.addRow(
            arrayOf<Any?>(
                if (p!!.getBoolean("enabled", true)) 1 else 0,
                ThemeChoice.valid(p!!.getString("mode", ThemeChoice.SYSTEM)),
                p!!.getLong("reset", 0),
            )
        )
        return out
    }

    override fun call(method: String, arg: String?, data: Bundle?): Bundle? {
        val app = SupportedApp.find(arg)
        require(!(app == null || ("status" != method) || data == null)) { "Unknown call" }
        checkCaller(app)
        val state = data.getString("state", "waiting")
        require(
            java.util.Set.of<String?>(
                    "dark",
                    "light",
                    "disabled",
                    "unsupported",
                    "blocked",
                    "error",
                )
                .contains(state)
        ) {
            "Invalid state"
        }
        app.settings(getContext()!!)!!
            .edit()
            .putString("state", state)
            .putString("host_version", data.getString("version", ""))
            .putLong("checked", System.currentTimeMillis())
            .apply()
        return Bundle.EMPTY
    }

    override fun getType(uri: Uri): String? {
        SupportedApp.fromUri(uri)
        return "vnd.android.cursor.item/vnd.darkapps.settings"
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException()
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        where: String?,
        args: Array<String?>?,
    ): Int {
        throw UnsupportedOperationException()
    }

    override fun delete(uri: Uri, where: String?, args: Array<String?>?): Int {
        throw UnsupportedOperationException()
    }

    companion object {
        @JvmField
        val URI: Uri? = Uri.parse("content://" + BuildConfig.APPLICATION_ID + ".config/settings")

        @JvmStatic
        fun changed(c: Context, app: SupportedApp) {
            c.getContentResolver().notifyChange(app.uri()!!, null)
        }
    }
}
