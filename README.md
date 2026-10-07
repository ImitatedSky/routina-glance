# Routina Glance

不開 App 也能看訊息，不留已讀。Routina 家族的成員之一，獨立安裝。

Glance 讀取聊天 App 的通知，把訊息抄進自己的資料庫。你在 Glance 裡看完整段對話，
聊天 App 完全沒被打開，對方就不會看到「已讀」。

內建支援 LINE、Instagram（私訊）、Messenger、Telegram、Microsoft Teams。
其他 App 可以在設定裡自己加，會用通用的方式解析，效果依 App 而定。

## 畫面

- **收件匣**：依最後訊息時間排序的對話列表，上面一排晶片依 App 篩選，右上角可以全文搜尋。
- **對話**：聊天泡泡，群組會標傳訊人、依日期分段。打開就算看過，未讀數歸零。
- **設定**：要保存哪些 App、保存多久（7／30／90 天／永久，預設 30 天）、
  存好後清掉原通知（預設關）、App 鎖（預設關）、開發者用的原始通知紀錄。

## 注意事項

Glance 只看得到「通知」，所以通知沒出現或沒寫內容的訊息，它也拿不到：

- 設成靜音的聊天室或群組不會跳通知，Glance 也收不到。
- 聊天室正開在 App 裡，或電腦版正開著時，手機通常不會跳通知。
- 聊天 App 的通知要設成「顯示訊息內容」，否則只抄得到「你有一則新訊息」。
- 照片、貼圖、語音只會留下「傳送了照片」這類文字。
- 從通知直接回覆，對方會看到已讀。
- 安裝 Glance 之前的訊息拿不到。

**清掉原通知**：原通知一點下去就會打開聊天室（＝已讀），所以預設在存好之後把它從通知欄清掉。
存失敗的話原通知會留著。

## 權限與隱私

- **通知存取權**：唯一需要的權限，要在系統設定裡手動開。
- **沒有網路權限**：訊息只存在這支手機上，除非你自己在開發者頁按分享。
- `allowBackup=false` 加上 `dataExtractionRules`：雲端備份與換機時的裝置轉移都不會帶走訊息。
- 訊息內容不寫進 logcat。
- App 鎖用指紋或螢幕鎖定密碼，開了之後「最近使用」畫面也不會顯示內容。

## 側載安裝後開不了通知存取？

Android 13 以後，不是從 Play 商店安裝的 App 預設不能開通知存取（系統稱為「受限制的設定」），
開關是灰的。解法：

1. 打開 Glance 的「應用程式資訊」（Glance 首頁的卡片上有按鈕直接過去）
2. 點右上角的 ⋮
3. 選「允許受限制的設定」
4. 驗證身分（指紋或螢幕鎖定密碼）
5. 回到 Glance，再按一次「前往開啟通知存取」

## 運作方式

```
NotificationListenerService → 通用解析 → 各 App 修正 → 去重 → Room → Compose
```

- **通用解析**（`capture/MessageParser.kt`）：先試 `MessagingStyle`（對話標題、是不是群組、
  每一則的傳訊人／內容／時間），沒有的話退回 `EXTRA_TITLE`、`EXTRA_TEXT_LINES`、
  `EXTRA_BIG_TEXT`、`EXTRA_TEXT`。對話的身分是「套件名＋shortcutId（沒有就用標題）」。
  解析只吃一個純資料 class，所以能在 JVM 上做單元測試。
- **各 App 修正**（`capture/AppAdapters.kt`）：刻意很薄，目前只有 Instagram 過濾掉非私訊的通知。
  **這些判斷都還沒用真機樣本驗證過**，其他 App 的已知疑點寫在註解裡，等拿到樣本再改。
- **去重**：`MessagingStyle` 每次更新都把整段歷史再送一次，靠唯一索引
  （對話、傳訊人、時間、內容雜湊）加 `INSERT OR IGNORE` 擋掉重複。
- **搜尋**：SQLite FTS4。內建斷詞器不懂中文，所以存進索引前把中日韓文字逐字隔開，
  查詢時用片語比對相鄰的字。
- **保存期限**：開 App 時清一次，監聽服務收到通知時一天最多再清一次。不用 WorkManager。
- **原始通知紀錄**：最近 200 則監聽中 App 的通知（中繼資料、全部 extras、解析結果）
  與移除事件（含原因碼），可以分享成純文字。用來確認各家格式，以及日後研究「訊息已收回」。

## 字體與顏色

沿用家族規則：中文字重封頂 Medium(500)、字距 0、關掉 `includeFontPadding`
（見 [`ui/theme/Type.kt`](app/src/main/java/com/routina/glance/ui/theme/Type.kt)）。

顏色取自啟動圖示：主色是圖示的灰綠 `#82907F`、強調色是琥珀 `#D19F57`、底色是奶油 `#FAF4E9`。
[`tools/generate_palette.py`](tools/generate_palette.py) 在 Oklch 裡從這三色算出完整色階，`ColorScheme` 每個角色都給值。
啟動圖示由 [`tools/generate_icon.py`](tools/generate_icon.py) 從設計稿產生。

## 建置

需要 JDK 17 與 Android SDK（`local.properties` 的 `sdk.dir`）。

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Room 的 schema 匯出在 `app/schemas/`，改資料表時要一起提交，寫 migration 才有舊版可以對照。

Release 在有完整簽章資訊時（根目錄 `keystore.properties` 或 CI 的 `KEYSTORE_*` 環境變數）
用正式簽章，否則沿用 debug 簽章。家族全體共用同一把 keystore，成員才能就地更新。

## 發版

`vX.Y.Z` 標籤會產出一個永久保留的 Release，附 `routina-glance-vX.Y.Z.apk`。
CI 會先擋下標籤與 `versionName` 不一致的發版。
