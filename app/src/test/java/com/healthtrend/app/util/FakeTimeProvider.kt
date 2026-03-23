package com.healthtrend.app.util

import java.time.LocalDate

/**
 * Fake TimeProvider for deterministic testing.
 */
class FakeTimeProvider(
    private val date: LocalDate = LocalDate.of(2026, 2, 8),
    private val hour: Int = 14
) : TimeProvider {
    override fun currentDate(): LocalDate = date
    override fun currentHour(): Int = hour
}
