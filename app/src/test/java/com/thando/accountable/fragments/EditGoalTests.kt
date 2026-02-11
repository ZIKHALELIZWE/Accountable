package com.thando.accountable.fragments

import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePickerState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.thando.accountable.AccountableComposeRobolectricTest
import com.thando.accountable.AccountableComposeRobolectricTest.TestMainActivity.Companion.addTime
import com.thando.accountable.AccountableNavigationController.AccountableFragment
import com.thando.accountable.MainActivity
import com.thando.accountable.R
import com.thando.accountable.database.Converters
import com.thando.accountable.database.tables.Deliverable
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.fragments.viewmodels.BooksViewModel
import com.thando.accountable.fragments.viewmodels.EditGoalViewModel
import com.thando.accountable.fragments.viewmodels.HomeViewModel
import com.thando.accountable.input_forms.DateAndTimePickerTest
import com.thando.accountable.input_forms.TimeBlockTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters


@OptIn(ExperimentalCoroutinesApi::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class EditGoalTests: AccountableComposeRobolectricTest() {
    @Test
    fun `01 coreClassesNotNull`() = runTest {
        val activity = getTestMainActivity()
        assertNotNull(activity)
        assertNotNull(activity.viewModel)
        assertNotNull(activity.viewModel.repository)
    }

    @Test
    fun `02 currentFragmentIsHomeFragment`() = runTest {
        val activity = getTestMainActivity()
        assertNotNull(activity.viewModel.currentFragment.value)
        assertEquals(
            AccountableFragment.HomeFragment,
            activity.viewModel.currentFragment.value
        )
        assertEquals(
            AccountableFragment.HomeFragment,
            activity.viewModel.currentFragment.value
        )
    }

    fun checkFragmentIs(
        activity: MainActivity,
        expectedFragment: AccountableFragment,
        expectedViewModel: String
    ) {
        composeTestRule.waitForIdle()
        assertEquals(
            expectedFragment,
            activity.viewModel.currentFragment.value
        )
        assertEquals(
            expectedViewModel,
            activity.viewModel.accountableNavigationController.fragmentViewModel.value?.javaClass?.name
        )
    }

    @Test
    fun `03 Can Go To Goals`() = runTest {
        val activity = getTestMainActivity()
        assertEquals(
            HomeViewModel::class.java.name,
            activity.viewModel.accountableNavigationController.fragmentViewModel.value?.javaClass?.name
        )
        assertEquals(
            AccountableFragment.HomeFragment,
            activity.viewModel.currentFragment.value
        )
        withTag("HomeFragmentLoadGoalsButton") {
            performPressWithoutScroll()
        }

        checkFragmentIs(
            activity, AccountableFragment.GoalsFragment,
            BooksViewModel::class.java.name
        )
    }

    @Test
    fun `04 Is In AppSettings Base, Set To Goals And Is Empty`() = runTest {
        val activity = getTestMainActivity()
        assertEquals(
            AccountableFragment.GoalsFragment,
            activity.viewModel.currentFragment.value
        )
        assertEquals(
            BooksViewModel::class.java.name,
            activity.viewModel.accountableNavigationController.fragmentViewModel.value?.javaClass?.name
        )
        val booksViewModel: BooksViewModel =
            activity.viewModel.accountableNavigationController.fragmentViewModel.value!! as BooksViewModel

        assertNull(booksViewModel.folder.value)
        assertNotNull(booksViewModel.appSettings.value)
        assertNotNull(booksViewModel.showScripts.value?.value)
        assertFalse(booksViewModel.showScripts.value!!.value)
        withTag("BooksSwitchFolderScriptButton") {
            performPressWithoutScroll()
        }
        assertTrue(booksViewModel.showScripts.value!!.value)
        assertNotNull(booksViewModel.goalsList.value)
        assertNull(booksViewModel.foldersList.value)
        assertNull(booksViewModel.scriptsList.value)

        assertEquals(0, booksViewModel.goalsList.value!!.first().size)
    }

    @Test
    fun `05 Open Edit Goal And Check Views`() = runTest {
        val activity = getTestMainActivity()
        withTag("BooksFloatingActionButton") {
            performPressWithoutScroll()
        }
        
        checkFragmentIs(
            activity,
            AccountableFragment.EditGoalFragment,
            EditGoalViewModel::class.java.name
        )

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        withTag("EditGoalTitle") {
            assertExists()
            assertIsDisplayed()
            assertTextEquals(activity.getString(R.string.new_goal))
        }
    }

    @Test
    fun `06 Input Goal`() = runTest {
        val activity = getTestMainActivity()
        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertNotNull(editGoalViewModel.newGoal.value?.first()?.goal)
        assertEquals("", editGoalViewModel.newGoal.value?.first()?.goal)

        withTag("EditGoalGoal") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(activity.getString(R.string.goal))
            assertTextContains("")
            performTextInput("My New Goal")
            assertTextContains("My New Goal")
        }
        assertEquals("My New Goal", editGoalViewModel.newGoal.value?.first()?.goal)
    }

    @Test
    fun `07 Input Location`() = runTest {
        val activity = getTestMainActivity()
        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertNotNull(editGoalViewModel.newGoal.value?.first()?.location)
        assertEquals("", editGoalViewModel.newGoal.value?.first()?.location)
        withTag("EditGoalLocation") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(activity.getString(R.string.location))
            assertTextContains("")
            performTextInput("My New Location")
            assertTextContains("My New Location")
        }
        assertEquals("My New Location", editGoalViewModel.newGoal.value?.first()?.location)
    }

    @Test
    fun `08 Input Image`() = runTest {
        val activity = getTestMainActivity()
        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertNull(editGoalViewModel.newGoal.value?.first()?.getGoalPicture())

        assertNull(editGoalViewModel.newGoal.value?.first()?.getUri(activity)?.value)

        withTag("EditGoalImage") {
            assertDoesNotExist()
        }

        withTag("EditGoalRemoveImageButton") {
            assertDoesNotExist()
        }

        val drawableResId = R.mipmap.ic_launcher
        val uri = Uri.parse("android.resource://${activity.packageName}/$drawableResId")

        assertNotNull(activity.viewModel.galleryLauncherReturnProcess)
        assertNull(activity.viewModel.galleryLauncherMultipleReturnProcess)
        withTag("EditGoalChooseImageButton") {
            assertExists()
            assertTextContains(activity.getString(R.string.choose_image))
            performPressWithScroll()
        }

        // Simulate return process
        activity.viewModel.processGalleryLauncherResult(uri)
        advanceUntilIdle()
        composeTestRule.waitForIdle()

        assertNotNull(editGoalViewModel.newGoal.value?.first()?.getGoalPicture())

        assertNotNull(editGoalViewModel.newGoal.value?.first()?.getUri(activity)?.value)

        withTag("EditGoalImage") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertWidthIsAtLeast(2.dp)
            assertHeightIsAtLeast(2.dp)
        }

        withTag("EditGoalRemoveImageButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertIsEnabled()
            performClick()
        }
        finishProcesses()

        assertNull(editGoalViewModel.newGoal.value?.first()?.getGoalPicture())
        assertNull(editGoalViewModel.newGoal.value?.first()?.getUri(activity)?.value)

        withTag("EditGoalImage") {
            assertDoesNotExist()
        }

        withTag("EditGoalRemoveImageButton") {
            assertDoesNotExist()
        }

        withTag("EditGoalChooseImageButton") {
            assertExists()
            assertTextContains(activity.getString(R.string.choose_image))
            performPressWithScroll()
        }

        // Simulate return process
        activity.viewModel.processGalleryLauncherResult(uri)
        advanceUntilIdle()
        composeTestRule.waitForIdle()

        withTag("EditGoalImage") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertWidthIsAtLeast(2.dp)
            assertHeightIsAtLeast(2.dp)
        }
    }

    @Test
    fun `09 Colour Input`() = runTest {
        val activity = getTestMainActivity()
        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(-1, editGoalViewModel.newGoal.value?.first()?.colour)
        withTag("EditGoalColourDisplayBox") {
            assertDoesNotExist()
        }

        withTag("EditGoalPickColourButton") {
            performPressWithScroll()
        }

        withTag("ColourPickerDialog") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("ColourPickerDialogDismissButton") {
            performPressWithScroll()
        }

        assertEquals(-1, editGoalViewModel.newGoal.value?.first()?.colour)
        withTag("ColourPickerDialog") {
            assertDoesNotExist()
        }

        withTag("EditGoalPickColourButton") {
            performPressWithScroll()
        }

        withTag("ColourPickerDialogCanvas") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            performTouchInput { click(Offset(50f, 50f)) }
        }
        finishProcesses()

        withTag("ColourPickerDialogOKButton") {
            performPressWithScroll()
        }

        withTag("ColourPickerDialog") {
            assertDoesNotExist()
        }

        assertNotEquals(-1, editGoalViewModel.newGoal.value?.first()?.colour)
        withTag("EditGoalColourDisplayBox") {
            assertExists()
            assertWidthIsAtLeast(2.dp)
            assertHeightIsAtLeast(2.dp)
        }
    }

    @Test
    fun `10 End Type Undefined`() = runTest {
        val activity = getTestMainActivity()
        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(
            Goal.GoalEndType.UNDEFINED.name,
            editGoalViewModel.newGoal.value?.first()?.endType
        )

        withTag("EditGoalEndTypeButton") {
            performPressWithScroll()
        }

        withTag("EditGoalEndTypeDropDownMenu") {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            "EditGoalDropdownMenuItem-${Goal.GoalEndType.DELIVERABLE.name}"
        ) {
            performPressWithoutScroll()
        }

        assertEquals(
            Goal.GoalEndType.DELIVERABLE.name,
            editGoalViewModel.newGoal.value?.first()?.endType
        )

        withTag("EditGoalEndTypeButton") {
            performPressWithScroll()
        }

        withTag("EditGoalEndTypeDropDownMenu") {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            "EditGoalDropdownMenuItem-${Goal.GoalEndType.UNDEFINED.name}"
        ) {
            performPressWithoutScroll()
        }

        assertEquals(
            Goal.GoalEndType.UNDEFINED.name,
            editGoalViewModel.newGoal.value?.first()?.endType
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun `11 End Type Date`() = runTest {
        val activity = getTestMainActivity()
        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(
            Goal.GoalEndType.UNDEFINED.name,
            editGoalViewModel.newGoal.value?.first()?.endType
        )

        DateAndTimePickerTest(
            getTimeAsLong = { editGoalViewModel.newGoal.value?.first()?.endDateTime },
            getExpectedEndType = { Goal.GoalEndType.DATE.name },
            getActualEndType = { editGoalViewModel.newGoal.value?.first()?.endType },
            endTypeButtonTag = { "EditGoalEndTypeButton" },
            dropDownMenuTag = { "EditGoalEndTypeDropDownMenu" },
            dropDownMenuItemTag = { "EditGoalDropdownMenuItem-${Goal.GoalEndType.DATE.name}" },
            selectDateAndTimeButtonTag = null,
            parentParameters = Triple(
                instantTaskExecutorRule,
                composeTestRule,
                activity
            )
        ).runTests(this@EditGoalTests::class)
    }

    @Test
    fun `12 End Type Deliverable`() = runTest {
        val activity = getTestMainActivity()
        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(
            Goal.GoalEndType.DATE.name,
            editGoalViewModel.newGoal.value?.first()?.endType
        )

        withTag("EditGoalEndTypeButton") {
            performPressWithScroll()
        }

        withTag("EditGoalEndTypeDropDownMenu") {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            "EditGoalDropdownMenuItem-${Goal.GoalEndType.DELIVERABLE.name}"
        ) {
            performPressWithoutScroll()
        }

        assertEquals(
            Goal.GoalEndType.DELIVERABLE.name,
            editGoalViewModel.newGoal.value?.first()?.endType
        )

        withTag(
            "EditGoalSelectDeliverableButton"
        ) {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            assertIsNotEnabled()
        }

        assertNotNull(editGoalViewModel.newGoal.value?.first()?.id)
        editGoalViewModel.newGoal.value?.first()?.id?.let { id ->
            assertEquals(
                0,
                activity.viewModel.repository.getDeliverables(id).first().size
            )
            assertEquals(
                0,
                activity.viewModel.repository.getGoalDeliverables(id).first().size
            )
        }

        withTag(
            "EditGoalAddDeliverableButton"
        ) {
            performPressWithoutScroll()
        }

        assertNull(editGoalViewModel.originalDeliverable.value)
        assertNotNull(editGoalViewModel.deliverable.value)

        withTag(
            "TasksFragmentDeliverablesBottomSheet"
        ) {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            "TasksFragmentDeliverableTitle"
        ) {
            assertExists()
            assertIsDisplayed()
            assertTextContains(
                activity.getString(
                    R.string.add, activity.getString(R.string.deliverable)
                )
            )
        }

        withTag(
            "TasksFragmentDeliverableProcessDeliverable"
        ) {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
        }

        assertEquals(
            "",
            editGoalViewModel.deliverable.value?.first()?.deliverable
        )

        withTag(
            "TaskFragmentDeliverableDeliverableText"
        ) {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains("")
            performTextInput("My Deliverable")
            assertTextContains("My Deliverable")
        }
        advanceUntilIdle()
        composeTestRule.waitForIdle()

        assertEquals(
            "My Deliverable",
            editGoalViewModel.deliverable.value?.first()?.deliverable
        )

        assertEquals(
            "My New Location",
            editGoalViewModel.deliverable.value?.first()?.location
        )

        withTag(
            "TaskFragmentDeliverableLocationText"
        ) {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains("My New Location")
            performTextReplacement("My Location")
            assertTextContains("My Location")
        }
        advanceUntilIdle()
        composeTestRule.waitForIdle()

        assertEquals(
            "My Location",
            editGoalViewModel.deliverable.value?.first()?.location
        )


    }

    @Test
    fun `13 End Type Deliverable End Type Undefined`() = runTest {
        val activity = getTestMainActivity()

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertNotNull(editGoalViewModel.deliverable.value)
        assertNotNull(editGoalViewModel.deliverable.value?.first())

        assertEquals(
            Deliverable.DeliverableEndType.UNDEFINED.name,
            editGoalViewModel.deliverable.value?.first()?.endType
        )

        withTag("TasksFragmentDeliverableEndTypeButton") {
            performPressWithScroll()
        }

        withTag("TasksFragmentDeliverableEndTypeDropDownMenu") {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            "TasksFragmentDeliverableDropdownMenuItem-${Deliverable.DeliverableEndType.DATE.name}"
        ) {
            performPressWithoutScroll()
        }

        assertEquals(
            Deliverable.DeliverableEndType.DATE.name,
            editGoalViewModel.deliverable.value?.first()?.endType
        )

        withTag("EditGoalFragmentDatePickerDialog") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("EditGoalDatePickerDialogCANCELButton") {
            performPressWithoutScroll()
        }

        assertEquals(
            Deliverable.DeliverableEndType.DATE.name,
            editGoalViewModel.deliverable.value?.first()?.endType
        )

        withTag("TasksFragmentDeliverableEndTypeButton") {
            performPressWithScroll()
        }

        withTag("TasksFragmentDeliverableEndTypeDropDownMenu") {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            "TasksFragmentDeliverableDropdownMenuItem-${Deliverable.DeliverableEndType.UNDEFINED.name}"
        ) {
            performPressWithoutScroll()
        }

        assertEquals(
            Deliverable.DeliverableEndType.UNDEFINED.name,
            editGoalViewModel.deliverable.value?.first()?.endType
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun `14 End Type Deliverable End Type Date`() = runTest {
        val activity = getTestMainActivity()

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(
            Deliverable.DeliverableEndType.UNDEFINED.name,
            editGoalViewModel.deliverable.value?.first()?.endType
        )

        withTag("TasksFragmentDeliverableEndTypeButton") {
            performPressWithScroll()
        }

        withTag("TasksFragmentDeliverableEndTypeDropDownMenu") {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            "TasksFragmentDeliverableDropdownMenuItem-${Deliverable.DeliverableEndType.DATE.name}"
        ) {
            performPressWithoutScroll()
        }

        assertEquals(
            Deliverable.DeliverableEndType.DATE.name,
            editGoalViewModel.deliverable.value?.first()?.endType
        )

        assertNotNull(editGoalViewModel.deliverable.value?.first()?.endDateTime)
        val previousEndDateTime = editGoalViewModel.deliverable.value?.first()?.endDateTime

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
            editGoalViewModel.deliverable.value?.first()?.endDateTime
        )

        assertEquals(
            Deliverable.DeliverableEndType.DATE.name,
            editGoalViewModel.deliverable.value?.first()?.endType
        )

        withTag("TasksFragmentDeliverableEndTypeButton") {
            performPressWithScroll()
        }

        withTag("TasksFragmentDeliverableEndTypeDropDownMenu") {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            "TasksFragmentDeliverableDropdownMenuItem-${Deliverable.DeliverableEndType.DATE.name}"
        ) {
            performPressWithoutScroll()
        }

        assertEquals(
            Deliverable.DeliverableEndType.DATE.name,
            editGoalViewModel.deliverable.value?.first()?.endType
        )

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
            editGoalViewModel.deliverable.value?.first()?.endDateTime
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
            editGoalViewModel.deliverable.value?.first()?.endDateTime
        )

        activity.timeToAdd(2, 2)
        activity.daysToAdd(2)

        withTag("TasksFragmentDeliverableEndTypeButton") {
            performPressWithScroll()
        }

        withTag("TasksFragmentDeliverableEndTypeDropDownMenu") {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            "TasksFragmentDeliverableDropdownMenuItem-${Deliverable.DeliverableEndType.DATE.name}"
        ) {
            performPressWithoutScroll()
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
            editGoalViewModel.deliverable.value!!.first()!!.endDateTime
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
            editGoalViewModel.deliverable.value!!.first()!!.endDateTime
        )

        activity.timeToAdd(0, 0)
        activity.daysToAdd(0)
    }

    @Test
    fun `15 End Type Deliverable End Type Goal`() = runTest {
        val activity = getTestMainActivity()

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertNotNull(editGoalViewModel.deliverable.value)
        assertNotNull(editGoalViewModel.deliverable.value?.first())

        assertEquals(
            Deliverable.DeliverableEndType.DATE.name,
            editGoalViewModel.deliverable.value?.first()?.endType
        )

        withTag("TasksFragmentDeliverableSwitch") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
        }

        withTag("TasksFragmentDeliverableEndTypeButton") {
            performPressWithScroll()
        }

        withTag("TasksFragmentDeliverableEndTypeDropDownMenu") {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            "TasksFragmentDeliverableDropdownMenuItem-${Deliverable.DeliverableEndType.GOAL.name}"
        ) {
            performPressWithoutScroll()
        }

        assertEquals(
            Deliverable.DeliverableEndType.GOAL.name,
            editGoalViewModel.deliverable.value?.first()?.endType
        )

        assertNull(editGoalViewModel.deliverable.value?.first()?.goalId)

        withTag("TasksFragmentDeliverableSwitch") {
            assertDoesNotExist()
        }

        withTag("TasksFragmentDeliverableEndTypeButton") {
            performPressWithScroll()
        }

        withTag("TasksFragmentDeliverableEndTypeDropDownMenu") {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            "TasksFragmentDeliverableDropdownMenuItem-${Deliverable.DeliverableEndType.UNDEFINED.name}"
        ) {
            performPressWithoutScroll()
        }

        assertEquals(
            Deliverable.DeliverableEndType.UNDEFINED.name,
            editGoalViewModel.deliverable.value?.first()?.endType
        )

        assertNull(editGoalViewModel.deliverable.value?.first()?.goalId)

        withTag("TasksFragmentDeliverableSwitch") {
            performPressWithScroll()
        }

        assertEquals(
            editGoalViewModel.newGoal.value?.first()?.id,
            editGoalViewModel.deliverable.value?.first()?.goalId
        )

        withTag("TasksFragmentDeliverableEndTypeButton") {
            performPressWithScroll()
        }

        withTag("TasksFragmentDeliverableEndTypeDropDownMenu") {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            "TasksFragmentDeliverableDropdownMenuItem-${Deliverable.DeliverableEndType.GOAL.name}"
        ) {
            performPressWithoutScroll()
        }

        assertEquals(
            Deliverable.DeliverableEndType.GOAL.name,
            editGoalViewModel.deliverable.value?.first()?.endType
        )

        assertNull(editGoalViewModel.deliverable.value?.first()?.goalId)
    }

    /**
     * <h2><b>Types of work:</b></h2>
     *  1. Complete a certain repeating task x amount of times (including streak)
     *  2. Complete a certain quantity/time once for a certain quantity/time task
     *  (including streak)
     *  3. Complete all tasks assigned to a deliverable
     *  (Have boolean value for when no tasks have been assigned yet, have another for 'should
     *  complete' when all tasks completed)
     *  4. Work deliverables can have deadlines stored in endDateTime (when there is no deadline
     *  it will be null)
     *
     *  I think this can wait until you finish the task input then you can come back TODO
     */

    @Test
    fun `16 End Type Deliverable End Type Work`() = runTest {
        val activity = getTestMainActivity()

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(
            Deliverable.DeliverableEndType.GOAL.name,
            editGoalViewModel.deliverable.value?.first()?.endType
        )

        assertNull(editGoalViewModel.deliverable.value?.first()?.goalId)

        withTag("TasksFragmentDeliverableEndTypeButton") {
            performPressWithScroll()
        }

        withTag("TasksFragmentDeliverableEndTypeDropDownMenu") {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            "TasksFragmentDeliverableDropdownMenuItem-${Deliverable.DeliverableEndType.WORK.name}"
        ) {
            performPressWithoutScroll()
        }

        assertEquals(
            Deliverable.DeliverableEndType.WORK.name,
            editGoalViewModel.deliverable.value?.first()?.endType
        )

        //todo check if work views and values are present

        withTag("TasksFragmentDeliverableEndTypeButton") {
            performPressWithScroll()
        }

        withTag("TasksFragmentDeliverableEndTypeDropDownMenu") {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            "TasksFragmentDeliverableDropdownMenuItem-${Deliverable.DeliverableEndType.UNDEFINED.name}"
        ) {
            performPressWithoutScroll()
        }

        assertEquals(
            Deliverable.DeliverableEndType.UNDEFINED.name,
            editGoalViewModel.deliverable.value?.first()?.endType
        )

        // todo check if work views and values are gone
    }

    @Test
    fun `17 Add Time Block`() = runTest {
        val activity = getTestMainActivity()

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(
            Deliverable.DeliverableEndType.UNDEFINED.name,
            editGoalViewModel.deliverable.value?.first()?.endType
        )

        assertNotNull(editGoalViewModel.deliverable.value?.first()?.times)

        TimeBlockTest(
            "TasksFragmentDeliverableAddTimeBlockButton",
            editGoalViewModel.deliverable.value!!.first()!!.times,
            Triple(
                instantTaskExecutorRule,
                composeTestRule,
                activity
            )
        ).runTests(this@EditGoalTests::class)
    }
}