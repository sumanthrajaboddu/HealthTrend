package com.healthtrend.app.ui.daycard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Unit tests for [DatePagerUtils] page-index ↔ date conversions,
 * future-navigation blocking, and week utilities.
 */
class DatePagerUtilsTest {

    private val today = LocalDate.of(2026, 2, 8) // Sunday
    private val todayIndex = DatePagerUtils.TODAY_PAGE_INDEX

    // ===================================================================
    // Page ↔ Date conversion tests (Story 2.1)
    // ===================================================================

    @Test
    fun `today page index maps to today date`() {
        val date = DatePagerUtils.pageIndexToDate(todayIndex, today)
        assertEquals(today, date)
    }

    @Test
    fun `page index minus one maps to yesterday`() {
        val date = DatePagerUtils.pageIndexToDate(todayIndex - 1, today)
        assertEquals(today.minusDays(1), date)
    }

    @Test
    fun `page index minus seven maps to one week ago`() {
        val date = DatePagerUtils.pageIndexToDate(todayIndex - 7, today)
        assertEquals(today.minusDays(7), date)
    }

    @Test
    fun `page index minus 365 maps to one year ago`() {
        val date = DatePagerUtils.pageIndexToDate(todayIndex - 365, today)
        assertEquals(today.minusDays(365), date)
    }

    @Test
    fun `today date maps to today page index`() {
        val index = DatePagerUtils.dateToPageIndex(today, today)
        assertEquals(todayIndex, index)
    }

    @Test
    fun `yesterday maps to todayIndex minus one`() {
        val index = DatePagerUtils.dateToPageIndex(today.minusDays(1), today)
        assertEquals(todayIndex - 1, index)
    }

    @Test
    fun `one week ago maps to todayIndex minus seven`() {
        val index = DatePagerUtils.dateToPageIndex(today.minusDays(7), today)
        assertEquals(todayIndex - 7, index)
    }

    @Test
    fun `round trip page index to date to page index`() {
        val pageIndex = todayIndex - 42
        val date = DatePagerUtils.pageIndexToDate(pageIndex, today)
        val result = DatePagerUtils.dateToPageIndex(date, today)
        assertEquals(pageIndex, result)
    }

    @Test
    fun `round trip date to page index to date`() {
        val date = LocalDate.of(2025, 12, 25)
        val pageIndex = DatePagerUtils.dateToPageIndex(date, today)
        val result = DatePagerUtils.pageIndexToDate(pageIndex, today)
        assertEquals(date, result)
    }

    @Test
    fun `round trip far past date`() {
        val date = LocalDate.of(2020, 1, 1)
        val pageIndex = DatePagerUtils.dateToPageIndex(date, today)
        val result = DatePagerUtils.pageIndexToDate(pageIndex, today)
        assertEquals(date, result)
    }

    @Test
    fun `pageCount equals todayIndex plus one`() {
        assertEquals(todayIndex + 1, DatePagerUtils.pageCount)
    }

    @Test
    fun `today page index is within valid range`() {
        assertTrue(todayIndex < DatePagerUtils.pageCount)
    }

    @Test
    fun `future date maps to index beyond page count`() {
        val tomorrow = today.plusDays(1)
        val index = DatePagerUtils.dateToPageIndex(tomorrow, today)
        assertTrue(
            "Future index $index should be >= pageCount ${DatePagerUtils.pageCount}",
            index >= DatePagerUtils.pageCount
        )
    }

    @Test
    fun `page index zero maps to distant past`() {
        val date = DatePagerUtils.pageIndexToDate(0, today)
        assertTrue("Page 0 date $date should be far in the past", date.isBefore(today))
    }

    @Test
    fun `same date different today anchor`() {
        val today1 = LocalDate.of(2026, 1, 1)
        val today2 = LocalDate.of(2026, 6, 15)
        val targetDate = LocalDate.of(2026, 3, 1)

        val index1 = DatePagerUtils.dateToPageIndex(targetDate, today1)
        val index2 = DatePagerUtils.dateToPageIndex(targetDate, today2)

        assertTrue(index1 != index2)
        assertEquals(targetDate, DatePagerUtils.pageIndexToDate(index1, today1))
        assertEquals(targetDate, DatePagerUtils.pageIndexToDate(index2, today2))
    }

    // ===================================================================
    // Week utility tests (Story 2.2)
    // ===================================================================

    @Test
    fun `weekStartDate returns Monday for a Sunday`() {
        // Feb 8, 2026 is a Sunday
        val monday = DatePagerUtils.weekStartDate(today)
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
        assertEquals(LocalDate.of(2026, 2, 2), monday)
    }

    @Test
    fun `weekStartDate returns same day for a Monday`() {
        val monday = LocalDate.of(2026, 2, 2) // Monday
        assertEquals(monday, DatePagerUtils.weekStartDate(monday))
    }

    @Test
    fun `weekStartDate returns Monday for mid-week Wednesday`() {
        val wednesday = LocalDate.of(2026, 2, 4) // Wednesday
        val monday = DatePagerUtils.weekStartDate(wednesday)
        assertEquals(LocalDate.of(2026, 2, 2), monday)
    }

    @Test
    fun `weekEndDate returns Sunday for a Monday`() {
        val monday = LocalDate.of(2026, 2, 2) // Monday
        val sunday = DatePagerUtils.weekEndDate(monday)
        assertEquals(DayOfWeek.SUNDAY, sunday.dayOfWeek)
        assertEquals(LocalDate.of(2026, 2, 8), sunday)
    }

    @Test
    fun `weekEndDate returns same day for a Sunday`() {
        // Feb 8, 2026 is a Sunday
        assertEquals(today, DatePagerUtils.weekEndDate(today))
    }

    @Test
    fun `weekDays returns 7 days starting from Monday`() {
        val days = DatePagerUtils.weekDays(today) // Sunday Feb 8
        assertEquals(7, days.size)
        assertEquals(LocalDate.of(2026, 2, 2), days.first()) // Monday
        assertEquals(LocalDate.of(2026, 2, 8), days.last())   // Sunday
    }

    @Test
    fun `weekDays all have consecutive dates`() {
        val days = DatePagerUtils.weekDays(LocalDate.of(2026, 2, 5)) // Thursday
        for (i in 1 until days.size) {
            assertEquals(days[i - 1].plusDays(1), days[i])
        }
    }

    @Test
    fun `weekDays first is Monday and last is Sunday`() {
        val days = DatePagerUtils.weekDays(LocalDate.of(2026, 2, 4))
        assertEquals(DayOfWeek.MONDAY, days.first().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, days.last().dayOfWeek)
    }

    @Test
    fun `weekDays for Monday returns same week`() {
        val monday = LocalDate.of(2026, 2, 9) // Next Monday
        val days = DatePagerUtils.weekDays(monday)
        assertEquals(monday, days.first())
    }

    @Test
    fun `weekStartDate across month boundary`() {
        // Feb 1, 2026 is a Sunday. Its week starts on Monday Jan 26.
        val feb1 = LocalDate.of(2026, 2, 1)
        val monday = DatePagerUtils.weekStartDate(feb1)
        assertEquals(LocalDate.of(2026, 1, 26), monday)
    }

    @Test
    fun `weekEndDate across month boundary`() {
        // Jan 26, 2026 is a Monday. Its week ends on Sunday Feb 1.
        val jan26 = LocalDate.of(2026, 1, 26)
        val sunday = DatePagerUtils.weekEndDate(jan26)
        assertEquals(LocalDate.of(2026, 2, 1), sunday)
    }

    @Test
    fun `weekDays across year boundary`() {
        // Dec 31, 2025 is a Wednesday. Week is Mon Dec 29 - Sun Jan 4.
        val dec31 = LocalDate.of(2025, 12, 31)
        val days = DatePagerUtils.weekDays(dec31)
        assertEquals(LocalDate.of(2025, 12, 29), days.first())
        assertEquals(LocalDate.of(2026, 1, 4), days.last())
    }
}
