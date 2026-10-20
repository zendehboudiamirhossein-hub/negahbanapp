package com.negahban.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** برنامه‌ای که والد تصمیم گرفته کنترل شود */
@Entity(tableName = "controlled_apps")
data class ControlledAppEntity(
    @PrimaryKey val packageName: String
)

/** قانون زمان‌بندی؛ مثلاً هر شب ۲۲:۰۰ تا ۰۶:۰۰ */
@Entity(tableName = "schedule_rules")
data class ScheduleRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** دقیقه گذشته از نیمه‌شب، مثل ۲۲:۰۰ = ۱۳۲۰ */
    val startMinute: Int,
    val endMinute: Int,
    /** بیت‌های روز هفته: شنبه = بیت ۰ ... جمعه = بیت ۶ */
    val daysMask: Int,
    val enabled: Boolean = true
)

/** جلسه تمرکز */
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMillis: Long,
    val endMillis: Long,
    val plannedMinutes: Int,
    /** ACTIVE / COMPLETED / CANCELLED */
    val status: String
)

/** اعلان گوشی که توسط NotificationListener ضبط شده و منتظر ارسال به پنل است */
@Entity(tableName = "notification_events")
data class NotificationEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val title: String,
    val text: String,
    val atMillis: Long
)

/** عکسی که thumbnail آن قبلاً به پنل ارسال شده (برای جلوگیری از ارسال مجدد) */
@Entity(tableName = "synced_photos")
data class SyncedPhotoEntity(
    @PrimaryKey val mediaId: Long
)
