package dev.ryuya.darkapps

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/* Explicitly reviewed host versions; unknown updates retain their original appearance. */
enum class SupportedApp(
    packageName: String,
    key: String,
    code: Long,
    version: String,
    activityPrefix: String,
    title: Int,
) {
    SESAME(
        "co.candyhouse.sesame2",
        "sesame",
        906,
        "3.0.266",
        "co.candyhouse.app.",
        R.string.sesame_title,
    ),
    POVO("com.kddi.kdla.jp", "povo", 864, "1.71.0-JP", "com.circles.", R.string.povo_title);

    @JvmField val packageName: String
    @JvmField val key: String
    @JvmField val version: String
    @JvmField val activityPrefix: String
    @JvmField val code: Long
    @JvmField val title: Int

    init {
        this.packageName = packageName
        this.key = key
        this.code = code
        this.version = version
        this.activityPrefix = activityPrefix
        this.title = title
    }

    fun accepts(versionCode: Long, versionName: String?): Boolean {
        return code == versionCode && version == versionName
    }

    fun uri(): Uri? {
        return ConfigProvider.URI!!.buildUpon().appendPath(key).build()
    }

    fun settings(context: Context): SharedPreferences? {
        // Each supported app owns an independent appearance configuration.
        return context.getSharedPreferences("appearance_" + key, 0)
    }

    companion object {
        @JvmStatic
        fun find(packageName: String?): SupportedApp? {
            for (app in SupportedApp.entries) if (app.packageName == packageName) return app
            return null
        }

        @JvmStatic
        fun fromUri(uri: Uri?): SupportedApp? {
            if (ConfigProvider.URI == uri) return SupportedApp.SESAME
            for (app in SupportedApp.entries) if (app.uri() == uri) return app
            throw IllegalArgumentException("Unknown configuration URI")
        }
    }
}
