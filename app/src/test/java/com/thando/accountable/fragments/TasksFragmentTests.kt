package com.thando.accountable.fragments

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.thando.accountable.AccountableComposeRobolectricTest
import com.thando.accountable.AccountableNavigationController.AccountableFragment
import com.thando.accountable.R
import com.thando.accountable.database.Converters
import com.thando.accountable.database.tables.Deliverable
import com.thando.accountable.database.tables.DeliverableWithLists
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.Marker
import com.thando.accountable.database.tables.Task
import com.thando.accountable.database.tables.TaskDeliverable
import com.thando.accountable.fragments.viewmodels.BooksViewModel
import com.thando.accountable.fragments.viewmodels.EditGoalViewModel
import com.thando.accountable.fragments.viewmodels.HomeViewModel
import com.thando.accountable.fragments.viewmodels.TaskViewModel
import com.thando.accountable.input_forms.DateAndTimePickerTest
import com.thando.accountable.input_forms.TimeBlockTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
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
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TasksFragmentTests: AccountableComposeRobolectricTest() {
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

        // Set end type to deliverable
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

        TimeBlockTest(
            "EditGoalAddTimeBlockButton",
            {editGoalViewModel.newGoal.first()!!.times.first()},
            Triple(
                instantTaskExecutorRule,
                composeTestRule,
                activity
            ),
            lazyColumnTag = "EditGoalLazyColumn"
        ).runTests(this@TasksFragmentTests::class)
        finishProcesses()

        var goal = editGoalViewModel.newGoal.first()
        val deliverables = goal?.goalDeliverables?.first()
        val times = goal?.times?.first()

        assertNotNull(goal)
        assertNotNull(deliverables)
        assertNotNull(times)
        assertTrue(deliverables!!.isEmpty())
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

    @Test
    fun `02 Image Is Loaded And Displayed`() = runMainTest {
        val activity = getTestMainActivity()
        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        assertNotNull(taskViewModel.goal.first()!!.getGoalPicture())
        assertNotNull(taskViewModel.goal.first()!!.getImageBitmap(activity).first())
        withTag("TasksFragmentImage"){
            assertExists()
            assertIsDisplayed()
            assertWidthIsAtLeast(2.dp)
            assertHeightIsAtLeast(2.dp)
        }
    }

    @Test
    fun `03 Can Navigate Back`() = runMainTest {
        val activity = getTestMainActivity()
        withTag("TasksFragmentNavigateBackIcon"){
            performPressWithoutScroll()
        }

        checkFragmentIs(
            activity,
            AccountableFragment.GoalsFragment,
            BooksViewModel::class.java.name
        )

        val booksViewModel: BooksViewModel = getViewModel(activity)
        assertTrue(booksViewModel.goalsList.first().isNotEmpty())
        assertNotNull(booksViewModel.goalsList.first()[0].id)
        val goal = activity.viewModel.repository.getGoal(booksViewModel.goalsList.first()[0].id).first()
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

    @Test
    fun `04 Tab Switching`() = runMainTest {
        val activity = getTestMainActivity()
        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        val goal = taskViewModel.goal.first()!!

        assertEquals(Goal.GoalTab.TASKS,Goal.GoalTab.valueOf(goal.selectedTab))

        listOf(
            Goal.GoalTab.DELIVERABLES,
            Goal.GoalTab.MARKERS,
            Goal.GoalTab.DELIVERABLES,
            Goal.GoalTab.TASKS,
            Goal.GoalTab.MARKERS
        ).forEach {
            switchToTab(it,taskViewModel, activity)
        }
    }

    private fun markersAreEqual(
        markerOne:Marker,
        markerTwo: Marker,
        idsEqual:Boolean = true,
        editing:Boolean = false
    ) = runMainTest {
        val assertionFunction: suspend TestScope.(Any?, Any?)->Unit = if (idsEqual)
            {objectA, objectB -> assertEquals(objectA,objectB)}
        else {objectA, objectB -> assertNotEquals(objectA,objectB)}

        assertionFunction(
            markerOne.id,
            markerTwo.id
        )

        assertEquals(
            markerOne.parent,
            markerTwo.parent
        )

        assertEquals(
            markerOne.dateTime,
            markerTwo.dateTime
        )
        assertEquals(
            markerOne.position,
            markerTwo.position
        )
        assertEquals(
            markerOne.scrollPosition,
            markerTwo.scrollPosition
        )
        assertEquals(
            markerOne.marker,
            markerTwo.marker
        )

        if (!idsEqual && editing) {
            assertEquals(markerOne.id, markerTwo.cloneId)
        }
    }

    @Test
    fun `05 Adding Marker`() = runMainTest {
        val activity = getTestMainActivity()
        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        val goal = taskViewModel.goal.first()!!

        assertEquals(Goal.GoalTab.MARKERS,getGoalTab(goal.selectedTab))
        withTag("TasksFragmentAddTextButton"){
            assertExists()
            assertTextContains(activity.getString(Goal.GoalTab.MARKERS.addStringRes))
            performPressWithoutScroll()
        }

        assertEquals(Goal.GoalTab.MARKERS,taskViewModel.bottomSheetType.value)
        withTag("TasksFragmentAddMarkerView"){
            assertExists()
            assertIsDisplayed()
        }

        assertNull(taskViewModel.originalMarker.value)
        withTag("TasksFragmentMarkerTitle"){
            assertExists()
            assertIsDisplayed()
            assertTextContains(activity.getString(R.string.add,activity.getString(R.string.marker)))
        }

        withTag("TasksFragmentMarkerSaveButton"){
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
        }

        assertNotNull(taskViewModel.marker.first())

        withTag("TasksFragmentMarkerMarkerText"){
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains("")
            performTextReplacement("February Ends")
            assertTextContains("February Ends")
        }

        withTag("TasksFragmentMarkerPickTimeButton"){
            performPressWithScroll()
        }

        DateAndTimePickerTest(
            getTimeAsLong = { taskViewModel.marker.first()?.dateTime },
            getExpectedEndType = null,
            getActualEndType = null,
            endTypeButtonTag = null,
            dropDownMenuTag = null,
            dropDownMenuItemTag = null,
            selectDateAndTimeButtonTag = "TasksFragmentMarkerPickTimeButton",
            parentParameters = Triple(
                instantTaskExecutorRule,
                composeTestRule,
                activity
            )
        ).runTests(this@TasksFragmentTests::class)

        val marker = taskViewModel.marker.first()
        assertNotNull(marker?.dateTime)
        assertTrue(marker!!.dateTime>0)

        withTag("TasksFragmentMarkerDeleteButton") {
            assertDoesNotExist()
        }

        withTag("TasksFragmentMarkerSaveButton"){
            performPressWithoutScroll()
        }

        var markersList = taskViewModel.goal.first()?.goalMarkers?.first()
        assertNotNull(markersList)
        markersList = markersList!!
        assertEquals(1,markersList.size)

        markersAreEqual(
            marker,
            markersList[0],
            true
        )
    }

    @Test
    fun `06 Editing Marker`() = runMainTest {
        val activity = getTestMainActivity()
        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        val goal = taskViewModel.goal.first()!!
        var markersList = goal.goalMarkers.first()
        assertNotNull(markersList)
        assertEquals(1,markersList.size)
        val oldMarker = markersList[0]

        withTag("TasksFragmentMarkerCardView-${markersList[0].id}") {
            performLongPressWithScroll()
        }

        assertNotNull(taskViewModel.marker.first())
        assertNotNull(taskViewModel.originalMarker.value?.first())

        assertEquals(Goal.GoalTab.MARKERS,taskViewModel.bottomSheetType.value)
        withTag("TasksFragmentAddMarkerView"){
            assertExists()
            assertIsDisplayed()
        }

        markersAreEqual(
            oldMarker,
            taskViewModel.marker.first()!!,
            idsEqual = false,
            editing = true
        )

        withTag("TasksFragmentMarkerTitle"){
            assertExists()
            assertIsDisplayed()
            assertTextContains(activity.getString(R.string.edit_with_arg,activity.getString(R.string.marker)))
        }

        withTag("TasksFragmentMarkerSaveButton"){
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
        }

        withTag("TasksFragmentMarkerMarkerText"){
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains("February Ends")
            performTextReplacement("February Ends Tonight")
            assertTextContains("February Ends Tonight")
        }

        setMarkerDateAndTime(
            activity,
            LocalDate.now().plusDays(4),
            LocalTime.of(2,2)
        ) {
            runMainTest {
                withTag("TasksFragmentMarkerPickTimeButton") {
                    performPressWithScroll()
                }
            }
        }

        val marker = taskViewModel.marker.first()
        assertNotNull(marker?.dateTime)
        assertTrue(marker!!.dateTime>0)

        withTag("TasksFragmentMarkerDeleteButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
        }

        withTag("TasksFragmentMarkerSaveButton"){
            performPressWithoutScroll()
        }

        assertNotNull(taskViewModel.goal.first()?.goalMarkers?.first())
        markersList = taskViewModel.goal.first()?.goalMarkers?.first()!!
        assertEquals(1,markersList.size)

        markersAreEqual(
            marker,
            markersList[0],
            false
        )

        assertNotEquals(
            oldMarker.marker,
            markersList[0].marker
        )

        assertEquals(oldMarker.id,markersList[0].id)

        assertNotEquals(oldMarker.dateTime,markersList[0].dateTime)

        assertEquals(
            Converters().fromLocalDateTime(LocalDateTime.of(
                LocalDate.now().plusDays(4),
                LocalTime.of(2,2)
            )),
            markersList[0].dateTime
        )
    }

    @Test
    fun `07 Deleting Marker`() = runMainTest {
        val activity = getTestMainActivity()
        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        val goal = taskViewModel.goal.first()!!
        var markersList = goal.goalMarkers.first()
        assertNotNull(markersList)
        assertEquals(1,markersList.size)
        val oldMarker = markersList[0]

        withTag("TasksFragmentMarkerCardView-${markersList[0].id}") {
            performLongPressWithScroll()
        }

        markersList = goal.goalMarkers.first()
        assertEquals(2,markersList.size)

        markersAreEqual(
            oldMarker,
            taskViewModel.marker.first()!!,
            idsEqual = false,
            editing = true
        )

        withTag("TasksFragmentMarkerBackButton") {
            performPressWithoutScroll()
        }

        markersList = goal.goalMarkers.first()
        assertEquals(1,markersList.size)

        markersAreEqual(
            oldMarker,
            markersList[0]
        )

        withTag("TasksFragmentMarkerCardView-${markersList[0].id}") {
            performLongPressWithScroll()
        }

        withTag("TasksFragmentAddMarkerViewLazyColumn"){
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentMarkerDeleteButton")
        }

        withTag("TasksFragmentMarkerDeleteButton"){
            performPressWithScroll()
        }

        markersList = goal.goalMarkers.first()
        assertNotNull(markersList)
        assertEquals(0,markersList.size)
    }

    private fun addMarker(
        activity: TestMainActivity,
        taskViewModel: TaskViewModel,
        markerText: String,
        date: LocalDate,
        time: LocalTime
    ) = runMainTest {
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        val goal = taskViewModel.goal.first()!!
        assertNotNull(taskViewModel.goal.first()?.goalMarkers?.first()?.size)
        val oldSize = taskViewModel.goal.first()!!.goalMarkers.first().size

        assertEquals(Goal.GoalTab.MARKERS,getGoalTab(goal.selectedTab))
        withTag("TasksFragmentAddTextButton"){
            assertExists()
            assertTextContains(activity.getString(Goal.GoalTab.MARKERS.addStringRes))
            performPressWithoutScroll()
        }

        assertEquals(Goal.GoalTab.MARKERS,taskViewModel.bottomSheetType.value)
        withTag("TasksFragmentAddMarkerView"){
            assertExists()
            assertIsDisplayed()
        }

        assertNull(taskViewModel.originalMarker.value)
        withTag("TasksFragmentMarkerTitle"){
            assertExists()
            assertIsDisplayed()
            assertTextContains(activity.getString(R.string.add,activity.getString(R.string.marker)))
        }

        withTag("TasksFragmentMarkerSaveButton"){
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
        }

        assertNotNull(taskViewModel.marker.first())

        withTag("TasksFragmentMarkerMarkerText"){
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            performTextReplacement(markerText)
            assertTextContains(markerText)
        }

        withTag("TasksFragmentMarkerPickTimeButton"){
            performPressWithScroll()
        }

        setMarkerDateAndTime(
            activity,
            date,
            time
        ) {
            runMainTest {
                withTag("TasksFragmentMarkerPickTimeButton") {
                    performPressWithScroll()
                }
            }
        }

        val marker = taskViewModel.marker.first()
        assertNotNull(marker?.dateTime)
        assertTrue(marker!!.dateTime>0)

        withTag("TasksFragmentMarkerDeleteButton") {
            assertDoesNotExist()
        }

        withTag("TasksFragmentMarkerSaveButton"){
            performPressWithoutScroll()
        }

        var markersList = taskViewModel.goal.first()?.goalMarkers?.first()
        assertNotNull(markersList)
        markersList = markersList!!
        assertEquals(oldSize+1,markersList.size)

        markersAreEqual(
            marker,
            markersList[oldSize],
            true
        )
    }

    @Test
    fun `08 Adding 3 Markers`() = runMainTest {
        val activity = getTestMainActivity()

        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        val goal = taskViewModel.goal.first()!!
        var markersList = goal.goalMarkers.first()
        assertNotNull(markersList)
        assertEquals(0,markersList.size)

        addMarker(
            activity,
            taskViewModel = taskViewModel,
            markerText = "Marker 1",
            date = LocalDate.now().plusDays(5),
            time = LocalTime.of(10,10)
        )

        markersList = goal.goalMarkers.first()
        assertNotNull(markersList)
        assertEquals(1,markersList.size)

        addMarker(
            activity,
            taskViewModel = taskViewModel,
            markerText = "Marker 2",
            date = LocalDate.now().plusDays(6),
            time = LocalTime.of(12,12)
        )

        markersList = goal.goalMarkers.first()
        assertNotNull(markersList)
        assertEquals(2,markersList.size)

        addMarker(
            activity,
            taskViewModel = taskViewModel,
            markerText = "Marker 3",
            date = LocalDate.now().plusDays(7),
            time = LocalTime.of(14,14)
        )

        markersList = goal.goalMarkers.first()
        assertNotNull(markersList)
        assertEquals(3,markersList.size)

        markersList.forEach {
            assertNotNull(it.id)
            withTag("TasksFragmentViewLazyColumn"){
                assertExists()
                assertIsDisplayed()
                performScrollToKey(it.id!!)
            }

            withTag("TasksFragmentMarkerCardView-${it.id}") {
                assertExists()
                performScrollTo()
                assertIsDisplayed()
                hasClickAction()
                assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
            }
        }
    }

    @Test
    fun `09 Adding Normal Date Task`() = runMainTest {
        val activity = getTestMainActivity()

        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        var goal = taskViewModel.goal.first()!!

        var tasksList = goal.goalTasks.first()
        assertNotNull(tasksList)
        assertEquals(0, tasksList.size)

        assertEquals(Goal.GoalTab.MARKERS, Goal.GoalTab.valueOf(goal.selectedTab))
        switchToTab(Goal.GoalTab.TASKS, taskViewModel, activity)
        goal = taskViewModel.goal.first()!!
        assertEquals(Goal.GoalTab.TASKS, Goal.GoalTab.valueOf(goal.selectedTab))

        withTag("TasksFragmentAddTextButton") {
            assertExists()
            assertTextContains(activity.getString(Goal.GoalTab.TASKS.addStringRes))
            performPressWithoutScroll()
        }

        assertEquals(Goal.GoalTab.TASKS, taskViewModel.bottomSheetType.value)
        withTag("TasksFragmentAddTaskView") {
            assertExists()
            assertIsDisplayed()
        }

        assertNull(taskViewModel.originalTask.value)
        assertNotNull(taskViewModel.task.first())

        withTag("TasksFragmentAddTaskViewTaskTypeButton-${Task.TaskType.NORMAL.name}") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            assertEquals(
                Color.Blue,
                fetchSemanticsNode().config[BackgroundColorKey]
            )
            assertTextContains(activity.getString(Task.TaskType.NORMAL.stringRes))
        }

        withTag("TasksFragmentAddTaskViewTaskTypeButton-${Task.TaskType.QUANTITY.name}") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            assertEquals(
                Color.LightGray,
                fetchSemanticsNode().config[BackgroundColorKey]
            )
            assertTextContains(activity.getString(Task.TaskType.QUANTITY.stringRes))
        }

        withTag("TasksFragmentAddTaskViewTaskTypeButton-${Task.TaskType.TIME.name}") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            assertEquals(
                Color.LightGray,
                fetchSemanticsNode().config[BackgroundColorKey]
            )
            assertTextContains(activity.getString(Task.TaskType.TIME.stringRes))
        }

        withTag("TasksFragmentAddTaskNormalQuantityTimeTextField") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("My Normal Date Task")
            assertTextContains(
                value = "My Normal Date Task",
                substring = true,
                ignoreCase = false
            )
        }

        withTag("TasksFragmentAddTaskLocationTextField") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("My Normal Date Location")
            assertTextContains(
                value = "My Normal Date Location",
                substring = true,
                ignoreCase = false
            )
        }

        assertNotNull(taskViewModel.task.first()?.colour)
        var taskColour = taskViewModel.task.first()!!.colour
        withTag("TasksFragmentAddTaskColourBox") {
            if (taskColour == -1) assertDoesNotExist()
            else {
                assertExists()
                performScrollTo()
                assertIsDisplayed()
                assertEquals(
                    taskColour,
                    fetchSemanticsNode().config[BackgroundColorKey].toArgb()
                )
            }
        }

        withTag("TasksFragmentAddTaskColourButton") {
            performPressWithScroll()
        }

        withTag("ColourPickerDialogCanvas") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            performTouchInput { click(Offset(60f, 60f)) }
        }
        finishProcesses()

        withTag("ColourPickerDialogOKButton") {
            performPressWithScroll()
        }

        withTag("ColourPickerDialog") {
            assertDoesNotExist()
        }

        assertNotNull(taskViewModel.task.first()?.colour)
        assertNotEquals(-1, taskViewModel.task.first()?.colour)
        val taskColourSixty = taskViewModel.task.first()!!.colour
        taskColour = taskViewModel.task.first()!!.colour
        withTag("TasksFragmentAddTaskColourBox") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertEquals(
                taskColour,
                fetchSemanticsNode().config[BackgroundColorKey].toArgb()
            )
        }

        withTag("TasksFragmentAddTaskColourButton") {
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

        assertNotNull(taskViewModel.task.first()?.colour)
        assertNotEquals(taskColourSixty, taskViewModel.task.first()?.colour)
        taskColour = taskViewModel.task.first()!!.colour
        withTag("TasksFragmentAddTaskColourBox") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertEquals(
                taskColour,
                fetchSemanticsNode().config[BackgroundColorKey].toArgb()
            )
        }

        withTag("TasksFragmentAddTaskColourButton") {
            performPressWithScroll()
        }

        withTag("ColourPickerDialogCanvas") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            performTouchInput { click(Offset(60f, 60f)) }
        }
        finishProcesses()

        withTag("ColourPickerDialogDismissButton") {
            performPressWithScroll()
        }

        withTag("ColourPickerDialog") {
            assertDoesNotExist()
        }

        assertNotNull(taskViewModel.task.first()?.colour)
        assertNotEquals(taskColourSixty, taskViewModel.task.first()?.colour)
        withTag("TasksFragmentAddTaskColourBox") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertEquals(
                taskColour,
                fetchSemanticsNode().config[BackgroundColorKey].toArgb()
            )
        }

        withTag("TasksFragmentAddTaskEndTypeButton") {
            performPressWithScroll()
        }

        withTag("TasksFragmentAddViewDropdownMenuItem-${Task.TaskEndType.DATE.name}") {
            performPressWithoutScroll()
        }

        DateAndTimePickerTest(
            getTimeAsLong = { taskViewModel.task.first()?.endDateTime },
            getExpectedEndType = { Task.TaskEndType.DATE.name },
            getActualEndType = { taskViewModel.task.first()?.endType },
            endTypeButtonTag = { "TasksFragmentAddTaskEndTypeButton" },
            dropDownMenuTag = { "TasksFragmentAddViewDropdownMenu" },
            dropDownMenuItemTag = { "TasksFragmentAddViewDropdownMenuItem-${Task.TaskEndType.DATE.name}" },
            selectDateAndTimeButtonTag = null,
            parentParameters = Triple(
                instantTaskExecutorRule,
                composeTestRule,
                activity
            )
        ).runTests(this@TasksFragmentTests::class)

        withTag("TasksFragmentAddTaskViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddTaskAddTimeBlockButton")
        }

        assertNotNull(taskViewModel.task.first()?.times)
        TimeBlockTest(
            "TasksFragmentAddTaskAddTimeBlockButton",
            {taskViewModel.task.first()!!.times.first()},
            Triple(
                instantTaskExecutorRule,
                composeTestRule,
                activity
            ),
            lazyColumnTag = "TasksFragmentAddTaskViewLazyColumn"
        ).runTests(this@TasksFragmentTests::class)

        withTag("TasksFragmentAddTaskDeleteButton").assertDoesNotExist()
        val task = taskViewModel.task.first()!!
        val times = task.times.first()
        val deliverablesLists = getTaskDeliverablesLists(task)

        withTag("TasksFragmentTaskSaveButton") {
            performPressWithoutScroll()
        }

        assertNotNull(taskViewModel.goal.first()?.goalTasks?.first())
        tasksList = taskViewModel.goal.first()!!.goalTasks.first()
        assertEquals(1, tasksList.size)

        tasksAreEqual(
            taskOne = task,
            taskTwo = tasksList[0],
            idsEqual = true,
            times = times,
            deliverablesLists = deliverablesLists
        )
    }

    @Test
    fun `10 Editing Task`() = runMainTest {
        val activity = getTestMainActivity()

        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        val goal = taskViewModel.goal.first()!!

        var tasksList = goal.goalTasks.first()
        assertNotNull(tasksList)
        assertEquals(1,tasksList.size)
        val oldTask = tasksList[0]
        val oldTimes = oldTask.times.first()
        val oldDeliverablesLists = getTaskDeliverablesLists(oldTask)

        withTag("TasksFragmentTaskCardView-${tasksList[0].taskId}") {
            performLongPressWithScroll()
        }

        assertNotNull(taskViewModel.task.first())
        assertNotNull(taskViewModel.originalTask.value?.first())

        assertEquals(Goal.GoalTab.TASKS,taskViewModel.bottomSheetType.value)
        withTag("TasksFragmentAddTaskView"){
            assertExists()
            assertIsDisplayed()
        }

        tasksAreEqual(
            oldTask,
            taskViewModel.task.first()!!,
            idsEqual = false,
            times = oldTimes,
            deliverablesLists = oldDeliverablesLists
        )

        withTag("TasksFragmentTaskTitle"){
            assertExists()
            assertIsDisplayed()
            assertTextContains(activity.getString(R.string.edit_with_arg,activity.getString(R.string.task)))
        }

        withTag("TasksFragmentTaskSaveButton"){
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
        }

        withTag("TasksFragmentAddTaskNormalQuantityTimeTextField"){
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                "My Normal Date Task",
                substring = true
            )
            performTextReplacement("My Normal Date Task Edited")
            assertTextContains(
                "My Normal Date Task Edited",
                substring = true
            )
        }

        withTag("TasksFragmentAddTaskViewLazyColumn"){
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddTaskLocationTextField")
        }

        withTag("TasksFragmentAddTaskLocationTextField") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                "My Normal Date Location",
                substring = true
            )
            performTextReplacement("My Normal Date Location Edited")
            assertTextContains(
                "My Normal Date Location Edited",
                substring = true
            )
        }

        setMarkerDateAndTime(
            activity,
            LocalDate.now().plusDays(4),
            LocalTime.of(2,2)
        ) {
            runMainTest {
                withTag("TasksFragmentAddTaskEndTypeButton") {
                    performPressWithScroll()
                }

                withTag("TasksFragmentAddViewDropdownMenuItem-${Task.TaskEndType.DATE.name}") {
                    performPressWithoutScroll()
                }
            }
        }

        val task = taskViewModel.task.first()
        val taskTimes = task?.times?.first()
        assertNotNull(task?.endDateTime)
        val deliverablesLists = getTaskDeliverablesLists(task!!)
        assertTrue(task.endDateTime>0)

        withTag("TasksFragmentAddTaskViewLazyColumn"){
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddTaskDeleteButton")
        }

        withTag("TasksFragmentAddTaskDeleteButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
        }

        withTag("TasksFragmentTaskSaveButton"){
            performPressWithoutScroll()
        }

        assertNotNull(taskViewModel.goal.first()?.goalTasks?.first())
        tasksList = taskViewModel.goal.first()?.goalTasks?.first()!!
        assertEquals(1,tasksList.size)

        tasksAreEqual(
            task,
            tasksList[0],
            false,
            taskTimes,
            deliverablesLists = deliverablesLists
        )

        assertNotEquals(
            oldTask.task,
            tasksList[0].task
        )

        assertNotEquals(
            oldTask.location,
            tasksList[0].location
        )

        assertEquals(oldTask.taskId,tasksList[0].taskId)

        assertNotEquals(oldTask.endDateTime,tasksList[0].endDateTime)

        assertEquals(
            Converters().fromLocalDateTime(LocalDateTime.of(
                LocalDate.now().plusDays(4),
                LocalTime.of(2,2)
            )),
            tasksList[0].endDateTime
        )
    }

    @Test
    fun `11 Deleting Task`() = runMainTest {
        val activity = getTestMainActivity()
        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        val goal = taskViewModel.goal.first()!!
        var tasksList = goal.goalTasks.first()
        assertNotNull(tasksList)
        assertEquals(1,tasksList.size)

        val taskDeliverablesList = tasksList[0].taskDeliverableList.first()
        val taskTimesList = tasksList[0].times.first()

        withTag("TasksFragmentTaskCardView-${tasksList[0].taskId}") {
            performLongPressWithScroll()
        }

        withTag("TasksFragmentAddTaskViewLazyColumn"){
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddTaskDeleteButton")
        }

        withTag("TasksFragmentAddTaskDeleteButton"){
            performPressWithScroll()
        }

        tasksList = goal.goalTasks.first()
        assertNotNull(tasksList)
        assertEquals(0,tasksList.size)

        taskDeliverablesList.forEach {
            val taskDeliverable = activity.viewModel.repository.getTaskDeliverable(it.taskId,it.deliverableId)
            assertNull(taskDeliverable.first())
        }

        taskTimesList.forEach {
            assertNull(
                activity.viewModel.repository.getGoalTaskDeliverableTime(it.id).first()
            )
        }
    }

    @Test
    fun `12 Adding Quantity Deliverable Task`() = runMainTest {
        val activity = getTestMainActivity()

        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        val goal = taskViewModel.goal.first()!!

        addQuantityDeliverableTask(activity,taskViewModel,goal)
    }

    private fun addQuantityDeliverableTask(
        activity: TestMainActivity,
        taskViewModel: TaskViewModel,
        goal: Goal
    ) = runMainTest {
        var tasksList = goal.goalTasks.first()
        assertNotNull(tasksList)
        assertEquals(0, tasksList.size)

        withTag("TasksFragmentAddTextButton"){
            assertExists()
            assertTextContains(activity.getString(Goal.GoalTab.TASKS.addStringRes))
            performPressWithoutScroll()
        }
        assertEquals(Goal.GoalTab.TASKS,taskViewModel.bottomSheetType.value)

        withTag("TasksFragmentAddTaskView"){
            assertExists()
            assertIsDisplayed()
        }

        assertNull(taskViewModel.originalTask.value)
        assertNotNull(taskViewModel.task.first())

        withTag("TasksFragmentAddTaskViewTaskTypeButton-${Task.TaskType.QUANTITY.name}") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            performPressWithoutScroll()
            assertEquals(
                Color.Blue,
                fetchSemanticsNode().config[BackgroundColorKey]
            )
            assertTextContains(activity.getString(Task.TaskType.QUANTITY.stringRes))
        }

        withTag("TasksFragmentAddTaskViewTaskTypeButton-${Task.TaskType.NORMAL.name}") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            assertEquals(
                Color.LightGray,
                fetchSemanticsNode().config[BackgroundColorKey]
            )
            assertTextContains(activity.getString(Task.TaskType.NORMAL.stringRes))
        }

        withTag("TasksFragmentAddTaskViewTaskTypeButton-${Task.TaskType.TIME.name}") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            assertEquals(
                Color.LightGray,
                fetchSemanticsNode().config[BackgroundColorKey]
            )
            assertTextContains(activity.getString(Task.TaskType.TIME.stringRes))
        }

        assertEquals(
            Task.TaskType.QUANTITY.name,
            taskViewModel.task.first()?.type
        )

        withTag("TasksFragmentAddTaskNormalQuantityTimeTextField") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("My Quantity 6 Deliverable Task 7")
            assertTextContains(
                value = "My Quantity 6 Deliverable Task ",
                substring = true,
                ignoreCase = false
            )
        }
        assertEquals(6L,taskViewModel.task.first()?.quantity)
        withTag("TasksFragmentAddTaskNormalQuantityTimeTextField") {
            performTextReplacement("Jump 2 times33")
            assertTextContains(
                value = "Jump 2 times",
                substring = true,
                ignoreCase = false
            )
        }
        assertEquals(2L,taskViewModel.task.first()?.quantity)

        withTag("TasksFragmentAddTaskLocationTextField") {
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

        assertNotNull(taskViewModel.task.first()?.colour)
        val taskColour = taskViewModel.task.first()!!.colour
        withTag("TasksFragmentAddTaskColourBox") {
            if (taskColour == -1) assertDoesNotExist()
            else{
                assertExists()
                performScrollTo()
                assertIsDisplayed()
                assertEquals(
                    taskColour,
                    fetchSemanticsNode().config[BackgroundColorKey].toArgb()
                )
            }
        }

        withTag("TasksFragmentAddTaskColourButton") {
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

        assertNotNull(taskViewModel.task.first()?.colour)
        assertNotEquals(-1, taskViewModel.task.first()?.colour)
        withTag("TasksFragmentAddTaskColourBox") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertEquals(
                taskColour,
                fetchSemanticsNode().config[BackgroundColorKey].toArgb()
            )
        }

        withTag("TasksFragmentAddTaskEndTypeButton") {
            performPressWithScroll()
        }

        withTag("TasksFragmentAddViewDropdownMenuItem-${Task.TaskEndType.DELIVERABLE.name}") {
            performPressWithoutScroll()
        }

        withTag("TasksFragmentAddTaskViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddTaskModifyDeliverableButtons")
        }

        withTag("TasksFragmentAddTaskAddDeliverableButton") {
            performPressWithScroll()
        }

        assertNotNull(taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()[0])

        withTag("DeliverableAdderDialog") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("TasksFragmentTaskDeliverableWorkInputTextField") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("My Quantity 6 Deliverable 7")
            assertTextContains(
                value = "My Quantity 6 Deliverable",
                substring = true,
                ignoreCase = false
            )
            finishProcesses()
        }
        assertEquals(6L,taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()[0]?.deliverable?.quantity)
        withTag("TasksFragmentTaskDeliverableWorkInputTextField") {
            performTextReplacement("Jump a total of 20 times33")
            assertTextContains(
                value = "Jump a total of 20 times",
                substring = true,
                ignoreCase = false
            )
            finishProcesses()
        }
        assertEquals(20L,taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()[0]?.deliverable?.quantity)

        withTag("DeliverableAdderDialogDismissButton") {
            performPressWithScroll()
        }

        assertEquals(0,taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()?.size)

        withTag("DeliverableAdderDialog") {
            assertDoesNotExist()
        }

        withTag("TasksFragmentAddTaskAddDeliverableButton") {
            performPressWithScroll()
        }

        assertNotNull(taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()[0])

        withTag("DeliverableAdderDialog") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("TasksFragmentTaskDeliverableWorkInputTextField") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("My Quantity 6 Deliverable 7")
            assertTextContains(
                value = "My Quantity 6 Deliverable",
                substring = true,
                ignoreCase = false
            )
        }
        assertEquals(6L,taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()[0]?.deliverable?.quantity)
        withTag("TasksFragmentTaskDeliverableWorkInputTextField") {
            performTextReplacement("Jump a total of 20 times33")
            assertTextContains(
                value = "Jump a total of 20 times",
                substring = true,
                ignoreCase = false
            )
        }
        assertEquals(20L,taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()[0]?.deliverable?.quantity)

        withTag("DeliverableAdderDialogOKButton") {
            performPressWithScroll()
        }

        assertEquals(1,taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()?.size)

        withTag("DeliverableAdderDialog") {
            assertDoesNotExist()
        }

        withTag("TasksFragmentAddTaskSelectDeliverableButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = activity.getString(R.string.select_deliverable) + " (${1})",
                substring = true,
                ignoreCase = false
            )
            performPressWithoutScroll()
        }

        withTag("DeliverablePickerDialog") {
            assertExists()
            assertIsDisplayed()
        }

        assertNotNull(taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()[0]?.deliverable?.deliverableId)
        var deliverableId = taskViewModel.task.first()!!.getNotSelectedDeliverablesList().first()[0].deliverable.deliverableId!!

        withTag("DeliverablePickerDialogLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(deliverableId)
        }

        withTag("DeliverablePickerDialogPickButton-${deliverableId}") {
            performPressWithScroll()
        }

        assertNotNull(taskViewModel.task.first()!!.taskId)

        assertNotNull(activity.viewModel.repository.getTaskDeliverable(
            taskViewModel.task.first()!!.taskId!!,
            deliverableId
        ).first())

        assertEquals(0,taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()?.size)
        assertEquals(1,taskViewModel.task.first()?.getDeliverablesList()?.first()?.size)

        withTag("DeliverablePickerDialog") {
            assertDoesNotExist()
        }

        assertNotNull(taskViewModel.task.first()?.getDeliverablesList()?.first()[0]?.deliverable?.deliverableId)
        deliverableId = taskViewModel.task.first()!!.getDeliverablesList().first()[0].deliverable.deliverableId!!

        withTag("TasksFragmentAddTaskViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(deliverableId)
        }

        withTag("TasksFragmentDeliverableCardViewCard-${deliverableId}") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("TasksFragmentAddTaskTaskDeliverableDeselectButton-${deliverableId}") {
            performPressWithScroll()
        }

        assertEquals(1,taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()?.size)
        assertEquals(0,taskViewModel.task.first()?.getDeliverablesList()?.first()?.size)

        withTag("TasksFragmentDeliverableCardViewCard-${deliverableId}") {
            assertDoesNotExist()
        }

        withTag("TasksFragmentAddTaskSelectDeliverableButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = activity.getString(R.string.select_deliverable) + " (${1})",
                substring = true,
                ignoreCase = false
            )
            performPressWithoutScroll()
        }

        withTag("DeliverablePickerDialog") {
            assertExists()
            assertIsDisplayed()
        }

        assertNotNull(taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()[0]?.deliverable?.deliverableId)
        deliverableId = taskViewModel.task.first()!!.getNotSelectedDeliverablesList().first()[0].deliverable.deliverableId!!

        withTag("DeliverablePickerDialogLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(deliverableId)
        }

        withTag("DeliverablePickerDialogPickButton-${deliverableId}") {
            performPressWithScroll()
        }

        assertNotNull(taskViewModel.task.first()!!.taskId)

        assertNotNull(activity.viewModel.repository.getTaskDeliverable(
            taskViewModel.task.first()!!.taskId!!,
            deliverableId
        ).first())

        assertEquals(0,taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()?.size)
        assertEquals(1,taskViewModel.task.first()?.getDeliverablesList()?.first()?.size)

        withTag("DeliverablePickerDialog") {
            assertDoesNotExist()
        }

        assertNotNull(taskViewModel.task.first()?.getDeliverablesList()?.first()[0]?.deliverable?.deliverableId)
        deliverableId = taskViewModel.task.first()!!.getDeliverablesList().first()[0].deliverable.deliverableId!!

        withTag("TasksFragmentAddTaskViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(deliverableId)
        }

        withTag("TasksFragmentDeliverableCardViewCard-${deliverableId}") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("TasksFragmentAddTaskViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddTaskEndTypeButton")
        }

        withTag("TasksFragmentAddTaskEndTypeButton") {
            performPressWithScroll()
        }

        withTag("TasksFragmentAddViewDropdownMenuItem-${Task.TaskEndType.UNDEFINED.name}") {
            performPressWithoutScroll()
        }

        assertEquals(
            emptyFlow<List<DeliverableWithLists>>(),
            taskViewModel.task.first()?.getDeliverablesList()
        )

        withTag("TasksFragmentDeliverableCardViewCard-${deliverableId}") {
            assertDoesNotExist()
        }

        withTag("TasksFragmentAddTaskEndTypeButton") {
            performPressWithScroll()
        }

        withTag("TasksFragmentAddViewDropdownMenuItem-${Task.TaskEndType.DELIVERABLE.name}") {
            performPressWithoutScroll()
        }

        assertEquals(0,taskViewModel.task.first()?.getDeliverablesList()?.first()?.size)

        withTag("TasksFragmentAddTaskSelectDeliverableButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = activity.getString(R.string.select_deliverable) + " (${1})",
                substring = true,
                ignoreCase = false
            )
            performPressWithoutScroll()
        }

        withTag("DeliverablePickerDialog") {
            assertExists()
            assertIsDisplayed()
        }

        assertNotNull(taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()[0]?.deliverable?.deliverableId)
        deliverableId = taskViewModel.task.first()!!.getNotSelectedDeliverablesList().first()[0].deliverable.deliverableId!!

        withTag("DeliverablePickerDialogLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(deliverableId)
        }

        withTag("DeliverablePickerDialogPickButton-${deliverableId}") {
            performPressWithScroll()
        }

        assertNotNull(taskViewModel.task.first()!!.taskId)

        assertNotNull(activity.viewModel.repository.getTaskDeliverable(
            taskViewModel.task.first()!!.taskId!!,
            deliverableId
        ).first())

        assertEquals(0,taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()?.size)
        assertEquals(1,taskViewModel.task.first()?.getDeliverablesList()?.first()?.size)

        withTag("DeliverablePickerDialog") {
            assertDoesNotExist()
        }

        assertNotNull(taskViewModel.task.first()?.getDeliverablesList()?.first()[0]?.deliverable?.deliverableId)
        deliverableId = taskViewModel.task.first()!!.getDeliverablesList().first()[0].deliverable.deliverableId!!

        withTag("TasksFragmentAddTaskViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(deliverableId)
        }

        withTag("TasksFragmentDeliverableCardViewCard-${deliverableId}") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("TasksFragmentAddTaskViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddTaskAddTimeBlockButton")
        }

        assertNotNull(taskViewModel.task.first()?.times)
        TimeBlockTest(
            "TasksFragmentAddTaskAddTimeBlockButton",
            {taskViewModel.task.first()!!.times.first()},
            Triple(
                instantTaskExecutorRule,
                composeTestRule,
                activity
            ),
            lazyColumnTag = "TasksFragmentAddTaskViewLazyColumn"
        ).runTests(this@TasksFragmentTests::class)

        withTag("TasksFragmentAddTaskDeleteButton").assertDoesNotExist()
        val task = taskViewModel.task.first()!!
        val deliverablesLists = getTaskDeliverablesLists(task)
        val times = task.times.first()

        withTag("TasksFragmentTaskSaveButton"){
            performPressWithoutScroll()
        }

        assertNotNull(taskViewModel.goal.first()?.goalTasks?.first())
        tasksList = taskViewModel.goal.first()!!.goalTasks.first()
        assertEquals(1,tasksList.size)

        tasksAreEqual(
            taskOne = task,
            taskTwo = tasksList[0],
            idsEqual = true,
            times = times,
            deliverablesLists = deliverablesLists
        )
    }

    @Test
    fun `13 Editing Quantity Deliverable Task`() = runMainTest {
        val activity = getTestMainActivity()

        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        val goal = taskViewModel.goal.first()!!

        var tasksList = goal.goalTasks.first()
        assertNotNull(tasksList)
        assertEquals(1, tasksList.size)

        val oldTask = tasksList[0]
        val oldTimes = oldTask.times.first()
        val oldDeliverablesLists = getTaskDeliverablesLists(oldTask)

        withTag("TasksFragmentTaskCardView-${tasksList[0].taskId}") {
            performLongPressWithScroll()
        }

        assertNotNull(taskViewModel.task.first())
        assertNotNull(taskViewModel.originalTask.value?.first())

        assertEquals(Goal.GoalTab.TASKS,taskViewModel.bottomSheetType.value)
        withTag("TasksFragmentAddTaskView"){
            assertExists()
            assertIsDisplayed()
        }

        tasksAreEqual(
            oldTask,
            taskViewModel.task.first()!!,
            idsEqual = false,
            times = oldTimes,
            deliverablesLists = oldDeliverablesLists
        )

        withTag("TasksFragmentTaskTitle"){
            assertExists()
            assertIsDisplayed()
            assertTextContains(activity.getString(R.string.edit_with_arg,activity.getString(R.string.task)))
        }

        withTag("TasksFragmentTaskSaveButton"){
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
        }

        assertEquals(2L,taskViewModel.task.first()?.quantity)
        withTag("TasksFragmentAddTaskNormalQuantityTimeTextField"){
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                "Jump 2 times",
                substring = true
            )
            performTextReplacement("Jump 3 times")
            assertTextContains(
                "Jump 3 times",
                substring = true
            )
        }
        assertEquals(3L,taskViewModel.task.first()?.quantity)

        withTag("TasksFragmentAddTaskViewLazyColumn"){
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddTaskLocationTextField")
        }

        withTag("TasksFragmentAddTaskLocationTextField") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                "At Home",
                substring = true
            )
            performTextReplacement("At Home Location")
            assertTextContains(
                "At Home Location",
                substring = true
            )
        }

        val task = taskViewModel.task.first()
        val taskTimes = task?.times?.first()
        val deliverablesLists = getTaskDeliverablesLists(task!!)

        withTag("TasksFragmentAddTaskViewLazyColumn"){
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddTaskDeleteButton")
        }

        withTag("TasksFragmentAddTaskDeleteButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
        }

        withTag("TasksFragmentTaskSaveButton"){
            performPressWithoutScroll()
        }

        assertNotNull(taskViewModel.goal.first()?.goalTasks?.first())
        tasksList = taskViewModel.goal.first()?.goalTasks?.first()!!
        assertEquals(1,tasksList.size)

        tasksAreEqual(
            task,
            tasksList[0],
            false,
            taskTimes,
            deliverablesLists = deliverablesLists
        )

        assertNotEquals(
            oldTask.task,
            tasksList[0].task
        )

        assertNotEquals(
            oldTask.location,
            tasksList[0].location
        )

        assertEquals(oldTask.taskId,tasksList[0].taskId)
    }

    @Test
    fun `14 Deleting Quantity Deliverable Task`() = runMainTest {
        val activity = getTestMainActivity()
        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        val goal = taskViewModel.goal.first()!!
        val tasksList = goal.goalTasks.first()
        assertNotNull(tasksList)
        assertEquals(1,tasksList.size)

        deleteQuantityDeliverableTask(
            goal,
            tasksList,
            activity,
            taskViewModel
        )
    }

    private fun deleteQuantityDeliverableTask(
        inputGoal: Goal,
        inputTasksList: List<Task>,
        activity: TestMainActivity,
        taskViewModel: TaskViewModel
    ) = runMainTest {
        var tasksList = inputTasksList
        var goal = inputGoal
        val taskDeliverablesList = tasksList[0].taskDeliverableList.first()
        val taskTimesList = tasksList[0].times.first()

        if (Goal.GoalTab.valueOf(goal.selectedTab) != Goal.GoalTab.TASKS) {
            switchToTab(Goal.GoalTab.TASKS, taskViewModel, activity)
            goal = taskViewModel.goal.first()!!

            assertEquals(Goal.GoalTab.TASKS, Goal.GoalTab.valueOf(goal.selectedTab))
        }

        withTag("TasksFragmentTaskCardView-${tasksList[0].taskId}") {
            performLongPressWithScroll()
        }

        withTag("TasksFragmentAddTaskViewLazyColumn"){
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddTaskDeleteButton")
        }

        withTag("TasksFragmentAddTaskDeleteButton"){
            performPressWithScroll()
        }

        tasksList = goal.goalTasks.first()
        assertNotNull(tasksList)
        assertEquals(0,tasksList.size)

        taskDeliverablesList.forEach {
            val taskDeliverable = activity.viewModel.repository.getTaskDeliverable(it.taskId,it.deliverableId)
            assertNull(taskDeliverable.first())
        }

        taskTimesList.forEach {
            assertNull(
                activity.viewModel.repository.getGoalTaskDeliverableTime(
                    it.id
                ).first()
            )
        }
    }

    @Test
    fun `15 Deleting Quantity Deliverable`() = runMainTest {
        val activity = getTestMainActivity()

        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        var goal = taskViewModel.goal.first()!!

        assertEquals(Goal.GoalTab.TASKS,Goal.GoalTab.valueOf(goal.selectedTab))

        switchToTab(Goal.GoalTab.DELIVERABLES, taskViewModel, activity)
        goal = taskViewModel.goal.first()!!

        assertEquals(Goal.GoalTab.DELIVERABLES,Goal.GoalTab.valueOf(goal.selectedTab))

        deleteQuantityDeliverable(
            inputGoal = goal,
            activity = activity,
            taskViewModel = taskViewModel
        )

        assertEquals(Goal.GoalTab.DELIVERABLES,Goal.GoalTab.valueOf(goal.selectedTab))

        switchToTab(Goal.GoalTab.TASKS, taskViewModel, activity)
        goal = taskViewModel.goal.first()!!

        assertEquals(Goal.GoalTab.TASKS,Goal.GoalTab.valueOf(goal.selectedTab))
    }

    private fun deleteQuantityDeliverable(
        inputGoal: Goal,
        activity: TestMainActivity,
        taskViewModel: TaskViewModel
    ) = runMainTest {
        var goal = inputGoal
        var deliverablesList = goal.goalDeliverables.first()
        assertNotNull(deliverablesList)
        assertEquals(1,deliverablesList.size)

        val taskDeliverablesList = deliverablesList[0].taskDeliverables
        val deliverableTimesList = deliverablesList[0].times

        if (Goal.GoalTab.valueOf(goal.selectedTab) != Goal.GoalTab.DELIVERABLES) {
            switchToTab(Goal.GoalTab.DELIVERABLES, taskViewModel, activity)
            goal = taskViewModel.goal.first()!!

            assertEquals(Goal.GoalTab.DELIVERABLES, Goal.GoalTab.valueOf(goal.selectedTab))
        }

        deliverablesList = goal.goalDeliverables.first()
        assertNotNull(deliverablesList)
        assertEquals(1,deliverablesList.size)

        withTag("TasksFragmentViewLazyColumn"){
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentDeliverableCardViewCard-${deliverablesList[0].deliverable.deliverableId}")
        }

        withTag("TasksFragmentDeliverableCardViewCard-${deliverablesList[0].deliverable.deliverableId}") {
            performLongPressWithScroll()
        }

        withTag("TasksFragmentAddDeliverableViewLazyColumn"){
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddDeliverableDeleteButton")
        }

        withTag("TasksFragmentAddDeliverableDeleteButton"){
            performPressWithScroll()
        }

        deliverablesList = goal.goalDeliverables.first()
        assertNotNull(deliverablesList)
        assertEquals(0,deliverablesList.size)

        taskDeliverablesList.forEach {
            val taskDeliverable = activity.viewModel.repository.getTaskDeliverable(it.taskId,it.deliverableId)
            assertNull(taskDeliverable.first())
        }

        deliverableTimesList.forEach {
            assertNull(
                activity.viewModel.repository.getGoalTaskDeliverableTime(it.id).first()
            )
        }
    }

    @Test
    fun `16 Adding Time Deliverable Task`() = runMainTest {
        val activity = getTestMainActivity()

        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        val goal = taskViewModel.goal.first()!!

        val tasksList = goal.goalTasks.first()
        assertNotNull(tasksList)
        assertEquals(0, tasksList.size)

        addTimeDeliverableTask(
            taskViewModel = taskViewModel,
            activity = activity
        )
    }

    private fun addTimeDeliverableTask(
        taskViewModel: TaskViewModel,
        activity: TestMainActivity
    ) = runMainTest {
        val goal = taskViewModel.goal.first()!!

        assertEquals(Goal.GoalTab.TASKS, Goal.GoalTab.valueOf(goal.selectedTab))

        withTag("TasksFragmentAddTextButton"){
            assertExists()
            assertTextContains(activity.getString(Goal.GoalTab.TASKS.addStringRes))
            performPressWithoutScroll()
        }
        assertEquals(Goal.GoalTab.TASKS,taskViewModel.bottomSheetType.value)

        withTag("TasksFragmentAddTaskView"){
            assertExists()
            assertIsDisplayed()
        }

        assertNull(taskViewModel.originalTask.value)
        assertNotNull(taskViewModel.task.first())

        withTag("TasksFragmentAddTaskViewTaskTypeButton-${Task.TaskType.TIME.name}") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            performPressWithoutScroll()
            assertEquals(
                Color.Blue,
                fetchSemanticsNode().config[BackgroundColorKey]
            )
            assertTextContains(activity.getString(Task.TaskType.TIME.stringRes))
        }

        withTag("TasksFragmentAddTaskViewTaskTypeButton-${Task.TaskType.NORMAL.name}") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            assertEquals(
                Color.LightGray,
                fetchSemanticsNode().config[BackgroundColorKey]
            )
            assertTextContains(activity.getString(Task.TaskType.NORMAL.stringRes))
        }

        withTag("TasksFragmentAddTaskViewTaskTypeButton-${Task.TaskType.QUANTITY.name}") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            assertEquals(
                Color.LightGray,
                fetchSemanticsNode().config[BackgroundColorKey]
            )
            assertTextContains(activity.getString(Task.TaskType.QUANTITY.stringRes))
        }

        assertEquals(
            Task.TaskType.TIME.name,
            taskViewModel.task.first()?.type
        )

        withTag("TasksFragmentAddTaskNormalQuantityTimeTextField") {
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
        val newTime = Converters().toLocalDateTime(taskViewModel.task.first()?.time).value
        assertNotNull(newTime)
        assertEquals(2,newTime!!.hour)
        assertEquals(30, newTime.minute)

        withTag("TasksFragmentAddTaskLocationTextField") {
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

        withTag("TasksFragmentAddTaskViewLazyColumn"){
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddTaskColourBoxAndButton")
        }

        assertNotNull(taskViewModel.task.first()?.colour)
        val taskColour = taskViewModel.task.first()!!.colour
        withTag("TasksFragmentAddTaskColourBox") {
            if (taskColour == -1) assertDoesNotExist()
            else{
                assertExists()
                performScrollTo()
                assertIsDisplayed()
                assertEquals(
                    taskColour,
                    fetchSemanticsNode().config[BackgroundColorKey].toArgb()
                )
            }
        }

        withTag("TasksFragmentAddTaskColourButton") {
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

        assertNotNull(taskViewModel.task.first()?.colour)
        assertNotEquals(-1, taskViewModel.task.first()?.colour)
        withTag("TasksFragmentAddTaskColourBox") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertEquals(
                taskColour,
                fetchSemanticsNode().config[BackgroundColorKey].toArgb()
            )
        }

        withTag("TasksFragmentAddTaskEndTypeButton") {
            performPressWithScroll()
        }

        withTag("TasksFragmentAddViewDropdownMenuItem-${Task.TaskEndType.DELIVERABLE.name}") {
            performPressWithoutScroll()
        }

        withTag("TasksFragmentAddTaskViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddTaskModifyDeliverableButtons")
        }

        withTag("TasksFragmentAddTaskAddDeliverableButton") {
            performPressWithScroll()
        }

        assertNotNull(taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()[0])

        withTag("DeliverableAdderDialog") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("TasksFragmentTaskDeliverableWorkInputTextField") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("Run an accumulated total of")
            assertTextContains(
                value = "Run an accumulated total of",
                substring = true,
                ignoreCase = false
            )
            finishProcesses()
        }

        withTag("TasksFragmentNormalQuantityTimeTextFieldTimeTimeButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            performPressWithoutScroll()
        }

        withTag("EditGoalTimeInputDurationPickerUnlimitedDialog"){
            assertExists()
            assertIsDisplayed()
        }

        withTag("EditGoalDurationPickerHourTextField") {
            assertExists()
            assertIsDisplayed()
            assertTextContains(
                value = "0",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("100")
            assertTextContains(
                value = "100",
                substring = true,
                ignoreCase = false
            )
        }
        finishProcesses()

        withTag("EditGoalDurationPickerMinuteTextField") {
            assertExists()
            assertIsDisplayed()
            assertTextContains(
                value = "0",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("121")
            assertTextContains(
                value = "121",
                substring = true,
                ignoreCase = false
            )
        }
        finishProcesses()

        withTag("EditGoalDurationPickerUnlimitedDurationText") {
            assertExists()
            assertIsDisplayed()
            assertTextContains(
                value = "102 Hours and 1 Minute",
                substring = true,
                ignoreCase = false
            )
        }

        withTag("EditGoalDurationPickerUnlimitedSaveAndCloseIconButton") {
            assertExists()
            assertIsDisplayed()
            performPressWithoutScroll()
        }

        withTag("EditGoalTimeInputDurationPickerUnlimitedDialog"){
            assertDoesNotExist()
        }

        withTag("TasksFragmentNormalQuantityTimeTextFieldTimeTimeButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            assertTextContains(
                value = "102 Hours and 1 Minute",
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
            performTextReplacement(".")
            assertTextContains(
                value = ".",
                substring = true,
                ignoreCase = false
            )
            assertTextContains(
                value = "Run an accumulated total of 102 Hours and 1 Minute .",
                substring = true,
                ignoreCase = false
            )
        }
        finishProcesses()

        assertEquals(1,taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()?.size)

        var deliverable = taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()[0]
        assertNotNull(deliverable)
        var deliverableDuration = Duration.between(
            Converters().toLocalDateTime(deliverable!!.deliverable.initialDateTime).value,
            Converters().toLocalDateTime(deliverable.deliverable.time).value
        )
        assertEquals(
            102L,
            deliverableDuration.toHours()
        )
        assertEquals(
            1L,
            deliverableDuration.toMinutes() - deliverableDuration.toHours()*60
        )

        withTag("DeliverableAdderDialogDismissButton") {
            performPressWithScroll()
        }

        assertEquals(0,taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()?.size)

        withTag("DeliverableAdderDialog") {
            assertDoesNotExist()
        }

        withTag("TasksFragmentAddTaskAddDeliverableButton") {
            performPressWithScroll()
        }

        assertNotNull(taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()[0])

        withTag("DeliverableAdderDialog") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("TasksFragmentTaskDeliverableWorkInputTextField") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("Run an accumulated total of")
            assertTextContains(
                value = "Run an accumulated total of",
                substring = true,
                ignoreCase = false
            )
            finishProcesses()
        }

        withTag("TasksFragmentNormalQuantityTimeTextFieldTimeTimeButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            performPressWithoutScroll()
        }

        withTag("EditGoalTimeInputDurationPickerUnlimitedDialog"){
            assertExists()
            assertIsDisplayed()
        }

        withTag("EditGoalDurationPickerHourTextField") {
            assertExists()
            assertIsDisplayed()
            assertTextContains(
                value = "0",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("100")
            assertTextContains(
                value = "100",
                substring = true,
                ignoreCase = false
            )
        }
        finishProcesses()

        withTag("EditGoalDurationPickerMinuteTextField") {
            assertExists()
            assertIsDisplayed()
            assertTextContains(
                value = "0",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("121")
            assertTextContains(
                value = "121",
                substring = true,
                ignoreCase = false
            )
        }
        finishProcesses()

        withTag("EditGoalDurationPickerUnlimitedDurationText") {
            assertExists()
            assertIsDisplayed()
            assertTextContains(
                value = "102 Hours and 1 Minute",
                substring = true,
                ignoreCase = false
            )
        }

        withTag("EditGoalDurationPickerUnlimitedSaveAndCloseIconButton") {
            assertExists()
            assertIsDisplayed()
            performPressWithoutScroll()
        }

        withTag("EditGoalTimeInputDurationPickerUnlimitedDialog"){
            assertDoesNotExist()
        }

        withTag("TasksFragmentNormalQuantityTimeTextFieldTimeTimeButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            assertTextContains(
                value = "102 Hours and 1 Minute",
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
            performTextReplacement(".")
            assertTextContains(
                value = ".",
                substring = true,
                ignoreCase = false
            )
            assertTextContains(
                value = "Run an accumulated total of 102 Hours and 1 Minute .",
                substring = true,
                ignoreCase = false
            )
        }
        finishProcesses()

        assertEquals(1,taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()?.size)

        deliverable = taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()[0]
        assertNotNull(deliverable)
        deliverableDuration = Duration.between(
            Converters().toLocalDateTime(deliverable!!.deliverable.initialDateTime).value,
            Converters().toLocalDateTime(deliverable.deliverable.time).value
        )
        assertEquals(
            102L,
            deliverableDuration.toHours()
        )
        assertEquals(
            1L,
            deliverableDuration.toMinutes() - deliverableDuration.toHours()*60
        )

        withTag("DeliverableAdderDialogOKButton") {
            performPressWithScroll()
        }

        assertEquals(1,taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()?.size)

        withTag("DeliverableAdderDialog") {
            assertDoesNotExist()
        }

        withTag("TasksFragmentAddTaskSelectDeliverableButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = activity.getString(R.string.select_deliverable) + " (${1})",
                substring = true,
                ignoreCase = false
            )
            performPressWithoutScroll()
        }

        withTag("DeliverablePickerDialog") {
            assertExists()
            assertIsDisplayed()
        }

        assertNotNull(taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()[0]?.deliverable?.deliverableId)
        var deliverableId = taskViewModel.task.first()!!.getNotSelectedDeliverablesList().first()[0].deliverable.deliverableId!!

        withTag("DeliverablePickerDialogLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(deliverableId)
        }

        withTag("DeliverablePickerDialogPickButton-${deliverableId}") {
            performPressWithScroll()
        }

        assertNotNull(taskViewModel.task.first()!!.taskId)

        assertNotNull(activity.viewModel.repository.getTaskDeliverable(
            taskViewModel.task.first()!!.taskId!!,
            deliverableId
        ).first())

        assertEquals(0,taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()?.size)
        assertEquals(1,taskViewModel.task.first()?.getDeliverablesList()?.first()?.size)

        withTag("DeliverablePickerDialog") {
            assertDoesNotExist()
        }

        assertNotNull(taskViewModel.task.first()?.getDeliverablesList()?.first()[0]?.deliverable?.deliverableId)
        deliverableId = taskViewModel.task.first()!!.getDeliverablesList().first()[0].deliverable.deliverableId!!

        withTag("TasksFragmentAddTaskViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(deliverableId)
        }

        withTag("TasksFragmentDeliverableCardViewCard-${deliverableId}") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("TasksFragmentAddTaskTaskDeliverableDeselectButton-${deliverableId}") {
            performPressWithScroll()
        }

        assertEquals(1,taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()?.size)
        assertEquals(0,taskViewModel.task.first()?.getDeliverablesList()?.first()?.size)

        withTag("TasksFragmentDeliverableCardViewCard-${deliverableId}") {
            assertDoesNotExist()
        }

        withTag("TasksFragmentAddTaskSelectDeliverableButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = activity.getString(R.string.select_deliverable) + " (${1})",
                substring = true,
                ignoreCase = false
            )
            performPressWithoutScroll()
        }

        withTag("DeliverablePickerDialog") {
            assertExists()
            assertIsDisplayed()
        }

        assertNotNull(taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()[0]?.deliverable?.deliverableId)
        deliverableId = taskViewModel.task.first()!!.getNotSelectedDeliverablesList().first()[0].deliverable.deliverableId!!

        withTag("DeliverablePickerDialogLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(deliverableId)
        }

        withTag("DeliverablePickerDialogPickButton-${deliverableId}") {
            performPressWithScroll()
        }

        assertNotNull(taskViewModel.task.first()!!.taskId)

        assertNotNull(activity.viewModel.repository.getTaskDeliverable(
            taskViewModel.task.first()!!.taskId!!,
            deliverableId
        ).first())

        assertEquals(0,taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()?.size)
        assertEquals(1,taskViewModel.task.first()?.getDeliverablesList()?.first()?.size)

        withTag("DeliverablePickerDialog") {
            assertDoesNotExist()
        }

        assertNotNull(taskViewModel.task.first()?.getDeliverablesList()?.first()[0]?.deliverable?.deliverableId)
        deliverableId = taskViewModel.task.first()!!.getDeliverablesList().first()[0].deliverable.deliverableId!!

        withTag("TasksFragmentAddTaskViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(deliverableId)
        }

        withTag("TasksFragmentDeliverableCardViewCard-${deliverableId}") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("TasksFragmentAddTaskViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddTaskEndTypeButton")
        }

        withTag("TasksFragmentAddTaskEndTypeButton") {
            performPressWithScroll()
        }

        withTag("TasksFragmentAddViewDropdownMenuItem-${Task.TaskEndType.UNDEFINED.name}") {
            performPressWithoutScroll()
        }

        assertEquals(
            emptyFlow<List<DeliverableWithLists>>(),
            taskViewModel.task.first()?.getDeliverablesList()
        )

        withTag("TasksFragmentDeliverableCardViewCard-${deliverableId}") {
            assertDoesNotExist()
        }

        withTag("TasksFragmentAddTaskEndTypeButton") {
            performPressWithScroll()
        }

        withTag("TasksFragmentAddViewDropdownMenuItem-${Task.TaskEndType.DELIVERABLE.name}") {
            performPressWithoutScroll()
        }

        assertEquals(0,taskViewModel.task.first()?.getDeliverablesList()?.first()?.size)

        withTag("TasksFragmentAddTaskSelectDeliverableButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = activity.getString(R.string.select_deliverable) + " (${1})",
                substring = true,
                ignoreCase = false
            )
            performPressWithoutScroll()
        }

        withTag("DeliverablePickerDialog") {
            assertExists()
            assertIsDisplayed()
        }

        assertNotNull(taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()[0]?.deliverable?.deliverableId)
        deliverableId = taskViewModel.task.first()!!.getNotSelectedDeliverablesList().first()[0].deliverable.deliverableId!!

        withTag("DeliverablePickerDialogLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(deliverableId)
        }

        withTag("DeliverablePickerDialogPickButton-${deliverableId}") {
            performPressWithScroll()
        }

        assertNotNull(taskViewModel.task.first()!!.taskId)

        assertNotNull(activity.viewModel.repository.getTaskDeliverable(
            taskViewModel.task.first()!!.taskId!!,
            deliverableId
        ).first())

        assertEquals(0,taskViewModel.task.first()?.getNotSelectedDeliverablesList()?.first()?.size)
        assertEquals(1,taskViewModel.task.first()?.getDeliverablesList()?.first()?.size)

        withTag("DeliverablePickerDialog") {
            assertDoesNotExist()
        }

        assertNotNull(taskViewModel.task.first()?.getDeliverablesList()?.first()[0]?.deliverable?.deliverableId)
        deliverableId = taskViewModel.task.first()!!.getDeliverablesList().first()[0].deliverable.deliverableId!!

        withTag("TasksFragmentAddTaskViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(deliverableId)
        }

        withTag("TasksFragmentDeliverableCardViewCard-${deliverableId}") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("TasksFragmentAddTaskViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddTaskAddTimeBlockButton")
        }

        assertNotNull(taskViewModel.task.first()?.times)
        TimeBlockTest(
            "TasksFragmentAddTaskAddTimeBlockButton",
            {taskViewModel.task.first()!!.times.first()},
            Triple(
                instantTaskExecutorRule,
                composeTestRule,
                activity
            ),
            lazyColumnTag = "TasksFragmentAddTaskViewLazyColumn"
        ).runTests(this@TasksFragmentTests::class)

        withTag("TasksFragmentAddTaskDeleteButton").assertDoesNotExist()
        val task = taskViewModel.task.first()!!
        val deliverablesLists = getTaskDeliverablesLists(task)
        val times = task.times.first()

        withTag("TasksFragmentTaskSaveButton"){
            performPressWithoutScroll()
        }

        assertNotNull(taskViewModel.goal.first()?.goalTasks?.first())
        val tasksList = taskViewModel.goal.first()!!.goalTasks.first()
        assertEquals(1,tasksList.size)

        tasksAreEqual(
            taskOne = task,
            taskTwo = tasksList[0],
            idsEqual = true,
            times = times,
            deliverablesLists = deliverablesLists
        )
    }

    @Test
    fun `17 Editing Time Deliverable Task`() = runMainTest {
        val activity = getTestMainActivity()

        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        val goal = taskViewModel.goal.first()!!

        var tasksList = goal.goalTasks.first()
        assertNotNull(tasksList)
        assertEquals(1, tasksList.size)

        val oldTask = tasksList[0]
        val oldTimes = oldTask.times.first()
        val oldDeliverablesLists = getTaskDeliverablesLists(oldTask)

        withTag("TasksFragmentTaskCardView-${tasksList[0].taskId}") {
            performLongPressWithScroll()
        }

        assertNotNull(taskViewModel.task.first())
        assertNotNull(taskViewModel.originalTask.value?.first())

        assertEquals(Goal.GoalTab.TASKS,taskViewModel.bottomSheetType.value)
        withTag("TasksFragmentAddTaskView"){
            assertExists()
            assertIsDisplayed()
        }

        tasksAreEqual(
            oldTask,
            taskViewModel.task.first()!!,
            idsEqual = false,
            times = oldTimes,
            deliverablesLists = oldDeliverablesLists
        )

        withTag("TasksFragmentTaskTitle"){
            assertExists()
            assertIsDisplayed()
            assertTextContains(activity.getString(R.string.edit_with_arg,activity.getString(R.string.task)))
        }

        withTag("TasksFragmentTaskSaveButton"){
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
        }

        withTag("TasksFragmentAddTaskNormalQuantityTimeTextField") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = "Run a total of",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("Walk a total of")
            assertTextContains(
                value = "Walk a total of",
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
                action(4F)
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
                action(44F)
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
                value = "4 Hours and 44 Minutes",
                substring = true,
                ignoreCase = false
            )
        }

        withTag("TasksFragmentNormalQuantityTimeTextFieldTimeSecondField") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = "in the snow.",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("in the sand.")
            assertTextContains(
                value = "in the sand.",
                substring = true,
                ignoreCase = false
            )
            assertTextContains(
                value = "Walk a total of 4 Hours and 44 Minutes in the sand.",
                substring = true,
                ignoreCase = false
            )
        }
        val newTime = Converters().toLocalDateTime(taskViewModel.task.first()?.time).value
        assertNotNull(newTime)
        assertEquals(4,newTime!!.hour)
        assertEquals(44, newTime.minute)

        withTag("TasksFragmentAddTaskViewLazyColumn"){
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddTaskLocationTextField")
        }

        withTag("TasksFragmentAddTaskLocationTextField") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                "At Home",
                substring = true
            )
            performTextReplacement("At Home Location")
            assertTextContains(
                "At Home Location",
                substring = true
            )
        }

        val deliverable = taskViewModel.task.first()?.getDeliverablesList()?.first()[0]
        assertNotNull(deliverable)
        val deliverableDuration = Duration.between(
            Converters().toLocalDateTime(deliverable!!.deliverable.initialDateTime).value,
            Converters().toLocalDateTime(deliverable.deliverable.time).value
        )
        assertEquals(
            102L,
            deliverableDuration.toHours()
        )
        assertEquals(
            1L,
            deliverableDuration.toMinutes() - deliverableDuration.toHours()*60
        )

        assertNotNull(taskViewModel.task.first()?.getDeliverablesList()?.first()[0]?.deliverable?.deliverableId)
        val deliverableId = taskViewModel.task.first()!!.getDeliverablesList().first()[0].deliverable.deliverableId!!

        withTag("TasksFragmentAddTaskViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(deliverableId)
        }

        withTag("TasksFragmentDeliverableCardViewCard-${deliverableId}") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            performPressWithoutScroll()
        }

        assertNotNull(taskViewModel.task.first()?.getDeliverablesList()?.first()[0])

        withTag("DeliverableAdderDialog") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("TasksFragmentTaskDeliverableWorkInputTextField") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = "Run an accumulated total of",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("Walk an accumulated total of")
            assertTextContains(
                value = "Walk an accumulated total of",
                substring = true,
                ignoreCase = false
            )
            finishProcesses()
        }

        withTag("TasksFragmentNormalQuantityTimeTextFieldTimeTimeButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            performPressWithoutScroll()
        }

        withTag("EditGoalTimeInputDurationPickerUnlimitedDialog"){
            assertExists()
            assertIsDisplayed()
        }

        withTag("EditGoalDurationPickerHourTextField") {
            assertExists()
            assertIsDisplayed()
            assertTextContains(
                value = "102",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("0")
            assertTextContains(
                value = "0",
                substring = true,
                ignoreCase = false
            )
        }
        finishProcesses()

        withTag("EditGoalDurationPickerMinuteTextField") {
            assertExists()
            assertIsDisplayed()
            assertTextContains(
                value = "1",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("62")
            assertTextContains(
                value = "62",
                substring = true,
                ignoreCase = false
            )
        }
        finishProcesses()

        withTag("EditGoalDurationPickerUnlimitedDurationText") {
            assertExists()
            assertIsDisplayed()
            assertTextContains(
                value = "1 Hour and 2 Minutes",
                substring = true,
                ignoreCase = false
            )
        }

        withTag("EditGoalDurationPickerUnlimitedSaveAndCloseIconButton") {
            assertExists()
            assertIsDisplayed()
            performPressWithoutScroll()
        }

        withTag("EditGoalTimeInputDurationPickerUnlimitedDialog"){
            assertDoesNotExist()
        }

        withTag("TasksFragmentNormalQuantityTimeTextFieldTimeTimeButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            assertTextContains(
                value = "1 Hour and 2 Minutes",
                substring = true,
                ignoreCase = false
            )
        }

        withTag("TasksFragmentNormalQuantityTimeTextFieldTimeSecondField") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(
                value = ".",
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
                value = "Walk an accumulated total of 1 Hour and 2 Minutes",
                substring = true,
                ignoreCase = false
            )
        }
        finishProcesses()

        withTag("DeliverableAdderDialog") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("DeliverableAdderDialogOKButton") {
            performPressWithScroll()
        }

        assertEquals(1,taskViewModel.task.first()?.getDeliverablesList()?.first()?.size)

        withTag("DeliverableAdderDialog") {
            assertDoesNotExist()
        }

        val task = taskViewModel.task.first()
        val taskTimes = task?.times?.first()
        val deliverablesLists = getTaskDeliverablesLists(task!!)

        withTag("TasksFragmentAddTaskViewLazyColumn"){
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddTaskDeleteButton")
        }

        withTag("TasksFragmentAddTaskDeleteButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
        }

        withTag("TasksFragmentTaskSaveButton"){
            performPressWithoutScroll()
        }

        assertNotNull(taskViewModel.goal.first()?.goalTasks?.first())
        tasksList = taskViewModel.goal.first()?.goalTasks?.first()!!
        assertEquals(1,tasksList.size)

        tasksAreEqual(
            task,
            tasksList[0],
            false,
            taskTimes,
            deliverablesLists = deliverablesLists
        )

        assertNotEquals(
            oldTask.task,
            tasksList[0].task
        )

        assertNotEquals(
            oldTask.location,
            tasksList[0].location
        )

        assertEquals(oldTask.taskId,tasksList[0].taskId)
    }

    @Test
    fun `18 Deleting Time Deliverable Task`() = runMainTest {
        val activity = getTestMainActivity()
        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        val goal = taskViewModel.goal.first()!!
        var tasksList = goal.goalTasks.first()
        assertNotNull(tasksList)
        assertEquals(1,tasksList.size)

        val taskDeliverablesList = tasksList[0].taskDeliverableList.first()
        val taskTimesList = tasksList[0].times.first()

        withTag("TasksFragmentTaskCardView-${tasksList[0].taskId}") {
            performLongPressWithScroll()
        }

        withTag("TasksFragmentAddTaskViewLazyColumn"){
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddTaskDeleteButton")
        }

        withTag("TasksFragmentAddTaskDeleteButton"){
            performPressWithScroll()
        }

        tasksList = goal.goalTasks.first()
        assertNotNull(tasksList)
        assertEquals(0,tasksList.size)

        taskDeliverablesList.forEach {
            val taskDeliverable = activity.viewModel.repository.getTaskDeliverable(it.taskId,it.deliverableId)
            assertNull(taskDeliverable.first())
        }

        taskTimesList.forEach {
            assertNull(
                activity.viewModel.repository.getGoalTaskDeliverableTime(
                    it.id
                ).first()
            )
        }
    }

    @Test
    fun `19 Deleting Time Deliverable`() = runMainTest {
        val activity = getTestMainActivity()

        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        var goal = taskViewModel.goal.first()!!

        assertEquals(Goal.GoalTab.TASKS,Goal.GoalTab.valueOf(goal.selectedTab))

        switchToTab(Goal.GoalTab.DELIVERABLES, taskViewModel, activity)
        goal = taskViewModel.goal.first()!!

        assertEquals(Goal.GoalTab.DELIVERABLES,Goal.GoalTab.valueOf(goal.selectedTab))

        var deliverablesList = goal.goalDeliverables.first()
        assertNotNull(deliverablesList)
        assertEquals(1,deliverablesList.size)

        val taskDeliverablesList = deliverablesList[0].taskDeliverables
        val deliverableTimesList = deliverablesList[0].times

        withTag("TasksFragmentDeliverableCardViewCard-${deliverablesList[0].deliverable.deliverableId}") {
            performLongPressWithScroll()
        }

        withTag("TasksFragmentAddDeliverableViewLazyColumn"){
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddDeliverableDeleteButton")
        }

        withTag("TasksFragmentAddDeliverableDeleteButton"){
            performPressWithScroll()
        }

        deliverablesList = goal.goalDeliverables.first()
        assertNotNull(deliverablesList)
        assertEquals(0,deliverablesList.size)

        taskDeliverablesList.forEach {
            val taskDeliverable = activity.viewModel.repository.getTaskDeliverable(it.taskId,it.deliverableId)
            assertNull(taskDeliverable.first())
        }

        deliverableTimesList.forEach {
            assertNull(
                activity.viewModel.repository.getGoalTaskDeliverableTime(it.id).first()
            )
        }

        assertEquals(Goal.GoalTab.DELIVERABLES,Goal.GoalTab.valueOf(goal.selectedTab))

        switchToTab(Goal.GoalTab.TASKS, taskViewModel, activity)
        goal = taskViewModel.goal.first()!!

        assertEquals(Goal.GoalTab.TASKS,Goal.GoalTab.valueOf(goal.selectedTab))
    }

    @Test
    fun `20 Changing Deliverable Type From Quantity Task Warning Dialog`() = runMainTest {
        val activity = getTestMainActivity()
        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        var goal = taskViewModel.goal.first()!!

        addQuantityDeliverableTask(
            activity,
            taskViewModel,
            goal
        )

        assertEquals(Goal.GoalTab.TASKS,Goal.GoalTab.valueOf(goal.selectedTab))

        switchToTab(Goal.GoalTab.DELIVERABLES, taskViewModel, activity)
        goal = taskViewModel.goal.first()!!

        assertEquals(Goal.GoalTab.DELIVERABLES,Goal.GoalTab.valueOf(goal.selectedTab))

        var tasksList = goal.goalTasks.first()
        assertNotNull(tasksList)
        assertEquals(1,tasksList.size)

        var taskTaskDeliverablesList = tasksList[0].taskDeliverableList.first()
        assertNotNull(taskTaskDeliverablesList)
        assertEquals(1,taskTaskDeliverablesList.size)

        var deliverablesList = goal.goalDeliverables.first()
        assertNotNull(deliverablesList)
        assertEquals(1,deliverablesList.size)

        var taskDeliverablesList = deliverablesList[0].taskDeliverables

        assertNotNull(taskDeliverablesList)
        assertEquals(1,taskDeliverablesList.size)

        assertEquals(
            TaskDeliverable.WorkType.QuantityTaskOnce.name,
            taskDeliverablesList.first().workType
        )

        withTag("TasksFragmentDeliverableCardViewCard-${deliverablesList[0].deliverable.deliverableId}") {
            performLongPressWithScroll()
        }

        deliverablesList = goal.goalDeliverables.first()
        assertNotNull(deliverablesList)
        assertEquals(2,deliverablesList.size)

        taskDeliverablesList = deliverablesList[1].taskDeliverables

        assertEquals(
            1,
            taskDeliverablesList.size
        )

        withTag("TasksFragmentAddDeliverableViewLazyColumn"){
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddDeliverableNQTButtons")
        }

        testTaskTypeButtons(
            Task.TaskType.NORMAL,
            selected = false,
            press = false,
            activity
        )

        testTaskTypeButtons(
            Task.TaskType.QUANTITY,
            selected = true,
            press = false,
            activity
        )

        testTaskTypeButtons(
            Task.TaskType.TIME,
            selected = false,
            press = false,
            activity
        )

        assertEquals(
            Deliverable.DeliverableEndType.WORK.name,
            deliverablesList[1].deliverable.endType
        )

        assertEquals(
            Task.TaskType.QUANTITY.name,
            deliverablesList[1].deliverable.type
        )

        assertEquals(
            TaskDeliverable.WorkType.QuantityTaskOnce.name,
            deliverablesList[1].deliverable.workType
        )

        testTaskTypeButtons(
            Task.TaskType.TIME,
            selected = false,
            press = true,
            activity
        )

        withTag("DeliverableWarningDialog") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("DeliverableWarningDialogLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("${taskDeliverablesList.first().taskId}:${taskDeliverablesList.first().deliverableId}")
        }

        withTag("TasksFragmentTaskCardView-${taskDeliverablesList.first().taskId}") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("DeliverableWarningDialogOKButton") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
        }

        withTag("DeliverableWarningDialogDismissButton") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            performPressWithoutScroll()
        }

        testTaskTypeButtons(
            Task.TaskType.TIME,
            selected = false,
            press = false,
            activity
        )

        testTaskTypeButtons(
            Task.TaskType.QUANTITY,
            selected = true,
            press = false,
            activity
        )

        assertEquals(
            Task.TaskType.QUANTITY.name,
            deliverablesList[1].deliverable.type
        )

        testTaskTypeButtons(
            Task.TaskType.NORMAL,
            selected = false,
            press = true,
            activity
        )

        withTag("DeliverableWarningDialog") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("DeliverableWarningDialogLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("${taskDeliverablesList.first().taskId}:${taskDeliverablesList.first().deliverableId}")
        }

        withTag("TasksFragmentTaskCardView-${taskDeliverablesList.first().taskId}") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("DeliverableWarningDialogDismissButton") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
        }

        withTag("DeliverableWarningDialogOKButton") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            performPressWithoutScroll()
        }

        testTaskTypeButtons(
            Task.TaskType.QUANTITY,
            selected = false,
            press = false,
            activity
        )

        testTaskTypeButtons(
            Task.TaskType.NORMAL,
            selected = true,
            press = false,
            activity
        )

        testTaskTypeButtons(
            Task.TaskType.TIME,
            selected = false,
            press = false,
            activity
        )

        deliverablesList = goal.goalDeliverables.first()
        assertNotNull(deliverablesList)
        assertEquals(2,deliverablesList.size)

        taskDeliverablesList = deliverablesList[1].taskDeliverables

        assertEquals(
            0,
            taskDeliverablesList.size
        )

        assertEquals(
            Deliverable.DeliverableEndType.WORK.name,
            deliverablesList[1].deliverable.endType
        )

        assertEquals(
            Task.TaskType.NORMAL.name,
            deliverablesList[1].deliverable.type
        )

        assertEquals(
            TaskDeliverable.WorkType.CompleteTasks.name,
            deliverablesList[1].deliverable.workType
        )

        testTaskTypeButtons(
            Task.TaskType.QUANTITY,
            selected = false,
            press = true,
            activity
        )

        withTag("DeliverableWarningDialog") {
            assertDoesNotExist()
            assertIsNotDisplayed()
        }

        testTaskTypeButtons(
            Task.TaskType.NORMAL,
            selected = false,
            press = false,
            activity
        )

        testTaskTypeButtons(
            Task.TaskType.QUANTITY,
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
            assertIsNotDisplayed()
        }

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

        testTaskTypeButtons(
            Task.TaskType.QUANTITY,
            selected = false,
            press = true,
            activity
        )

        goal = taskViewModel.goal.first()!!
        tasksList = goal.goalTasks.first()
        assertNotNull(tasksList)
        assertEquals(1,tasksList.size)

        taskTaskDeliverablesList = tasksList[0].taskDeliverableList.first()
        assertNotNull(taskTaskDeliverablesList)
        assertEquals(1,taskTaskDeliverablesList.size)

        deliverablesList = goal.goalDeliverables.first()
        assertNotNull(deliverablesList)
        assertEquals(2,deliverablesList.size)

        taskDeliverablesList = deliverablesList[0].taskDeliverables

        assertNotNull(taskDeliverablesList)
        assertEquals(1,taskDeliverablesList.size)

        taskDeliverablesList = deliverablesList[1].taskDeliverables

        assertNotNull(taskDeliverablesList)
        assertEquals(0,taskDeliverablesList.size)

        withTag("TasksFragmentDeliverableBackButton") {
            performPressWithoutScroll()
        }

        goal = taskViewModel.goal.first()!!
        tasksList = goal.goalTasks.first()
        assertNotNull(tasksList)
        assertEquals(1,tasksList.size)

        taskTaskDeliverablesList = tasksList[0].taskDeliverableList.first()
        assertNotNull(taskTaskDeliverablesList)
        assertEquals(1,taskTaskDeliverablesList.size)

        deliverablesList = goal.goalDeliverables.first()
        assertNotNull(deliverablesList)
        assertEquals(1,deliverablesList.size)

        taskDeliverablesList = deliverablesList[0].taskDeliverables

        assertNotNull(taskDeliverablesList)
        assertEquals(1,taskDeliverablesList.size)

        deleteQuantityDeliverableTask(
            inputGoal = goal,
            inputTasksList = tasksList,
            activity = activity,
            taskViewModel = taskViewModel
        )

        goal = taskViewModel.goal.first()!!

        assertEquals(Goal.GoalTab.TASKS,Goal.GoalTab.valueOf(goal.selectedTab))

        switchToTab(Goal.GoalTab.DELIVERABLES, taskViewModel, activity)
        goal = taskViewModel.goal.first()!!

        assertEquals(Goal.GoalTab.DELIVERABLES,Goal.GoalTab.valueOf(goal.selectedTab))

        tasksList = goal.goalTasks.first()
        assertNotNull(tasksList)
        assertEquals(0,tasksList.size)

        taskTaskDeliverablesList = emptyList()
        assertNotNull(taskTaskDeliverablesList)

        deliverablesList = goal.goalDeliverables.first()
        assertNotNull(deliverablesList)
        assertEquals(1,deliverablesList.size)

        taskDeliverablesList = deliverablesList[0].taskDeliverables

        assertNotNull(taskDeliverablesList)
        assertEquals(0,taskDeliverablesList.size)

        deleteQuantityDeliverable(
            inputGoal = goal,
            activity = activity,
            taskViewModel = taskViewModel
        )

        deliverablesList = goal.goalDeliverables.first()
        assertNotNull(deliverablesList)
        assertEquals(0,deliverablesList.size)
    }

    @Test
    fun `21 Changing Deliverable Type From Time Task Warning Dialog`() = runMainTest {
        val activity = getTestMainActivity()
        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        var goal = taskViewModel.goal.first()!!

        assertEquals(Goal.GoalTab.DELIVERABLES,Goal.GoalTab.valueOf(goal.selectedTab))

        switchToTab(Goal.GoalTab.TASKS, taskViewModel, activity)
        goal = taskViewModel.goal.first()!!

        assertEquals(Goal.GoalTab.TASKS,Goal.GoalTab.valueOf(goal.selectedTab))

        addTimeDeliverableTask(
            taskViewModel,
            activity
        )

        assertEquals(Goal.GoalTab.TASKS,Goal.GoalTab.valueOf(goal.selectedTab))

        switchToTab(Goal.GoalTab.DELIVERABLES, taskViewModel, activity)
        goal = taskViewModel.goal.first()!!

        assertEquals(Goal.GoalTab.DELIVERABLES,Goal.GoalTab.valueOf(goal.selectedTab))

        var tasksList = goal.goalTasks.first()
        assertNotNull(tasksList)
        assertEquals(1,tasksList.size)

        var taskTaskDeliverablesList = tasksList[0].taskDeliverableList.first()
        assertNotNull(taskTaskDeliverablesList)
        assertEquals(1,taskTaskDeliverablesList.size)

        var deliverablesList = goal.goalDeliverables.first()
        assertNotNull(deliverablesList)
        assertEquals(1,deliverablesList.size)

        var taskDeliverablesList = deliverablesList[0].taskDeliverables

        assertNotNull(taskDeliverablesList)
        assertEquals(1,taskDeliverablesList.size)

        assertEquals(
            TaskDeliverable.WorkType.TimeTaskOnce.name,
            taskDeliverablesList.first().workType
        )

        withTag("TasksFragmentDeliverableCardViewCard-${deliverablesList[0].deliverable.deliverableId}") {
            performLongPressWithScroll()
        }

        deliverablesList = goal.goalDeliverables.first()
        assertNotNull(deliverablesList)
        assertEquals(2,deliverablesList.size)

        taskDeliverablesList = deliverablesList[1].taskDeliverables

        assertEquals(
            1,
            taskDeliverablesList.size
        )

        withTag("TasksFragmentAddDeliverableViewLazyColumn"){
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddDeliverableNQTButtons")
        }

        testTaskTypeButtons(
            Task.TaskType.NORMAL,
            selected = false,
            press = false,
            activity
        )

        testTaskTypeButtons(
            Task.TaskType.QUANTITY,
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

        assertEquals(
            Deliverable.DeliverableEndType.WORK.name,
            deliverablesList[1].deliverable.endType
        )

        assertEquals(
            Task.TaskType.TIME.name,
            deliverablesList[1].deliverable.type
        )

        assertEquals(
            TaskDeliverable.WorkType.TimeTaskOnce.name,
            deliverablesList[1].deliverable.workType
        )

        testTaskTypeButtons(
            Task.TaskType.QUANTITY,
            selected = false,
            press = true,
            activity
        )

        withTag("DeliverableWarningDialog") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("DeliverableWarningDialogLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("${taskDeliverablesList.first().taskId}:${taskDeliverablesList.first().deliverableId}")
        }

        withTag("TasksFragmentTaskCardView-${taskDeliverablesList.first().taskId}") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("DeliverableWarningDialogOKButton") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
        }

        withTag("DeliverableWarningDialogDismissButton") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            performPressWithoutScroll()
        }

        testTaskTypeButtons(
            Task.TaskType.TIME,
            selected = true,
            press = false,
            activity
        )

        testTaskTypeButtons(
            Task.TaskType.QUANTITY,
            selected = false,
            press = false,
            activity
        )

        assertEquals(
            Task.TaskType.TIME.name,
            deliverablesList[1].deliverable.type
        )

        testTaskTypeButtons(
            Task.TaskType.NORMAL,
            selected = false,
            press = true,
            activity
        )

        withTag("DeliverableWarningDialog") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("DeliverableWarningDialogLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("${taskDeliverablesList.first().taskId}:${taskDeliverablesList.first().deliverableId}")
        }

        withTag("TasksFragmentTaskCardView-${taskDeliverablesList.first().taskId}") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("DeliverableWarningDialogDismissButton") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
        }

        withTag("DeliverableWarningDialogOKButton") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            performPressWithoutScroll()
        }

        testTaskTypeButtons(
            Task.TaskType.QUANTITY,
            selected = false,
            press = false,
            activity
        )

        testTaskTypeButtons(
            Task.TaskType.NORMAL,
            selected = true,
            press = false,
            activity
        )

        testTaskTypeButtons(
            Task.TaskType.TIME,
            selected = false,
            press = false,
            activity
        )

        deliverablesList = goal.goalDeliverables.first()
        assertNotNull(deliverablesList)
        assertEquals(2,deliverablesList.size)

        taskDeliverablesList = deliverablesList[1].taskDeliverables

        assertEquals(
            0,
            taskDeliverablesList.size
        )

        assertEquals(
            Deliverable.DeliverableEndType.WORK.name,
            deliverablesList[1].deliverable.endType
        )

        assertEquals(
            Task.TaskType.NORMAL.name,
            deliverablesList[1].deliverable.type
        )

        assertEquals(
            TaskDeliverable.WorkType.CompleteTasks.name,
            deliverablesList[1].deliverable.workType
        )

        testTaskTypeButtons(
            Task.TaskType.QUANTITY,
            selected = false,
            press = true,
            activity
        )

        withTag("DeliverableWarningDialog") {
            assertDoesNotExist()
            assertIsNotDisplayed()
        }

        testTaskTypeButtons(
            Task.TaskType.NORMAL,
            selected = false,
            press = false,
            activity
        )

        testTaskTypeButtons(
            Task.TaskType.QUANTITY,
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
            assertIsNotDisplayed()
        }

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

        testTaskTypeButtons(
            Task.TaskType.QUANTITY,
            selected = false,
            press = true,
            activity
        )

        goal = taskViewModel.goal.first()!!
        tasksList = goal.goalTasks.first()
        assertNotNull(tasksList)
        assertEquals(1,tasksList.size)

        taskTaskDeliverablesList = tasksList[0].taskDeliverableList.first()
        assertNotNull(taskTaskDeliverablesList)
        assertEquals(1,taskTaskDeliverablesList.size)

        deliverablesList = goal.goalDeliverables.first()
        assertNotNull(deliverablesList)
        assertEquals(2,deliverablesList.size)

        taskDeliverablesList = deliverablesList[0].taskDeliverables

        assertNotNull(taskDeliverablesList)
        assertEquals(1,taskDeliverablesList.size)

        taskDeliverablesList = deliverablesList[1].taskDeliverables

        assertNotNull(taskDeliverablesList)
        assertEquals(0,taskDeliverablesList.size)

        withTag("TasksFragmentDeliverableBackButton") {
            performPressWithoutScroll()
        }

        goal = taskViewModel.goal.first()!!
        tasksList = goal.goalTasks.first()
        assertNotNull(tasksList)
        assertEquals(1,tasksList.size)

        taskTaskDeliverablesList = tasksList[0].taskDeliverableList.first()
        assertNotNull(taskTaskDeliverablesList)
        assertEquals(1,taskTaskDeliverablesList.size)

        deliverablesList = goal.goalDeliverables.first()
        assertNotNull(deliverablesList)
        assertEquals(1,deliverablesList.size)

        taskDeliverablesList = deliverablesList[0].taskDeliverables

        assertNotNull(taskDeliverablesList)
        assertEquals(1,taskDeliverablesList.size)

        deleteQuantityDeliverableTask(
            inputGoal = goal,
            inputTasksList = tasksList,
            activity = activity,
            taskViewModel = taskViewModel
        )

        goal = taskViewModel.goal.first()!!

        assertEquals(Goal.GoalTab.TASKS,Goal.GoalTab.valueOf(goal.selectedTab))

        switchToTab(Goal.GoalTab.DELIVERABLES, taskViewModel, activity)
        goal = taskViewModel.goal.first()!!

        assertEquals(Goal.GoalTab.DELIVERABLES,Goal.GoalTab.valueOf(goal.selectedTab))

        tasksList = goal.goalTasks.first()
        assertNotNull(tasksList)
        assertEquals(0,tasksList.size)

        taskTaskDeliverablesList = emptyList()
        assertNotNull(taskTaskDeliverablesList)

        deliverablesList = goal.goalDeliverables.first()
        assertNotNull(deliverablesList)
        assertEquals(1,deliverablesList.size)

        taskDeliverablesList = deliverablesList[0].taskDeliverables

        assertNotNull(taskDeliverablesList)
        assertEquals(0,taskDeliverablesList.size)

        deleteQuantityDeliverable(
            inputGoal = goal,
            activity = activity,
            taskViewModel = taskViewModel
        )

        deliverablesList = goal.goalDeliverables.first()
        assertNotNull(deliverablesList)
        assertEquals(0,deliverablesList.size)
    }

    @Test
    fun `22 Adding Normal Undefined End Type With Times Deliverable`() = runMainTest {
        val activity = getTestMainActivity()
        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        var goal = taskViewModel.goal.first()!!

        assertEquals(Goal.GoalTab.DELIVERABLES,Goal.GoalTab.valueOf(goal.selectedTab))

        assertEquals(0,goal.goalTasks.first().size)
        assertEquals(0, goal.goalDeliverables.first().size)

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

        testTaskTypeButtons(
            Task.TaskType.NORMAL,
            selected = true,
            press = false,
            activity
        )

        withTag("TasksFragmentAddDeliverableNormalQuantityTimeTextField") {
            assertExists()
            assertIsDisplayed()
            performScrollTo()
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("Complete Edit Goal Tests")
            assertTextContains(
                value = "Complete Edit Goal Tests",
                substring = true,
                ignoreCase = false
            )
        }

        withTag("TaskFragmentDeliverableLocationText") {
            assertExists()
            assertIsDisplayed()
            performScrollTo()
            assertTextContains(
                value = "",
                substring = true,
                ignoreCase = false
            )
            performTextReplacement("At desk at home")
            assertTextContains(
                value = "At desk at home",
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

        assertEquals(
            Goal.GoalEndType.DELIVERABLE.name,
            goal.endType
        )

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
            assertDoesNotExist()
        }

        withTag(
            "TasksFragmentDeliverableDropdownMenuItem-${Deliverable.DeliverableEndType.UNDEFINED.name}"
        ) {
            assertExists()
            assertIsDisplayed()
            performPressWithoutScroll()
        }

        goal = taskViewModel.goal.first()!!
        var deliverablesList = goal.goalDeliverables.first()

        assertEquals(1,deliverablesList.size)

        assertEquals(
            "Complete Edit Goal Tests",
            deliverablesList[0].deliverable.deliverable
        )

        assertEquals(
            "At desk at home",
            deliverablesList[0].deliverable.location
        )

        assertEquals(
            Deliverable.DeliverableEndType.UNDEFINED.name,
            deliverablesList[0].deliverable.endType
        )

        withTag("TasksFragmentDeliverableEndTypeButton") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            assertTextContains(
                value = "End Type: Undefined",
                substring = true,
                ignoreCase = false
            )
        }

        withTag("TasksFragmentAddDeliverableViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentDeliverableSwitch")
        }

        assertNull(deliverablesList[0].deliverable.goalId)

        withTag("TasksFragmentDeliverableSwitch") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            assertIsToggleable()
            assertIsOff()
            performPressWithoutScroll()
        }

        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()

        assertEquals(
            goal.id,
            deliverablesList[0].deliverable.goalId
        )

        assertEquals(0,deliverablesList[0].times.size)

        TimeBlockTest(
            "TasksFragmentDeliverableAddTimeBlockButton",
            { taskViewModel.goal.first()?.goalDeliverables?.first()[0]?.times },
            Triple(
                instantTaskExecutorRule,
                composeTestRule,
                activity
            ),
            lazyColumnTag = "TasksFragmentAddDeliverableViewLazyColumn"
        ).runTests(this@TasksFragmentTests::class)

        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()
        assertEquals(1,deliverablesList[0].times.size)
        assertNotNull(deliverablesList[0].times[0].id)

        withTag("TasksFragmentAddDeliverableViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(deliverablesList[0].times[0].id!!)
        }

        withTag("EditGoalTimeInputViewCard-${deliverablesList[0].times[0].id}") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
        }

        withTag("TasksFragmentAddDeliverableDeleteButton") {
            assertDoesNotExist()
        }

        withTag("TasksFragmentDeliverableProcessDeliverable") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            performPressWithoutScroll()
        }

        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()
        assertEquals(1,deliverablesList.size)
        assertEquals(1,deliverablesList[0].times.size)
    }

    @Test
    fun `23 Edit And Back Press On Deliverable`() = runMainTest {
        val activity = getTestMainActivity()
        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        var goal = taskViewModel.goal.first()!!

        assertEquals(Goal.GoalTab.DELIVERABLES, Goal.GoalTab.valueOf(goal.selectedTab))

        var deliverablesList = goal.goalDeliverables.first()
        assertEquals(1,deliverablesList.size)
        assertEquals(1,deliverablesList[0].times.size)

        withTag("TasksFragmentDeliverableCardViewCard-${deliverablesList[0].deliverable.deliverableId}") {
            performLongPressWithScroll()
        }

        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()

        assertNotNull(deliverablesList)
        assertEquals(2,deliverablesList.size)
        assertEquals(1,deliverablesList[0].times.size)
        assertEquals(1,deliverablesList[1].times.size)

        assertNotEquals(
            deliverablesList[0].times[0].id,
            deliverablesList[1].times[0].id
        )

        withTag("TasksFragmentAddDeliverableView") {
            assertExists()
            assertIsDisplayed()
        }

        withTag("TasksFragmentAddDeliverableViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentAddDeliverableNQTButtons")
        }

        testTaskTypeButtons(
            Task.TaskType.NORMAL,
            selected = true,
            press = false,
            activity
        )

        withTag("TasksFragmentAddDeliverableNormalQuantityTimeTextField") {
            assertExists()
            assertIsDisplayed()
            performScrollTo()
            assertTextContains(
                value = "Complete Edit Goal Tests",
                substring = true,
                ignoreCase = false
            )
        }

        withTag("TaskFragmentDeliverableLocationText") {
            assertExists()
            assertIsDisplayed()
            performScrollTo()
            assertTextContains(
                value = "At desk at home",
                substring = true,
                ignoreCase = false
            )
        }

        withTag("TasksFragmentAddDeliverableViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentDeliverableEndTypeButton")
        }

        assertEquals(
            Goal.GoalEndType.DELIVERABLE.name,
            goal.endType
        )

        assertEquals(
            "Complete Edit Goal Tests",
            deliverablesList[1].deliverable.deliverable
        )

        assertEquals(
            "At desk at home",
            deliverablesList[1].deliverable.location
        )

        assertEquals(
            Deliverable.DeliverableEndType.UNDEFINED.name,
            deliverablesList[1].deliverable.endType
        )

        withTag("TasksFragmentDeliverableEndTypeButton") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            assertTextContains(
                value = "End Type: Undefined",
                substring = true,
                ignoreCase = false
            )
        }

        withTag("TasksFragmentAddDeliverableViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey("TasksFragmentDeliverableSwitch")
        }

        assertNotNull(deliverablesList[1].deliverable.goalId)

        withTag("TasksFragmentDeliverableSwitch") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            assertIsToggleable()
            assertIsOn()
        }

        assertEquals(
            goal.id,
            deliverablesList[1].deliverable.goalId
        )

        withTag("TasksFragmentAddDeliverableViewLazyColumn") {
            assertExists()
            assertIsDisplayed()
            performScrollToKey(deliverablesList[1].times[0].id!!)
        }

        withTag("EditGoalTimeInputViewCard-${deliverablesList[1].times[0].id}") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
        }

        withTag("TasksFragmentAddDeliverableDeleteButton") {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
        }

        withTag("TasksFragmentDeliverableProcessDeliverable") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
        }

        withTag("TasksFragmentDeliverableBackButton") {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            performPressWithoutScroll()
        }

        goal = taskViewModel.goal.first()!!
        deliverablesList = goal.goalDeliverables.first()
        assertEquals(1,deliverablesList.size)
        assertEquals(1,deliverablesList[0].times.size)
    }
}