package com.thando.accountable.fragments

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import com.thando.accountable.AccountableNavigationController.AccountableFragment
import com.thando.accountable.MainActivity
import com.thando.accountable.MainActivityTest.FilteredPrintStream
import com.thando.accountable.R
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.fragments.TestMainActivity.Companion.addTime
import com.thando.accountable.fragments.viewmodels.BooksViewModel
import com.thando.accountable.fragments.viewmodels.EditGoalViewModel
import com.thando.accountable.fragments.viewmodels.HomeViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLog
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class TestMainActivity : MainActivity() {
    val jvmTempDir = File(System.getProperty("java.io.tmpdir"), "robolectricFiles")
    private var daysToAdd = 0L
    private var timeToAdd = packInts(0,0)

    override fun getFilesDir(): File {
        return jvmTempDir
    }

    fun daysToAdd (input: Long) {
        daysToAdd = input
    }

    fun timeToAdd(minutes:Int,hours:Int){
        timeToAdd = packInts(minutes,hours)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun DatePicker(
        state: DatePickerState,
        onDismiss:(()->Unit)->Unit
    ) {
        onDismiss.invoke {
            state.addDays(daysToAdd)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun TimePicker(
        state: TimePickerState,
        onDismiss: (() -> Unit) -> Unit
    ) {
        androidx.compose.material3.TimePicker(state)
        onDismiss.invoke {
            state.addTime(unpackInt2(timeToAdd),unpackInt1(timeToAdd))
        }
    }

    companion object {
        fun DatePickerState.addDays(days: Long) {
            val currentMillis = this.selectedDateMillis ?: return
            val currentDate = Instant.ofEpochMilli(currentMillis)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
            val newDate = currentDate.plusDays(days)
            this.selectedDateMillis = newDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        }

        @OptIn(ExperimentalMaterial3Api::class)
        fun TimePickerState.addTime(hours: Int, minutes: Int){
            val totalMinutes = (this.hour + hours) * 60 + this.minute + minutes
            this.hour = if ((totalMinutes/60)>23) 23 else (totalMinutes / 60) % 24
            this.minute = if ((totalMinutes/60)>23) 59 else totalMinutes % 60
        }

        /*fun logDirectoryContents(dir: File) {
            if (!dir.exists() || !dir.isDirectory) {
                Log.i("DirLogger", "Invalid directory: ${dir.absolutePath}")
                return
            }
            fun walk(file: File, indent: String = "") {
                Log.i("DirLogger", "$indent${file.name}: Children = ${file.listFiles()?.size}")
                if (file.isDirectory) {
                    file.listFiles()?.forEach { child ->
                        walk(child, "$indent ")
                    }
                }
            }
            walk(dir)
        }*/
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34] ) // Force Robolectric to use API 34
@LooperMode(LooperMode.Mode.LEGACY)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class EditGoalTests {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        ShadowLog.stream = FilteredPrintStream(System.out)
    }

    @After
    fun cleanup() {

    }

    private fun getActivity(): TestMainActivity {
        return Robolectric.buildActivity(TestMainActivity::class.java)
            .setup().get()
    }

    @Test
    fun `01 coreClassesNotNull`() = runTest {
        val activity = getActivity()
        composeTestRule.waitForIdle()
        assertNotNull(activity)
        assertNotNull(activity.viewModel)
        assertNotNull(activity.viewModel.repository)
    }

    @Test
    fun `02 currentFragmentIsHomeFragment`() = runTest {
        val activity = getActivity()
        composeTestRule.waitForIdle()
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
        val activity = getActivity()
        composeTestRule.waitForIdle()
        assertEquals(
            HomeViewModel::class.java.name,
            activity.viewModel.accountableNavigationController.fragmentViewModel.value?.javaClass?.name
        )
        assertEquals(
            AccountableFragment.HomeFragment,
            activity.viewModel.currentFragment.value
        )
        with(composeTestRule.onNodeWithTag("HomeFragmentLoadGoalsButton")){
            assertExists()
            assertHasClickAction()
            assertIsDisplayed()
            performClick()
        }
        advanceUntilIdle()
        checkFragmentIs(
            activity, AccountableFragment.GoalsFragment,
            BooksViewModel::class.java.name
        )
    }

    @Test
    fun `04 Is In AppSettings Base, Set To Goals And Is Empty`() = runTest {
        val activity = getActivity()
        composeTestRule.waitForIdle()
        assertEquals(
            AccountableFragment.GoalsFragment,
            activity.viewModel.currentFragment.value
        )
        assertEquals(
            BooksViewModel::class.java.name,
            activity.viewModel.accountableNavigationController.fragmentViewModel.value?.javaClass?.name
        )
        val booksViewModel: BooksViewModel = activity.viewModel.accountableNavigationController.fragmentViewModel.value!! as BooksViewModel

        assertNull(booksViewModel.folder.value)
        assertNotNull(booksViewModel.appSettings.value)
        assertNotNull(booksViewModel.showScripts.value?.value)
        assertFalse(booksViewModel.showScripts.value!!.value)
        with(composeTestRule.onNodeWithTag("BooksSwitchFolderScriptButton")) {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            performClick()
        }
        advanceUntilIdle()
        composeTestRule.waitForIdle()
        assertTrue(booksViewModel.showScripts.value!!.value)
        assertNotNull(booksViewModel.goalsList.value)
        assertNull(booksViewModel.foldersList.value)
        assertNull(booksViewModel.scriptsList.value)

        assertEquals(0,booksViewModel.goalsList.value!!.first().size)
    }

    @Test
    fun `05 Open Edit Goal And Check Views`() = runTest {
        val activity = getActivity()
        composeTestRule.waitForIdle()

        with(composeTestRule.onNodeWithTag("BooksFloatingActionButton")){
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            performClick()
        }
        advanceUntilIdle()
        checkFragmentIs(
            activity,
            AccountableFragment.EditGoalFragment,
            EditGoalViewModel::class.java.name
        )

        val editGoalViewModel: EditGoalViewModel =
            activity.viewModel.accountableNavigationController.fragmentViewModel.value!! as EditGoalViewModel
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        val titleTextCompose = composeTestRule.onNodeWithTag("EditGoalTitle")
        titleTextCompose.apply {
            assertExists()
            assertIsDisplayed()
            assertTextEquals(activity.getString(R.string.new_goal))
        }
    }

    @Test
    fun `06 Input Goal`() = runTest {
        val activity = getActivity()
        composeTestRule.waitForIdle()

        val editGoalViewModel: EditGoalViewModel =
            activity.viewModel.accountableNavigationController.fragmentViewModel.value!! as EditGoalViewModel
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertNotNull(editGoalViewModel.newGoal.value?.first()?.goal)
        assertEquals("",editGoalViewModel.newGoal.value?.first()?.goal )

        composeTestRule.onNodeWithTag("EditGoalGoal").apply {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(activity.getString(R.string.goal))
            assertTextContains("")
            performTextInput("My New Goal")
            assertTextContains("My New Goal")
        }
        assertEquals("My New Goal",editGoalViewModel.newGoal.value?.first()?.goal )
    }

    @Test
    fun `07 Input Location`() = runTest {
        val activity = getActivity()
        composeTestRule.waitForIdle()

        val editGoalViewModel: EditGoalViewModel =
            activity.viewModel.accountableNavigationController.fragmentViewModel.value!! as EditGoalViewModel
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertNotNull(editGoalViewModel.newGoal.value?.first()?.location)
        assertEquals("",editGoalViewModel.newGoal.value?.first()?.location )
        composeTestRule.onNodeWithTag("EditGoalLocation").apply {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(activity.getString(R.string.location))
            assertTextContains("")
            performTextInput("My New Location")
            assertTextContains("My New Location")
        }
        assertEquals("My New Location",editGoalViewModel.newGoal.value?.first()?.location )
    }

    @Test
    fun `08 Input Image`() = runTest {
        val activity = getActivity()

        composeTestRule.waitForIdle()

        val editGoalViewModel: EditGoalViewModel =
            activity.viewModel.accountableNavigationController.fragmentViewModel.value!! as EditGoalViewModel
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertNull(editGoalViewModel.newGoal.value?.first()?.getGoalPicture())

        assertNull(editGoalViewModel.newGoal.value?.first()?.getUri(activity)?.value)

        composeTestRule.onNodeWithTag("EditGoalImage").apply {
            assertDoesNotExist()
        }

        composeTestRule.onNodeWithTag("EditGoalRemoveImageButton").apply {
            assertDoesNotExist()
        }

        val drawableResId = R.mipmap.ic_launcher
        val uri = Uri.parse("android.resource://${activity.packageName}/$drawableResId")

        assertNotNull(activity.viewModel.galleryLauncherReturnProcess)
        assertNull(activity.viewModel.galleryLauncherMultipleReturnProcess)
        composeTestRule.onNodeWithTag("EditGoalChooseImageButton").apply {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            assertTextContains(activity.getString(R.string.choose_image))
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        // Simulate return process
        activity.viewModel.processGalleryLauncherResult(uri)
        advanceUntilIdle()
        composeTestRule.waitForIdle()

        assertNotNull(editGoalViewModel.newGoal.value?.first()?.getGoalPicture())

        assertNotNull(editGoalViewModel.newGoal.value?.first()?.getUri(activity)?.value)

        composeTestRule.onNodeWithTag("EditGoalImage").apply {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertWidthIsAtLeast(2.dp)
            assertHeightIsAtLeast(2.dp)
        }

        composeTestRule.onNodeWithTag("EditGoalRemoveImageButton").apply {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertIsEnabled()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        assertNull(editGoalViewModel.newGoal.value?.first()?.getGoalPicture())
        assertNull(editGoalViewModel.newGoal.value?.first()?.getUri(activity)?.value)

        composeTestRule.onNodeWithTag("EditGoalImage").apply {
            assertDoesNotExist()
        }

        composeTestRule.onNodeWithTag("EditGoalRemoveImageButton").apply {
            assertDoesNotExist()
        }

        composeTestRule.onNodeWithTag("EditGoalChooseImageButton").apply {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            assertTextContains(activity.getString(R.string.choose_image))
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()
        // Simulate return process
        activity.viewModel.processGalleryLauncherResult(uri)
        advanceUntilIdle()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("EditGoalImage").apply {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertWidthIsAtLeast(2.dp)
            assertHeightIsAtLeast(2.dp)
        }
    }

    @Test
    fun `09 Colour Input`() = runTest {
        val activity = getActivity()
        composeTestRule.waitForIdle()

        val editGoalViewModel: EditGoalViewModel =
            activity.viewModel.accountableNavigationController.fragmentViewModel.value!! as EditGoalViewModel
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(-1,editGoalViewModel.newGoal.value?.first()?.colour)
        composeTestRule.onNodeWithTag("EditGoalColourDisplayBox").apply {
            assertDoesNotExist()
        }

        composeTestRule.onNodeWithTag("EditGoalPickColourButton").apply {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ColourPickerDialog").apply {
            assertExists()
            assertIsDisplayed()
        }

        composeTestRule.onNodeWithTag("ColourPickerDialogDismissButton").apply {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        assertEquals(-1,editGoalViewModel.newGoal.value?.first()?.colour)
        composeTestRule.onNodeWithTag("ColourPickerDialog").apply {
            assertDoesNotExist()
        }

        composeTestRule.onNodeWithTag("EditGoalPickColourButton").apply {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ColourPickerDialogCanvas").apply {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            performTouchInput { click(Offset(50f,50f)) }
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ColourPickerDialogOKButton").apply {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ColourPickerDialog").apply {
            assertDoesNotExist()
        }

        assertNotEquals(-1,editGoalViewModel.newGoal.value?.first()?.colour)
        composeTestRule.onNodeWithTag("EditGoalColourDisplayBox").apply {
            assertExists()
            assertWidthIsAtLeast(2.dp)
            assertHeightIsAtLeast(2.dp)
        }
    }

    @Test
    fun `10 End Type Undefined`() = runTest {
        val activity = getActivity()
        composeTestRule.waitForIdle()

        val editGoalViewModel: EditGoalViewModel =
            activity.viewModel.accountableNavigationController.fragmentViewModel.value!! as EditGoalViewModel
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(
            Goal.GoalEndType.UNDEFINED.name,
            editGoalViewModel.newGoal.value?.first()?.endType
        )

        composeTestRule.onNodeWithTag("EditGoalEndTypeButton").apply {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("EditGoalEndTypeDropDownMenu").apply {
            assertExists()
            assertIsDisplayed()
        }

        composeTestRule.onNodeWithTag(
            "EditGoalDropdownMenuItem-${Goal.GoalEndType.DELIVERABLE.name}"
        ).apply {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        assertEquals(
            Goal.GoalEndType.DELIVERABLE.name,
            editGoalViewModel.newGoal.value?.first()?.endType
        )

        composeTestRule.onNodeWithTag("EditGoalEndTypeButton").apply {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("EditGoalEndTypeDropDownMenu").apply {
            assertExists()
            assertIsDisplayed()
        }

        composeTestRule.onNodeWithTag(
            "EditGoalDropdownMenuItem-${Goal.GoalEndType.UNDEFINED.name}"
        ).apply {
            assertExists()
            assertIsDisplayed()
            hasClickAction()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        assertEquals(
            Goal.GoalEndType.UNDEFINED.name,
            editGoalViewModel.newGoal.value?.first()?.endType
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun `11 End Type Date`() = runTest {
        val activity = getActivity()
        composeTestRule.waitForIdle()

        val editGoalViewModel: EditGoalViewModel =
            activity.viewModel.accountableNavigationController.fragmentViewModel.value!! as EditGoalViewModel
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(
            Goal.GoalEndType.UNDEFINED.name,
            editGoalViewModel.newGoal.value?.first()?.endType
        )

        composeTestRule.onNodeWithTag("EditGoalEndTypeButton").apply {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("EditGoalEndTypeDropDownMenu").apply {
            assertExists()
            assertIsDisplayed()
        }

        composeTestRule.onNodeWithTag(
            "EditGoalDropdownMenuItem-${Goal.GoalEndType.DATE.name}"
        ).apply {
            assertExists()
            assertIsDisplayed()
            hasClickAction()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        assertEquals(Goal.GoalEndType.DATE.name,
            editGoalViewModel.newGoal.value?.first()?.endType)

        assertNotNull(editGoalViewModel.newGoal.value?.first()?.endDateTime)
        val previousEndDateTime = editGoalViewModel.newGoal.value?.first()?.endDateTime

        composeTestRule.onNodeWithTag(
            "EditGoalFragmentDatePickerDialog"
        ).apply {
            assertExists()
            assertIsDisplayed()
        }

        composeTestRule.onNodeWithTag(
            "EditGoalDatePickerDialogOKButton"
        ).apply {
            assertExists()
            assertIsDisplayed()
            hasClickAction()
        }

        composeTestRule.onNodeWithTag(
            "EditGoalDatePickerDialogCANCELButton"
        ).apply {
            assertExists()
            assertIsDisplayed()
            hasClickAction()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        assertEquals(
            previousEndDateTime,
            editGoalViewModel.newGoal.value?.first()?.endDateTime
        )

        assertEquals(Goal.GoalEndType.DATE.name,
            editGoalViewModel.newGoal.value?.first()?.endType)

        composeTestRule.onNodeWithTag("EditGoalEndTypeButton").apply {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("EditGoalEndTypeDropDownMenu").apply {
            assertExists()
            assertIsDisplayed()
        }

        composeTestRule.onNodeWithTag(
            "EditGoalDropdownMenuItem-${Goal.GoalEndType.DATE.name}"
        ).apply {
            assertExists()
            assertIsDisplayed()
            hasClickAction()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        assertEquals(Goal.GoalEndType.DATE.name,
            editGoalViewModel.newGoal.value?.first()?.endType)

        composeTestRule.onNodeWithTag(
            "EditGoalFragmentDatePickerDialog"
        ).apply {
            assertExists()
            assertIsDisplayed()
        }

        composeTestRule.onNodeWithTag(
            "EditGoalDatePickerDialogOKButton"
        ).apply {
            assertExists()
            assertIsDisplayed()
            hasClickAction()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        assertEquals(
            previousEndDateTime,
            editGoalViewModel.newGoal.value?.first()?.endDateTime
        )

        composeTestRule.onNodeWithTag(
            "EditGoalFragmentTimePickerDialog"
        ).apply {
            assertExists()
            assertIsDisplayed()
        }

        composeTestRule.onNodeWithTag(
            "EditGoalTimePickerDialogCANCELButton"
        ).apply {
            assertExists()
            assertIsDisplayed()
            hasClickAction()
        }

        composeTestRule.onNodeWithTag(
            "EditGoalTimePickerDialogOKButton"
        ).apply {
            assertExists()
            assertIsDisplayed()
            hasClickAction()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        assertEquals(
            previousEndDateTime,
            editGoalViewModel.newGoal.value?.first()?.endDateTime
        )

        activity.timeToAdd(2,2)
        activity.daysToAdd(2)

        composeTestRule.onNodeWithTag("EditGoalEndTypeButton").apply {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("EditGoalEndTypeDropDownMenu").apply {
            assertExists()
            assertIsDisplayed()
        }

        composeTestRule.onNodeWithTag(
            "EditGoalDropdownMenuItem-${Goal.GoalEndType.DATE.name}"
        ).apply {
            assertExists()
            assertIsDisplayed()
            hasClickAction()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(
            "EditGoalFragmentDatePickerDialog"
        ).apply {
            assertExists()
            assertIsDisplayed()
        }

        composeTestRule.onNodeWithTag(
            "EditGoalDatePickerDialogOKButton"
        ).apply {
            assertExists()
            assertIsDisplayed()
            hasClickAction()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        assertEquals(
            LocalDateTime.ofEpochSecond(
                previousEndDateTime!!/1000, 0,
                ZoneOffset.UTC
            ).plusDays(2).toInstant(ZoneOffset.UTC).toEpochMilli(),
            editGoalViewModel.newGoal.value!!.first()!!.endDateTime
        )

        composeTestRule.onNodeWithTag(
            "EditGoalFragmentTimePickerDialog"
        ).apply {
            assertExists()
            assertIsDisplayed()
        }

        composeTestRule.onNodeWithTag(
            "EditGoalTimePickerDialogCANCELButton"
        ).apply {
            assertExists()
            assertIsDisplayed()
            hasClickAction()
        }

        composeTestRule.onNodeWithTag(
            "EditGoalTimePickerDialogOKButton"
        ).apply {
            assertExists()
            assertIsDisplayed()
            hasClickAction()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        val endTimeDate = LocalDateTime.ofEpochSecond(
            previousEndDateTime /1000, 0,
            ZoneOffset.UTC
        )
        val timePickerState = TimePickerState(endTimeDate.hour,endTimeDate.minute,true)
        timePickerState.addTime(2,2)
        assertEquals(
            endTimeDate.plusDays(2).withHour(timePickerState.hour)
                .withMinute(timePickerState.minute).toInstant(ZoneOffset.UTC)
                .toEpochMilli(),
            editGoalViewModel.newGoal.value!!.first()!!.endDateTime
        )

        activity.timeToAdd(0,0)
        activity.daysToAdd(0)
    }

    @Test
    fun `12 End Type Deliverable`() = runTest {
        val activity = getActivity()
        composeTestRule.waitForIdle()

        val editGoalViewModel: EditGoalViewModel =
            activity.viewModel.accountableNavigationController.fragmentViewModel.value!! as EditGoalViewModel
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        assertEquals(
            Goal.GoalEndType.DATE.name,
            editGoalViewModel.newGoal.value?.first()?.endType
        )

        composeTestRule.onNodeWithTag("EditGoalEndTypeButton").apply {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("EditGoalEndTypeDropDownMenu").apply {
            assertExists()
            assertIsDisplayed()
        }

        composeTestRule.onNodeWithTag(
            "EditGoalDropdownMenuItem-${Goal.GoalEndType.DELIVERABLE.name}"
        ).apply {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            performClick()
            advanceUntilIdle()
        }
        composeTestRule.waitForIdle()

        assertEquals(
            Goal.GoalEndType.DELIVERABLE.name,
            editGoalViewModel.newGoal.value?.first()?.endType
        )

        composeTestRule.onNodeWithTag(
            "EditGoalAddDeliverableButton"
        ).apply {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
        }

        composeTestRule.onNodeWithTag(
            "EditGoalSelectDeliverableButton"
        ).apply {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            assertIsNotEnabled()
        }


    }
}