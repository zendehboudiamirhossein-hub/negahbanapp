package com.negahban.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.negahban.app.NegahbanApp
import kotlinx.coroutines.runBlocking

/**
 * بعد از روشن شدن گوشی یا به‌روزرسانی برنامه:
 * سرویس پایش دوباره راه‌اندازی می‌شود و همگام‌سازی با پنل از سر گرفته می‌شود.
 *
 * توجه: آدرس سرور و توکن دستگاه در DataStore ماندگارند، پس بعد از ری‌استارت
 * نیازی به جفت‌سازی دوباره نیست. زمان‌بندی دوره‌ای WorkManager هم خودش بعد از
 * بوت بازیابی می‌شود؛ WatchdogService در onCreate آن را دوباره ثبت می‌کند.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val app = context.applicationContext as NegahbanApp
        val shouldStart = runBlocking { app.settings.isSetupDone() }
        if (shouldStart) {
            // شروع سرویس؛ در onCreate سرویس، WakeLock گرفته می‌شود و
            // زمان‌بند همگام‌سازی + یک ارسال فوری (بعد از وصل شدن اینترنت) ثبت می‌شود
            WatchdogService.start(context)
        }
    }
}
