package com.tether.app

import android.app.Application
import com.tether.app.core.notification.TetherNotificationHelper

class TetherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            System.loadLibrary("sqlcipher")
        } catch (_: UnsatisfiedLinkError) {}
        TetherNotificationHelper.initNotificationChannel(this)
    }
}
