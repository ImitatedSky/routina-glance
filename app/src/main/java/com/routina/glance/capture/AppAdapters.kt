package com.routina.glance.capture

/** 預設就監聽的聊天 App */
object KnownApps {
    const val LINE = "jp.naver.line.android"
    const val INSTAGRAM = "com.instagram.android"
    const val MESSENGER = "com.facebook.orca"
    const val TELEGRAM = "org.telegram.messenger"
    const val TEAMS = "com.microsoft.teams"

    val defaults = listOf(LINE, INSTAGRAM, MESSENGER, TELEGRAM, TEAMS)

    /** 手機上沒裝的話讀不到 App 名稱，設定頁至少要顯示人看得懂的名字 */
    val names = mapOf(
        LINE to "LINE",
        INSTAGRAM to "Instagram",
        MESSENGER to "Messenger",
        TELEGRAM to "Telegram",
        TEAMS to "Microsoft Teams"
    )
}

/**
 * 各 App 的怪癖修正。刻意很薄：通用解析（[MessageParser]）自己就要能動，
 * 這裡只處理「確定會出錯」的情況。
 *
 * **這裡的判斷都還沒用真機的通知樣本驗證過。** 各家 App 的通知格式沒有公開文件、
 * 也會隨版本改。拿到樣本（設定 → 開發者：原始通知紀錄 → 分享）之前，
 * 寧可不修，也不要憑猜測改寫標題或傳訊人。
 */
object AppAdapters {

    /** 這則通知要不要收。false = 不是聊天訊息 */
    fun accept(snapshot: NotificationSnapshot): Boolean = when (snapshot.packageName) {
        // Instagram 的按讚、追蹤、留言、限動提醒也走同一個 App 的通知。
        // 推測只有私訊（DM）會用 MessagingStyle，其他提醒是一般的文字通知，
        // 所以規則是：有 MessagingStyle，或 category 標成訊息，才收。
        // 未驗證：DM 的 channelId 也許能分得更準，等原始紀錄確認後再改成看 channelId。
        KnownApps.INSTAGRAM ->
            snapshot.style != null || snapshot.category == CATEGORY_MESSAGE

        else -> true
    }

    /**
     * 解析完之後的修正。目前全部原樣通過，下面記的是已知「可能」要處理、但還沒有樣本的地方：
     *
     * - LINE：群組通知的標題可能是「群組名」或「傳訊人」，要看 MessagingStyle 有沒有帶
     *   conversationTitle。有的話通用解析已經處理好；沒有的話才需要在這裡拆。
     * - Teams：頻道訊息的標題可能是「某人 in 頻道名」這類格式（語系不同字樣也不同），
     *   拆錯的話同一個頻道會變成每個人各一個對話。
     */
    fun adjust(snapshot: NotificationSnapshot, parsed: ParsedNotification): ParsedNotification = parsed

    /** 與 Notification.CATEGORY_MESSAGE 同值；抄一份是為了讓這層不依賴 Android */
    private const val CATEGORY_MESSAGE = "msg"
}
