package com.healthtrend.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimationSpecTest {

    @Test
    fun `picker expand duration is 200ms`() {
        assertEquals(200, HealthTrendAnimation.PICKER_EXPAND_MS)
    }

    @Test
    fun `picker collapse duration is 0ms (instant)`() {
        assertEquals(0, HealthTrendAnimation.PICKER_COLLAPSE_MS)
    }

    @Test
    fun `color fill bloom duration is 150ms`() {
        assertEquals(150, HealthTrendAnimation.COLOR_FILL_BLOOM_MS)
    }

    @Test
    fun `day swipe duration is 250ms`() {
        assertEquals(250, HealthTrendAnimation.DAY_SWIPE_MS)
    }

    @Test
    fun `all complete bloom duration is 300ms`() {
        assertEquals(300, HealthTrendAnimation.ALL_COMPLETE_BLOOM_MS)
    }

    @Test
    fun `maximum animation cap is 300ms`() {
        assertEquals(300, HealthTrendAnimation.MAX_ANIMATION_MS)
    }

    @Test
    fun `no animation exceeds 300ms cap`() {
        val allDurations = listOf(
            HealthTrendAnimation.PICKER_EXPAND_MS,
            HealthTrendAnimation.PICKER_COLLAPSE_MS,
            HealthTrendAnimation.COLOR_FILL_BLOOM_MS,
            HealthTrendAnimation.DAY_SWIPE_MS,
            HealthTrendAnimation.ALL_COMPLETE_BLOOM_MS
        )
        allDurations.forEach { duration ->
            assertTrue(
                "Animation duration $duration exceeds cap of ${HealthTrendAnimation.MAX_ANIMATION_MS}ms",
                duration <= HealthTrendAnimation.MAX_ANIMATION_MS
            )
        }
    }
}
