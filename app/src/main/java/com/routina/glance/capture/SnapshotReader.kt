package com.routina.glance.capture

import android.app.Notification
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

/**
 * 把系統給的通知抄成 [NotificationSnapshot]，以及做成開發者頁用的原始紀錄。
 *
 * extras 是別的 App 塞的，型別與內容都無法預期，每一個欄位都各自包 runCatching：
 * 一個欄位讀壞了，其他欄位照樣要拿得到。
 */
object SnapshotReader {

    fun read(sbn: StatusBarNotification): NotificationSnapshot {
        val n = sbn.notification
        val extras = n.extras ?: Bundle.EMPTY
        return NotificationSnapshot(
            packageName = sbn.packageName,
            postedAt = if (n.`when` > 0) n.`when` else sbn.postTime,
            isGroupSummary = n.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            channelId = safe { n.channelId },
            category = safe { n.category },
            shortcutId = safe { n.shortcutId },
            title = safe { extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() },
            text = safe { extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() },
            bigText = safe { extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() },
            textLines = safe {
                extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.map { it.toString() }
            }.orEmpty(),
            conversationTitle = safe {
                extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            },
            style = safe { readStyle(n) }
        )
    }

    private fun readStyle(n: Notification): StyleSnapshot? {
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)
            ?: return null
        val user = style.user
        return StyleSnapshot(
            conversationTitle = style.conversationTitle?.toString(),
            isGroup = style.isGroupConversation,
            userName = user.name?.toString(),
            messages = style.messages.map { message ->
                val person = message.person
                // person 為 null 代表是使用者自己傳的（例如從通知直接回覆）
                val isSelf = person == null ||
                    (person.key != null && person.key == user.key) ||
                    (person.key == null && person.name != null &&
                        person.name.toString() == user.name?.toString())
                StyleMessage(
                    sender = person?.name?.toString(),
                    text = message.text?.toString(),
                    timestamp = message.timestamp,
                    isSelf = isSelf,
                    dataMimeType = message.dataMimeType
                )
            }
        )
    }

    /** 開發者頁的原始紀錄：通知的中繼資料、所有 extras，以及解析結果 */
    fun dump(sbn: StatusBarNotification, snapshot: NotificationSnapshot?, parsed: ParsedNotification?): String =
        buildString {
            val n = sbn.notification
            appendLine("key: ${sbn.key}")
            appendLine("postTime: ${sbn.postTime}  when: ${n.`when`}")
            appendLine("channelId: ${safe { n.channelId }}  category: ${safe { n.category }}")
            appendLine("flags: 0x${Integer.toHexString(n.flags)}  group: ${safe { n.group }}")
            appendLine("shortcutId: ${safe { n.shortcutId }}")
            appendLine("-- extras --")
            val extras = n.extras
            if (extras != null) {
                for (key in safe { extras.keySet().sorted() }.orEmpty()) {
                    appendLine("$key = ${describe(safe { @Suppress("DEPRECATION") extras.get(key) })}")
                }
            }
            appendLine("-- MessagingStyle --")
            val style = snapshot?.style
            if (style == null) {
                appendLine("(none)")
            } else {
                appendLine("conversationTitle: ${style.conversationTitle}  isGroup: ${style.isGroup}  user: ${style.userName}")
                style.messages.forEach { m ->
                    appendLine("[${m.timestamp}] ${if (m.isSelf) "(self)" else m.sender}: ${m.text} ${m.dataMimeType ?: ""}")
                }
            }
            appendLine("-- parsed --")
            if (parsed == null) {
                appendLine("(dropped)")
            } else {
                appendLine("key: ${parsed.conversationKey}  title: ${parsed.title}  group: ${parsed.isGroup}")
                appendLine("${parsed.messages.size} messages")
            }
        }.take(MAX_DUMP_CHARS)

    /** extras 的值什麼型別都有：陣列、Bundle（MessagingStyle 的每一則訊息就是一個 Bundle） */
    private fun describe(value: Any?): String = when {
        value == null -> "null"
        value is CharSequence -> value.toString()
        value is Bundle -> value.keySet().sorted().joinToString(prefix = "{", postfix = "}") { key ->
            "$key=${describe(safe { @Suppress("DEPRECATION") value.get(key) })}"
        }
        value is Array<*> -> value.joinToString(prefix = "[", postfix = "]") { describe(it) }
        // Person 的 toString 只有位址，看不出是誰（android.app.Person 是 Android 9 才有的）
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && value is android.app.Person ->
            "Person(name=${value.name}, key=${value.key}, bot=${value.isBot})"
        value is IntArray -> value.contentToString()
        value is LongArray -> value.contentToString()
        else -> value.toString()
    }.take(MAX_VALUE_CHARS)

    private inline fun <T> safe(block: () -> T): T? = runCatching(block).getOrNull()

    private const val MAX_VALUE_CHARS = 1_000
    private const val MAX_DUMP_CHARS = 6_000
}
