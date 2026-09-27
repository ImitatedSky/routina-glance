package com.routina.glance.capture

import java.security.MessageDigest

/** 一則通知解析出來的結果：屬於哪個對話、裡面有哪些訊息 */
data class ParsedNotification(
    val packageName: String,
    val conversationKey: String,
    val title: String,
    val isGroup: Boolean,
    val messages: List<ParsedMessage>
)

data class ParsedMessage(
    val sender: String,
    val text: String,
    val postedAt: Long,
    val isSelf: Boolean,
    /** true = 不是從 MessagingStyle 來的，傳訊人與時間都只是推測 */
    val fromFallback: Boolean
) {
    /** 去重用。MessagingStyle 每次更新都會把整段歷史再送一次，靠這個認出看過的訊息 */
    val textHash: String get() = hashText(text)
}

/**
 * 通用解析：任何 App 的通知都走這裡，[AppAdapters] 只修已知的怪癖。
 * 不需要任何 adapter 也能運作——沒見過的 App 就靠這一層。
 */
object MessageParser {

    fun parse(snapshot: NotificationSnapshot): ParsedNotification? {
        // 群組摘要（「3 則新訊息」）只是把底下的通知數一數，內容在各別的子通知裡
        if (snapshot.isGroupSummary) return null
        if (!AppAdapters.accept(snapshot)) return null

        val parsed = snapshot.style?.let { fromStyle(snapshot, it) } ?: fromExtras(snapshot) ?: return null
        return AppAdapters.adjust(snapshot, parsed)
    }

    private fun fromStyle(snapshot: NotificationSnapshot, style: StyleSnapshot): ParsedNotification? {
        val messages = style.messages.mapNotNull { message ->
            val text = messageText(message) ?: return@mapNotNull null
            ParsedMessage(
                sender = if (message.isSelf) "" else message.sender.orEmpty().trim(),
                text = text,
                postedAt = if (message.timestamp > 0) message.timestamp else snapshot.postedAt,
                isSelf = message.isSelf,
                fromFallback = false
            )
        }
        if (messages.isEmpty()) return null

        // 一對一的對話通常沒有 conversationTitle，標題就是對方的名字
        val title = style.conversationTitle.clean()
            ?: snapshot.conversationTitle.clean()
            ?: snapshot.title.clean()
            ?: messages.firstOrNull { !it.isSelf }?.sender?.ifBlank { null }
            ?: return null

        return ParsedNotification(
            packageName = snapshot.packageName,
            conversationKey = conversationKey(snapshot, title),
            title = title,
            isGroup = style.isGroup,
            messages = messages
        )
    }

    /** 沒有 MessagingStyle 的舊式通知：標題當對話，內文當訊息 */
    private fun fromExtras(snapshot: NotificationSnapshot): ParsedNotification? {
        val title = snapshot.conversationTitle.clean() ?: snapshot.title.clean() ?: return null
        val sender = snapshot.title.clean() ?: title

        // 順序：InboxStyle 的逐行 > BIG_TEXT > TEXT。BIG_TEXT 是展開後的全文，TEXT 常被截斷
        val bodies = snapshot.textLines.mapNotNull { it.clean() }.ifEmpty {
            listOfNotNull(snapshot.bigText.clean() ?: snapshot.text.clean())
        }
        if (bodies.isEmpty()) return null

        return ParsedNotification(
            packageName = snapshot.packageName,
            conversationKey = conversationKey(snapshot, title),
            title = title,
            isGroup = false,
            messages = bodies.map { body ->
                // 舊式通知沒有逐則的時間，只能用通知本身的時間。
                // 同一則通知被更新而 when 變了的話，舊內容會再存一次——這是這條路的已知限制。
                ParsedMessage(
                    sender = sender,
                    text = body,
                    postedAt = snapshot.postedAt,
                    isSelf = false,
                    fromFallback = true
                )
            }
        )
    }

    private fun messageText(message: StyleMessage): String? {
        message.text.clean()?.let { return it }
        val mime = message.dataMimeType ?: return null
        return when {
            mime.startsWith("image/") -> "（圖片）"
            mime.startsWith("audio/") -> "（語音）"
            mime.startsWith("video/") -> "（影片）"
            else -> "（附件）"
        }
    }

    /**
     * 對話的身分：套件名 + shortcutId（有的話）或標題。
     * shortcutId 是 App 自己給的對話 id，對方改名也不會變，所以優先用它。
     */
    fun conversationKey(snapshot: NotificationSnapshot, title: String): String {
        val id = snapshot.shortcutId.clean() ?: title
        return "${snapshot.packageName}/$id"
    }

    private fun String?.clean(): String? = this?.trim()?.ifEmpty { null }
}

/** SHA-256 取前 16 個十六進位字元，拿來比對「同一段文字」已經夠用 */
fun hashText(text: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
    return digest.take(8).joinToString("") { "%02x".format(it) }
}
