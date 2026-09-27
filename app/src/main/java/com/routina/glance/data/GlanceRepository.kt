package com.routina.glance.data

import android.content.Context
import com.routina.glance.capture.ParsedNotification
import kotlinx.coroutines.flow.Flow

/** 資料庫與設定的唯一入口，畫面與監聽服務共用 */
class GlanceRepository(context: Context) {

    private val dao = GlanceDatabase.create(context).dao()
    val settings = SettingsStore(context)

    val conversations: Flow<List<Conversation>> = dao.conversations()
    val rawEvents: Flow<List<RawEvent>> = dao.rawEvents()

    fun conversation(id: Long): Flow<Conversation?> = dao.conversation(id)
    fun messages(conversationId: Long): Flow<List<Message>> = dao.messages(conversationId)

    suspend fun store(parsed: ParsedNotification): Int = dao.store(parsed)

    suspend fun search(input: String): List<SearchHit> {
        val match = SearchText.query(input) ?: return emptyList()
        return dao.search(match)
    }

    suspend fun markSeen(conversationId: Long) = dao.markSeen(conversationId)
    suspend fun deleteConversation(conversationId: Long) = dao.deleteConversation(conversationId)

    suspend fun addRawEvent(event: RawEvent) = dao.addRawEvent(event)
    suspend fun clearRawEvents() = dao.clearRawEvents()

    /**
     * 刪掉超過保存期限的訊息。App 啟動時一定跑；監聽服務每收一則通知就問一次，
     * 但一天最多真的做一次——不用 WorkManager，因為通知本來就會一直進來。
     */
    suspend fun pruneIfDue(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - settings.lastPruneAt < DAY_MS) return
        settings.lastPruneAt = now
        val days = settings.current.retentionDays
        if (days <= 0) return
        dao.prune(now - days * DAY_MS)
    }

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
