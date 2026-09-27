package com.routina.glance

import android.app.Application
import com.routina.glance.data.GlanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 整個 App 只有一份 [GlanceRepository]，掛在 Application 上。
 * 畫面與監聽服務在同一個行程，所以兩邊拿到的是同一個資料庫連線與同一份設定。
 */
class GlanceApp : Application() {

    lateinit var repository: GlanceRepository
        private set

    /** 跟著行程活的背景工作（寫資料庫）。不綁畫面，畫面關掉也要寫完 */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        repository = GlanceRepository(this)
        // 開 App 時一定清一次過期訊息（監聽服務那邊是一天最多一次）
        appScope.launch { runCatching { repository.pruneIfDue(force = true) } }
    }
}
