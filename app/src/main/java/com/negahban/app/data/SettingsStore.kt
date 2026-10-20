package com.negahban.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.negahban.app.util.randomSalt
import com.negahban.app.util.sha256
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "negahban_settings")

data class SettingsState(
    val setupDone: Boolean = false,
    val pinLength: Int = 4,
    val breakUntil: Long = 0L,
    val instantLock: Boolean = false,
    val serverUrl: String = "",
    val deviceToken: String = ""
)

/** تنظیمات حساس (رمز والد و وضعیت‌ها) با DataStore ذخیره می‌شوند */
class SettingsStore(private val context: Context) {

    private object Keys {
        val SETUP = booleanPreferencesKey("setup_done")
        val SALT = stringPreferencesKey("pin_salt")
        val HASH = stringPreferencesKey("pin_hash")
        val PIN_LENGTH = intPreferencesKey("pin_length")
        val BREAK_UNTIL = longPreferencesKey("break_until")
        val INSTANT_LOCK = booleanPreferencesKey("instant_lock")
        val SERVER_URL = stringPreferencesKey("server_url")
        val DEVICE_TOKEN = stringPreferencesKey("device_token")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val LAST_SMS_ID = longPreferencesKey("last_sms_id")
        val LAST_NOTIF_ID = longPreferencesKey("last_notif_id")
        val LAST_CONTACTS_HASH = longPreferencesKey("last_contacts_hash")
        val LAST_CONTACTS_SYNC = longPreferencesKey("last_contacts_sync")
    }

    val state: Flow<SettingsState> = context.dataStore.data.map { prefs ->
        SettingsState(
            setupDone = prefs[Keys.SETUP] ?: false,
            pinLength = prefs[Keys.PIN_LENGTH] ?: 4,
            breakUntil = prefs[Keys.BREAK_UNTIL] ?: 0L,
            instantLock = prefs[Keys.INSTANT_LOCK] ?: false,
            serverUrl = prefs[Keys.SERVER_URL] ?: "",
            deviceToken = prefs[Keys.DEVICE_TOKEN] ?: ""
        )
    }

    suspend fun isSetupDone(): Boolean =
        context.dataStore.data.first()[Keys.SETUP] ?: false

    suspend fun setSetupDone() {
        context.dataStore.edit { it[Keys.SETUP] = true }
    }

    suspend fun setPin(pin: String) {
        val salt = randomSalt()
        context.dataStore.edit { prefs ->
            prefs[Keys.SALT] = salt
            prefs[Keys.HASH] = sha256(salt + pin)
            prefs[Keys.PIN_LENGTH] = pin.length
        }
    }

    suspend fun checkPin(pin: String): Boolean {
        val prefs = context.dataStore.data.first()
        val salt = prefs[Keys.SALT] ?: return false
        val hash = prefs[Keys.HASH] ?: return false
        return sha256(salt + pin) == hash
    }

    suspend fun breakUntil(): Long =
        context.dataStore.data.first()[Keys.BREAK_UNTIL] ?: 0L

    suspend fun setBreakUntil(until: Long) {
        context.dataStore.edit { it[Keys.BREAK_UNTIL] = until }
    }

    suspend fun instantLockEnabled(): Boolean =
        context.dataStore.data.first()[Keys.INSTANT_LOCK] ?: false

    suspend fun setInstantLock(enabled: Boolean) {
        context.dataStore.edit { it[Keys.INSTANT_LOCK] = enabled }
    }

    // ---------- اتصال به پنل مدیریت ----------

    suspend fun serverUrl(): String =
        context.dataStore.data.first()[Keys.SERVER_URL] ?: ""

    suspend fun deviceToken(): String =
        context.dataStore.data.first()[Keys.DEVICE_TOKEN] ?: ""

    suspend fun setDeviceRegistration(serverUrl: String, deviceId: String, token: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SERVER_URL] = serverUrl
            prefs[Keys.DEVICE_ID] = deviceId
            prefs[Keys.DEVICE_TOKEN] = token
        }
    }

    suspend fun clearDeviceRegistration() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.SERVER_URL)
            prefs.remove(Keys.DEVICE_ID)
            prefs.remove(Keys.DEVICE_TOKEN)
        }
    }

    // ---------- مکان‌نماهای ارسال داده (برای ارسال افزایشی) ----------

    suspend fun lastSmsId(): Long =
        context.dataStore.data.first()[Keys.LAST_SMS_ID] ?: 0L

    suspend fun setLastSmsId(value: Long) {
        context.dataStore.edit { it[Keys.LAST_SMS_ID] = value }
    }

    suspend fun lastNotifId(): Long =
        context.dataStore.data.first()[Keys.LAST_NOTIF_ID] ?: 0L

    suspend fun setLastNotifId(value: Long) {
        context.dataStore.edit { it[Keys.LAST_NOTIF_ID] = value }
    }

    // ---------- مخاطبین (ارسال کامل فقط در صورت تغییر) ----------

    suspend fun lastContactsHash(): Long =
        context.dataStore.data.first()[Keys.LAST_CONTACTS_HASH] ?: 0L

    suspend fun setLastContactsHash(value: Long) {
        context.dataStore.edit { it[Keys.LAST_CONTACTS_HASH] = value }
    }

    suspend fun lastContactsSync(): Long =
        context.dataStore.data.first()[Keys.LAST_CONTACTS_SYNC] ?: 0L

    suspend fun setLastContactsSync(value: Long) {
        context.dataStore.edit { it[Keys.LAST_CONTACTS_SYNC] = value }
    }

    /** پاک کردن همه تنظیمات (بازنشانی برنامه) */
    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }
}
