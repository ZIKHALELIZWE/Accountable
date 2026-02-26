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
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelectable
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
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
import com.thando.accountable.AccountableNavigationController.AccountableFragment
import com.thando.accountable.R
import com.thando.accountable.database.Converters
import com.thando.accountable.database.tables.Deliverable
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import com.thando.accountable.database.tables.Marker
import com.thando.accountable.database.tables.Task
import com.thando.accountable.fragments.viewmodels.BooksViewModel
import com.thando.accountable.fragments.viewmodels.EditGoalViewModel
import com.thando.accountable.fragments.viewmodels.HomeViewModel
import com.thando.accountable.fragments.viewmodels.TaskViewModel
import com.thando.accountable.input_forms.DateAndTimePickerTest
import com.thando.accountable.input_forms.TimeBlockTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestResult
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

    private fun getGoalTab(name:String): Goal.GoalTab {
        return try{
            Goal.GoalTab.valueOf(name)
        }catch (e: Exception){
            LogTest.e("Failed to get tab",e.message?:"")
            Goal.GoalTab.TASKS
        }
    }

    private fun switchToTab(
        tab: Goal.GoalTab,
        taskViewModel: TaskViewModel,
        activity: TestMainActivity
    ) = runMainTest {
        assertNotNull(taskViewModel.goal.first())
        var goal = taskViewModel.goal.first()!!
        val previousTab = getGoalTab(goal.selectedTab)
        val tabsAreTheSame = previousTab == tab

        withTag("TasksFragmentGoalTab-${previousTab.ordinal}"){
            assertExists()
            assertIsDisplayed()
            assertIsSelectable()
            assertIsSelected()
            assertTextContains(activity.getString(previousTab.stringRes))
            assertHasClickAction()
        }

        withTag("TasksFragmentAddTextButton"){
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            assertTextContains(activity.getString(previousTab.addStringRes))
        }

        withTag("TasksFragmentGoalTab-${tab.ordinal}"){
            assertExists()
            assertIsDisplayed()
            assertIsSelectable()
            assertIsNotSelected()
            assertTextContains(activity.getString(tab.stringRes))
            assertHasClickAction()
            performClick()
            finishProcesses()
            assertIsSelected()
        }

        withTag("TasksFragmentGoalTab-${previousTab.ordinal}") {
            assertExists()
            assertIsDisplayed()
            assertIsSelectable()
            assertTextContains(activity.getString(previousTab.stringRes))
            assertIsNotSelected()
        }

        goal = taskViewModel.goal.first()!!
        val currentTab = getGoalTab(goal.selectedTab)

        withTag("TasksFragmentAddTextButton"){
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            assertTextContains(activity.getString(currentTab.addStringRes))
        }

        if (tabsAreTheSame){
            assertEquals(previousTab,currentTab)
        }
        else{
            assertNotEquals(previousTab,currentTab)
        }
        assertEquals(tab,currentTab)
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

    private fun setMarkerDateAndTime(
        activity: TestMainActivity,
        date: LocalDate,
        time: LocalTime,
        openPickerDialog: suspend TestScope.() -> TestResult
    ) = runMainTest {
        activity.setDate(LocalDateTime.of(date,time))

        openPickerDialog()

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

        withTag(
            "EditGoalFragmentTimePickerDialog"
        ) {
            assertExists()
            assertIsDisplayed()
        }

        withTag(
            "EditGoalTimePickerDialogOKButton"
        ) {
            performPressWithoutScroll()
        }

        activity.setDate(null)
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
        assertEquals(0,tasksList.size)

        assertEquals(Goal.GoalTab.MARKERS,Goal.GoalTab.valueOf(goal.selectedTab))
        switchToTab(Goal.GoalTab.TASKS,taskViewModel, activity)
        goal = taskViewModel.goal.first()!!
        assertEquals(Goal.GoalTab.TASKS,Goal.GoalTab.valueOf(goal.selectedTab))

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

        withTag("TasksFragmentAddViewDropdownMenuItem-${Task.TaskEndType.DATE.name}") {
            performPressWithoutScroll()
        }

        DateAndTimePickerTest(
            getTimeAsLong = { taskViewModel.task.first()?.endDateTime },
            getExpectedEndType = { Task.TaskEndType.DATE.name },
            getActualEndType = { taskViewModel.task.first()?.endType },
            endTypeButtonTag = {"TasksFragmentAddTaskEndTypeButton"},
            dropDownMenuTag = { "TasksFragmentAddViewDropdownMenu" },
            dropDownMenuItemTag = {"TasksFragmentAddViewDropdownMenuItem-${Task.TaskEndType.DATE.name}"},
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
            taskViewModel.task.first()!!.times,
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

    private suspend fun getTaskDeliverablesLists(
        task: Task
    ): Triple<List<Pair<Deliverable, List<GoalTaskDeliverableTime>>>, List<Pair<Deliverable, List<GoalTaskDeliverableTime>>>, List<Pair<Deliverable, List<GoalTaskDeliverableTime>>>> {
        val deliverablesNormal = task.deliverableNormalList.first()
        val deliverablesQuantity = task.deliverableQuantityList.first()
        val deliverablesTime = task.deliverableTimeList.first()

        val deliverableTimesNormal = mutableListOf<Pair<Deliverable, List<GoalTaskDeliverableTime>>>()
        deliverablesNormal.forEach { deliverableTimesNormal.add(it to it.times.first()) }

        val deliverableTimesQuantity = mutableListOf<Pair<Deliverable, List<GoalTaskDeliverableTime>>>()
        deliverablesQuantity.forEach { deliverableTimesQuantity.add(it to it.times.first()) }

        val deliverableTimesTime = mutableListOf<Pair<Deliverable, List<GoalTaskDeliverableTime>>>()
        deliverablesTime.forEach { deliverableTimesTime.add(it to it.times.first()) }

        return Triple(
            deliverableTimesNormal.toList(),
            deliverableTimesQuantity.toList(),
            deliverableTimesTime.toList()
        )
    }

    private fun tasksAreEqual(
        taskOne:Task,
        taskTwo: Task,
        idsEqual:Boolean = true,
        times:List<GoalTaskDeliverableTime>? = null,
        deliverablesLists:Triple<List<Pair<Deliverable, List<GoalTaskDeliverableTime>>>, List<Pair<Deliverable, List<GoalTaskDeliverableTime>>>, List<Pair<Deliverable, List<GoalTaskDeliverableTime>>>>? = null
    ) = runMainTest {
        val assertionFunction: suspend TestScope.(Any?, Any?)->Unit = if (idsEqual)
            {objectA, objectB -> assertEquals(objectA,objectB)}
        else {objectA, objectB -> assertNotEquals(objectA,objectB)}

        assertionFunction(
            taskOne.taskId,
            taskTwo.taskId
        )

        assertEquals(
            taskOne.parent,
            taskTwo.parent
        )

        assertEquals(
            taskOne.parentType,
            taskTwo.parentType
        )
        assertEquals(
            taskOne.position,
            taskTwo.position
        )
        assertEquals(
            taskOne.initialDateTime,
            taskTwo.initialDateTime
        )
        assertEquals(
            taskOne.endDateTime,
            taskTwo.endDateTime
        )
        assertEquals(
            taskOne.endType,
            taskTwo.endType
        )
        assertEquals(
            taskOne.scrollPosition,
            taskTwo.scrollPosition
        )
        assertEquals(
            taskOne.task,
            taskTwo.task
        )
        assertEquals(
            taskOne.type,
            taskTwo.type
        )
        assertEquals(
            taskOne.quantity,
            taskTwo.quantity
        )
        assertEquals(
            taskOne.time,
            taskTwo.time
        )
        assertEquals(
            taskOne.status,
            taskTwo.status
        )
        assertEquals(
            taskOne.colour,
            taskTwo.colour
        )
        assertEquals(
            taskOne.location,
            taskTwo.location
        )

        assertEquals(
            (times?:taskOne.times.first()).size,
            taskTwo.times.first().size
        )

        assertEquals(
            (deliverablesLists?.first?:taskOne.deliverableNormalList.first()).size,
            taskTwo.deliverableNormalList.first().size
        )

        assertEquals(
            (deliverablesLists?.second?:taskOne.deliverableQuantityList.first()).size,
            taskTwo.deliverableQuantityList.first().size
        )

        assertEquals(
            (deliverablesLists?.third?:taskOne.deliverableTimeList.first()).size,
            taskTwo.deliverableTimeList.first().size
        )

        if (!idsEqual && times == null) {
            assertEquals(taskOne.taskId, taskTwo.cloneId)
        }

        val timesTwoList = taskTwo.times.first()
        (times?:taskOne.times.first()).forEachIndexed { index, timeOne ->
            val timeTwo = timesTwoList[index]
            timesAreEqual(
                timeOne,
                timeTwo,
                idsEqual = idsEqual,
                parentsEqual = idsEqual
            )
        }

        val deliverablesNormalTwoList = taskTwo.deliverableNormalList.first()
        if (deliverablesLists?.first != null) {
            deliverablesLists.first.forEachIndexed { index, pair ->
                val deliverableTwo = deliverablesNormalTwoList[index]
                deliverablesAreEqual(
                    pair.first,
                    deliverableTwo,
                    idsEqual = idsEqual,
                    parentsEqual = idsEqual,
                    times = pair.second
                )
            }
        } else {
            taskOne.deliverableNormalList.first().forEachIndexed { index, deliverableOne ->
                val deliverableTwo = deliverablesNormalTwoList[index]
                deliverablesAreEqual(
                    deliverableOne,
                    deliverableTwo,
                    idsEqual = idsEqual,
                    parentsEqual = idsEqual
                )
            }
        }

        val deliverablesQuantityTwoList = taskTwo.deliverableQuantityList.first()
        if (deliverablesLists?.second != null) {
            deliverablesLists.second.forEachIndexed { index, pair ->
                val deliverableTwo = deliverablesQuantityTwoList[index]
                deliverablesAreEqual(
                    pair.first,
                    deliverableTwo,
                    idsEqual = idsEqual,
                    parentsEqual = idsEqual,
                    times = pair.second
                )
            }
        } else {
            taskOne.deliverableQuantityList.first().forEachIndexed { index, deliverableOne ->
                val deliverableTwo = deliverablesQuantityTwoList[index]
                deliverablesAreEqual(
                    deliverableOne,
                    deliverableTwo,
                    idsEqual = idsEqual,
                    parentsEqual = idsEqual
                )
            }
        }

        val deliverablesTimeTwoList = taskTwo.deliverableTimeList.first()
        if (deliverablesLists?.third != null) {
            deliverablesLists.third.forEachIndexed { index, pair ->
                val deliverableTwo = deliverablesTimeTwoList[index]
                deliverablesAreEqual(
                    pair.first,
                    deliverableTwo,
                    idsEqual = idsEqual,
                    parentsEqual = idsEqual,
                    times = pair.second
                )
            }
        } else {
            taskOne.deliverableTimeList.first().forEachIndexed { index, deliverableOne ->
                val deliverableTwo = deliverablesTimeTwoList[index]
                deliverablesAreEqual(
                    deliverableOne,
                    deliverableTwo,
                    idsEqual = idsEqual,
                    parentsEqual = idsEqual
                )
            }
        }
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
    }

    @Test
    fun `12 Adding Quantity Deliverable Task`() = runMainTest {
        val activity = getTestMainActivity()

        val taskViewModel: TaskViewModel = getViewModel(activity)
        assertNotNull(taskViewModel)
        assertNotNull(taskViewModel.goal.first())
        val goal = taskViewModel.goal.first()!!

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

        assertNotNull(taskViewModel.task.first()?.deliverableQuantityListNotSelected?.first()[0])

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
        assertEquals(6L,taskViewModel.task.first()?.deliverableQuantityListNotSelected?.first()[0]?.quantity)
        withTag("TasksFragmentTaskDeliverableWorkInputTextField") {
            performTextReplacement("Jump a total of 20 times33")
            assertTextContains(
                value = "Jump a total of 20 times",
                substring = true,
                ignoreCase = false
            )
            finishProcesses()
        }
        assertEquals(20L,taskViewModel.task.first()?.deliverableQuantityListNotSelected?.first()[0]?.quantity)

        withTag("DeliverableAdderDialogDismissButton") {
            performPressWithScroll()
        }

        assertEquals(0,taskViewModel.task.first()?.deliverableQuantityListNotSelected?.first()?.size)

        withTag("DeliverableAdderDialog") {
            assertDoesNotExist()
        }

        withTag("TasksFragmentAddTaskAddDeliverableButton") {
            performPressWithScroll()
        }

        assertNotNull(taskViewModel.task.first()?.deliverableQuantityListNotSelected?.first()[0])

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
        assertEquals(6L,taskViewModel.task.first()?.deliverableQuantityListNotSelected?.first()[0]?.quantity)
        withTag("TasksFragmentTaskDeliverableWorkInputTextField") {
            performTextReplacement("Jump a total of 20 times33")
            assertTextContains(
                value = "Jump a total of 20 times",
                substring = true,
                ignoreCase = false
            )
        }
        assertEquals(20L,taskViewModel.task.first()?.deliverableQuantityListNotSelected?.first()[0]?.quantity)

        withTag("DeliverableAdderDialogOKButton") {
            performPressWithScroll()
        }

        assertEquals(1,taskViewModel.task.first()?.deliverableQuantityListNotSelected?.first()?.size)

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

        assertNotNull(taskViewModel.task.first()?.deliverableQuantityListNotSelected?.first()[0]?.deliverableId)
        var deliverableId = taskViewModel.task.first()!!.deliverableQuantityListNotSelected.first()[0].deliverableId!!

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

        assertEquals(0,taskViewModel.task.first()?.deliverableQuantityListNotSelected?.first()?.size)
        assertEquals(1,taskViewModel.task.first()?.deliverableQuantityList?.first()?.size)

        withTag("DeliverablePickerDialog") {
            assertDoesNotExist()
        }

        assertNotNull(taskViewModel.task.first()?.deliverableQuantityList?.first()[0]?.deliverableId)
        deliverableId = taskViewModel.task.first()!!.deliverableQuantityList.first()[0].deliverableId!!

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

        assertEquals(1,taskViewModel.task.first()?.deliverableQuantityListNotSelected?.first()?.size)
        assertEquals(0,taskViewModel.task.first()?.deliverableQuantityList?.first()?.size)

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

        assertNotNull(taskViewModel.task.first()?.deliverableQuantityListNotSelected?.first()[0]?.deliverableId)
        deliverableId = taskViewModel.task.first()!!.deliverableQuantityListNotSelected.first()[0].deliverableId!!

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

        assertEquals(0,taskViewModel.task.first()?.deliverableQuantityListNotSelected?.first()?.size)
        assertEquals(1,taskViewModel.task.first()?.deliverableQuantityList?.first()?.size)

        withTag("DeliverablePickerDialog") {
            assertDoesNotExist()
        }

        assertNotNull(taskViewModel.task.first()?.deliverableQuantityList?.first()[0]?.deliverableId)
        deliverableId = taskViewModel.task.first()!!.deliverableQuantityList.first()[0].deliverableId!!

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

        assertEquals(0,taskViewModel.task.first()?.deliverableQuantityList?.first()?.size)

        withTag("TasksFragmentDeliverableCardViewCard-${deliverableId}") {
            assertDoesNotExist()
        }

        withTag("TasksFragmentAddTaskEndTypeButton") {
            performPressWithScroll()
        }

        withTag("TasksFragmentAddViewDropdownMenuItem-${Task.TaskEndType.DELIVERABLE.name}") {
            performPressWithoutScroll()
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

        assertNotNull(taskViewModel.task.first()?.deliverableQuantityListNotSelected?.first()[0]?.deliverableId)
        deliverableId = taskViewModel.task.first()!!.deliverableQuantityListNotSelected.first()[0].deliverableId!!

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

        assertEquals(0,taskViewModel.task.first()?.deliverableQuantityListNotSelected?.first()?.size)
        assertEquals(1,taskViewModel.task.first()?.deliverableQuantityList?.first()?.size)

        withTag("DeliverablePickerDialog") {
            assertDoesNotExist()
        }

        assertNotNull(taskViewModel.task.first()?.deliverableQuantityList?.first()[0]?.deliverableId)
        deliverableId = taskViewModel.task.first()!!.deliverableQuantityList.first()[0].deliverableId!!

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
            taskViewModel.task.first()!!.times,
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
}