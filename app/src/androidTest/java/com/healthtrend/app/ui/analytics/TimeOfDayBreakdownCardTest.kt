package com.healthtrend.app.ui.analytics

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimeOfDayBreakdownCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dataCard_announcesSlotAverageWithDayCount() {
        composeTestRule.setContent {
            MaterialTheme {
                TimeOfDayBreakdownCard(
                    timeSlot = TimeSlot.AFTERNOON,
                    averageSeverity = Severity.MILD,
                    periodDays = 7
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Afternoon average: Mild over 7 days.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Afternoon").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mild").assertIsDisplayed()
    }

    @Test
    fun emptyCard_showsNeutralDashAndNoDataAnnouncement() {
        composeTestRule.setContent {
            MaterialTheme {
                TimeOfDayBreakdownCard(
                    timeSlot = TimeSlot.EVENING,
                    averageSeverity = null,
                    periodDays = 7
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Evening: No data for this period.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Evening").assertIsDisplayed()
        composeTestRule.onNodeWithText("—").assertIsDisplayed()
    }

    @Test
    fun breakdownSection_rendersAllFourCards() {
        composeTestRule.setContent {
            MaterialTheme {
                TimeOfDayBreakdownSection(
                    slotAverages = mapOf(
                        TimeSlot.MORNING to Severity.MODERATE,
                        TimeSlot.AFTERNOON to Severity.MILD,
                        TimeSlot.EVENING to null,
                        TimeSlot.NIGHT to Severity.SEVERE
                    ),
                    periodDays = 30
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Morning average: Moderate over 30 days.")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Afternoon average: Mild over 30 days.")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Evening: No data for this period.")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Night average: Severe over 30 days.")
            .assertIsDisplayed()

        composeTestRule.onAllNodesWithText("—").assertCountEquals(1)
    }
}
