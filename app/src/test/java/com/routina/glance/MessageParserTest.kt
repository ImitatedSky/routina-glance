package com.routina.glance

import com.routina.glance.capture.KnownApps
import com.routina.glance.capture.MessageParser
import com.routina.glance.capture.NotificationSnapshot
import com.routina.glance.capture.StyleMessage
import com.routina.glance.capture.StyleSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageParserTest {

    private fun styleSnapshot(
        messages: List<StyleMessage>,
        conversationTitle: String? = "週末爬山",
        isGroup: Boolean = true,
        packageName: String = KnownApps.LINE
    ) = NotificationSnapshot(
        packageName = packageName,
        postedAt = 5_000,
        title = "阿明",
        text = messages.lastOrNull()?.text,
        style = StyleSnapshot(conversationTitle = conversationTitle, isGroup = isGroup, messages = messages)
    )

    @Test
    fun messagingStyleKeepsEveryMessage() {
        val parsed = MessageParser.parse(
            styleSnapshot(
                listOf(
                    StyleMessage("阿明", "明天幾點？", 1_000),
                    StyleMessage("小華", "七點車站", 2_000),
                    StyleMessage(null, "好", 3_000, isSelf = true)
                )
            )
        )

        assertNotNull(parsed)
        parsed!!
        assertEquals("週末爬山", parsed.title)
        assertTrue(parsed.isGroup)
        assertEquals(listOf("阿明", "小華", ""), parsed.messages.map { it.sender })
        assertEquals(listOf("明天幾點？", "七點車站", "好"), parsed.messages.map { it.text })
        assertEquals(listOf(1_000L, 2_000L, 3_000L), parsed.messages.map { it.postedAt })
        assertTrue(parsed.messages[2].isSelf)
        assertFalse(parsed.messages[0].fromFallback)
    }

    @Test
    fun oneToOneStyleUsesTitleAsConversation() {
        val snapshot = styleSnapshot(
            listOf(StyleMessage("阿明", "在嗎", 1_000)),
            conversationTitle = null,
            isGroup = false
        )
        val parsed = MessageParser.parse(snapshot)!!
        assertEquals("阿明", parsed.title)
        assertEquals("${KnownApps.LINE}/阿明", parsed.conversationKey)
    }

    @Test
    fun shortcutIdWinsOverTitleForTheKey() {
        val snapshot = styleSnapshot(listOf(StyleMessage("阿明", "在嗎", 1_000)))
            .copy(shortcutId = "chat-42")
        assertEquals("${KnownApps.LINE}/chat-42", MessageParser.parse(snapshot)!!.conversationKey)
    }

    @Test
    fun imageWithoutTextBecomesPlaceholder() {
        val parsed = MessageParser.parse(
            styleSnapshot(listOf(StyleMessage("阿明", null, 1_000, dataMimeType = "image/jpeg")))
        )!!
        assertEquals("（圖片）", parsed.messages.single().text)
    }

    @Test
    fun inboxStyleLinesBecomeSeparateMessages() {
        val parsed = MessageParser.parse(
            NotificationSnapshot(
                packageName = "com.example.chat",
                postedAt = 9_000,
                title = "小華",
                text = "2 則新訊息",
                textLines = listOf("第一句", "第二句")
            )
        )!!
        assertEquals(listOf("第一句", "第二句"), parsed.messages.map { it.text })
        assertTrue(parsed.messages.all { it.sender == "小華" && it.postedAt == 9_000L && it.fromFallback })
    }

    @Test
    fun bigTextIsPreferredOverText() {
        val parsed = MessageParser.parse(
            NotificationSnapshot(
                packageName = "com.example.chat",
                postedAt = 9_000,
                title = "小華",
                text = "今天晚上要不要一起…",
                bigText = "今天晚上要不要一起吃飯？我訂好位子了"
            )
        )!!
        assertEquals("今天晚上要不要一起吃飯？我訂好位子了", parsed.messages.single().text)
    }

    @Test
    fun groupSummaryIsDropped() {
        val summary = NotificationSnapshot(
            packageName = KnownApps.LINE,
            postedAt = 9_000,
            isGroupSummary = true,
            title = "LINE",
            text = "3 則新訊息"
        )
        assertNull(MessageParser.parse(summary))
    }

    @Test
    fun notificationWithoutContentIsDropped() {
        assertNull(MessageParser.parse(NotificationSnapshot(packageName = "com.example.chat", postedAt = 1)))
    }

    @Test
    fun instagramWithoutMessagingStyleIsNotADirectMessage() {
        val like = NotificationSnapshot(
            packageName = KnownApps.INSTAGRAM,
            postedAt = 1,
            title = "Instagram",
            text = "someone 說你的貼文讚"
        )
        assertNull(MessageParser.parse(like))

        val dm = styleSnapshot(
            listOf(StyleMessage("someone", "hi", 1_000)),
            conversationTitle = null,
            isGroup = false,
            packageName = KnownApps.INSTAGRAM
        )
        assertNotNull(MessageParser.parse(dm))
    }

    @Test
    fun dedupeKeyIsStableAcrossReposts() {
        // MessagingStyle 每次更新都帶著整段歷史：第二次多了一則，前兩則一模一樣
        val first = MessageParser.parse(
            styleSnapshot(
                listOf(
                    StyleMessage("阿明", "明天幾點？", 1_000),
                    StyleMessage("小華", "七點車站", 2_000)
                )
            )
        )!!
        val second = MessageParser.parse(
            styleSnapshot(
                listOf(
                    StyleMessage("阿明", "明天幾點？", 1_000),
                    StyleMessage("小華", "七點車站", 2_000),
                    StyleMessage("阿明", "OK", 4_000)
                )
            ).copy(postedAt = 4_000, text = "OK")
        )!!

        fun keys(p: com.routina.glance.capture.ParsedNotification) =
            p.messages.map { listOf(p.packageName, p.conversationKey, it.sender, it.postedAt, it.textHash) }

        assertEquals(first.conversationKey, second.conversationKey)
        assertEquals(keys(first), keys(second).take(2))
        assertEquals(1, (keys(second) - keys(first).toSet()).size)
    }

    @Test
    fun differentTextGivesDifferentHash() {
        val a = styleSnapshot(listOf(StyleMessage("阿明", "好", 1_000)))
        val b = styleSnapshot(listOf(StyleMessage("阿明", "好喔", 1_000)))
        val hashA = MessageParser.parse(a)!!.messages.single().textHash
        val hashB = MessageParser.parse(b)!!.messages.single().textHash
        assertTrue(hashA != hashB)
        assertEquals(16, hashA.length)
    }
}
