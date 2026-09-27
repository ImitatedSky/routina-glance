package com.routina.glance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.routina.glance.capture.ParsedNotification
import kotlinx.coroutines.flow.Flow

/** 搜尋結果的一列 */
data class SearchHit(
    val messageId: Long,
    val conversationId: Long,
    val sender: String,
    val text: String,
    val postedAt: Long,
    val isSelf: Boolean,
    val title: String,
    val packageName: String
)

@Dao
abstract class GlanceDao {

    // ---------- 讀 ----------

    @Query("SELECT * FROM conversations ORDER BY lastMessageAt DESC")
    abstract fun conversations(): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    abstract fun conversation(id: Long): Flow<Conversation?>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY postedAt, id")
    abstract fun messages(conversationId: Long): Flow<List<Message>>

    @Query(
        """
        SELECT m.id AS messageId, m.conversationId, m.sender, m.text, m.postedAt, m.isSelf,
               c.title, c.packageName
        FROM messages_fts
        JOIN messages m ON m.id = messages_fts.rowid
        JOIN conversations c ON c.id = m.conversationId
        WHERE messages_fts MATCH :match
        ORDER BY m.postedAt DESC
        LIMIT 200
        """
    )
    abstract suspend fun search(match: String): List<SearchHit>

    @Query("SELECT * FROM raw_events ORDER BY id DESC")
    abstract fun rawEvents(): Flow<List<RawEvent>>

    // ---------- 寫 ----------

    @Query("SELECT * FROM conversations WHERE packageName = :packageName AND conversationKey = :key")
    abstract suspend fun findConversation(packageName: String, key: String): Conversation?

    @Insert
    abstract suspend fun insertConversation(conversation: Conversation): Long

    @Query(
        """
        UPDATE conversations
        SET title = :title, isGroup = :isGroup, lastMessageAt = :lastMessageAt,
            lastPreview = :lastPreview, unseenCount = unseenCount + :added
        WHERE id = :id
        """
    )
    abstract suspend fun updateConversation(
        id: Long,
        title: String,
        isGroup: Boolean,
        lastMessageAt: Long,
        lastPreview: String,
        added: Int
    )

    /** 已經存過的訊息回 -1（唯一索引擋下來） */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertMessage(message: Message): Long

    /**
     * 存一則解析好的通知，回傳真的新增了幾則訊息。
     * 整段包在交易裡：對話與訊息要嘛一起寫進去，要嘛都沒有。
     */
    @Transaction
    open suspend fun store(parsed: ParsedNotification): Int {
        val existing = findConversation(parsed.packageName, parsed.conversationKey)
        val conversationId = existing?.id ?: insertConversation(
            Conversation(
                packageName = parsed.packageName,
                conversationKey = parsed.conversationKey,
                title = parsed.title,
                isGroup = parsed.isGroup,
                lastMessageAt = 0,
                lastPreview = "",
                unseenCount = 0
            )
        )

        var added = 0
        var latest = existing
            ?.let { it.lastMessageAt to it.lastPreview }
            ?: (0L to "")
        for (m in parsed.messages) {
            val rowId = insertMessage(
                Message(
                    conversationId = conversationId,
                    sender = m.sender,
                    text = m.text,
                    postedAt = m.postedAt,
                    isSelf = m.isSelf,
                    fromFallback = m.fromFallback,
                    textHash = m.textHash,
                    searchText = SearchText.index("${m.sender} ${m.text}")
                )
            )
            if (rowId == -1L) continue
            // 自己傳的不算「沒看過」
            if (!m.isSelf) added++
            if (m.postedAt >= latest.first) latest = m.postedAt to m.text
        }

        updateConversation(
            id = conversationId,
            title = parsed.title,
            isGroup = parsed.isGroup || existing?.isGroup == true,
            lastMessageAt = latest.first,
            lastPreview = latest.second,
            added = added
        )
        return added
    }

    @Query("UPDATE conversations SET unseenCount = 0 WHERE id = :id")
    abstract suspend fun markSeen(id: Long)

    @Query("DELETE FROM messages WHERE conversationId = :id")
    abstract suspend fun deleteMessagesOf(id: Long)

    @Query("DELETE FROM conversations WHERE id = :id")
    abstract suspend fun deleteConversationRow(id: Long)

    /** 先刪訊息再刪對話：全文索引的觸發器掛在 messages 的 DELETE 上 */
    @Transaction
    open suspend fun deleteConversation(id: Long) {
        deleteMessagesOf(id)
        deleteConversationRow(id)
    }

    @Query("DELETE FROM messages WHERE postedAt < :cutoff")
    abstract suspend fun deleteMessagesBefore(cutoff: Long): Int

    @Query("DELETE FROM conversations WHERE id NOT IN (SELECT DISTINCT conversationId FROM messages)")
    abstract suspend fun deleteEmptyConversations()

    @Transaction
    open suspend fun prune(cutoff: Long) {
        deleteMessagesBefore(cutoff)
        deleteEmptyConversations()
    }

    @Insert
    abstract suspend fun insertRawEvent(event: RawEvent)

    @Query("DELETE FROM raw_events WHERE id NOT IN (SELECT id FROM raw_events ORDER BY id DESC LIMIT :keep)")
    abstract suspend fun trimRawEvents(keep: Int)

    @Transaction
    open suspend fun addRawEvent(event: RawEvent) {
        insertRawEvent(event)
        trimRawEvents(RAW_EVENT_LIMIT)
    }

    @Query("DELETE FROM raw_events")
    abstract suspend fun clearRawEvents()
}
