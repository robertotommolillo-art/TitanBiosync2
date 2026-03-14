package com.titanbiosync

import android.app.Application
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TitanBiosyncApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Material You (Android 12+): usa colori dinamici dal wallpaper quando disponibili.
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}