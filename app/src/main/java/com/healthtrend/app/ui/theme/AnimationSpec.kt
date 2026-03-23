package com.healthtrend.app.ui.theme

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween

/**
 * ALL animation durations for HealthTrend.
 * No animation exceeds 300ms — this is a hard cap.
 * NEVER use inline duration values — always reference constants from this object.
 */
object HealthTrendAnimation {
    /** Severity picker expand: 200ms ease-out */
    const val PICKER_EXPAND_MS = 200

    /** Severity picker collapse on selection: 0ms (instant) */
    const val PICKER_COLLAPSE_MS = 0

    /** Color fill bloom after selection: 150ms */
    const val COLOR_FILL_BLOOM_MS = 150

    /** Day swipe transition: 250ms */
    const val DAY_SWIPE_MS = 250

    /** All-complete bloom celebration: 300ms */
    const val ALL_COMPLETE_BLOOM_MS = 300

    /** Hard cap — no animation may exceed this: 300ms */
    const val MAX_ANIMATION_MS = 300

    /** Tween spec for picker expand */
    fun <T> pickerExpandSpec() = tween<T>(
        durationMillis = PICKER_EXPAND_MS,
        easing = EaseOut
    )

    /** Tween spec for color fill bloom */
    fun <T> colorFillBloomSpec() = tween<T>(
        durationMillis = COLOR_FILL_BLOOM_MS
    )

    /** Tween spec for day swipe */
    fun <T> daySwipeSpec() = tween<T>(
        durationMillis = DAY_SWIPE_MS
    )

    /** Tween spec for all-complete bloom */
    fun <T> allCompleteBloomSpec() = tween<T>(
        durationMillis = ALL_COMPLETE_BLOOM_MS
    )
}
