package com.routina.glance

import com.routina.glance.data.SearchText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchTextTest {

    @Test
    fun cjkIsSplitPerCharacterAndLatinStaysWhole() {
        assertEquals("今 天 晚 餐 at 7pm", SearchText.index("今天晚餐 at 7PM!"))
    }

    @Test
    fun queryBuildsPhrasesWithPrefixForLatin() {
        assertEquals("\"晚 餐\" \"lin*\"", SearchText.query("晚餐  Lin"))
    }

    @Test
    fun queryMixedWordKeepsItTogether() {
        assertEquals("\"line 群 組\"", SearchText.query("LINE群組"))
    }

    @Test
    fun punctuationOnlyQueryIsNothing() {
        assertNull(SearchText.query(" \"?! "))
    }
}
