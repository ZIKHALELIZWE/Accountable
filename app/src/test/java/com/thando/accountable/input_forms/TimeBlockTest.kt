package com.thando.accountable.input_forms

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.thando.accountable.AccountableComposeRobolectricTest
import com.thando.accountable.AppResources
import com.thando.accountable.database.Converters
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import com.thando.accountable.fragments.BackgroundColorKey
import com.thando.accountable.fragments.SliderRangeKey
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
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

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

            DateAndTimePickerTest(
                getTimeAsLong = { timeBlockList.value?.first()[0]?.start },
                getExpectedEndType = { expectedTimeBlockType },
                getActualEndType = { timeBlockList.value?.first()[0]?.timeBlockType },
                endTypeButtonTag = { "EditGoalPickerMenuButton-${timeBlockList.value?.first()[0]?.id}" },
                dropDownMenuTag = { "EditGoalPickerMenuDropdownMenu-${timeBlockList.value?.first()[0]?.id}" },
                dropDownMenuItemTag = {
                    "EditGoalPickerMenuItem-${timeBlockList.value?.first()[0]?.id}-${expectedTimeBlockType}"
                },
                selectDateAndTimeButtonTag = "EditGoalTimeInputSelectDateAndTimeButton",
                parentParameters = Triple(
                    instantTaskExecutorRule,
                    composeTestRule,
                    activity
                )
            ).runTests(this@TimeBlockTest::class)

            previousTimeBlockType = expectedTimeBlockType
        }
    }

    @Test
    fun `06 Picker Menu Weekly Switching Days`() = runTest {
        val activity = getTestMainActivity()

        assertEquals(
            Goal.TimeBlockType.ONCE.name,
            timeBlockList.value?.first()[0]?.timeBlockType
        )
        pickerMenuTypeSwitchFromTo(
            from = Goal.TimeBlockType.ONCE,
            to = Goal.TimeBlockType.WEEKLY,
            index = 0
        )
        assertEquals(
            Goal.TimeBlockType.WEEKLY.name,
            timeBlockList.value?.first()[0]?.timeBlockType
        )

        val startLocalDateTime = Converters().toLocalDateTime(
            timeBlockList.value?.first()[0]?.start
        ).value
        val currentLocalDatetime = LocalDateTime.now().plusDays(6)
        assertNotNull(startLocalDateTime)
        assertEquals(
            // Previous test adds 6 days, 2 hours and 2 minutes to current date
            AppResources.getDayWord(activity, currentLocalDatetime),
            AppResources.getDayWord(
                activity,
                startLocalDateTime!!
            )
        )

        var previousDay:String = AppResources.getDayWord(activity, currentLocalDatetime)
        AppResources.Companion.DaysOfTheWeek.entries.map {
            activity.getString(it.day)
        }.forEach { outerLoopDay ->
            withTag("EditGoalTimeInputWeeklyAndTimeButton") {
                performPressWithScroll()
            }

            withTag("EditGoalPickWeekdayDialog") {
                assertExists()
            }

            AppResources.Companion.DaysOfTheWeek.entries.map {
                activity.getString(it.day)
            }.forEach { day ->
                withTag("EditGoalPickWeekdayDay-${day}") {
                    assertExists()
                    assertIsDisplayed()
                    assertHasClickAction()
                    assertEquals(
                        if (day == previousDay)
                            Color.Blue else Color.LightGray,
                        fetchSemanticsNode().config[BackgroundColorKey]
                    )
                }
            }
            previousDay = outerLoopDay
            withTag("EditGoalPickWeekdayDay-${outerLoopDay}"){
                performPressWithoutScroll()
            }

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

            assertNotNull(Converters().toLocalDateTime(
                timeBlockList.value?.first()[0]?.start
            ).value)
            assertEquals(
                // Previous test adds 6 days, 2 hours and 2 minutes to current date
                AppResources.getDayWord(
                    activity,
                    currentLocalDatetime.with(TemporalAdjusters.nextOrSame(
                        AppResources.getDayOfWeek(activity,outerLoopDay)
                    ))
                ),
                AppResources.getDayWord(
                    activity,
                    Converters().toLocalDateTime(
                        timeBlockList.value!!.first()[0].start
                    ).value
                )
            )
        }
    }

    @Test
    fun `07 Picker Menu Daily`() = runTest {
        val activity = getTestMainActivity()

        assertEquals(
            Goal.TimeBlockType.WEEKLY.name,
            timeBlockList.value?.first()[0]?.timeBlockType
        )
        pickerMenuTypeSwitchFromTo(
            from = Goal.TimeBlockType.WEEKLY,
            to = Goal.TimeBlockType.DAILY,
            index = 0
        )
        assertEquals(
            Goal.TimeBlockType.DAILY.name,
            timeBlockList.value?.first()[0]?.timeBlockType
        )

        assertNotNull(
            Converters().toLocalDateTime(
                timeBlockList.value?.first()[0]?.start
            ).value
        )

        val hours = Converters().toLocalDateTime(
            timeBlockList.value!!.first()[0].start
        ).value.hour
        val minutes = Converters().toLocalDateTime(
            timeBlockList.value!!.first()[0].start
        ).value.minute
        activity.timeToAdd(-minutes,-hours)
        changeDailyTimeTest(
            timeBlockList,
            0,
            0
        )
        activity.timeToAdd(0,0)
    }

    private fun changeDailyTimeTest(
        timeBlockList: MutableStateFlow<Flow<List<GoalTaskDeliverableTime>>?>,
        expectedMinute:Int,
        expectedHour:Int
    ) = runTest {
        withTag("EditGoalTimeInputDailyTimeButton"){
            performPressWithScroll()
        }

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

        assertNotNull(
            Converters().toLocalDateTime(
                timeBlockList.value?.first()[0]?.start
            ).value
        )

        assertEquals(
            expectedHour,
            Converters().toLocalDateTime(
                timeBlockList.value!!.first()[0].start
            ).value.hour
        )
        assertEquals(
            expectedMinute,
            Converters().toLocalDateTime(
                timeBlockList.value!!.first()[0].start
            ).value.minute
        )
    }

    private fun getSelectableMinutes(
        selectedHours:Int,
        selectableHours:Int,
        expectedMinute:Int
    ):Int{
        return if (selectedHours  == selectableHours) 59 - expectedMinute else 59
    }

    @Test
    fun `08 Picker Menu Duration Picker`() = runTest {
        val activity = getTestMainActivity()
        assertEquals(
            Goal.TimeBlockType.DAILY.name,
            timeBlockList.value?.first()[0]?.timeBlockType
        )

        assertNotNull(
            Converters().toLocalDateTime(
                timeBlockList.value?.first()[0]?.start
            ).value
        )

        val minuteStep = 5
        val hourStep = 2
        activity.timeToAdd(minuteStep,hourStep)
        for (i in 2..22 step 2) {
            val expectedMinute = (i/2)*minuteStep
            val expectedHour = (i/2)*hourStep

            changeDailyTimeTest(
                timeBlockList,
                expectedMinute,
                expectedHour
            )

            // Testing duration picker
            withTag("EditGoalTimeInputPickDurationButton"){
                performPressWithScroll()
            }

            withTag("EditGoalTimeInputDurationPickerDialog"){
                assertExists()
                assertIsDisplayed()
            }

            var selectedHours = 0
            val selectableHours = 23 - expectedHour
            var selectableMinutes = getSelectableMinutes(selectedHours,selectableHours,expectedMinute)

            withTag("EditGoalDurationPickerMinuteSlider"){
                assertExists()
                assertIsDisplayed()
                assertEquals(
                    0f..selectableMinutes.toFloat(),
                    fetchSemanticsNode().config[SliderRangeKey]
                )
            }

            selectedHours = selectableHours
            selectableMinutes = getSelectableMinutes(selectedHours,selectableHours,expectedMinute)

            withTag("EditGoalDurationPickerHourSlider"){
                assertExists()
                assertIsDisplayed()
                assertEquals(
                    0f..selectableHours.toFloat(),
                    fetchSemanticsNode().config[SliderRangeKey]
                )
                performSemanticsAction(SemanticsActions.SetProgress) { action ->
                    action(selectedHours.toFloat())
                }
            }
            finishProcesses()

            withTag("EditGoalDurationPickerMinuteSlider"){
                assertExists()
                assertIsDisplayed()
                assertEquals(
                    0f..selectableMinutes.toFloat(),
                    fetchSemanticsNode().config[SliderRangeKey]
                )
            }

            withTag("EditGoalDurationPickerHourSlider"){
                assertExists()
                assertIsDisplayed()
                assertEquals(
                    0f..selectableHours.toFloat(),
                    fetchSemanticsNode().config[SliderRangeKey]
                )
                performSemanticsAction(SemanticsActions.SetProgress) { action ->
                    action(0f)
                }
            }
            finishProcesses()
        }
    }
}