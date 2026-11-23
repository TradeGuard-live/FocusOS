package com.naman.focusos

import android.app.Application
import com.naman.focusos.data.db.FocusDatabase
import com.naman.focusos.notification.FocusNotificationManager

class FocusOSApplication : Application() {

    companion object {
        /**
         * Tracks whether the splash animation has already played in this process session.
         * Stored in RAM (static memory):
         * - On cold start: false -> splash animation plays once.
         * - Once completed: set to true.
         * - When switching to other apps and returning (app remains in RAM): remains true -> splash is NEVER replayed.
         * - If the app process is terminated/killed: resets to false on next cold start.
         */
        @Volatile
        var hasPlayedSplash: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize Database & seed defaults
        FocusDatabase.getInstance(this)
        // Initialize Notification Channels
        FocusNotificationManager(this)
    }
}
