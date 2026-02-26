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
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.thando.accountable.AccountableComposeRobolectricTest
import com.thando.accountable.AccountableComposeRobolectricTest.TestMainActivity.Companion.addTime
import com.thando.accountable.AccountableNavigationController.AccountableFragment
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
    fun `01 coreClassesNotNull`() = runMainTest {
        val activity = getTestMainActivity()
        assertNotNull(activity)
        assertNotNull(activity.viewModel)
        assertNotNull(activity.viewModel.repository)
    }

    @Test
    fun `02 currentFragmentIsHomeFragment`() = runMainTest {
        val activity = getTestMainActivity()
        assertNotNull(activity.viewModel.currentFragment.value)
        assertEquals(
            AccountableFragment.HomeFragment,
            activity.viewModel.currentFragment.value
        )
    }

    @Test
    fun `03 Can Go To Goals`() = runMainTest {
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
            activity,
            AccountableFragment.GoalsFragment,
            BooksViewModel::class.java.name
        )
    }

    @Test
    fun `04 Is In AppSettings Base, Set To Goals And Is Empty`() = runMainTest {
        val activity = getTestMainActivity()
        checkFragmentIs(
            activity,
            AccountableFragment.GoalsFragment,
            BooksViewModel::class.java.name
        )
        val booksViewModel: BooksViewModel = getViewModel(activity)
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
    fun `05 Open Edit Goal And Check Views`() = runMainTest {
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
    fun `06 Input Goal`() = runMainTest {
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
    fun `07 Input Location`() = runMainTest {
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
    fun `08 Input Image`() = runMainTest {
        val activity = getTestMainActivity()
        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertNull(editGoalViewModel.newGoal.first()?.getGoalPicture())

        assertNull(editGoalViewModel.newGoal.first()?.getImageBitmap(activity)?.first())

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
        assertNotNull(editGoalViewModel.newGoal.first()?.getImageBitmap(activity)?.first())

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
        assertNull(editGoalViewModel.newGoal.first()?.getImageBitmap(activity)?.first())

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
    fun `09 Colour Input`() = runMainTest {
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
    fun `10 End Type Undefined`() = runMainTest {
        val activity = getTestMainActivity()
        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(
            Goal.GoalEndType.UNDEFINED.name,
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
    fun `11 End Type Date`() = runMainTest {
        val activity = getTestMainActivity()
        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(
            Goal.GoalEndType.UNDEFINED.name,
            editGoalViewModel.newGoal.first()?.endType
        )

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
    fun `12 End Type Deliverable`() = runMainTest {
        val activity = getTestMainActivity()
        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(
            Goal.GoalEndType.DATE.name,
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
    fun `13 End Type Deliverable End Type Undefined`() = runMainTest {
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
    fun `14 End Type Deliverable End Type Date`() = runMainTest {
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
    fun `15 End Type Deliverable End Type Goal`() = runMainTest {
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
     *  I think this can wait until you finish the task input then you can come back
     *  (this is tested in TasksFragment)
     */

    @Test
    fun `16 End Type Deliverable End Type Work`() = runMainTest {
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
    fun `17 Add Time Block`() = runMainTest {
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
    fun `18 Save Deliverable`() = runMainTest {
        val activity = getTestMainActivity()

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(
            Deliverable.DeliverableEndType.UNDEFINED.name,
            editGoalViewModel.deliverable.first()?.endType
        )

        assertNotNull(editGoalViewModel.deliverable.first()?.times)
        assertNotNull(editGoalViewModel.deliverable.first()?.times?.first())
        val savedDeliverable = editGoalViewModel.deliverable.first()!!
        val savedTimes = editGoalViewModel.deliverable.first()!!.times.first()

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

        deliverablesAreEqual(
            savedDeliverable,
            databaseDeliverable,
            times = savedTimes
        )
        assertEquals(1, savedDeliverable.times.first().size)
    }

    @Test
    fun `19 Select Deliverable Button`() = runMainTest {
        val activity = getTestMainActivity()

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        var databaseDeliverableFlow = activity.viewModel.repository.getDeliverable(1L).first()
        assertNotNull(databaseDeliverableFlow)
        var databaseDeliverable = databaseDeliverableFlow!!

        withTag("EditGoalSelectDeliverableButton") {
            performPressWithScroll()
        }

        withTag("EditGoalSelectDeliverableDialog") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("EditGoalPickDeliverableRow-${databaseDeliverable.deliverableId}") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("EditGoalPickDeliverableButton-${databaseDeliverable.deliverableId}") {
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
    fun `20 Edit Deliverable`() = runMainTest {
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

        withTag("EditGoalLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(1L)
        }

        withTag("TasksFragmentDeliverableCard-${databaseDeliverable.deliverableId}"){
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
    fun `21 Edit Deliverable Time`() = runMainTest {
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

        withTag("EditGoalLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(1L)
        }

        withTag("TasksFragmentDeliverableCard-${databaseDeliverable.deliverableId}"){
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
    fun `22 Remove Selected Deliverable By Editing Deliverable`() = runMainTest {
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

        withTag("EditGoalLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(1L)
        }

        withTag("TasksFragmentDeliverableCard-${databaseDeliverable.deliverableId}"){
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
    fun `23 Remove Deliverable By Clicking Button`() = runMainTest {
        val activity = getTestMainActivity()

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        var databaseDeliverableFlow = activity.viewModel.repository.getDeliverable(1L).first()
        assertNotNull(databaseDeliverableFlow)
        var databaseDeliverable = databaseDeliverableFlow!!

        withTag("EditGoalSelectDeliverableButton") {
            performPressWithScroll()
        }

        withTag("EditGoalSelectDeliverableDialog") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("EditGoalPickDeliverableRow-${databaseDeliverable.deliverableId}") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("EditGoalPickDeliverableButton-${databaseDeliverable.deliverableId}") {
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
        assertNotNull(databaseDeliverable.deliverableId)

        withTag("EditGoalLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(databaseDeliverable.deliverableId!!)
        }

        withTag("EditGoalUnpickDeliverableButton-${databaseDeliverable.deliverableId}"){
            performPressWithScroll()
        }

        assertEquals(
            0,
            editGoalViewModel.newGoal.first()?.selectedGoalDeliverables?.first()?.size
        )
    }

    @Test
    fun `24 Add Goal Time Block`() = runMainTest {
        val activity = getTestMainActivity()

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

    private fun goalsAreEqual(
        goalOne:Goal,
        goalTwo:Goal,
        idsEqual:Boolean = true,
        timesAndDeliverables:Pair<List<GoalTaskDeliverableTime>, List<Deliverable>>? = null
    ) = runMainTest {
        val assertionFunction: suspend TestScope.(Any?, Any?)->Unit = if (idsEqual)
            {objectA, objectB -> assertEquals(objectA,objectB)}
        else {objectA, objectB -> assertNotEquals(objectA,objectB)}

        assertionFunction(
            goalOne.id,
            goalTwo.id
        )

        assertEquals(
            goalOne.parent,
            goalTwo.parent
        )

        assertEquals(
            goalOne.initialDateTime,
            goalTwo.initialDateTime
        )
        assertEquals(
            goalOne.position,
            goalTwo.position
        )
        assertEquals(
            goalOne.scrollPosition,
            goalTwo.scrollPosition
        )
        assertEquals(
            goalOne.goal,
            goalTwo.goal
        )
        assertEquals(
            goalOne.dateOfCompletion,
            goalTwo.dateOfCompletion
        )
        assertEquals(
            goalOne.endDateTime,
            goalTwo.endDateTime
        )
        assertEquals(
            goalOne.endType,
            goalTwo.endType
        )
        assertionFunction(
            goalOne.getGoalPicture(),
            goalTwo.getGoalPicture()
        )
        assertEquals(
            goalOne.status,
            goalTwo.status
        )
        assertEquals(
            goalOne.colour,
            goalTwo.colour
        )
        assertEquals(
            goalOne.location,
            goalTwo.location
        )
        assertEquals(
            goalOne.selectedTab,
            goalTwo.selectedTab
        )
        assertEquals(
            goalOne.tabListState,
            goalTwo.tabListState
        )

        assertEquals(
            (timesAndDeliverables?.first?:goalOne.times.first()).size,
            goalTwo.times.first().size
        )

        assertEquals(
            (timesAndDeliverables?.second?:goalOne.goalDeliverables.first()).size,
            goalTwo.goalDeliverables.first().size
        )

        if (!idsEqual && timesAndDeliverables == null) {
            assertEquals(goalOne.id, goalTwo.cloneId)
        }

        val deliverablesTwoList = goalTwo.goalDeliverables.first()
        (timesAndDeliverables?.second?:goalOne.goalDeliverables.first()).forEachIndexed { index, deliverableOne ->
            val deliverableTwo = deliverablesTwoList[index]
            deliverablesAreEqual(
                deliverableOne,
                deliverableTwo,
                idsEqual = idsEqual,
                parentsEqual = idsEqual
            )
        }

        val timesTwoList = goalTwo.times.first()
        (timesAndDeliverables?.first?:goalOne.times.first()).forEachIndexed { index, timeOne ->
            val timeTwo = timesTwoList[index]
            timesAreEqual(
                timeOne,
                timeTwo,
                idsEqual = idsEqual,
                parentsEqual = idsEqual
            )
        }
    }

    @Test
    fun `25 Save Goal`() = runMainTest {
        val activity = getTestMainActivity()

        val editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        val databaseDeliverableFlow = activity.viewModel.repository.getDeliverable(1L).first()
        assertNotNull(databaseDeliverableFlow)
        val databaseDeliverable = databaseDeliverableFlow!!

        withTag("EditGoalSelectDeliverableButton") {
            performPressWithScroll()
        }

        withTag("EditGoalSelectDeliverableDialog") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("EditGoalPickDeliverableRow-${databaseDeliverable.deliverableId}") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("EditGoalPickDeliverableButton-${databaseDeliverable.deliverableId}") {
            performPressWithScroll()
        }

        val goal = editGoalViewModel.newGoal.first()
        val deliverables = goal?.goalDeliverables?.first()
        val times = goal?.times?.first()

        withTag("EditGoalSaveAndCloseIconButton") {
            performPressWithoutScroll()
        }

        assertEquals(
            AccountableFragment.GoalsFragment,
            activity.viewModel.currentFragment.value
        )
        assertEquals(
            BooksViewModel::class.java.name,
            activity.viewModel.accountableNavigationController.fragmentViewModel.value?.javaClass?.name
        )
        val booksViewModel: BooksViewModel = getViewModel(activity)
        assertNull(booksViewModel.folder.value)
        assertNotNull(booksViewModel.appSettings.value)
        assertNotNull(booksViewModel.showScripts.first())

        assertTrue(booksViewModel.showScripts.first())
        assertTrue(booksViewModel.goalsList.first().isNotEmpty())
        assertTrue(booksViewModel.foldersList.first().isEmpty())
        assertTrue(booksViewModel.scriptsList.first().isEmpty())

        assertEquals(1, booksViewModel.goalsList.first().size)

        assertNotNull(goal)
        assertNotNull(times)
        assertNotNull(deliverables)

        goalsAreEqual(
            goal!!,
            booksViewModel.goalsList.first()[0],
            false,
            Pair(times!!,deliverables!!)
        )
    }

    @Test
    fun `26 Edit Goal`() = runMainTest {
        val activity = getTestMainActivity()
        var booksViewModel: BooksViewModel = getViewModel(activity)
        assertTrue(booksViewModel.goalsList.first().isNotEmpty())
        assertNotNull(booksViewModel.goalsList.first()[0].id)
        val goal = activity.viewModel.repository.getGoal(booksViewModel.goalsList.first()[0].id).first()
        assertNotNull(goal)
        withTag("BooksFragmentGoalCard-${goal!!.id}"){
            performLongPressWithoutScroll()
        }

        withTag("BooksFragmentBottomSheetEditButton") {
            performPressWithoutScroll()
        }

        var editGoalViewModel: EditGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNotNull(editGoalViewModel.editGoal.value)
        assertNotNull(editGoalViewModel.newGoal.first())

        goalsAreEqual(goal,editGoalViewModel.editGoal.value!!)
        goalsAreEqual(
            editGoalViewModel.editGoal.value!!,
            editGoalViewModel.newGoal.first()!!,
            idsEqual = false
        )

        withTag("EditGoalSaveAndCloseIconButton") {
            performPressWithoutScroll()
        }

        booksViewModel = getViewModel(activity)

        assertEquals(1, booksViewModel.goalsList.first().size)
        assertNotNull(booksViewModel.goalsList.first()[0].id)
        assertNotNull(activity.viewModel.repository.getGoal(booksViewModel.goalsList.first()[0].id).first())
        goalsAreEqual(
            goal,
            activity.viewModel.repository.getGoal(booksViewModel.goalsList.first()[0].id).first()!!
        )

        withTag("BooksFragmentGoalCard-${goal.id}"){
            performLongPressWithoutScroll()
        }

        withTag("BooksFragmentBottomSheetEditButton") {
            performPressWithoutScroll()
        }

        withTag("EditGoalCloseGoalButton") {
            performPressWithoutScroll()
        }

        booksViewModel = getViewModel(activity)

        assertEquals(1, booksViewModel.goalsList.first().size)
        assertNotNull(booksViewModel.goalsList.first()[0].id)
        assertNotNull(activity.viewModel.repository.getGoal(booksViewModel.goalsList.first()[0].id).first())
        goalsAreEqual(
            goal,
            activity.viewModel.repository.getGoal(booksViewModel.goalsList.first()[0].id).first()!!
        )

        // Changing the values now
        withTag("BooksFragmentGoalCard-${goal.id}"){
            performLongPressWithoutScroll()
        }

        withTag("BooksFragmentBottomSheetEditButton") {
            performPressWithoutScroll()
        }

        editGoalViewModel = getViewModel(activity)
        assertNotNull(editGoalViewModel)
        assertNotNull(editGoalViewModel.editGoal.value)
        assertNotNull(editGoalViewModel.newGoal.first())

        checkFragmentIs(
            activity,
            AccountableFragment.EditGoalFragment,
            EditGoalViewModel::class.java.name
        )

        withTag("EditGoalTitle") {
            assertExists()
            assertIsDisplayed()
            assertTextEquals(activity.getString(R.string.edit_goal))
        }

        withTag("EditGoalLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToIndex(0)
        }

        withTag("EditGoalGoal") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains("My New Goal")
            performTextReplacement("Edited Goal")
            assertTextContains("Edited Goal")
        }
        finishProcesses()

        withTag("EditGoalLocation") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains("My New Location")
            performTextReplacement("Edited Location")
            assertTextContains("Edited Location")
        }
        finishProcesses()

        withTag("EditGoalSaveAndCloseIconButton") {
            performPressWithoutScroll()
        }

        booksViewModel = getViewModel(activity)

        assertEquals(1, booksViewModel.goalsList.first().size)
        assertNotNull(booksViewModel.goalsList.first()[0].id)
        assertNotNull(activity.viewModel.repository.getGoal(booksViewModel.goalsList.first()[0].id).first())
        val editedGoal = activity.viewModel.repository.getGoal(booksViewModel.goalsList.first()[0].id).first()!!
        assertNotEquals(
            goal.goal,editedGoal.goal
        )
        assertNotEquals(
            goal.location,editedGoal.goal
        )
    }
}