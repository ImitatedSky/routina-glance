package com.routina.glance.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.routina.glance.GlanceApp
import com.routina.glance.capture.GlanceListener
import com.routina.glance.data.Conversation
import com.routina.glance.data.GlanceSettings
import com.routina.glance.data.Message
import com.routina.glance.data.RawEvent
import com.routina.glance.data.SearchHit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 可以加進監聽清單的 App */
data class LauncherApp(val packageName: String, val label: String)

class GlanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as GlanceApp).repository

    val conversations: StateFlow<List<Conversation>> = repository.conversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<GlanceSettings> = repository.settings.settings

    val rawEvents: StateFlow<List<RawEvent>> = repository.rawEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 通知存取權有沒有開。回到畫面時由 MainActivity 重新檢查 */
    private val _accessEnabled = MutableStateFlow(GlanceListener.isEnabled(application))
    val accessEnabled: StateFlow<Boolean> = _accessEnabled.asStateFlow()

    /** 收件匣上方的 App 篩選，null = 全部 */
    private val _filter = MutableStateFlow<String?>(null)
    val filter: StateFlow<String?> = _filter.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchHit>?>(null)
    /** null = 沒在搜尋 */
    val searchResults: StateFlow<List<SearchHit>?> = _searchResults.asStateFlow()

    fun refreshAccess() {
        val context = getApplication<Application>()
        _accessEnabled.value = GlanceListener.isEnabled(context)
        GlanceListener.rebindIfNeeded(context)
    }

    fun setFilter(packageName: String?) {
        _filter.value = packageName
    }

    fun search(input: String) {
        if (input.isBlank()) {
            _searchResults.value = null
            return
        }
        viewModelScope.launch {
            _searchResults.value = runCatching { repository.search(input) }.getOrDefault(emptyList())
        }
    }

    fun clearSearch() {
        _searchResults.value = null
    }

    fun conversation(id: Long): Flow<Conversation?> = repository.conversation(id)
    fun messages(id: Long): Flow<List<Message>> = repository.messages(id)

    fun markSeen(id: Long) {
        viewModelScope.launch { repository.markSeen(id) }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch { repository.deleteConversation(id) }
    }

    // ---------- 設定 ----------

    fun setMonitored(packageName: String, on: Boolean) = repository.settings.update {
        it.copy(monitored = if (on) it.monitored + packageName else it.monitored - packageName)
    }

    fun addApp(packageName: String) = repository.settings.update {
        it.copy(monitored = it.monitored + packageName, extraApps = it.extraApps + packageName)
    }

    fun setRetention(days: Int) {
        repository.settings.update { it.copy(retentionDays = days) }
        // 期限縮短時馬上生效，不用等到明天
        viewModelScope.launch { repository.pruneIfDue(force = true) }
    }

    fun setClearOriginal(on: Boolean) = repository.settings.update { it.copy(clearOriginal = on) }

    fun setAppLock(on: Boolean) = repository.settings.update { it.copy(appLock = on) }

    fun clearRawEvents() {
        viewModelScope.launch { repository.clearRawEvents() }
    }

    /** 有桌面圖示的 App（不含自己），給「新增其他 App」挑 */
    suspend fun launcherApps(): List<LauncherApp> = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        runCatching {
            pm.queryIntentActivities(intent, 0)
                .map { it.activityInfo.packageName }
                .distinct()
                .filter { it != context.packageName }
                .map { LauncherApp(it, appLabel(context, it)) }
                .sortedBy { it.label }
        }.getOrDefault(emptyList())
    }
}
