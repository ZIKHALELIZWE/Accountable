package com.thando.accountable.input_forms

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePickerState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.thando.accountable.AccountableComposeRobolectricTest
import com.thando.accountable.AccountableComposeRobolectricTest.TestMainActivity.Companion.addTime
import com.thando.accountable.database.Converters
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@OptIn(ExperimentalMaterial3Api::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class DateAndTimePickerTest(
    val getTimeAsLong: suspend ()->Long?,
    val getExpectedEndType:()->String,
    val getActualEndType: suspend ()->String?,
    val endTypeButtonTag: suspend ()->String,
    val dropDownMenuTag: suspend ()->String,
    val dropDownMenuItemTag: suspend ()->String,
    val selectDateAndTimeButtonTag: String? = null,
    parentParameters:Triple<
            InstantTaskExecutorRule,
            ComposeContentTestRule,
            TestMainActivity
            >?=null
): AccountableComposeRobolectricTest(
    parentParameters
) {
    @Test
    fun `01 Can Cancel Date Picker Test`() = runTest {
        withTag(endTypeButtonTag()) {
            performPressWithScroll()
        }

        withTag(dropDownMenuTag()) {
            assertExists()
            assertIsDisplayed()
        }

        withTag(dropDownMenuItemTag()) {
            performPressWithoutScroll()
        }

        assertEquals(
            getExpectedEndType(),
            getActualEndType()
        )

        val previousEndDateTime = getTimeAsLong()
        assertNotNull(previousEndDateTime)

        selectDateAndTimeButtonTag?.let {
            withTag(it) {
                performPressWithScroll()
            }
        }

        withTag(
            "EditGoalFragmentDatePickerDialog"
        ) {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            "EditGoalDatePickerDialogOKButton"
        ) {
            assertExists()
            assertIsDisplayed()
            hasClickAction()
        }

        withTag(
            "EditGoalDatePickerDialogCANCELButton"
        ) {
            performPressWithoutScroll()
        }

        assertEquals(
            previousEndDateTime,
            getTimeAsLong()
        )

        assertEquals(
            getExpectedEndType(),
            getActualEndType()
        )
    }

    @Test
    fun `02 Select Same Date Test`() = runTest {
        val previousEndDateTime = getTimeAsLong()
        assertNotNull(previousEndDateTime)
        withTag(endTypeButtonTag()) {
            performPressWithScroll()
        }

        withTag(dropDownMenuTag()) {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            dropDownMenuItemTag()
        ) {
            performPressWithoutScroll()
        }

        assertEquals(
            getExpectedEndType(),
            getActualEndType()
        )

        selectDateAndTimeButtonTag?.let {
            withTag(it) {
                performPressWithScroll()
            }
        }

        withTag(
            "EditGoalFragmentDatePickerDialog"
        ) {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            "EditGoalDatePickerDialogOKButton"
        ) {
            performPressWithoutScroll()
        }

        assertEquals(
            previousEndDateTime,
            getTimeAsLong()
        )

        withTag(
            "EditGoalFragmentTimePickerDialog"
        ) {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            "EditGoalTimePickerDialogCANCELButton"
        ) {
            assertExists()
            assertIsDisplayed()
            hasClickAction()
        }

        withTag(
            "EditGoalTimePickerDialogOKButton"
        ) {
            performPressWithoutScroll()
        }

        assertEquals(
            previousEndDateTime,
            getTimeAsLong()
        )
    }

    @Test
    fun `03 Select Different Date And Time Test`() = runTest {
        val activity = getTestMainActivity()
        val previousEndDateTime = getTimeAsLong()

        activity.timeToAdd(2, 2)
        activity.daysToAdd(2)

        withTag(endTypeButtonTag()) {
            performPressWithScroll()
        }

        withTag(dropDownMenuTag()) {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            dropDownMenuItemTag()
        ) {
            performPressWithoutScroll()
        }

        selectDateAndTimeButtonTag?.let {
            withTag(it){
                performPressWithScroll()
            }
        }

        withTag(
            "EditGoalFragmentDatePickerDialog"
        ) {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            "EditGoalDatePickerDialogOKButton"
        ) {
            performPressWithoutScroll()
        }

        assertEquals(
            Converters().fromLocalDateTime(
                Converters().toLocalDateTime(
                    previousEndDateTime!!
                ).value.plusDays(2)
            ),
            getTimeAsLong()
        )

        withTag(
            "EditGoalFragmentTimePickerDialog"
        ) {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            "EditGoalTimePickerDialogCANCELButton"
        ) {
            assertExists()
            assertIsDisplayed()
            hasClickAction()
        }

        withTag(
            "EditGoalTimePickerDialogOKButton"
        ) {
            performPressWithoutScroll()
        }

        val endTimeDate = Converters().toLocalDateTime(
            previousEndDateTime
        ).value
        val timePickerState = TimePickerState(endTimeDate.hour, endTimeDate.minute, true)
        timePickerState.addTime(2, 2)
        assertEquals(
            Converters().fromLocalDateTime(
                endTimeDate.plusDays(2).withHour(timePickerState.hour)
                    .withMinute(timePickerState.minute)
            ),
            getTimeAsLong()
        )

        activity.timeToAdd(0, 0)
        activity.daysToAdd(0)
    }
}