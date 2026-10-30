package com.thando.accountable.fragments

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.thando.accountable.AccountableComposeRobolectricTest
import com.thando.accountable.AccountableNavigationController.AccountableFragment
import com.thando.accountable.AppResources.Companion.getStandardDate
import com.thando.accountable.AppResources.Companion.getTime
import com.thando.accountable.R
import com.thando.accountable.database.Converters
import com.thando.accountable.database.tables.Deliverable
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.Task
import com.thando.accountable.fragments.viewmodels.BooksViewModel
import com.thando.accountable.fragments.viewmodels.EditGoalViewModel
import com.thando.accountable.fragments.viewmodels.HomeViewModel
import com.thando.accountable.fragments.viewmodels.TaskViewModel
import com.thando.accountable.input_forms.DateAndTimePickerTest
import com.thando.accountable.input_forms.TimeBlockTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class TasksFragmentGoalNotDeliverableEndTypeTests: AccountableComposeRobolectricTest() {
    @Test
    fun `01 Create Goal And Open Tasks`() = runMainTest{
        val activity = getTestMainActivity()
        assertNotNull(activity)
        assertNotNull(activity.viewModel)
        assertNotNull(activity.viewModel.repository)
        assertNotNull(activity.viewModel.currentFragment.value)
        checkFragmentIs(
            activity,
            AccountableFragment.HomeFragment,
            HomeViewModel::class.java.name
        )
        withTag("HomeFragmentLoadGoalsButton") {
            performPressWithoutScroll()
        }
        checkFragmentIs(
            activity,
            AccountableFragment.GoalsFragment,
            BooksViewModel::class.java.name
        )

        var booksViewModel: BooksViewModel = getViewModel(activity)
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

        withTag("EditGoalColourDisplayBox") {
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

        // Set end type to task
        withTag("EditGoalEndTypeButton") {
            performPressWithScroll()
        }

        withTag("EditGoalEndTypeDropDownMenu") {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            "EditGoalDropdownMenuItem-${Goal.GoalEndType.TASK.name}"
        ) {
            performPressWithoutScroll()
        }

        assertEquals(
            Goal.GoalEndType.TASK.name,
            editGoalViewModel.newGoal.first()?.endType
        )

        TimeBlockTest(
            "EditGoalAddTimeBlockButton",
            {editGoalViewModel.newGoal.first()!!.times.first()},
            Triple(
                instantTaskExecutorRule,
                composeTestRule,
                activity
            ),
            lazyColumnTag = "EditGoalLazyColumn"
        ).runTests(this@TasksFragmentGoalNotDeliverableEndTypeTests::class)
        finishProcesses()

        var goal = editGoalViewModel.newGoal.first()
        val tasks = goal?.goalTasks?.first()
        val deliverables = goal?.goalDeliverables?.first()
        val markers = goal?.goalMarkers?.first()
        val times = goal?.times?.first()

        assertNotNull(goal)
        assertNotNull(tasks)
        assertNotNull(deliverables)
        assertNotNull(markers)
        assertNotNull(times)
        assertTrue(tasks!!.isEmpty())
        assertTrue(deliverables!!.isEmpty())
        assertTrue(markers!!.isEmpty())
        assertTrue(times!!.isNotEmpty())

        withTag("EditGoalSaveAndCloseIconButton") {
            performPressWithoutScroll()
        }

        checkFragmentIs(
            activity,
            AccountableFragment.GoalsFragment,
            BooksViewModel::class.java.name
        )

        booksViewModel = getViewModel(activity)
        assertTrue(booksViewModel.goalsList.first().isNotEmpty())
        assertNotNull(booksViewModel.goalsList.first()[0].id)
        goal = activity.viewModel.repository.getGoal(booksViewModel.goalsList.first()[0].id).first()
        assertNotNull(goal)
        withTag("BooksFragmentGoalCard-${goal!!.id}"){
            performPressWithScroll()
        }

        checkFragmentIs(
            activity,
            AccountableFragment.TaskFragment,
            TaskViewModel::class.java.name
        )
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
     */

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `02 Adding Quantity Goal End Type Deliverable`() = runMainTest {
        val activity = getTestMainActivity()
        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        var goal = taskViewModel.goal.first()!!

        assertEquals(Goal.GoalTab.TASKS,Goal.GoalTab.valueOf(goal.selectedTab))

        switchToTab(Goal.GoalTab.DELIVERABLES, taskViewModel, activity)
        goal = taskViewModel.goal.first()!!
        var deliverablesList = goal.goalDeliverables.first()

        assertEquals(Goal.GoalTab.DELIVERABLES,Goal.GoalTab.valueOf(goal.selectedTab))

        assertEquals(0,goal.goalTasks.first().size)
        assertEquals(0, deliverablesList.size)

        withTag("TasksFragmentAddTextButton"){
            assertExists()
            assertTextContains(activity.getString(Goal.GoalTab.DELIVERABLES.addStringRes))
            performPressWithoutScroll()
        }

        assertEquals(Goal.GoalTab.DELIVERABLES,taskViewModel.bottomSheetType.value)

        withTag("TasksFragmentAddDeliverableView") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("TasksFragmentAddDeliverableViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddDeliverableNQTButtons")
        }

        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()

        assertEquals(1,deliverablesList.size)
        assertEquals(Task.TaskType.NORMAL.name, deliverablesList.first().deliverable.type)

        testTaskTypeButtons(
            Task.TaskType.NORMAL,
            selected = true,
            press = false,
            activity
        )

        testTaskTypeButtons(
            Task.TaskType.QUANTITY,
            selected = false,
            press = true,
            activity
        )

        withTag("DeliverableWarningDialog") {
            assertDoesNotExist()
        }

        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()

        assertEquals(1,deliverablesList.size)
        assertEquals(Task.TaskType.QUANTITY.name, deliverablesList.first().deliverable.type)


        testTaskTypeButtons(
            Task.TaskType.QUANTITY,
            selected = true,
            press = false,
            activity
        )

        testTaskTypeButtons(
            Task.TaskType.NORMAL,
            selected = false,
            press = false,
            activity
        )

        testTaskTypeButtons(
            Task.TaskType.TIME,
            selected = false,
            press = false,
            activity
        )

        assertEquals(0L,deliverablesList.first().deliverable.quantity)
        withTag("TasksFragmentAddDeliverableNormalQuantityTimeTextField") {
            assertExists()
            assertIsDisplayed()
            performScrollTo()
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("Jump 2 times33")
            assertTextContains(
                value = "Jump 2 times",
                substring = true,
                ignoreCase = false
            )
        }
        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()
        assertEquals(2L,deliverablesList.first().deliverable.quantity)

        withTag("TaskFragmentDeliverableLocationText") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("At Home")
            assertTextContains(
                value = "At Home",
                substring = true,
                ignoreCase = false
            )
        }

        withTag("TasksFragmentAddDeliverableViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentDeliverableEndTypeButton")
        }

        withTag("TasksFragmentDeliverableEndTypeButton") {
            performPressWithScroll()
        }

        withTag("TasksFragmentDeliverableEndTypeDropDownMenu") {
            assertExists()
            assertIsDisplayed()
        }

        goal = taskViewModel.goal.first()!!
        assertEquals(
            Goal.GoalEndType.TASK.name,
            goal.endType
        )

        withTag(
            "TasksFragmentDeliverableDropdownMenuItem-${Deliverable.DeliverableEndType.UNDEFINED.name}"
        ) {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
        }

        withTag(
            "TasksFragmentDeliverableDropdownMenuItem-${Deliverable.DeliverableEndType.DATE.name}"
        ) {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
        }

        withTag(
            "TasksFragmentDeliverableDropdownMenuItem-${Deliverable.DeliverableEndType.WORK.name}"
        ) {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
        }

        withTag(
            "TasksFragmentDeliverableDropdownMenuItem-${Deliverable.DeliverableEndType.GOAL.name}"
        ) {
            assertExists()
            assertIsDisplayed()
            performPressWithoutScroll()
        }

        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()

        assertEquals(1,deliverablesList.size)

        assertEquals(
            "Jump 2 times",
            deliverablesList[0].deliverable.deliverable
        )

        assertEquals(
            "At Home",
            deliverablesList[0].deliverable.location
        )

        assertEquals(
            Deliverable.DeliverableEndType.GOAL.name,
            deliverablesList[0].deliverable.endType
        )

        withTag("TasksFragmentDeliverableEndTypeButton") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            assertTextContains(
                value = "End Type: Goal",
                substring = true,
                ignoreCase = false
            )
        }

        assertNull(deliverablesList[0].deliverable.goalId)

        withTag("TasksFragmentAddDeliverableViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentDeliverableAddTimeBlockButton")
        }

        withTag("TasksFragmentDeliverableSwitch") {
            assertDoesNotExist()
        }

        withTag("TasksFragmentAddDeliverableViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddDeliverableNormalQuantityTimeTextField")
        }

        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()
        assertEquals(2L,deliverablesList.first().deliverable.quantity)
        withTag("TasksFragmentAddDeliverableNormalQuantityTimeTextField") {
            assertExists()
            assertIsDisplayed()
            performScrollTo()
            assertTextContains(
                value = "Jump 2 times",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("Jump a lot of times")
            assertTextContains(
                value = "Jump a lot of times",
                substring = true,
                ignoreCase = false
            )
        }
        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()
        assertEquals(0L,deliverablesList.first().deliverable.quantity)

        withTag("TasksFragmentDeliverableProcessDeliverable") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            performPressWithoutScroll()
        }

        testErrorMessageView(
            errorMessage = "Please enter a value for deliverable.",
            errorMessageTextTag = "TasksFragmentAddDeliverableErrorMessageText",
            errorMessageBoxTag = "TasksFragmentAddDeliverableErrorMessageBox"
        )

        withTag("TasksFragmentAddDeliverableView") {
            assertExists()
            assertIsDisplayed()
        }

        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()
        assertEquals(0L,deliverablesList.first().deliverable.quantity)
        withTag("TasksFragmentAddDeliverableNormalQuantityTimeTextField") {
            assertExists()
            assertIsDisplayed()
            performScrollTo()
            assertTextContains(
                value = "Jump a lot of times",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("")
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
        }
        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()
        assertEquals(0L,deliverablesList.first().deliverable.quantity)

        withTag("TasksFragmentDeliverableProcessDeliverable") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            performPressWithoutScroll()
        }

        testErrorMessageView(
            errorMessage = "Please Enter A Deliverable",
            errorMessageTextTag = "TasksFragmentAddDeliverableErrorMessageText",
            errorMessageBoxTag = "TasksFragmentAddDeliverableErrorMessageBox"
        )

        withTag("TasksFragmentAddDeliverableView") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("TasksFragmentAddDeliverableNormalQuantityTimeTextField") {
            assertExists()
            assertIsDisplayed()
            performScrollTo()
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("Jump 2 times33")
            assertTextContains(
                value = "Jump 2 times",
                substring = true,
                ignoreCase = false
            )
        }
        finishProcesses()
        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()
        assertEquals(2L,deliverablesList.first().deliverable.quantity)
        assertEquals("Jump 2 times", deliverablesList.first().deliverable.deliverable)

        withTag("TasksFragmentAddDeliverableErrorMessageBox") {
            assertDoesNotExist()
        }

        withTag("TaskFragmentDeliverableLocationText") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = "At Home",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("")
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
        }

        withTag("TasksFragmentDeliverableProcessDeliverable") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            performPressWithoutScroll()
        }
        finishProcesses()

        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()
        assertEquals(2L,deliverablesList.first().deliverable.quantity)
        assertTrue(deliverablesList.first().deliverable.deliverable.isNotEmpty())

        testErrorMessageView(
            errorMessage = "Please enter a location.",
            errorMessageTextTag = "TasksFragmentAddDeliverableErrorMessageText",
            errorMessageBoxTag = "TasksFragmentAddDeliverableErrorMessageBox"
        )

        withTag("TasksFragmentAddDeliverableView") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("TaskFragmentDeliverableLocationText") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("At Home")
            assertTextContains(
                value = "At Home",
                substring = true,
                ignoreCase = false
            )
        }

        TimeBlockTest(
            "TasksFragmentDeliverableAddTimeBlockButton",
            { taskViewModel.goal.first()?.goalDeliverables?.first()[0]?.times },
            Triple(
                instantTaskExecutorRule,
                composeTestRule,
                activity
            ),
            lazyColumnTag = "TasksFragmentAddDeliverableViewLazyColumn"
        ).runTests(this@TasksFragmentGoalNotDeliverableEndTypeTests::class)

        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()
        assertEquals(1,deliverablesList[0].times.size)

        withTag("EditGoalTimeInputPickDurationButton"){
            performPressWithScroll()
        }

        withTag("EditGoalTimeInputDurationPickerDialog"){
            assertExists()
            assertIsDisplayed()
        }

        withTag("EditGoalDurationPickerHourSlider"){
            assertExists()
            assertIsDisplayed()
            performSemanticsAction(SemanticsActions.SetProgress) { action ->
                action((0).toFloat())
            }
        }
        finishProcesses()

        withTag("EditGoalDurationPickerMinuteSlider"){
            assertExists()
            assertIsDisplayed()
            performSemanticsAction(SemanticsActions.SetProgress) { action ->
                action((0).toFloat())
            }
        }

        withTag("EditGoalDurationPickerSaveAndCloseIconButton") {
            assertExists()
            assertIsDisplayed()
            performPressWithoutScroll()
        }

        withTag("EditGoalTimeInputDurationPickerDialog"){
            assertDoesNotExist()
        }
        finishProcesses()

        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()
        assertEquals(1,deliverablesList[0].times.size)
        var time = deliverablesList[0].times[0]
        var duration = Converters().toLocalDateTime(time.duration).value
        assertEquals(0,duration.hour)
        assertEquals(0,duration.minute)

        withTag("TasksFragmentDeliverableProcessDeliverable") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            performPressWithoutScroll()
        }
        finishProcesses()

        testErrorMessageView(
            errorMessage = "Please Select A Duration",
            errorMessageTextTag = "TasksFragmentAddDeliverableErrorMessageText",
            errorMessageBoxTag = "TasksFragmentAddDeliverableErrorMessageBox"
        )

        withTag("TasksFragmentAddDeliverableView") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("EditGoalTimeInputPickDurationButton"){
            performPressWithScroll()
        }

        withTag("EditGoalTimeInputDurationPickerDialog"){
            assertExists()
            assertIsDisplayed()
        }

        withTag("EditGoalDurationPickerHourSlider"){
            assertExists()
            assertIsDisplayed()
            performSemanticsAction(SemanticsActions.SetProgress) { action ->
                action((0).toFloat())
            }
        }
        finishProcesses()

        withTag("EditGoalDurationPickerMinuteSlider"){
            assertExists()
            assertIsDisplayed()
            performSemanticsAction(SemanticsActions.SetProgress) { action ->
                action((1).toFloat())
            }
        }

        withTag("EditGoalDurationPickerSaveAndCloseIconButton") {
            assertExists()
            assertIsDisplayed()
            performPressWithoutScroll()
        }

        withTag("EditGoalTimeInputDurationPickerDialog"){
            assertDoesNotExist()
        }
        finishProcesses()

        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()
        assertEquals(1,deliverablesList[0].times.size)
        time = deliverablesList[0].times[0]
        duration = Converters().toLocalDateTime(time.duration).value
        assertEquals(0,duration.hour)
        assertEquals(1,duration.minute)

        activity.onBackPressedDispatcher.onBackPressed()
        finishProcesses()

        withTag("TasksFragmentAddDeliverableView") {
            assertDoesNotExist()
        }

        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()
        assertEquals(0,goal.goalTasks.first().size)
        assertEquals(0, deliverablesList.size)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun testErrorMessageView(
        errorMessage: String,
        errorMessageTextTag: String,
        errorMessageBoxTag: String
    ) = runMainTest {
        withTag(errorMessageBoxTag) {
            assertExists()
            assertIsDisplayed()
        }
        withTag(errorMessageTextTag){
            assertExists()
            assertIsDisplayed()
            assertTextContains(
                value = errorMessage,
                substring = true,
                ignoreCase = false
            )
        }
        finishProcesses()

        composeTestRule.mainClock.advanceTimeBy(2100)
        finishProcesses()
        withTag(errorMessageBoxTag) {
            assertDoesNotExist()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `03 Adding Time Date End Type Deliverable`() = runMainTest {
        val activity = getTestMainActivity()
        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        var goal = taskViewModel.goal.first()!!
        var deliverablesList = goal.goalDeliverables.first()

        assertEquals(Goal.GoalTab.DELIVERABLES,Goal.GoalTab.valueOf(goal.selectedTab))

        assertEquals(0,goal.goalTasks.first().size)
        assertEquals(0, deliverablesList.size)

        withTag("TasksFragmentAddTextButton"){
            assertExists()
            assertTextContains(activity.getString(Goal.GoalTab.DELIVERABLES.addStringRes))
            performPressWithoutScroll()
        }

        assertEquals(Goal.GoalTab.DELIVERABLES,taskViewModel.bottomSheetType.value)

        withTag("TasksFragmentAddDeliverableView") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("TasksFragmentAddDeliverableViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddDeliverableNQTButtons")
        }

        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()

        assertEquals(1,deliverablesList.size)
        assertEquals(Task.TaskType.NORMAL.name, deliverablesList.first().deliverable.type)

        testTaskTypeButtons(
            Task.TaskType.NORMAL,
            selected = true,
            press = false,
            activity
        )

        testTaskTypeButtons(
            Task.TaskType.TIME,
            selected = false,
            press = true,
            activity
        )

        withTag("DeliverableWarningDialog") {
            assertDoesNotExist()
        }

        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()

        assertEquals(1,deliverablesList.size)
        assertEquals(Task.TaskType.TIME.name, deliverablesList.first().deliverable.type)


        testTaskTypeButtons(
            Task.TaskType.QUANTITY,
            selected = false,
            press = false,
            activity
        )

        testTaskTypeButtons(
            Task.TaskType.NORMAL,
            selected = false,
            press = false,
            activity
        )

        testTaskTypeButtons(
            Task.TaskType.TIME,
            selected = true,
            press = false,
            activity
        )

        withTag("TasksFragmentAddDeliverableNormalQuantityTimeTextField") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("")
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
        }

        withTag("TasksFragmentNormalQuantityTimeTextFieldTimeTimeButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            performPressWithoutScroll()
        }

        withTag("EditGoalTimeInputDurationPickerDialog"){
            assertExists()
            assertIsDisplayed()
        }

        withTag("EditGoalDurationPickerHourSlider"){
            assertExists()
            assertIsDisplayed()
            assertEquals(
                0f..23F,
                fetchSemanticsNode().config[SliderRangeKey]
            )
            performSemanticsAction(SemanticsActions.SetProgress) { action ->
                action(2F)
            }
        }
        finishProcesses()

        withTag("EditGoalDurationPickerMinuteSlider"){
            assertExists()
            assertIsDisplayed()
            assertEquals(
                0f..59F,
                fetchSemanticsNode().config[SliderRangeKey]
            )
            performSemanticsAction(SemanticsActions.SetProgress) { action ->
                action(30F)
            }
        }
        finishProcesses()

        withTag("EditGoalDurationPickerSaveAndCloseIconButton") {
            assertExists()
            assertIsDisplayed()
            performPressWithoutScroll()
        }

        withTag("EditGoalTimeInputDurationPickerDialog"){
            assertDoesNotExist()
        }

        withTag("TasksFragmentNormalQuantityTimeTextFieldTimeTimeButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            assertTextContains(
                value = "2 Hours and 30 Minutes",
                substring = true,
                ignoreCase = false
            )
        }

        withTag("TasksFragmentNormalQuantityTimeTextFieldTimeSecondField") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("")
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
            assertTextContains(
                value = " 2 Hours and 30 Minutes ",
                substring = true,
                ignoreCase = false
            )
        }
        finishProcesses()

        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()
        val newTime = Converters().toLocalDateTime(deliverablesList[0].deliverable.time).value
        assertNotNull(newTime)
        assertEquals(2, newTime.hour)
        assertEquals(30, newTime.minute)

        withTag("TasksFragmentDeliverableProcessDeliverable") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            performPressWithoutScroll()
        }

        testErrorMessageView(
            errorMessage = "Please Enter A Deliverable",
            errorMessageTextTag = "TasksFragmentAddDeliverableErrorMessageText",
            errorMessageBoxTag = "TasksFragmentAddDeliverableErrorMessageBox"
        )

        withTag("TasksFragmentAddDeliverableView") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("TasksFragmentAddDeliverableNormalQuantityTimeTextField") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("Run a total of")
            assertTextContains(
                value = "Run a total of",
                substring = true,
                ignoreCase = false
            )
        }

        withTag("TasksFragmentNormalQuantityTimeTextFieldTimeSecondField") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("in the snow.")
            assertTextContains(
                value = "in the snow.",
                substring = true,
                ignoreCase = false
            )
            assertTextContains(
                value = "Run a total of 2 Hours and 30 Minutes in the snow.",
                substring = true,
                ignoreCase = false
            )
        }

        withTag("TaskFragmentDeliverableLocationText") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("At Home")
            assertTextContains(
                value = "At Home",
                substring = true,
                ignoreCase = false
            )
        }

        withTag("TasksFragmentAddDeliverableViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentDeliverableEndTypeButton")
        }

        withTag("TasksFragmentDeliverableEndTypeButton") {
            performPressWithScroll()
        }

        withTag("TasksFragmentDeliverableEndTypeDropDownMenu") {
            assertExists()
            assertIsDisplayed()
        }

        goal = taskViewModel.goal.first()!!
        assertEquals(
            Goal.GoalEndType.TASK.name,
            goal.endType
        )

        withTag(
            "TasksFragmentDeliverableDropdownMenuItem-${Deliverable.DeliverableEndType.UNDEFINED.name}"
        ) {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
        }

        withTag(
            "TasksFragmentDeliverableDropdownMenuItem-${Deliverable.DeliverableEndType.GOAL.name}"
        ) {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
        }

        withTag(
            "TasksFragmentDeliverableDropdownMenuItem-${Deliverable.DeliverableEndType.WORK.name}"
        ) {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
        }

        withTag(
            "TasksFragmentDeliverableDropdownMenuItem-${Deliverable.DeliverableEndType.DATE.name}"
        ) {
            assertExists()
            assertIsDisplayed()
            performPressWithoutScroll()
        }

        DateAndTimePickerTest(
            getTimeAsLong = { taskViewModel.goal.first()?.goalDeliverables?.first()[0]?.deliverable?.endDateTime },
            getExpectedEndType = { Deliverable.DeliverableEndType.DATE.name },
            getActualEndType = { taskViewModel.goal.first()?.goalDeliverables?.first()[0]?.deliverable?.endType },
            endTypeButtonTag = { "TasksFragmentDeliverableEndTypeButton" },
            dropDownMenuTag = { "TasksFragmentDeliverableEndTypeDropDownMenu" },
            dropDownMenuItemTag = { "TasksFragmentDeliverableDropdownMenuItem-${Deliverable.DeliverableEndType.DATE.name}" },
            selectDateAndTimeButtonTag = null,
            parentParameters = Triple(
                instantTaskExecutorRule,
                composeTestRule,
                activity
            )
        ).runTests(this@TasksFragmentGoalNotDeliverableEndTypeTests::class)

        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()
        val pickedDate = Converters().toLocalDateTime(deliverablesList[0].deliverable.endDateTime).value
        assertNotNull(pickedDate)

        withTag("TasksFragmentDeliverableEndTypeButton") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            assertTextContains(
                value = "Start Time and Date: ${getTime(pickedDate!!)} ${getStandardDate( activity.applicationContext, pickedDate)}",
                substring = true,
                ignoreCase = false
            )
        }

        assertNull(deliverablesList[0].deliverable.goalId)

        activity.onBackPressedDispatcher.onBackPressed()
        finishProcesses()

        withTag("TasksFragmentAddDeliverableView") {
            assertDoesNotExist()
        }

        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()
        assertEquals(0,goal.goalTasks.first().size)
        assertEquals(0, deliverablesList.size)
    }
}