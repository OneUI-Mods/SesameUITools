package dev.ryuya.darkapps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import kotlin.jvm.JvmStatic

/* A narrow URI grant also makes the preferences provider visible to the scoped app. */
object ConfigurationAccess {
    @JvmStatic
    fun grant(context: Context) {
        for (app in SupportedApp.entries) {
            try {
                context.getPackageManager().getApplicationInfo(app.packageName, 0)
                context.grantUriPermission(
                    app.packageName,
                    app.uri(),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (unavailable: NameNotFoundException) {
                // One supported app may be absent while the others remain available.
            } catch (unavailable: SecurityException) {}
        }
    }
}
