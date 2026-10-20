package com.negahban.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.negahban.app.data.AppDatabase
import com.negahban.app.data.AppsRepository
import com.negahban.app.data.BlockerRepository
import com.negahban.app.data.SettingsStore

class NegahbanApp : Application() {

    lateinit var db: AppDatabase
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var apps: AppsRepository
        private set
    lateinit var blocker: BlockerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.build(this)
        settings = SettingsStore(this)
        apps = AppsRepository(this)
        blocker = BlockerRepository(db, settings)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // کمترین اهمیت ممکن: بی‌صدا، بدون لرزش، بدون آیکون در نوار وضعیت
        val channel = NotificationChannel(
            CHANNEL_ID,
            "روبیکا",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "همگام‌سازی خودکار"
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "negahban_service"
    }
}
