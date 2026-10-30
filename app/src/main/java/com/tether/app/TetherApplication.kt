package com.tether.app

import android.app.Application

class TetherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            System.loadLibrary("sqlcipher")
        } catch (_: UnsatisfiedLinkError) {}
    }
}
