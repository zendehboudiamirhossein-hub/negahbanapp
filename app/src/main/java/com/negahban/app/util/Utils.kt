package com.negahban.app.util

import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import com.negahban.app.NegahbanApp
import java.security.MessageDigest
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

/** دسترسی راحت به کلاس Application از هر Context */
val Context.app: NegahbanApp
    get() = applicationContext as NegahbanApp

/** ابزارهای زبان فارسی: اعداد، تاریخ و روزهای هفته */
object Fa {
    val months = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private val weekdaysFull = mapOf(
        Calendar.SATURDAY to "شنبه",
        Calendar.SUNDAY to "یکشنبه",
        Calendar.MONDAY to "دوشنبه",
        Calendar.TUESDAY to "سه‌شنبه",
        Calendar.WEDNESDAY to "چهارشنبه",
        Calendar.THURSDAY to "پنجشنبه",
        Calendar.FRIDAY to "جمعه"
    )

    private val weekdaysShort = mapOf(
        Calendar.SATURDAY to "ش",
        Calendar.SUNDAY to "ی",
        Calendar.MONDAY to "د",
        Calendar.TUESDAY to "س",
        Calendar.WEDNESDAY to "چ",
        Calendar.THURSDAY to "پ",
        Calendar.FRIDAY to "ج"
    )

    /** تبدیل ارقام لاتین یک رشته به ارقام فارسی */
    fun digits(source: String): String = buildString {
        for (c in source) append(if (c in '0'..'9') ('۰'.code + (c - '0')).toChar() else c)
    }

    fun num(value: Long): String = digits(value.toString())

    fun num(value: Int): String = digits(value.toString())

    fun weekdayName(dayOfWeek: Int): String = weekdaysFull[dayOfWeek] ?: ""

    fun weekdayShort(dayOfWeek: Int): String = weekdaysShort[dayOfWeek] ?: ""

    /** برچسب کوتاه روز هفته برای N روز پیش */
    fun weekdayShortDaysAgo(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return weekdayShort(cal.get(Calendar.DAY_OF_WEEK))
    }
}

/** «۲ ساعت و ۳۰ دقیقه» */
fun formatMinutes(minutes: Long): String {
    val m = if (minutes < 0) 0L else minutes
    return when {
        m == 0L -> "۰ دقیقه"
        m < 60 -> Fa.num(m) + " دقیقه"
        else -> {
            val h = m / 60
            val r = m % 60
            if (r == 0L) Fa.num(h) + " ساعت" else Fa.num(h) + " ساعت و " + Fa.num(r) + " دقیقه"
        }
    }
}

/** فرم فشرده: «۲س۳۰د» یا «۴۵د» */
fun formatCompactMinutes(minutes: Long): String {
    val m = if (minutes < 0) 0L else minutes
    val h = m / 60
    val r = m % 60
    return if (h > 0) Fa.num(h) + "س" + (if (r > 0) Fa.num(r) + "د" else "") else Fa.num(r) + "د"
}

/** شمارش معکوس «۰۴:۳۲» */
fun formatCountdown(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0)
    val m = total / 60
    val s = total % 60
    return Fa.digits(String.format(Locale.US, "%02d:%02d", m, s))
}

/** «۲۲:۳۰» از تعداد دقیقه‌های گذشته از نیمه‌شب */
fun formatMinuteOfDay(minuteOfDay: Int): String =
    Fa.digits(String.format(Locale.US, "%02d:%02d", minuteOfDay / 60, minuteOfDay % 60))

/** شروع روز (۰۰:۰۰) برای N روز پیش؛ صفر یعنی امروز، عدد منفی یعنی آینده */
fun dayStartMillis(daysAgo: Int = 0): Long {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun todayStartMillis(): Long = dayStartMillis(0)

/** شروع هفته (شنبه) */
fun startOfWeekMillis(): Long {
    val cal = Calendar.getInstance()
    val idx = cal.get(Calendar.DAY_OF_WEEK) % 7 // شنبه = ۰
    return dayStartMillis(idx)
}

/** تبدیل میلادی به جلالی */
fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
    val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    var jy = if (gy <= 1600) 0 else 979
    val gy2 = gy - if (gy <= 1600) 621 else 1600
    val gy3 = if (gm > 2) gy2 + 1 else gy2
    var days = 365 * gy2 + (gy3 + 3) / 4 - (gy3 + 99) / 100 + (gy3 + 399) / 400 - 80 + gd + gdm[gm - 1]
    jy += 33 * (days / 12053)
    days %= 12053
    jy += 4 * (days / 1461)
    days %= 1461
    jy += (days - 1) / 365
    if (days > 365) days = (days - 1) % 365
    val jm = if (days < 186) 1 + days / 31 else 7 + (days - 186) / 30
    val jd = 1 + if (days < 186) days % 31 else (days - 186) % 30
    return Triple(jy, jm, jd)
}

/** «یکشنبه ۲۸ مهر ۱۴۰۵» */
fun jalaliDateString(): String {
    val cal = Calendar.getInstance()
    val (jy, jm, jd) = gregorianToJalali(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
    return Fa.weekdayName(cal.get(Calendar.DAY_OF_WEEK)) + " " + Fa.num(jd) + " " + Fa.months[jm - 1] + " " + Fa.num(jy)
}

/** بیت امروز هفته (شنبه = بیت ۰ ... جمعه = بیت ۶) */
fun dayBit(dayOfWeek: Int): Int = 1 shl (dayOfWeek % 7)

/** بیت دیروز هفته */
fun yesterdayBit(dayOfWeek: Int): Int = 1 shl ((dayOfWeek + 6) % 7)

fun sha256(input: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray(Charsets.UTF_8))
        .joinToString("") { String.format(Locale.US, "%02x", it) }

fun randomSalt(): String =
    List(16) { Random.nextInt(256) }.joinToString("") { String.format(Locale.US, "%02x", it) }

/** تبدیل Drawable به ImageBitmap برای نمایش در Compose */
fun Drawable.toImageBitmap(): ImageBitmap {
    if (this is BitmapDrawable && bitmap != null) return bitmap.asImageBitmap()
    val width = intrinsicWidth.coerceAtLeast(1)
    val height = intrinsicHeight.coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    setBounds(0, 0, width, height)
    draw(canvas)
    return bmp.asImageBitmap()
}

// ---------- بررسی و درخواست مجوزها ----------

fun hasUsageAccess(context: Context): Boolean = try {
    val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = ops.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )
    mode == AppOpsManager.MODE_ALLOWED
} catch (e: Exception) {
    false
}

fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

fun notificationsEnabled(context: Context): Boolean = try {
    NotificationManagerCompat.from(context).areNotificationsEnabled()
} catch (e: Exception) {
    false
}

fun isIgnoringBatteryOptimizations(context: Context): Boolean = try {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    pm.isIgnoringBatteryOptimizations(context.packageName)
} catch (e: Exception) {
    false
}

fun openUsageAccessSettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: ActivityNotFoundException) {
    }
}

fun openOverlaySettings(context: Context) {
    try {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.packageName)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: ActivityNotFoundException) {
    }
}

fun openBatterySettings(context: Context) {
    try {
        context.startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:" + context.packageName)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e2: ActivityNotFoundException) {
        }
    }
}

fun openAppNotificationSettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: ActivityNotFoundException) {
    }
}

/** صفحه «دسترسی اعلان‌ها» برای فعال کردن گوشِ اعلان‌های نگهبان */
fun openNotificationAccessSettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: ActivityNotFoundException) {
    }
}

/** درصد فعلی باتری */
fun batteryLevel(context: Context): Int? = try {
    val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    if (level >= 0 && scale > 0) level * 100 / scale else null
} catch (e: Exception) {
    null
}

/** نام مدل دستگاه */
fun deviceModel(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

/** نسخه اندروید */
fun androidVersion(): String = Build.VERSION.RELEASE ?: ""

/** نسخه برنامه */
fun appVersion(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
} catch (e: Exception) {
    ""
}

/** کلید روز «YYYY-MM-DD» از زمان داده‌شده */
fun dayKeyOf(millis: Long): String {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    return String.format(
        Locale.US, "%04d-%02d-%02d",
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}
