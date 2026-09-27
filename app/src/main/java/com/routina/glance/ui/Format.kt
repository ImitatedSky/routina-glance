package com.routina.glance.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val monthDayFormat = DateTimeFormatter.ofPattern("M/d")
private val fullDateFormat = DateTimeFormatter.ofPattern("yyyy/M/d")
private val dayLabelFormat = DateTimeFormatter.ofPattern("M月d日 EEEE", java.util.Locale.TAIWAN)

fun localDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

fun clockTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(timeFormat)

/** 收件匣右上角的時間：今天只寫時刻，今年寫月日，更早的寫完整日期 */
fun listTime(millis: Long): String {
    val date = localDate(millis)
    val today = LocalDate.now()
    return when {
        date == today -> clockTime(millis)
        date.year == today.year -> date.format(monthDayFormat)
        else -> date.format(fullDateFormat)
    }
}

/** 對話裡的日期分隔線 */
fun dayLabel(date: LocalDate, today: String, yesterday: String): String {
    val now = LocalDate.now()
    return when (date) {
        now -> today
        now.minusDays(1) -> yesterday
        else -> if (date.year == now.year) date.format(dayLabelFormat) else date.format(fullDateFormat)
    }
}

/** 搜尋結果的時間：一律帶日期，才看得出是哪一天說的 */
fun hitTime(millis: Long): String =
    if (localDate(millis) == LocalDate.now()) clockTime(millis) else "${listTime(millis)} ${clockTime(millis)}"
