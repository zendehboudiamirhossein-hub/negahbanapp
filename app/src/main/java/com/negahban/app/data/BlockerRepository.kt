package com.negahban.app.data

import com.negahban.app.util.dayBit
import com.negahban.app.util.yesterdayBit
import java.util.Calendar

/** دلیل قفل بودن یک برنامه */
sealed class BlockReason {
    data class ScheduleLock(val ruleName: String) : BlockReason()
    data class FocusLock(val endMillis: Long) : BlockReason()
    object InstantLock : BlockReason()
}

/** تصمیم‌گیرنده اصلی: آیا این برنامه الان باید قفل باشد؟ چرا؟ */
class BlockerRepository(
    private val db: AppDatabase,
    private val settings: SettingsStore
) {

    suspend fun currentBlockReason(pkg: String): BlockReason? {
        if (!db.controlledAppDao().isControlled(pkg)) return null
        val now = System.currentTimeMillis()
        if (settings.breakUntil() > now) return null
        if (settings.instantLockEnabled()) return BlockReason.InstantLock

        val active = db.focusDao().active()
        if (active != null) {
            if (active.endMillis > now) return BlockReason.FocusLock(active.endMillis)
            db.focusDao().update(active.copy(status = STATUS_COMPLETED))
        }

        val cal = Calendar.getInstance()
        for (rule in db.scheduleDao().all()) {
            if (rule.enabled && isRuleActive(rule, cal)) return BlockReason.ScheduleLock(rule.name)
        }
        return null
    }

    suspend fun activeScheduleNow(): ScheduleRuleEntity? {
        val cal = Calendar.getInstance()
        for (rule in db.scheduleDao().all()) {
            if (rule.enabled && isRuleActive(rule, cal)) return rule
        }
        return null
    }

    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_CANCELLED = "CANCELLED"

        /**
         * آیا قانون در این لحظه فعال است؟
         * پشتیبانی از بازه‌های عبوری از نیمه‌شب (مثل ۲۲:۰۰ تا ۰۶:۰۰).
         */
        fun isRuleActive(rule: ScheduleRuleEntity, cal: Calendar): Boolean {
            val nowMinute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val crossesMidnight = rule.startMinute >= rule.endMinute
            val todayActive = (rule.daysMask and dayBit(dow)) != 0

            if (todayActive) {
                return if (crossesMidnight) {
                    nowMinute >= rule.startMinute || nowMinute < rule.endMinute
                } else {
                    nowMinute >= rule.startMinute && nowMinute < rule.endMinute
                }
            }

            // بخش صبحِ بازه‌ی شب‌گذر که از دیروز شروع شده
            if (crossesMidnight && (rule.daysMask and yesterdayBit(dow)) != 0) {
                return nowMinute < rule.endMinute
            }
            return false
        }
    }
}
