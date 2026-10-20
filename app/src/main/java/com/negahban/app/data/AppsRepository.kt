package com.negahban.app.data

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import com.negahban.app.util.dayStartMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** دسترسی به لیست برنامه‌های نصب‌شده و آمار مصرف آن‌ها */
class AppsRepository(private val context: Context) {

    data class AppInfo(
        val packageName: String,
        val label: String,
        val icon: Drawable
    )

    data class AppUsage(
        val packageName: String,
        val label: String,
        val minutes: Long,
        val controlled: Boolean
    )

    data class DayUsage(
        val minutes: Long,
        val daysAgo: Int
    )

    /** مصرف یک برنامه در یک روز مشخص */
    data class DailyAppUsage(
        val day: String,
        val packageName: String,
        val minutes: Long
    )

    private val pm = context.packageManager
    private val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val labelCache = HashMap<String, String>()

    /** پکیج‌های لانچر (صفحه اصلی) — نباید قابل کنترل باشند وگرنه گوشی کاملاً قفل می‌شود */
    fun launcherPackages(): Set<String> = try {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        pm.queryIntentActivities(home, 0).map { it.activityInfo.packageName }.toSet()
    } catch (e: Exception) {
        emptySet()
    }

    /** لیست برنامه‌های قابل اجرا (به‌جز خود نگهبان، لانچر و SystemUI) */
    suspend fun launchableApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val out = ArrayList<AppInfo>()
        val excluded = launcherPackages() + context.packageName + "com.android.systemui"
        try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val ris = pm.queryIntentActivities(intent, 0)
            for (ri in ris) {
                val pkg = ri.activityInfo.packageName
                if (pkg in excluded) continue
                val label = try {
                    ri.activityInfo.applicationInfo.loadLabel(pm).toString()
                } catch (e: Exception) {
                    pkg
                }
                val icon = try {
                    ri.activityInfo.applicationInfo.loadIcon(pm)
                } catch (e: Exception) {
                    continue
                }
                out.add(AppInfo(pkg, label, icon))
            }
        } catch (e: Exception) {
        }
        out.distinctBy { it.packageName }.sortedBy { it.label }
    }

    fun appLabel(pkg: String): String = synchronized(labelCache) {
        labelCache[pkg]?.let { return it }
        val label = try {
            val info: ApplicationInfo = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            pkg
        }
        labelCache[pkg] = label
        label
    }

    /** مجموع زمان فعال (foreground) هر برنامه در بازه داده‌شده */
    private fun usageMillis(start: Long, end: Long): Map<String, Long> = try {
        val stats = usm.queryAndAggregateUsageStats(start, end)
        val out = HashMap<String, Long>()
        for ((pkg, s) in stats) {
            val t = s.totalTimeInForeground
            if (t > 0 && pkg != context.packageName) out[pkg] = t
        }
        out
    } catch (e: Exception) {
        emptyMap()
    }

    /** مصرف روزانه هر برنامه برای N روز اخیر — برای ارسال به پنل */
    suspend fun perAppDailyUsage(days: Int): List<DailyAppUsage> = withContext(Dispatchers.IO) {
        val out = ArrayList<DailyAppUsage>()
        for (ago in days - 1 downTo 0) {
            val start = dayStartMillis(ago)
            val end = dayStartMillis(ago - 1)
            val dayKey = com.negahban.app.util.dayKeyOf(start)
            for ((pkg, millis) in usageMillis(start, end)) {
                out.add(DailyAppUsage(dayKey, pkg, millis / 60000L))
            }
        }
        out
    }

    /** مصرف کل هر روز برای N روز اخیر (قدیمی‌ترین اول؛ آخرین عضو = امروز) */
    suspend fun dailyUsageMinutes(days: Int): List<DayUsage> = withContext(Dispatchers.IO) {
        (days - 1 downTo 0).map { ago ->
            val start = dayStartMillis(ago)
            val end = dayStartMillis(ago - 1)
            DayUsage(usageMillis(start, end).values.sum() / 60000L, ago)
        }
    }

    /** مصرف هر برنامه از زمان داده‌شده تا الان، مرتب از پر مصرف‌ترین */
    suspend fun appUsageSince(start: Long, controlled: Set<String>): List<AppUsage> =
        withContext(Dispatchers.IO) {
            usageMillis(start, System.currentTimeMillis()).map { (pkg, millis) ->
                AppUsage(pkg, appLabel(pkg), millis / 60000L, pkg in controlled)
            }.filter { it.minutes > 0 }.sortedByDescending { it.minutes }
        }

    suspend fun totalMinutesSince(start: Long): Long = withContext(Dispatchers.IO) {
        usageMillis(start, System.currentTimeMillis()).values.sum() / 60000L
    }
}
