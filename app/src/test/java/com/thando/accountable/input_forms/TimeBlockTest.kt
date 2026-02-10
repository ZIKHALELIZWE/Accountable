package com.thando.accountable.input_forms

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.performScrollTo
import com.thando.accountable.AccountableComposeRobolectricTest
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TimeBlockTest(
    val addTimeBlockButtonTag: String,
    val timeBlockList: MutableStateFlow<Flow<List<GoalTaskDeliverableTime>>?>,
    parentParameters:Triple<
            InstantTaskExecutorRule,
            ComposeContentTestRule,
            TestMainActivity
    >?=null
): AccountableComposeRobolectricTest(
    parentParameters
) {
    @Test
    fun `01 Add Time Button Exists`() = runTest {
        withTag(addTimeBlockButtonTag){
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
        }
    }

    @Test
    fun `02 Add Time Button Press`() = runTest {
        assertNotNull(timeBlockList.value?.first())
        assertEquals(emptyList<GoalTaskDeliverableTime>(),timeBlockList.value?.first())

        withTag(addTimeBlockButtonTag){
            performPressWithScroll()
        }

        assertEquals(1,timeBlockList.value?.first()?.size)
        assertNotNull(timeBlockList.value?.first()[0]?.id)
    }

    @Test
    fun `03 Delete Time Button Press`() = runTest {
        assertNotNull(timeBlockList.value?.first())
        assertEquals(1,timeBlockList.value?.first()?.size)

        val firstItemId = timeBlockList.value?.first()[0]?.id
        assertNotNull(firstItemId)

        withTag("EditGoalTimeInputViewCard-$firstItemId"){
            assertExists()
            performScrollTo()
            assertIsDisplayed()
        }

        withTag("EditGoalTimeInputDeleteButton-$firstItemId") {
            performPressWithoutScroll()
        }

        assertEquals(0,timeBlockList.value?.first()?.size)

        withTag("EditGoalTimeInputViewCard-$firstItemId"){
            assertDoesNotExist()
        }
    }

    private fun pickerMenuTypeSwitchFromTo(
        from:Goal.TimeBlockType,
        to:Goal.TimeBlockType,
        index:Int,
    ) = runTest {
        assertTrue(index>-1)
        assertTrue(timeBlockList.value?.first()?.isNotEmpty() == true)
        assertTrue(index < (timeBlockList.value?.first()?.size ?: -1))
        val firstItemId = timeBlockList.value?.first()[0]?.id
        assertNotNull(firstItemId)

        assertEquals(
            from.name,
            timeBlockList.value?.first()[0]?.timeBlockType
        )

        withTag("EditGoalPickerMenuButton-$firstItemId"){
            performPressWithScroll()
        }

        withTag("EditGoalPickerMenuDropdownMenu-$firstItemId"){
            assertExists()
            assertIsDisplayed()
        }

        withTag("EditGoalPickerMenuItem-$firstItemId-${to.name}"){
            performPressWithoutScroll()
        }

        assertEquals(
            to.name,
            timeBlockList.value?.first()[0]?.timeBlockType
        )
    }

    @Test
    fun `04 Picker Menu Type Switching`() = runTest {
        `02 Add Time Button Press`()

        var currentTimeBlockType = Goal.TimeBlockType.ONCE
        val listToSwitchFrom = listOf(
            Goal.TimeBlockType.DAILY,
            Goal.TimeBlockType.WEEKLY,
            Goal.TimeBlockType.MONTHLY,
            Goal.TimeBlockType.YEARLY,
            Goal.TimeBlockType.WEEKLY,
            Goal.TimeBlockType.DAILY,
            Goal.TimeBlockType.MONTHLY,
            Goal.TimeBlockType.ONCE
        )
        listToSwitchFrom.forEach { newTimeBlockType ->
            pickerMenuTypeSwitchFromTo(
                currentTimeBlockType,
                newTimeBlockType,
                0
            )
            currentTimeBlockType = newTimeBlockType
        }
    }

    @Test
    fun `05 Picker Menu Once Monthly Yearly`() = runTest {
        val activity = getTestMainActivity()

        var previousTimeBlockType = Goal.TimeBlockType.ONCE.name
        listOf(
            Goal.TimeBlockType.MONTHLY.name,
            Goal.TimeBlockType.YEARLY.name,
            Goal.TimeBlockType.ONCE.name
        ).forEach { expectedTimeBlockType ->
            assertEquals(
                previousTimeBlockType,
                timeBlockList.value?.first()[0]?.timeBlockType
            )

            testDateAndTimePicker(
                activity,
                getTimeAsLong = { timeBlockList.value?.first()[0]?.start },
                getExpectedEndType = { expectedTimeBlockType },
                getActualEndType = { timeBlockList.value?.first()[0]?.timeBlockType },
                endTypeButtonTag = { "EditGoalPickerMenuButton-${timeBlockList.value?.first()[0]?.id}" },
                dropDownMenuTag = { "EditGoalPickerMenuDropdownMenu-${timeBlockList.value?.first()[0]?.id}" },
                dropDownMenuItemTag = {
                    "EditGoalPickerMenuItem-${timeBlockList.value?.first()[0]?.id}-${expectedTimeBlockType}"
                },
                selectDateAndTimeButtonTag = "EditGoalTimeInputSelectDateAndTimeButton"
            )

            previousTimeBlockType = expectedTimeBlockType
        }
    }

    @Test
    fun `06 Picker Menu Weekly`() = runTest {

    }
}