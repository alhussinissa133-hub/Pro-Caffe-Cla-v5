package com.cafe.accounts

import android.app.Application
import java.io.File

/** Saves any crash to a file so the next launch can show the error text. */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val old = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                File(filesDir, "crash.txt").writeText(android.util.Log.getStackTraceString(e))
            } catch (_: Throwable) {}
            old?.uncaughtException(t, e)
        }
    }
}
