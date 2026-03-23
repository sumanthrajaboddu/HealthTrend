package com.healthtrend.app.ui.analytics

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DateRangeSelectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun oneWeek_isSelectedByDefault_whenProvidedAsSelectedRange() {
        composeTestRule.setContent {
            DateRangeSelector(
                selectedRange = DateRange.ONE_WEEK,
                onRangeSelected = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("1 Week, selected. Double tap to select.")
            .assertIsDisplayed()
            .assert(hasStateDescription("selected"))
    }

    @Test
    fun clickingOneMonth_updatesSelectionState() {
        composeTestRule.setContent {
            val selectedRange = remember { mutableStateOf(DateRange.ONE_WEEK) }
            DateRangeSelector(
                selectedRange = selectedRange.value,
                onRangeSelected = { selectedRange.value = it }
            )
        }

        composeTestRule
            .onNodeWithContentDescription("1 Month, not selected. Double tap to select.")
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("1 Month, selected. Double tap to select.")
            .assertIsDisplayed()
            .assert(hasStateDescription("selected"))
    }
}
