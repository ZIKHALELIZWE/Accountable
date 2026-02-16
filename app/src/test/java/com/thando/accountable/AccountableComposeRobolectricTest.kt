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
import com.thando.accountable.MainActivityTest.FilteredPrintStream
import com.thando.accountable.MainActivityTest.Log
import com.thando.accountable.fragments.viewmodels.EditGoalViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
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
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import kotlin.reflect.KClass
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
    val scope = TestScope(TestMainActivity.dispatcher)

    @get:Rule
    val instantTaskExecutorRule = parentParameters?.first ?: InstantTaskExecutorRule()
    @get:Rule
    val composeTestRule = parentParameters?.second ?: createComposeRule()
    val outerGetTestMainActivity = parentParameters?.third

    @Before
    open fun setup() {
        ShadowLog.stream = FilteredPrintStream(System.out)
        Dispatchers.setMain(TestMainActivity.dispatcher)
    }

    @After
    open fun cleanup() {
        Dispatchers.resetMain()
    }

    fun getTestMainActivity(): TestMainActivity = outerGetTestMainActivity ?: innerGetTestMainActivity()

    private fun innerGetTestMainActivity(): TestMainActivity {
        val activity = Robolectric.buildActivity(TestMainActivity::class.java)
            .setup().get()
        finishProcesses()
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
        TestMainActivity.dispatcher.scheduler.advanceUntilIdle()
        advanceUntilIdle()
        composeTestRule.waitForIdle()
        TestMainActivity.dispatcher.scheduler.advanceUntilIdle()
    }

    private fun getTestFunctions(): List<KFunction<*>> {
        return this::class.declaredMemberFunctions
            .filter { it.findAnnotation<Test>() != null}
            .map {
                it
            }
    }

    object LogTest {
        private var tabs = 0

        fun increaseTabs() { tabs++ }

        fun decreaseTabs(num:Int) { tabs-=num }

        private fun getTabs(): String = StringBuilder().apply {
            repeat(tabs) { append("\t") }
        }.toString()

        fun i(tag: String, message: String) {
            Log.i(getTabs() + tag,message)
        }

        fun e(tag: String, message: String) {
            Log.e(getTabs() + tag,message)
        }

        fun w(tag: String, message: String) {
            Log.w(getTabs() + tag,message)
        }
    }

    fun runTests(parentClass: KClass<*>){
        LogTest.i("${parentClass.simpleName}","")
        LogTest.increaseTabs()
        LogTest.i("${this::class.simpleName}","")
        LogTest.increaseTabs()
        getTestFunctions().forEach { function ->
            runCatching {
                function.call(this)
            }.onSuccess {
                LogTest.w(function.name,"Passed")
            }.onFailure {
                LogTest.e(function.name,"Failed")
                throw it
            }
        }
        LogTest.decreaseTabs(2)
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
            IO = dispatcher
            Main = dispatcher
        }

        companion object {
            val dispatcher = StandardTestDispatcher()
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
                    LogTest.i("DirLogger", "Invalid directory: ${dir.absolutePath}")
                    return
                }
                fun walk(file: File, indent: String = "") {
                    LogTest.i("DirLogger", "$indent${file.name}: Children = ${file.listFiles()?.size}")
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