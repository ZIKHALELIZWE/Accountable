package com.thando.accountable

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.thando.accountable.AccountableComposeRobolectricTest.TestMainActivity.Companion.addTime
import com.thando.accountable.MainActivityTest.FilteredPrintStream
import com.thando.accountable.MainActivityTest.Log
import com.thando.accountable.database.Converters
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.fragments.viewmodels.EditGoalViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
import java.time.ZoneOffset
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34] ) // Force Robolectric to use API 34
@LooperMode(LooperMode.Mode.LEGACY)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
abstract class AccountableComposeRobolectricTest(
    parentParameters:Triple<
        InstantTaskExecutorRule,
        ComposeContentTestRule,
        TestMainActivity
    >?=null
) {
    @get:Rule
    val instantTaskExecutorRule = parentParameters?.first ?: InstantTaskExecutorRule()
    @get:Rule
    val composeTestRule = parentParameters?.second ?: createComposeRule()
    val outerGetTestMainActivity = parentParameters?.third

    @Before
    open fun setup() {
        ShadowLog.stream = FilteredPrintStream(System.out)
    }

    @After
    open fun cleanup() {

    }

    fun getTestMainActivity(): TestMainActivity = outerGetTestMainActivity ?: innerGetTestMainActivity()

    private fun innerGetTestMainActivity(): TestMainActivity {
        val activity = Robolectric.buildActivity(TestMainActivity::class.java)
            .setup().get()
        composeTestRule.waitForIdle()
        return activity
    }

    inline fun <reified T:ViewModel> getViewModel(activity: TestMainActivity): T {
        return activity.viewModel.accountableNavigationController.fragmentViewModel.value!! as T
    }

    fun withTag(tag:String, block: SemanticsNodeInteraction.() -> Unit){
        composeTestRule.onNodeWithTag(tag).apply(block)
    }

    fun withTag(tag:String): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithTag(tag)
    }

    fun SemanticsNodeInteraction.performPressWithScroll() {
        assertExists()
        performScrollTo()
        assertIsDisplayed()
        assertHasClickAction()
        performClick()
        finishProcesses()
    }

    fun SemanticsNodeInteraction.performPressWithoutScroll(){
        assertExists()
        assertIsDisplayed()
        assertHasClickAction()
        performClick()
        finishProcesses()
    }

    fun finishProcesses() = runTest {
        advanceUntilIdle()
        composeTestRule.waitForIdle()
    }

    private fun getTestFunctions(): List<KFunction<*>> {
        return this::class.declaredMemberFunctions
            .filter { it.findAnnotation<Test>() != null}
            .map {
                it
            }
    }

    fun runTests(){
        Log.i("${this::class.simpleName}","")
        getTestFunctions().forEach {
            it.call(this)
            Log.w(it.name,"Passed")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    fun testDateAndTimePicker(
        activity: TestMainActivity,
        getTimeAsLong: suspend ()->Long?,
        getExpectedEndType:()->String,
        getActualEndType: suspend ()->String?,
        endTypeButtonTag: suspend ()->String,
        dropDownMenuTag: suspend ()->String,
        dropDownMenuItemTag: suspend ()->String,
        selectDateAndTimeButtonTag: String? = null
    ) = runTest {
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

    @OptIn(ExperimentalMaterial3Api::class)
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

        @Volatile
        private var INSTANCE: EditGoalViewModel? = null

        fun getInstance(accountableRepository: AccountableRepository): EditGoalViewModel {
            synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = EditGoalViewModel(accountableRepository)
                }
                return INSTANCE!!
            }
        }

        init {
            datePickerImpl = { state, onDismiss ->
                onDismiss.invoke {
                    state.addDays(daysToAdd)
                }
            }
            timePickerImpl = { state, onDismiss ->
                androidx.compose.material3.TimePicker(state)
                onDismiss.invoke {
                    state.addTime(unpackInt2(timeToAdd),unpackInt1(timeToAdd))
                }
            }
            iconImpl = { _, _, _ ->
                Text("Stub Icon")
            }
            modalBottomSheetImpl = { onDismissRequest, _, modifier, content ->
                androidx.compose.material3.ModalBottomSheet(
                    onDismissRequest = onDismissRequest,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    modifier = modifier,
                    content = content
                )
            }
            getEditGoalViewModelFactoryImpl = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras
                ): T {
                    // Get the Application object from extras
                    val application = checkNotNull(extras[APPLICATION_KEY])

                    val accountableRepository = AccountableRepository.getInstance(application)

                    return getInstance(accountableRepository) as T
                }
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
}