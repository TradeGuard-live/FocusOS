package com.naman.focusos

import android.app.Application
import com.naman.focusos.data.db.FocusDatabase
import com.naman.focusos.notification.FocusNotificationManager

class FocusOSApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize Database & seed defaults
        FocusDatabase.getInstance(this)
        // Initialize Notification Channels
        FocusNotificationManager(this)
    }
}
