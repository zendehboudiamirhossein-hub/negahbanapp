package com.negahban.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ControlledAppDao {

    @Query("SELECT * FROM controlled_apps")
    fun observeAll(): Flow<List<ControlledAppEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM controlled_apps WHERE packageName = :pkg)")
    suspend fun isControlled(pkg: String): Boolean

    @Query("SELECT packageName FROM controlled_apps")
    suspend fun allPackages(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: ControlledAppEntity)

    @Query("DELETE FROM controlled_apps")
    suspend fun deleteAll()

    @Query("DELETE FROM controlled_apps WHERE packageName = :pkg")
    suspend fun delete(pkg: String)
}

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedule_rules ORDER BY startMinute")
    fun observeAll(): Flow<List<ScheduleRuleEntity>>

    @Query("SELECT * FROM schedule_rules")
    suspend fun all(): List<ScheduleRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: ScheduleRuleEntity): Long

    @Query("DELETE FROM schedule_rules")
    suspend fun deleteAll()

    @Update
    suspend fun update(rule: ScheduleRuleEntity)

    @Delete
    suspend fun delete(rule: ScheduleRuleEntity)
}

@Dao
interface FocusDao {

    @Query("SELECT * FROM focus_sessions WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun active(): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions WHERE status = 'ACTIVE' LIMIT 1")
    fun observeActive(): Flow<FocusSessionEntity?>

    @Insert
    suspend fun insert(session: FocusSessionEntity): Long

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAll()

    @Update
    suspend fun update(session: FocusSessionEntity)

    @Query("SELECT COALESCE(SUM(plannedMinutes), 0) FROM focus_sessions WHERE status = 'COMPLETED' AND startMillis >= :fromMillis")
    suspend fun completedMinutesSince(fromMillis: Long): Int

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE status = 'COMPLETED' AND startMillis >= :fromMillis")
    suspend fun completedCountSince(fromMillis: Long): Int
}

@Dao
interface NotificationEventDao {

    @Insert
    suspend fun insert(event: NotificationEventEntity): Long

    /** اعلان‌های جدید بعد از آخرین ارسال (برای بسته بعدی) */
    @Query("SELECT * FROM notification_events WHERE id > :afterId ORDER BY id LIMIT :limit")
    suspend fun since(afterId: Long, limit: Int): List<NotificationEventEntity>

    @Query("DELETE FROM notification_events WHERE id <= :untilId")
    suspend fun deleteSynced(untilId: Long)

    @Query("DELETE FROM notification_events")
    suspend fun deleteAll()
}

@Dao
interface SyncedPhotoDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(photos: List<SyncedPhotoEntity>)

    @Query("SELECT mediaId FROM synced_photos")
    suspend fun allIds(): List<Long>

    @Query("DELETE FROM synced_photos")
    suspend fun deleteAll()
}
