package com.negahban.app.sync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/** یک مخاطب گوشی به همراه همه شماره‌های ثبت‌شده‌اش */
data class ContactEntry(
    val name: String,
    val phones: List<String>
)

/**
 * خواندن دفترچه مخاطبین گوشی برای نمایش در پنل والدین.
 * نیازمند مجوز READ_CONTACTS است.
 *
 * لیست مخاطبین فقط وقتی برای پنل ارسال می‌شود که تغییری کرده باشد
 * (یا حداقل هر ۲۴ ساعت یک بار به‌صورت کامل تازه‌سازی شود).
 */
object ContactsReader {

    /** حداکثر تعداد مخاطب ارسالی در هر بار */
    private const val MAX_CONTACTS = 3000

    private const val MAX_NAME = 120
    private const val MAX_PHONE = 40

    /** انباشتگر شماره‌های یک مخاطب */
    private class Accumulator(val name: String) {
        val phones = LinkedHashSet<String>()
    }

    /** آیا مجوز خواندن مخاطبین داده شده است؟ */
    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * همه مخاطبین دارای شماره تلفن، گروه‌بندی‌شده بر اساس شناسه مخاطب.
     * خروجی بر اساس نام مرتب نیست؛ ترتیب از سمت ContentProvider است.
     */
    fun read(context: Context): List<ContactEntry> {
        if (!hasPermission(context)) return emptyList()
        val byId = LinkedHashMap<Long, Accumulator>()
        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " IS NOT NULL",
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " ASC"
            )?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val phoneIdx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (c.moveToNext()) {
                    if (byId.size >= MAX_CONTACTS) break
                    val name = (c.getString(nameIdx) ?: "").trim().take(MAX_NAME)
                    if (name.isEmpty()) continue
                    val phone = (c.getString(phoneIdx) ?: "").trim().take(MAX_PHONE)
                    if (phone.isEmpty()) continue
                    byId.getOrPut(c.getLong(idIdx)) { Accumulator(name) }.phones.add(phone)
                }
            }
        } catch (e: Exception) {
            // دسترسی به مخاطبین ممکن است توسط سیستم محدود شده باشد
        }
        return byId.values.map { ContactEntry(it.name, it.phones.toList()) }
    }

    /**
     * امضای دفترچه مخاطبین: با هر افزودن/حذف/ویرایش مخاطب یا شماره،
     * این مقدار تغییر می‌کند تا لیست بدون تغییر دوباره ارسال نشود.
     */
    fun signature(contacts: List<ContactEntry>): Long {
        var hash = 1125899906842597L
        for (c in contacts) {
            hash = 31L * hash + c.name.hashCode()
            hash = 31L * hash + c.phones.joinToString("|").hashCode()
        }
        return hash
    }
}
