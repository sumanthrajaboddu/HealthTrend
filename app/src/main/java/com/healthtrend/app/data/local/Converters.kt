package com.healthtrend.app.data.local

import androidx.room.TypeConverter
import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot

/**
 * Room TypeConverters for Severity and TimeSlot enums.
 */
class Converters {

    @TypeConverter
    fun fromSeverity(severity: Severity?): String? = severity?.name

    @TypeConverter
    fun toSeverity(value: String?): Severity? = value?.let { Severity.valueOf(it) }

    @TypeConverter
    fun fromTimeSlot(timeSlot: TimeSlot?): String? = timeSlot?.name

    @TypeConverter
    fun toTimeSlot(value: String?): TimeSlot? = value?.let { TimeSlot.valueOf(it) }
}
