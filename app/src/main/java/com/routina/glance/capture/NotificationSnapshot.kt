package com.routina.glance.capture

/**
 * 一則通知裡解析會用到的欄位，從 Notification 的 extras 抄出來的純資料。
 *
 * 解析邏輯只吃這個 class，不碰 Android 的型別，所以能在 JVM 上做單元測試。
 * 從真正的 Notification 建出它的程式在 [SnapshotReader]。
 */
data class NotificationSnapshot(
    val packageName: String,
    /** 通知的時間（notification.when，沒有就用 postTime） */
    val postedAt: Long,
    val isGroupSummary: Boolean = false,
    val channelId: String? = null,
    val category: String? = null,
    val shortcutId: String? = null,
    val title: String? = null,
    val text: String? = null,
    val bigText: String? = null,
    /** InboxStyle 的每一行 */
    val textLines: List<String> = emptyList(),
    val conversationTitle: String? = null,
    /** 有 MessagingStyle 時才有值 */
    val style: StyleSnapshot? = null
)

data class StyleSnapshot(
    val conversationTitle: String?,
    val isGroup: Boolean,
    val messages: List<StyleMessage>,
    /** MessagingStyle 的 user，也就是使用者自己的名字。拿來排除「標題是自己」的情況 */
    val userName: String? = null
)

data class StyleMessage(
    /** 傳訊人名稱；[isSelf] 為 true 時是使用者自己 */
    val sender: String?,
    val text: String?,
    val timestamp: Long,
    val isSelf: Boolean = false,
    /** 圖片、貼圖這類訊息只有 MIME type，沒有文字 */
    val dataMimeType: String? = null
)
