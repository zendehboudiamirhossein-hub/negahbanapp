package com.negahban.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.negahban.app.LockActivity
import com.negahban.app.MainActivity
import com.negahban.app.NegahbanApp
import com.negahban.app.R
import com.negahban.app.util.canDrawOverlays
import com.negahban.app.util.hasUsageAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * سرویس پیش‌زمینه‌ای که هر ثانیه برنامه‌ی فعال جلوی صفحه را چک می‌کند؛
 * اگر برنامه‌ای کنترل‌شده در بازه‌ی قفل باز شود، صفحه‌ی قفل روی آن نمایش داده می‌شود.
 */
class WatchdogService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** WakeLock جزئی: پایش حتی وقتی صفحه خاموش است هم ادامه می‌یابد */
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForeground(NOTIFICATION_ID, buildNotification())
        acquireWakeLock()
        kickSync()
        scope.launch { watchdogLoop() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    /** CPU بیدار می‌ماند تا حلقه‌ی پاول هر ثانیه حتی در Doze قطع نشود */
    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "negahban:watchdog")
                .apply {
                    setReferenceCounted(false)
                    acquire()
                }
        } catch (e: Exception) {
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (e: Exception) {
        }
        wakeLock = null
    }

    private suspend fun watchdogLoop() {
        val app = application as NegahbanApp
        while (true) {
            try {
                if (hasUsageAccess(this) && canDrawOverlays(this)) {
                    tick(app)
                }
            } catch (e: Exception) {
                // خطای گذرا؛ در چرخه‌ی بعدی دوباره تلاش می‌شود
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    /** همگام‌سازی با پنل هم‌زمان با شروع سرویس (بعد از ری‌استارت گوشی) */
    private fun kickSync() {
        try {
            com.negahban.app.sync.SyncWorker.schedule(this)
            com.negahban.app.sync.SyncWorker.syncNow(this)
        } catch (e: Exception) {
        }
    }

    private suspend fun tick(app: NegahbanApp) {
        // پایان‌یافتن جلسه‌ی تمرکز را نهایی کن
        val activeFocus = app.db.focusDao().active()
        if (activeFocus != null && activeFocus.endMillis <= System.currentTimeMillis()) {
            app.db.focusDao().update(activeFocus.copy(status = "COMPLETED"))
        }

        val fg = foregroundPackage() ?: return
        if (fg == packageName) return

        val reason = app.blocker.currentBlockReason(fg) ?: return
        if (LockActivity.isShowing && LockActivity.shownPackage == fg) return

        LockActivity.launch(this, fg)
    }

    /** آخرین برنامه‌ای که به foreground رفته (۱۵ ثانیه‌ی اخیر) */
    private fun foregroundPackage(): String? = try {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val events = usm.queryEvents(end - EVENT_WINDOW_MS, end)
        var pkg: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                pkg = event.packageName
            }
        }
        pkg
    } catch (e: Exception) {
        null
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NegahbanApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    companion object {
        var isRunning = false
            private set

        private const val NOTIFICATION_ID = 1001
        private const val POLL_INTERVAL_MS = 1000L
        private const val EVENT_WINDOW_MS = 15000L

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, WatchdogService::class.java)
                )
            } catch (e: Exception) {
            }
        }
    }
}
