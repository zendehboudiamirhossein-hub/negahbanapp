package com.negahban.app.service

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.negahban.app.NegahbanApp
import com.negahban.app.data.NotificationEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * گوشِ اعلان‌های گوشی: هر اعلان برنامه‌های دیگر (به‌جز خود نگهبان)
 * در دیتابیس محلی ذخیره می‌شود تا SyncWorker آن را به پنل والدین بفرستد.
 *
 * فعال‌سازی: تنظیمات سیستم ← دسترسی اعلان‌ها (کاربر باید دستی تیک نگهبان را بزند).
 */
class NotificationCollector : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            if (sbn.packageName == packageName) return
            val n = sbn.notification ?: return
            // اعلان‌های «جمع‌کننده گروه» محتوای تکراری دارند
            if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
            val extras = n.extras ?: return

            var title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
            var text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
            if (text.isEmpty()) text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim() ?: ""

            // اعلان‌های بی‌متن (مثل آپدیت‌های دائمی پخش‌کننده موسیقی) ارزش گزارشی ندارند
            if (title.isEmpty() && text.isEmpty()) return

            val event = NotificationEventEntity(
                packageName = sbn.packageName,
                title = title.take(300),
                text = text.take(1000),
                atMillis = sbn.postTime
            )

            val app = applicationContext as? NegahbanApp ?: return
            scope.launch { app.db.notificationEventDao().insert(event) }
        } catch (e: Exception) {
            // هرگز نباید کرش کنیم؛ اعلان بعدی دوباره امتحان می‌شود
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** آیا دسترسی «خواندن اعلان‌ها» به نگهبان داده شده است؟ */
        fun isEnabled(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
}
