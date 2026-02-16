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
import androidx.compose.ui.test.performScrollToKey
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
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import com.thando.accountable.fragments.viewmodels.BooksViewModel
import com.thando.accountable.fragments.viewmodels.EditGoalViewModel
import com.thando.accountable.fragments.viewmodels.HomeViewModel
import com.thando.accountable.input_forms.DateAndTimePickerTest
import com.thando.accountable.input_forms.TimeBlockTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
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
    fun `01 coreClassesNotNull`() = runTest(TestMainActivity.dispatcher) {
        val activity = getTestMainActivity()
        assertNotNull(activity)
        assertNotNull(activity.viewModel)
        assertNotNull(activity.viewModel.repository)
    }

    @Test
    fun `02 currentFragmentIsHomeFragment`() = runTest(TestMainActivity.dispatcher) {
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
    fun `03 Can Go To Goals`() = runTest(TestMainActivity.dispatcher) {
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
    fun `04 Is In AppSettings Base, Set To Goals And Is Empty`() = runTest(TestMainActivity.dispatcher) {
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
        assertNotNull(booksViewModel.showScripts.first())
        assertFalse(booksViewModel.showScripts.first())
        withTag("BooksSwitchFolderScriptButton") {
            performPressWithoutScroll()
        }
        assertTrue(booksViewModel.showScripts.first())
        assertTrue(booksViewModel.goalsList.first().isEmpty())
        assertTrue(booksViewModel.foldersList.first().isEmpty())
        assertTrue(booksViewModel.scriptsList.first().isEmpty())

        assertEquals(0, booksViewModel.goalsList.first().size)
    }

    @Test
    fun `05 Open Edit Goal And Check Views`() = runTest(TestMainActivity.dispatcher) {
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
    fun `06 Input Goal`() = runTest(TestMainActivity.dispatcher) {
        val activity = getTestMainActivity()
        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertNotNull(editGoalViewModel.newGoal.first()?.goal)
        assertEquals("", editGoalViewModel.newGoal.first()?.goal)

        withTag("EditGoalGoal") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(activity.getString(R.string.goal))
            assertTextContains("")
            performTextInput("My New Goal")
            assertTextContains("My New Goal")
        }
        finishProcesses()
        assertEquals("My New Goal", editGoalViewModel.newGoal.first()?.goal)
    }

    @Test
    fun `07 Input Location`() = runTest(TestMainActivity.dispatcher) {
        val activity = getTestMainActivity()
        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertNotNull(editGoalViewModel.newGoal.first()?.location)
        assertEquals("", editGoalViewModel.newGoal.first()?.location)
        withTag("EditGoalLocation") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(activity.getString(R.string.location))
            assertTextContains("")
            performTextInput("My New Location")
            assertTextContains("My New Location")
        }
        finishProcesses()
        assertEquals("My New Location", editGoalViewModel.newGoal.first()?.location)
    }

    @Test
    fun `08 Input Image`() = runTest(TestMainActivity.dispatcher) {
        val activity = getTestMainActivity()
        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertNull(editGoalViewModel.newGoal.first()?.getGoalPicture())

        assertNull(editGoalViewModel.newGoal.first()?.getUri(activity)?.first())

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
        finishProcesses()

        assertNotNull(editGoalViewModel.newGoal.first()?.getGoalPicture())
        assertNotNull(editGoalViewModel.newGoal.first()?.getUri(activity)?.first())

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

        assertNull(editGoalViewModel.newGoal.first()?.getGoalPicture())
        assertNull(editGoalViewModel.newGoal.first()?.getUri(activity)?.first())

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
        finishProcesses()

        withTag("EditGoalImage") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertWidthIsAtLeast(2.dp)
            assertHeightIsAtLeast(2.dp)
        }
    }

    @Test
    fun `09 Colour Input`() = runTest(TestMainActivity.dispatcher) {
        val activity = getTestMainActivity()
        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(-1, editGoalViewModel.newGoal.first()?.colour)
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

        assertEquals(-1, editGoalViewModel.newGoal.first()?.colour)
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

        assertNotEquals(-1, editGoalViewModel.newGoal.first()?.colour)
        withTag("EditGoalColourDisplayBox") {
            assertExists()
            assertWidthIsAtLeast(2.dp)
            assertHeightIsAtLeast(2.dp)
        }
    }

    @Test
    fun `10 End Type Undefined`() = runTest(TestMainActivity.dispatcher) {
        val activity = getTestMainActivity()
        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(
            Goal.GoalEndType.UNDEFINED.name,
            editGoalViewModel.newGoal.first()?.endType
        )

        finishProcesses()
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
            editGoalViewModel.newGoal.first()?.endType
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
            editGoalViewModel.newGoal.first()?.endType
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun `11 End Type Date`() = runTest(TestMainActivity.dispatcher) {
        val activity = getTestMainActivity()
        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(
            Goal.GoalEndType.UNDEFINED.name,
            editGoalViewModel.newGoal.first()?.endType
        )
        finishProcesses()

        DateAndTimePickerTest(
            getTimeAsLong = { editGoalViewModel.newGoal.first()?.endDateTime },
            getExpectedEndType = { Goal.GoalEndType.DATE.name },
            getActualEndType = { editGoalViewModel.newGoal.first()?.endType },
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
    fun `12 End Type Deliverable`() = runTest(TestMainActivity.dispatcher) {
        val activity = getTestMainActivity()
        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(
            Goal.GoalEndType.DATE.name,
            editGoalViewModel.newGoal.first()?.endType
        )
        finishProcesses()

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
            editGoalViewModel.newGoal.first()?.endType
        )

        withTag(
            "EditGoalSelectDeliverableButton"
        ) {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            assertIsNotEnabled()
        }

        assertNotNull(editGoalViewModel.newGoal.first()?.id)
        editGoalViewModel.newGoal.first()?.id?.let { id ->
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

        assertNull(editGoalViewModel.originalDeliverable.first())
        assertNotNull(editGoalViewModel.deliverable.first())

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
            editGoalViewModel.deliverable.first()?.deliverable
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
        finishProcesses()

        assertEquals(
            "My Deliverable",
            editGoalViewModel.deliverable.first()?.deliverable
        )

        assertEquals(
            "My New Location",
            editGoalViewModel.deliverable.first()?.location
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
        finishProcesses()

        assertEquals(
            "My Location",
            editGoalViewModel.deliverable.first()?.location
        )


    }

    @Test
    fun `13 End Type Deliverable End Type Undefined`() = runTest(TestMainActivity.dispatcher) {
        val activity = getTestMainActivity()

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertNotNull(editGoalViewModel.deliverable.first())

        assertEquals(
            Deliverable.DeliverableEndType.UNDEFINED.name,
            editGoalViewModel.deliverable.first()?.endType
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
            editGoalViewModel.deliverable.first()?.endType
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
            editGoalViewModel.deliverable.first()?.endType
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
            editGoalViewModel.deliverable.first()?.endType
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun `14 End Type Deliverable End Type Date`() = runTest(TestMainActivity.dispatcher) {
        val activity = getTestMainActivity()

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(
            Deliverable.DeliverableEndType.UNDEFINED.name,
            editGoalViewModel.deliverable.first()?.endType
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
            editGoalViewModel.deliverable.first()?.endType
        )

        assertNotNull(editGoalViewModel.deliverable.first()?.endDateTime)
        val previousEndDateTime = editGoalViewModel.deliverable.first()?.endDateTime

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
            editGoalViewModel.deliverable.first()?.endDateTime
        )

        assertEquals(
            Deliverable.DeliverableEndType.DATE.name,
            editGoalViewModel.deliverable.first()?.endType
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
            editGoalViewModel.deliverable.first()?.endType
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
            editGoalViewModel.deliverable.first()?.endDateTime
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
            editGoalViewModel.deliverable.first()?.endDateTime
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
            editGoalViewModel.deliverable.first()!!.endDateTime
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
            editGoalViewModel.deliverable.first()!!.endDateTime
        )

        activity.timeToAdd(0, 0)
        activity.daysToAdd(0)
    }

    @Test
    fun `15 End Type Deliverable End Type Goal`() = runTest(TestMainActivity.dispatcher) {
        val activity = getTestMainActivity()

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertNotNull(editGoalViewModel.deliverable.first())

        assertEquals(
            Deliverable.DeliverableEndType.DATE.name,
            editGoalViewModel.deliverable.first()?.endType
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
            editGoalViewModel.deliverable.first()?.endType
        )

        assertNull(editGoalViewModel.deliverable.first()?.goalId)

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
            editGoalViewModel.deliverable.first()?.endType
        )

        assertNull(editGoalViewModel.deliverable.first()?.goalId)

        withTag("TasksFragmentDeliverableSwitch") {
            performPressWithScroll()
        }

        assertEquals(
            editGoalViewModel.newGoal.first()?.id,
            editGoalViewModel.deliverable.first()?.goalId
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
            editGoalViewModel.deliverable.first()?.endType
        )

        assertNull(editGoalViewModel.deliverable.first()?.goalId)
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
    fun `16 End Type Deliverable End Type Work`() = runTest(TestMainActivity.dispatcher) {
        val activity = getTestMainActivity()

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(
            Deliverable.DeliverableEndType.GOAL.name,
            editGoalViewModel.deliverable.first()?.endType
        )

        assertNull(editGoalViewModel.deliverable.first()?.goalId)

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
            editGoalViewModel.deliverable.first()?.endType
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
            editGoalViewModel.deliverable.first()?.endType
        )

        // todo check if work views and values are gone
    }

    @Test
    fun `17 Add Time Block`() = runTest(TestMainActivity.dispatcher) {
        val activity = getTestMainActivity()

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(
            Deliverable.DeliverableEndType.UNDEFINED.name,
            editGoalViewModel.deliverable.first()?.endType
        )

        assertNotNull(editGoalViewModel.deliverable.first()?.times)

        TimeBlockTest(
            "TasksFragmentDeliverableAddTimeBlockButton",
            editGoalViewModel.deliverable.first()!!.times,
            Triple(
                instantTaskExecutorRule,
                composeTestRule,
                activity
            )
        ).runTests(this@EditGoalTests::class)
    }

    @Test
    fun `18 Save Deliverable`() = runTest(TestMainActivity.dispatcher) {
        val activity = getTestMainActivity()

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(
            Deliverable.DeliverableEndType.UNDEFINED.name,
            editGoalViewModel.deliverable.first()?.endType
        )

        assertNotNull(editGoalViewModel.deliverable.first()?.times)
        val savedDeliverable = editGoalViewModel.deliverable.first()!!

        withTag(
            "TasksFragmentDeliverableProcessDeliverable"
        ) {
            performPressWithoutScroll()
        }

        assertNull(editGoalViewModel.deliverable.first())

        withTag("TasksFragmentDeliverablesBottomSheet").assertDoesNotExist()

        withTag("EditGoalAddDeliverableButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertIsEnabled()
            assertHasClickAction()
        }

        withTag("EditGoalSelectDeliverableButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertIsEnabled()
            assertHasClickAction()
            assertTextContains("1",substring = true,ignoreCase = true)
        }

        val databaseDeliverableFlow = activity.viewModel.repository.getDeliverable(1L).first()
        assertNotNull(databaseDeliverableFlow)
        val databaseDeliverable = databaseDeliverableFlow!!

        deliverablesAreEqual(savedDeliverable,databaseDeliverable)
        assertEquals(1, savedDeliverable.times.first().size)
    }

    private fun deliverablesAreEqual(
        deliverableOne:Deliverable,
        deliverableTwo:Deliverable,
        idsEqual:Boolean = true
    ) = runTest(TestMainActivity.dispatcher) {
        val assertionFunction: suspend TestScope.(Any?, Any?)->Unit = if (idsEqual)
            {objectA, objectB -> assertEquals(objectA,objectB)}
        else {objectA, objectB -> assertNotEquals(objectA,objectB)}

        assertionFunction(
            deliverableOne.id,
            deliverableTwo.id
        )

        assertEquals(
            deliverableOne.initialDateTime,
            deliverableTwo.initialDateTime
        )

        assertEquals(
            deliverableOne.deliverable,
            deliverableTwo.deliverable
        )
        assertEquals(
            deliverableOne.location,
            deliverableTwo.location
        )
        assertEquals(
            deliverableOne.endDateTime,
            deliverableTwo.endDateTime
        )
        assertEquals(
            deliverableOne.status,
            deliverableTwo.status
        )
        assertEquals(
            deliverableOne.endType,
            deliverableTwo.endType
        )
        assertEquals(
            deliverableOne.parent,
            deliverableTwo.parent
        )
        assertEquals(
            deliverableOne.times.first().size,
            deliverableTwo.times.first().size
        )

        if (!idsEqual) {
            assertEquals(deliverableOne.id,deliverableTwo.cloneId)
        }

        val timesTwoList = deliverableTwo.times.first()
        deliverableOne.times.first().forEachIndexed { index, timeOne ->
            val timeTwo = timesTwoList[index]
            timesAreEqual(
                timeOne,
                timeTwo,
                idsEqual = idsEqual
            )
        }
    }

    private fun timesAreEqual(
        timeOne: GoalTaskDeliverableTime,
        timeTwo: GoalTaskDeliverableTime,
        idsEqual:Boolean = true
    ) = runTest(TestMainActivity.dispatcher) {
        val assertionFunction: suspend TestScope.(Any?, Any?)->Unit = if (idsEqual)
            {objectA, objectB -> assertEquals(objectA,objectB)}
        else {objectA, objectB -> assertNotEquals(objectA,objectB)}

        assertionFunction(timeOne.id, timeTwo.id)

        assertionFunction(timeOne.parent,timeTwo.parent)

        assertEquals(timeOne.type,timeTwo.type)

        assertEquals(timeOne.timeBlockType,timeTwo.timeBlockType)

        assertEquals(timeOne.start,timeTwo.start)

        assertEquals(timeOne.duration,timeTwo.duration)

        if (!idsEqual) {
            assertEquals(timeOne.id,timeTwo.cloneId)
        }
    }

    @Test
    fun `19 Select Deliverable Button`() = runTest(TestMainActivity.dispatcher) {
        val activity = getTestMainActivity()

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        var databaseDeliverableFlow = activity.viewModel.repository.getDeliverable(1L).first()
        assertNotNull(databaseDeliverableFlow)
        var databaseDeliverable = databaseDeliverableFlow!!
        finishProcesses()

        withTag("EditGoalSelectDeliverableButton") {
            performPressWithScroll()
        }

        withTag("EditGoalSelectDeliverableDialog") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("EditGoalPickDeliverableRow-${databaseDeliverable.id}") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("EditGoalPickDeliverableButton-${databaseDeliverable.id}") {
            performPressWithScroll()
        }

        assertNull(databaseDeliverable.goalId)

        databaseDeliverableFlow = activity.viewModel.repository.getDeliverable(1L).first()
        assertNotNull(databaseDeliverableFlow)
        databaseDeliverable = databaseDeliverableFlow!!

        assertNotNull(databaseDeliverable.goalId)
        assertEquals(
            0,
            editGoalViewModel.newGoal.first()?.notSelectedGoalDeliverables?.first()?.size
        )
        finishProcesses()

        withTag("EditGoalSelectDeliverableDialog") {
            assertDoesNotExist()
        }

        withTag("EditGoalSelectDeliverableButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertIsNotEnabled()
        }
    }

    @Test
    fun `20 Edit Deliverable`() = runTest(TestMainActivity.dispatcher) {
        val activity = getTestMainActivity()

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        val databaseDeliverableFlow = activity.viewModel.repository.getDeliverable(1L).first()
        assertNotNull(databaseDeliverableFlow)
        val databaseDeliverable = databaseDeliverableFlow!!

        assertEquals(
            Goal.GoalEndType.DELIVERABLE.name,
            editGoalViewModel.newGoal.first()?.endType
        )

        finishProcesses()
        withTag("EditGoalLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(1L)
        }

        withTag("TasksFragmentDeliverableCard-${databaseDeliverable.id}"){
            performPressWithScroll()
        }

        assertNotNull(editGoalViewModel.originalDeliverable.first())
        assertNotNull(editGoalViewModel.deliverable.first())

        deliverablesAreEqual(
            editGoalViewModel.originalDeliverable.first()!!,
            editGoalViewModel.deliverable.first()!!,
            false
        )

        assertNotNull(editGoalViewModel.originalDeliverable.first()?.times?.first()[0])

        val oldDeliverable = editGoalViewModel.originalDeliverable.first()!!

        withTag(
            "TaskFragmentDeliverableDeliverableText"
        ) {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains("My Deliverable")
            performTextReplacement("My Deliverable (Edited)")
            assertTextContains("My Deliverable (Edited)")
        }
        finishProcesses()

        assertEquals(
            "My Deliverable (Edited)",
            editGoalViewModel.deliverable.first()?.deliverable
        )

        assertEquals(
            "My Location",
            editGoalViewModel.deliverable.first()?.location
        )

        withTag(
            "TaskFragmentDeliverableLocationText"
        ) {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains("My Location")
            performTextReplacement("My Location (Edited)")
            assertTextContains("My Location (Edited)")
        }
        finishProcesses()

        assertEquals(
            "My Location (Edited)",
            editGoalViewModel.deliverable.first()?.location
        )

        withTag(
            "TasksFragmentDeliverableProcessDeliverable"
        ) {
            performPressWithoutScroll()
        }

        assertNull(editGoalViewModel.originalDeliverable.first())
        assertNull(editGoalViewModel.deliverable.first())

        val newDeliverable = editGoalViewModel.newGoal.first()?.goalDeliverables?.first()[0]
        assertNotNull(newDeliverable)

        assertNotEquals(oldDeliverable.deliverable,newDeliverable?.deliverable)

        assertNotEquals(oldDeliverable.location,newDeliverable?.location)
    }

    @Test
    fun `21 Edit Deliverable Time`() = runTest(TestMainActivity.dispatcher) {
        val activity = getTestMainActivity()

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        var databaseDeliverableFlow = activity.viewModel.repository.getDeliverable(1L).first()
        assertNotNull(databaseDeliverableFlow)
        var databaseDeliverable = databaseDeliverableFlow!!

        assertEquals(
            Goal.GoalEndType.DELIVERABLE.name,
            editGoalViewModel.newGoal.first()?.endType
        )

        finishProcesses()
        withTag("EditGoalLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(1L)
        }

        withTag("TasksFragmentDeliverableCard-${databaseDeliverable.id}"){
            performPressWithScroll()
        }

        assertNotNull(editGoalViewModel.originalDeliverable.first())
        assertNotNull(editGoalViewModel.deliverable.first())

        deliverablesAreEqual(
            editGoalViewModel.originalDeliverable.first()!!,
            editGoalViewModel.deliverable.first()!!,
            false
        )

        assertNotNull(editGoalViewModel.deliverable.first()?.times?.first()[0])

        val oldTime = editGoalViewModel.deliverable.first()!!.times.first()[0]
        assertNotNull(oldTime.id)

        finishProcesses()
        withTag("EditGoalTimeInputViewCard-${oldTime.id}"){
            assertExists()
            performScrollTo()
            assertIsDisplayed()
        }

        activity.timeToAdd(-1,0)
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

        val timeBlockList = editGoalViewModel.deliverable.first()!!.times
        assertNotNull(
            Converters().toLocalDateTime(
                timeBlockList.first()[0].start
            ).value
        )

        assertEquals(
            Converters().toLocalDateTime(oldTime.start).value.hour,
            Converters().toLocalDateTime(
                timeBlockList.first()[0].start
            ).value.hour
        )
        assertNotEquals(
            Converters().toLocalDateTime(oldTime.start).value.minute,
            Converters().toLocalDateTime(
                timeBlockList.first()[0].start
            ).value.minute
        )
        activity.timeToAdd(0,0)

        withTag(
            "TasksFragmentDeliverableProcessDeliverable"
        ) {
            performPressWithoutScroll()
        }

        assertNull(editGoalViewModel.originalDeliverable.first())
        assertNull(editGoalViewModel.deliverable.first())

        databaseDeliverableFlow = activity.viewModel.repository.getDeliverable(1L).first()
        assertNotNull(databaseDeliverableFlow)
        databaseDeliverable = databaseDeliverableFlow!!
        val newTime = databaseDeliverable.times.first()[0]
        assertNotNull(newTime)

        assertNotEquals(
            Converters().toLocalDateTime(oldTime.start).value.minute,
            Converters().toLocalDateTime(newTime.start).value.minute
        )
    }

    @Test
    fun `22 Remove Selected Deliverable By Editing Deliverable`() = runTest(TestMainActivity.dispatcher) {
        val activity = getTestMainActivity()

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        var databaseDeliverableFlow = activity.viewModel.repository.getDeliverable(1L).first()
        assertNotNull(databaseDeliverableFlow)
        var databaseDeliverable = databaseDeliverableFlow!!

        assertEquals(
            Goal.GoalEndType.DELIVERABLE.name,
            editGoalViewModel.newGoal.first()?.endType
        )

        finishProcesses()
        withTag("EditGoalLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(1L)
        }

        withTag("TasksFragmentDeliverableCard-${databaseDeliverable.id}"){
            performPressWithScroll()
        }

        assertNotNull(editGoalViewModel.originalDeliverable.first())
        assertNotNull(editGoalViewModel.deliverable.first())

        deliverablesAreEqual(
            editGoalViewModel.originalDeliverable.first()!!,
            editGoalViewModel.deliverable.first()!!,
            false
        )

        assertNotNull(editGoalViewModel.deliverable.first()!!.goalId)

        withTag("TasksFragmentDeliverableSwitch"){
            performPressWithScroll()
        }

        assertNull(editGoalViewModel.deliverable.first()!!.goalId)

        withTag(
            "TasksFragmentDeliverableProcessDeliverable"
        ) {
            performPressWithoutScroll()
        }

        assertNull(editGoalViewModel.originalDeliverable.first())
        assertNull(editGoalViewModel.deliverable.first())

        databaseDeliverableFlow = activity.viewModel.repository.getDeliverable(1L).first()
        assertNotNull(databaseDeliverableFlow)
        databaseDeliverable = databaseDeliverableFlow!!
        assertNull(databaseDeliverable.goalId)

        assertEquals(
            0,
            editGoalViewModel.newGoal.first()?.selectedGoalDeliverables?.first()?.size
        )
    }

    @Test
    fun `23 Remove Deliverable By Clicking Button`() = runTest(TestMainActivity.dispatcher) {
        val activity = getTestMainActivity()

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        var databaseDeliverableFlow = activity.viewModel.repository.getDeliverable(1L).first()
        assertNotNull(databaseDeliverableFlow)
        var databaseDeliverable = databaseDeliverableFlow!!

        finishProcesses()
        withTag("EditGoalSelectDeliverableButton") {
            performPressWithScroll()
        }

        withTag("EditGoalSelectDeliverableDialog") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("EditGoalPickDeliverableRow-${databaseDeliverable.id}") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("EditGoalPickDeliverableButton-${databaseDeliverable.id}") {
            performPressWithScroll()
        }

        assertNull(databaseDeliverable.goalId)

        databaseDeliverableFlow = activity.viewModel.repository.getDeliverable(1L).first()
        assertNotNull(databaseDeliverableFlow)
        databaseDeliverable = databaseDeliverableFlow!!

        assertNotNull(databaseDeliverable.goalId)

        withTag("EditGoalSelectDeliverableDialog") {
            assertDoesNotExist()
        }

        withTag("EditGoalSelectDeliverableButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertIsNotEnabled()
        }

        assertNotNull(editGoalViewModel.newGoal.first()?.id)

        databaseDeliverable = activity.viewModel.repository.getDeliverables(
            editGoalViewModel.newGoal.first()!!.id!!
        ).first()[0]
        assertNotNull(databaseDeliverable.id)

        withTag("EditGoalLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(databaseDeliverable.id!!)
        }

        withTag("EditGoalUnpickDeliverableButton-${databaseDeliverable.id}"){
            performPressWithScroll()
        }

        assertEquals(
            0,
            editGoalViewModel.newGoal.first()?.selectedGoalDeliverables?.first()?.size
        )
    }

    @Test
    fun `24 Add Goal Time Block`() = runTest(TestMainActivity.dispatcher) {
        val activity = getTestMainActivity()
        finishProcesses()

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertNotNull(editGoalViewModel.newGoal.first()?.times)

        withTag("EditGoalLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("EditGoalFragmentStickyAddGoalTimeBlockButton")
        }

        TimeBlockTest(
            "EditGoalAddTimeBlockButton",
            editGoalViewModel.newGoal.first()!!.times,
            Triple(
                instantTaskExecutorRule,
                composeTestRule,
                activity
            ),
            lazyColumnTag = "EditGoalLazyColumn"
        ).runTests(this@EditGoalTests::class)
    }
}