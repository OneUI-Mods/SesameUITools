package dev.ryuya.darkapps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException

/*
 * Renews configuration access after boot/update or a request from the verified supported app UID.
 */
class ConfigurationAccessReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getAction()
        if (Intent.ACTION_BOOT_COMPLETED == action || Intent.ACTION_MY_PACKAGE_REPLACED == action) {
            ConfigurationAccess.grant(context)
            return
        }
        val sender = getSentFromUid()
        for (app in SupportedApp.entries) {
            try {
                if (
                    sender == context.getPackageManager().getApplicationInfo(app.packageName, 0).uid
                ) {
                    ConfigurationAccess.grant(context)
                    return
                }
            } catch (unavailable: NameNotFoundException) {}
        }
    }
}
