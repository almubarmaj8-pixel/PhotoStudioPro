package com.photostudio.pro

import android.app.Application
import com.google.android.material.color.DynamicColors

class PhotoStudioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
