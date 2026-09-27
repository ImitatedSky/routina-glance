package com.routina.glance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/** 一個對話：某個 App 裡的一個聊天室或一個人 */
@Entity(
    tableName = "conversations",
    indices = [Index(value = ["packageName", "conversationKey"], unique = true)]
)
data class Conversation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val conversationKey: String,
    val title: String,
    val isGroup: Boolean,
    val lastMessageAt: Long,
    /** 收件匣的預覽文字。存一份比每次 join 最後一則訊息簡單 */
    val lastPreview: String,
    val unseenCount: Int
)

/**
 * 一則訊息。
 *
 * 去重靠唯一索引＋INSERT OR IGNORE：MessagingStyle 每次更新都把整段歷史再送一次，
 * 同一則訊息的傳訊人、時間、內容都不會變。conversationId 一對一對應
 * (packageName, conversationKey)，所以索引用它就等於用那兩欄。
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = Conversation::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["conversationId", "sender", "postedAt", "textHash"], unique = true),
        Index(value = ["postedAt"])
    ]
)
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val sender: String,
    val text: String,
    val postedAt: Long,
    val isSelf: Boolean,
    /** 不是從 MessagingStyle 解析來的：傳訊人與時間只是推測 */
    val fromFallback: Boolean,
    val textHash: String,
    /** 給全文搜尋用的斷詞版本，見 [SearchText] */
    val searchText: String
)

/** 全文索引。內容來自 messages 表，Room 會自動建觸發器保持同步 */
@Fts4(contentEntity = Message::class)
@Entity(tableName = "messages_fts")
data class MessageFts(
    val searchText: String
)

/**
 * 開發者頁的原始通知紀錄，只留最新的 [RAW_EVENT_LIMIT] 筆。
 * 用來蒐集各家 App 真實的通知格式，以及研究「訊息已收回」時通知會怎麼變。
 */
@Entity(tableName = "raw_events")
data class RawEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long,
    val packageName: String,
    /** posted 或 removed */
    val kind: String,
    val body: String
)

const val RAW_EVENT_LIMIT = 200
