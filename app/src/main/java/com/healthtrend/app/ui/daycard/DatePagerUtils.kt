package com.healthtrend.app.ui.daycard

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Utility constants and functions for the Day Card horizontal pager and week strip.
 * Maps between pager page indices and [LocalDate] using a large center index for today.
 * This allows virtually unlimited past navigation without pre-loading.
 */
object DatePagerUtils {

    /**
     * Page index representing today.
     * Using Int.MAX_VALUE / 2 provides virtually unlimited past navigation.
     */
    const val TODAY_PAGE_INDEX = Int.MAX_VALUE / 2

    /**
     * Total page count — only past dates and today are navigable.
     * Future pages are blocked by limiting count to [TODAY_PAGE_INDEX] + 1.
     */
    val pageCount: Int get() = TODAY_PAGE_INDEX + 1

    /**
     * Converts a pager page index to a [LocalDate].
     *
     * @param pageIndex The pager page index.
     * @param today Today's date (anchor for the center index).
     * @return The [LocalDate] corresponding to this page index.
     */
    fun pageIndexToDate(pageIndex: Int, today: LocalDate): LocalDate {
        val dayOffset = pageIndex.toLong() - TODAY_PAGE_INDEX.toLong()
        return today.plusDays(dayOffset)
    }

    /**
     * Converts a [LocalDate] to a pager page index.
     *
     * @param date The date to convert.
     * @param today Today's date (anchor for the center index).
     * @return The pager page index corresponding to this date.
     */
    fun dateToPageIndex(date: LocalDate, today: LocalDate): Int {
        val dayOffset = ChronoUnit.DAYS.between(today, date)
        return TODAY_PAGE_INDEX + dayOffset.toInt()
    }

    // ===================================================================
    // Week strip utilities (Story 2.2)
    // ===================================================================

    /**
     * Returns the Monday (start) of the week containing [date].
     * Week starts on Monday (ISO standard).
     */
    fun weekStartDate(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /**
     * Returns the Sunday (end) of the week containing [date].
     */
    fun weekEndDate(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    /**
     * Returns the 7 dates (Monday through Sunday) of the week containing [date].
     */
    fun weekDays(date: LocalDate): List<LocalDate> {
        val monday = weekStartDate(date)
        return (0L..6L).map { monday.plusDays(it) }
    }
}
