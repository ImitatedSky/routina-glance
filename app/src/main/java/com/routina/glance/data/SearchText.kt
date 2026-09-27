package com.routina.glance.data

/**
 * 全文搜尋用的斷詞。
 *
 * SQLite FTS4 內建的斷詞器不懂中文：一整串中文會被當成「一個字」，
 * 搜「晚餐」找不到「今天晚餐吃什麼」。常見的解法是把中日韓文字逐字用空白隔開，
 * 查詢時再把關鍵字同樣拆開、用片語（"晚 餐"）比對相鄰的字。
 * 英文與數字維持整個單字，並轉小寫。
 */
object SearchText {

    /** 存進 FTS 的版本 */
    fun index(text: String): String {
        val out = StringBuilder(text.length * 2)
        for (ch in text.lowercase()) {
            when {
                isCjk(ch) -> out.append(' ').append(ch).append(' ')
                ch.isLetterOrDigit() -> out.append(ch)
                else -> out.append(' ')
            }
        }
        return out.toString().split(' ').filter { it.isNotEmpty() }.joinToString(" ")
    }

    /**
     * 使用者輸入 → FTS 的 MATCH 字串。空白分開的每個詞都要出現（AND），
     * 每個詞是一個片語；最後一段是英數時加 *，打到一半也找得到。
     * 回 null 表示沒有可以搜的東西。
     */
    fun query(input: String): String? {
        val phrases = input.trim().split(Regex("\\s+")).mapNotNull { word ->
            val tokens = index(word)
            if (tokens.isEmpty()) return@mapNotNull null
            val last = tokens.last()
            val prefix = if (!isCjk(last)) "*" else ""
            "\"$tokens$prefix\""
        }
        return phrases.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }

    private fun isCjk(ch: Char): Boolean = when (Character.UnicodeScript.of(ch.code)) {
        Character.UnicodeScript.HAN,
        Character.UnicodeScript.HIRAGANA,
        Character.UnicodeScript.KATAKANA,
        Character.UnicodeScript.HANGUL,
        Character.UnicodeScript.BOPOMOFO -> true
        else -> false
    }
}
