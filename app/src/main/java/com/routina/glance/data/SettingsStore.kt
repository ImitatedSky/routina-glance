package com.routina.glance.data

import android.content.Context
import androidx.core.content.edit
import com.routina.glance.capture.KnownApps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GlanceSettings(
    val monitored: Set<String> = KnownApps.defaults.toSet(),
    /** 其他 App 加進清單後，就算暫時關掉也要留在設定頁上，不然使用者找不回來 */
    val extraApps: Set<String> = emptySet(),
    /** 0 = 永久保留 */
    val retentionDays: Int = 30,
    val clearOriginal: Boolean = true,
    val appLock: Boolean = false
)

/**
 * 設定存在 SharedPreferences。
 *
 * 監聽服務與畫面在同一個行程，共用這一份物件，所以畫面改了設定，
 * 下一則通知進來時服務就讀得到，不需要另外通知。
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<GlanceSettings> = _settings.asStateFlow()

    val current: GlanceSettings get() = _settings.value

    fun update(transform: (GlanceSettings) -> GlanceSettings) {
        val next = transform(_settings.value)
        prefs.edit {
            putStringSet(KEY_MONITORED, next.monitored)
            putStringSet(KEY_EXTRA_APPS, next.extraApps)
            putInt(KEY_RETENTION, next.retentionDays)
            putBoolean(KEY_CLEAR_ORIGINAL, next.clearOriginal)
            putBoolean(KEY_APP_LOCK, next.appLock)
        }
        _settings.value = next
    }

    /** 上次清理過期訊息的時間。只有 [GlanceRepository] 用得到，不放進畫面的狀態 */
    var lastPruneAt: Long
        get() = prefs.getLong(KEY_LAST_PRUNE, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_PRUNE, value) }

    private fun load(): GlanceSettings {
        val defaults = GlanceSettings()
        return GlanceSettings(
            // getStringSet 回傳的集合不能改，複製一份
            monitored = prefs.getStringSet(KEY_MONITORED, null)?.toSet() ?: defaults.monitored,
            extraApps = prefs.getStringSet(KEY_EXTRA_APPS, null)?.toSet() ?: defaults.extraApps,
            retentionDays = prefs.getInt(KEY_RETENTION, defaults.retentionDays),
            clearOriginal = prefs.getBoolean(KEY_CLEAR_ORIGINAL, defaults.clearOriginal),
            appLock = prefs.getBoolean(KEY_APP_LOCK, defaults.appLock)
        )
    }

    private companion object {
        const val KEY_MONITORED = "monitored"
        const val KEY_EXTRA_APPS = "extra_apps"
        const val KEY_RETENTION = "retention_days"
        const val KEY_CLEAR_ORIGINAL = "clear_original"
        const val KEY_APP_LOCK = "app_lock"
        const val KEY_LAST_PRUNE = "last_prune_at"
    }
}
