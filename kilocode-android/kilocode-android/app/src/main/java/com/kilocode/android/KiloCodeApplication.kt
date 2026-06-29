package com.kilocode.android

import android.app.Application

class KiloCodeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: KiloCodeApplication
            private set
    }
}
