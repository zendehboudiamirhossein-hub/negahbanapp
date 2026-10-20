package com.negahban.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ControlledAppEntity::class,
        ScheduleRuleEntity::class,
        FocusSessionEntity::class,
        NotificationEventEntity::class,
        SyncedPhotoEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun controlledAppDao(): ControlledAppDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun focusDao(): FocusDao
    abstract fun notificationEventDao(): NotificationEventDao
    abstract fun syncedPhotoDao(): SyncedPhotoDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "negahban.db")
                // برنامه هنوز منتشر نشده؛ تغییر اسکیما = بازسازی بدون مهاجرت
                .fallbackToDestructiveMigration()
                .build()
    }
}
