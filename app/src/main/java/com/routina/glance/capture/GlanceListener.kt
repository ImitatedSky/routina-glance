package com.routina.glance.capture

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.routina.glance.GlanceApp
import com.routina.glance.data.RawEvent
import kotlinx.coroutines.launch

/**
 * 通知的入口：監聽 → 通用解析 → 各 App 修正 → 去重 → 存進資料庫。
 *
 * 系統在使用者開了「通知存取權」後綁定這個服務，不需要任何常駐服務。
 * 回呼跑在主執行緒，資料庫一律丟到 [GlanceApp.appScope]。
 *
 * 隱私：訊息內容不寫進 logcat，一個字都不寫。
 */
class GlanceListener : NotificationListenerService() {

    private val app get() = application as GlanceApp

    override fun onListenerConnected() {
        connected = true
    }

    override fun onListenerDisconnected() {
        connected = false
        // 被系統斷開（例如 App 更新後）時請系統重新綁定，否則要等到重開機
        runCatching { requestRebind(ComponentName(this, GlanceListener::class.java)) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // 系統回呼不得讓 App 崩潰：通知的 extras 由別的 App 產生，內容無法預期
        runCatching { handlePosted(sbn ?: return) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?, reason: Int) {
        runCatching { handleRemoved(sbn ?: return, reason) }
    }

    private fun handlePosted(sbn: StatusBarNotification) {
        val source = sbn.packageName ?: return
        if (source == packageName) return
        val settings = app.repository.settings.current
        if (source !in settings.monitored) return

        val notification = sbn.notification ?: return
        // 常駐通知（通話中、前景服務）不是訊息
        if (notification.flags and IGNORED_FLAGS != 0) return

        val snapshot = runCatching { SnapshotReader.read(sbn) }.getOrNull()
        val parsed = snapshot?.let { runCatching { MessageParser.parse(it) }.getOrNull() }
        val dump = runCatching { SnapshotReader.dump(sbn, snapshot, parsed) }.getOrDefault("(dump failed)")
        val key = sbn.key

        val repository = app.repository
        app.appScope.launch {
            runCatching {
                repository.addRawEvent(
                    RawEvent(at = System.currentTimeMillis(), packageName = source, kind = KIND_POSTED, body = dump)
                )
            }
            if (parsed == null) return@launch
            val stored = runCatching { repository.store(parsed); true }.getOrDefault(false)
            // 存好了才清掉原通知：原通知一點下去就會打開聊天室，對方就看到已讀。
            // 存失敗就留著，至少訊息不會兩邊都消失。
            if (stored && settings.clearOriginal) {
                runCatching { cancelNotification(key) }
            }
            runCatching { repository.pruneIfDue() }
        }
    }

    /**
     * 通知被移除時記下原因碼（REASON_CANCEL = 使用者滑掉、REASON_APP_CANCEL = App 自己收回……）。
     * 目前只記錄，之後拿來研究「對方收回訊息」時通知會怎麼變。
     */
    private fun handleRemoved(sbn: StatusBarNotification, reason: Int) {
        val source = sbn.packageName ?: return
        if (source == packageName) return
        if (source !in app.repository.settings.current.monitored) return

        val n = sbn.notification
        val body = buildString {
            appendLine("key: ${sbn.key}")
            appendLine("reason: $reason (${reasonName(reason)})")
            appendLine("postTime: ${sbn.postTime}  when: ${n?.`when`}")
            appendLine("channelId: ${runCatching { n?.channelId }.getOrNull()}")
            appendLine("flags: 0x${Integer.toHexString(n?.flags ?: 0)}")
            appendLine("title: ${runCatching { n?.extras?.getCharSequence(Notification.EXTRA_TITLE) }.getOrNull()}")
        }
        val repository = app.repository
        app.appScope.launch {
            runCatching {
                repository.addRawEvent(
                    RawEvent(at = System.currentTimeMillis(), packageName = source, kind = KIND_REMOVED, body = body)
                )
            }
        }
    }

    private fun reasonName(reason: Int): String = when (reason) {
        REASON_CLICK -> "CLICK"
        REASON_CANCEL -> "CANCEL"
        REASON_CANCEL_ALL -> "CANCEL_ALL"
        REASON_ERROR -> "ERROR"
        REASON_PACKAGE_CHANGED -> "PACKAGE_CHANGED"
        REASON_USER_STOPPED -> "USER_STOPPED"
        REASON_PACKAGE_BANNED -> "PACKAGE_BANNED"
        REASON_APP_CANCEL -> "APP_CANCEL"
        REASON_APP_CANCEL_ALL -> "APP_CANCEL_ALL"
        REASON_LISTENER_CANCEL -> "LISTENER_CANCEL"
        REASON_LISTENER_CANCEL_ALL -> "LISTENER_CANCEL_ALL"
        REASON_GROUP_SUMMARY_CANCELED -> "GROUP_SUMMARY_CANCELED"
        REASON_GROUP_OPTIMIZATION -> "GROUP_OPTIMIZATION"
        REASON_PACKAGE_SUSPENDED -> "PACKAGE_SUSPENDED"
        REASON_PROFILE_TURNED_OFF -> "PROFILE_TURNED_OFF"
        REASON_UNAUTOBUNDLED -> "UNAUTOBUNDLED"
        REASON_CHANNEL_BANNED -> "CHANNEL_BANNED"
        REASON_SNOOZED -> "SNOOZED"
        REASON_TIMEOUT -> "TIMEOUT"
        else -> "?"
    }

    companion object {
        const val KIND_POSTED = "posted"
        const val KIND_REMOVED = "removed"

        private val IGNORED_FLAGS =
            Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE

        /** 系統目前有沒有綁著這個服務。只在同一個行程裡有意義 */
        @Volatile
        var connected = false
            private set

        /** 使用者有沒有開「通知存取權」 */
        fun isEnabled(context: Context): Boolean = runCatching {
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        }.getOrDefault(false)

        /** 開了權限卻沒綁上（常見於 App 更新或被系統殺掉之後）時，請系統重新綁定 */
        fun rebindIfNeeded(context: Context) {
            if (!isEnabled(context) || connected) return
            runCatching {
                requestRebind(ComponentName(context, GlanceListener::class.java))
            }
        }
    }
}
