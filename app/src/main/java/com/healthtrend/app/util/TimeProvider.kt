package com.healthtrend.app.util

import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * Abstraction for system time — enables deterministic testing of time-dependent logic.
 */
interface TimeProvider {
    fun currentDate(): LocalDate
    fun currentHour(): Int
}

/**
 * Production implementation using system clock.
 */
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun currentDate(): LocalDate = LocalDate.now()
    override fun currentHour(): Int = LocalTime.now().hour
}
