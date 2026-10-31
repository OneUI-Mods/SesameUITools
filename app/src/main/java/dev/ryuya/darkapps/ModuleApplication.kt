package dev.ryuya.darkapps

import android.app.Application

class ModuleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FrameworkServiceConnection.register(this)
    }
}
