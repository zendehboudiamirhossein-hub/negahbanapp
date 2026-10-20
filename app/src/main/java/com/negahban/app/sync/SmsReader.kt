package com.negahban.app.sync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat

/** یک پیامک گوشی (از content provider پیامک) */
data class SmsEntry(
    val id: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int
)

/**
 * خواندن پیامک‌های گوشی برای ارسال به پنل والدین.
 * نیازمند مجوز READ_SMS است.
 */
object SmsReader {

    private const val PROJECTION_ID = "_id"
    private const val MAX_BODY = 2000

    /** آیا مجوز خواندن پیامک داده شده است؟ */
    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * اولین ارسال: جدیدترین پیامک‌ها (تا سقف limit).
     * بعد از آن با [since] فقط پیامک‌های جدید ارسال می‌شوند.
     */
    fun latest(context: Context, limit: Int): List<SmsEntry> =
        query(context, null, null, "$PROJECTION_ID DESC", limit)

    /** پیامک‌های جدیدتر از afterId (برای ارسال‌های بعدی) */
    fun since(context: Context, afterId: Long, limit: Int): List<SmsEntry> =
        query(context, "$PROJECTION_ID > ?", arrayOf(afterId.toString()), "$PROJECTION_ID ASC", limit)

    private fun query(
        context: Context,
        selection: String?,
        args: Array<String>?,
        sortOrder: String,
        limit: Int
    ): List<SmsEntry> {
        if (!hasPermission(context)) return emptyList()
        val result = mutableListOf<SmsEntry>()
        try {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(PROJECTION_ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE),
                selection,
                args,
                "$sortOrder LIMIT $limit"
            )?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(PROJECTION_ID)
                val addrIdx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val typeIdx = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
                while (c.moveToNext()) {
                    val body = (c.getString(bodyIdx) ?: "").take(MAX_BODY)
                    result.add(
                        SmsEntry(
                            id = c.getLong(idIdx),
                            address = c.getString(addrIdx) ?: "",
                            body = body,
                            date = c.getLong(dateIdx),
                            type = c.getInt(typeIdx)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // دسترسی به پیامک ممکن است توسط سیستم محدود شده باشد
        }
        return result
    }
}
