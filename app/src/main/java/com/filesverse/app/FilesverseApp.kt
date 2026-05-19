package com.filesverse.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FilesverseApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val channel = NotificationChannel(
            CHANNEL_UPLOAD,
            "File Uploads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifications for file upload progress"
        }

        val syncChannel = NotificationChannel(
            CHANNEL_SYNC,
            "Cloud Sync",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifications for cloud synchronization"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        notificationManager.createNotificationChannel(syncChannel)
    }

    companion object {
        const val CHANNEL_UPLOAD = "channel_uploads"
        const val CHANNEL_SYNC = "channel_sync"
    }
}
