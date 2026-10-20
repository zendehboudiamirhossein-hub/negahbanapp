package com.negahban.app.sync

import com.negahban.app.data.NotificationEventEntity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** خطای ارتباط با سرور */
class SyncException(message: String) : Exception(message)

/** نتیجه‌ی عکس دوربین برای یک فرمان پنل */
data class CaptureResult(
    val commandId: String,
    val base64: String,
    val atMillis: Long
)

/**
 * ارتباط با پنل مدیریت (APIهای REST).
 * همه فراخوانی‌ها blocking هستند و باید داخل کورتین IO صدا زده شوند.
 */
object SyncApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    /** حذف اسلش انتهایی از آدرس سرور */
    fun normalizeUrl(raw: String): String {
        var url = raw.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        while (url.endsWith("/")) url = url.dropLast(1)
        return url
    }

    /**
     * ثبت‌نام دستگاه با کد جفت‌سازی.
     * خروجی: جفت (شناسه دستگاه، توکن)
     */
    fun register(serverUrl: String, pairingCode: String, name: String): Pair<String, String> {
        val body = JSONObject()
            .put("pairingCode", pairingCode)
            .put("name", name)
            .put("model", com.negahban.app.util.deviceModel())
            .put("androidVersion", com.negahban.app.util.androidVersion())
            .put("appVersion", "1.0")

        val json = post("$serverUrl/api/device/register", body.toString(), token = null)
        val deviceId = json.optString("deviceId")
        val token = json.optString("token")
        if (deviceId.isEmpty() || token.isEmpty()) throw SyncException("پاسخ سرور نامعتبر بود")
        return deviceId to token
    }

    /**
     * ارسال گزارش همگام‌سازی به پنل:
     * لیست برنامه‌ها، مصرف روزانه، باتری، موقعیت و (در صورت مجوز) پیامک‌ها،
     * اعلان‌ها، گالری + thumbnail عکس‌ها.
     * خروجی: پاسخ سرور (شامل فرمان‌های در انتظار، مثل عکس‌برداری).
     */
    fun sync(
        serverUrl: String,
        token: String,
        battery: Int?,
        apps: List<Triple<String, String, Boolean>>, // packageName, label, controlled
        usage: List<Triple<String, String, Long>>,    // day, packageName, minutes
        sms: List<SmsEntry>? = null,
        notifications: List<NotificationEventEntity>? = null,
        photos: List<PhotoEntry>? = null,
        thumbs: List<PhotoThumb>? = null,
        contacts: List<ContactEntry>? = null,
        location: LocationReader.Fix? = null
    ): JSONObject {
        val appsArray = JSONArray()
        for ((pkg, label, controlled) in apps) {
            appsArray.put(JSONObject().put("packageName", pkg).put("label", label).put("controlled", controlled))
        }
        val usageArray = JSONArray()
        for ((day, pkg, minutes) in usage) {
            usageArray.put(JSONObject().put("day", day).put("packageName", pkg).put("minutes", minutes))
        }
        val smsArray = JSONArray()
        sms?.forEach { s ->
            smsArray.put(
                JSONObject()
                    .put("id", s.id)
                    .put("address", s.address)
                    .put("body", s.body)
                    .put("date", s.date)
                    .put("type", s.type)
            )
        }
        val notifArray = JSONArray()
        notifications?.forEach { n ->
            notifArray.put(
                JSONObject()
                    .put("id", n.id)
                    .put("packageName", n.packageName)
                    .put("title", n.title)
                    .put("text", n.text)
                    .put("at", n.atMillis)
            )
        }
        val photosArray = JSONArray()
        photos?.forEach { p ->
            photosArray.put(
                JSONObject()
                    .put("mediaId", p.mediaId)
                    .put("takenAt", p.takenAt)
                    .put("size", p.size)
                    .put("bucket", p.bucket)
            )
        }
        val thumbsArray = JSONArray()
        thumbs?.forEach { t ->
            thumbsArray.put(
                JSONObject()
                    .put("mediaId", t.mediaId)
                    .put("image", t.base64)
            )
        }
        val contactsArray = JSONArray()
        contacts?.forEach { c ->
            val phones = JSONArray()
            c.phones.forEach { phones.put(it) }
            contactsArray.put(
                JSONObject()
                    .put("name", c.name)
                    .put("phones", phones)
            )
        }
        val body = JSONObject()
        if (battery != null) body.put("battery", battery)
        if (apps.isNotEmpty()) body.put("apps", appsArray)
        if (usage.isNotEmpty()) body.put("usage", usageArray)
        if (!sms.isNullOrEmpty()) body.put("sms", smsArray)
        if (!notifications.isNullOrEmpty()) body.put("notifications", notifArray)
        if (!photos.isNullOrEmpty()) body.put("photos", photosArray)
        if (!thumbs.isNullOrEmpty()) body.put("thumbs", thumbsArray)
        if (!contacts.isNullOrEmpty()) body.put("contacts", contactsArray)
        if (location != null) {
            body.put(
                "location",
                JSONObject()
                    .put("lat", location.lat)
                    .put("lng", location.lng)
                    .put("accuracy", location.accuracy ?: JSONObject.NULL)
                    .put("provider", location.provider)
                    .put("at", location.at)
            )
        }

        return post("$serverUrl/api/device/sync", body.toString(), token = token)
    }

    /**
     * ارسال نتیجه‌ی فرمان‌های عکس‌برداری به پنل.
     */
    fun sendCaptures(
        serverUrl: String,
        token: String,
        captures: List<CaptureResult>
    ): Boolean {
        val arr = JSONArray()
        captures.forEach { c ->
            arr.put(
                JSONObject()
                    .put("commandId", c.commandId)
                    .put("image", c.base64)
                    .put("at", c.atMillis)
            )
        }
        val body = JSONObject().put("captures", arr)
        post("$serverUrl/api/device/captures", body.toString(), token = token)
        return true
    }

    private fun post(url: String, jsonBody: String, token: String?): JSONObject {
        val builder = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody(JSON))
            .header("Content-Type", "application/json")
        if (token != null) builder.header("X-Device-Token", token)

        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string() ?: ""
            val json = try {
                JSONObject(text)
            } catch (e: Exception) {
                JSONObject()
            }
            if (!response.isSuccessful) {
                throw SyncException(json.optString("error", "خطای سرور (کد ${response.code})"))
            }
            return json
        }
    }
}
