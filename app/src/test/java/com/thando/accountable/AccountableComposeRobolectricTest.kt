package com.thando.accountable

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.setSelectedDate
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.thando.accountable.AccountableNavigationController.AccountableFragment
import com.thando.accountable.MainActivityTest.FilteredPrintStream
import com.thando.accountable.MainActivityTest.Log
import com.thando.accountable.database.tables.Deliverable
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import com.thando.accountable.database.tables.Task
import com.thando.accountable.fragments.viewmodels.EditGoalViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Force Robolectric to use API 34
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
        Dispatchers.setMain(TestMainActivity.dispatcher)
    }

    @After
    open fun cleanup() {
        Dispatchers.resetMain()
    }

    fun getTestMainActivity(): TestMainActivity {
        val activity = outerGetTestMainActivity ?: innerGetTestMainActivity()
        finishProcesses()
        return activity
    }

    private fun innerGetTestMainActivity(): TestMainActivity {
        val activity = Robolectric.buildActivity(
            TestMainActivity::class.java
        ).setup().get()
        finishProcesses()
        return activity
    }

    inline fun <reified T:ViewModel> getViewModel(activity: TestMainActivity): T {
        return activity.viewModel.accountableNavigationController.fragmentViewModel.value!! as T
    }

    protected fun runMainTest(block: suspend TestScope.() -> Unit) = runTest (
        context = TestMainActivity.dispatcher,
        testBody = block
    )

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

    fun SemanticsNodeInteraction.performLongPressWithoutScroll(){
        assertExists()
        assertIsDisplayed()
        assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
        performSemanticsAction(SemanticsActions.OnLongClick)
        finishProcesses()
    }

    fun SemanticsNodeInteraction.performLongPressWithScroll(){
        assertExists()
        performScrollTo()
        assertIsDisplayed()
        assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
        performSemanticsAction(SemanticsActions.OnLongClick)
        finishProcesses()
    }

    fun finishProcesses() = runMainTest {
        TestMainActivity.dispatcher.scheduler.advanceUntilIdle()
        advanceUntilIdle()
        composeTestRule.waitForIdle()
        TestMainActivity.dispatcher.scheduler.advanceUntilIdle()
    }

    fun checkFragmentIs(
        activity: MainActivity,
        expectedFragment: AccountableFragment,
        expectedViewModel: String
    ) {
        finishProcesses()
        assertEquals(
            expectedFragment,
            activity.viewModel.currentFragment.value
        )
        assertEquals(
            expectedViewModel,
            activity.viewModel.accountableNavigationController.fragmentViewModel.value?.javaClass?.name
        )
    }

    fun timesAreEqual(
        timeOne: GoalTaskDeliverableTime,
        timeTwo: GoalTaskDeliverableTime,
        idsEqual:Boolean = true,
        parentsEqual: Boolean = true
    ) = runMainTest {
        val assertionFunction: suspend TestScope.(Any?, Any?)->Unit = if (idsEqual)
            {objectA, objectB -> assertEquals(objectA,objectB)}
        else {objectA, objectB -> assertNotEquals(objectA,objectB)}

        assertionFunction(timeOne.id, timeTwo.id)

        if (parentsEqual) assertionFunction(timeOne.parent,timeTwo.parent)
        else assertNotEquals(timeOne.parent,timeTwo.parent)

        assertEquals(timeOne.type,timeTwo.type)

        assertEquals(timeOne.timeBlockType,timeTwo.timeBlockType)

        assertEquals(timeOne.start,timeTwo.start)

        assertEquals(timeOne.duration,timeTwo.duration)

        if (!idsEqual && parentsEqual) {
            assertEquals(timeOne.id,timeTwo.cloneId)
        }
    }

    fun deliverablesAreEqual(
        deliverableOne:Deliverable,
        deliverableTwo:Deliverable,
        idsEqual:Boolean = true,
        parentsEqual:Boolean = true,
        times:List<GoalTaskDeliverableTime>? = null
    ) = runMainTest {
        val assertionFunction: suspend TestScope.(Any?, Any?)->Unit = if (idsEqual)
            {objectA, objectB -> assertEquals(objectA,objectB)}
        else {objectA, objectB -> assertNotEquals(objectA,objectB)}

        assertionFunction(
            deliverableOne.deliverableId,
            deliverableTwo.deliverableId
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
        if (parentsEqual) assertEquals(
            deliverableOne.parent,
            deliverableTwo.parent
        )
        else assertNotEquals(deliverableOne.parent,deliverableTwo.parent)

        assertEquals(
            (times?:deliverableOne.times.first()).size,
            deliverableTwo.times.first().size
        )

        if (!idsEqual && parentsEqual) {
            assertEquals(deliverableOne.deliverableId,deliverableTwo.cloneId)
        }

        val timesTwoList = deliverableTwo.times.first()
        (times?:deliverableOne.times.first()).forEachIndexed { index, timeOne ->
            val timeTwo = timesTwoList[index]
            timesAreEqual(
                timeOne,
                timeTwo,
                idsEqual = idsEqual,
                parentsEqual = parentsEqual
            )
        }
    }

    suspend fun getTaskDeliverablesLists(
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

    fun setMarkerDateAndTime(
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

    fun tasksAreEqual(
        taskOne: Task,
        taskTwo: Task,
        idsEqual: Boolean = true,
        times: List<GoalTaskDeliverableTime>? = null,
        deliverablesLists: Triple<List<Pair<Deliverable, List<GoalTaskDeliverableTime>>>, List<Pair<Deliverable, List<GoalTaskDeliverableTime>>>, List<Pair<Deliverable, List<GoalTaskDeliverableTime>>>>? = null
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
                    idsEqual = true,
                    parentsEqual = true,
                    times = pair.second
                )
            }
        } else {
            taskOne.deliverableNormalList.first().forEachIndexed { index, deliverableOne ->
                val deliverableTwo = deliverablesNormalTwoList[index]
                deliverablesAreEqual(
                    deliverableOne,
                    deliverableTwo,
                    idsEqual = true,
                    parentsEqual = true
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
                    idsEqual = true,
                    parentsEqual = true,
                    times = pair.second
                )
            }
        } else {
            taskOne.deliverableQuantityList.first().forEachIndexed { index, deliverableOne ->
                val deliverableTwo = deliverablesQuantityTwoList[index]
                deliverablesAreEqual(
                    deliverableOne,
                    deliverableTwo,
                    idsEqual = true,
                    parentsEqual = true
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
                    idsEqual = true,
                    parentsEqual = true,
                    times = pair.second
                )
            }
        } else {
            taskOne.deliverableTimeList.first().forEachIndexed { index, deliverableOne ->
                val deliverableTwo = deliverablesTimeTwoList[index]
                deliverablesAreEqual(
                    deliverableOne,
                    deliverableTwo,
                    idsEqual = true,
                    parentsEqual = true
                )
            }
        }
    }

    private fun getTestFunctions(): List<KFunction<*>> {
        return this::class.declaredMemberFunctions
            .filter { it.findAnnotation<Test>() != null}
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
        private var daysToAdd = 0L
        private var timeToAdd = packInts(0,0)

        private var dateToSet: LocalDateTime? = null

        val jvmTempDir:File = File(
            System.getProperty("java.io.tmpdir"),
            "robolectricFiles"
        )

        override fun getFilesDir(): File {
            return jvmTempDir
        }

        fun daysToAdd (input: Long) {
            daysToAdd = input
        }

        fun timeToAdd(minutes:Int,hours:Int){
            timeToAdd = packInts(minutes,hours)
        }

        fun setDate(date: LocalDateTime?){
            dateToSet = date
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
                    dateToSet?.let { state.setSelectedDate(
                        it.toLocalDate()
                    ) }
                        ?:run {
                            state.addDays(daysToAdd)
                        }
                }
            }
            timePickerImpl = { state, onDismiss ->
                androidx.compose.material3.TimePicker(state)
                onDismiss.invoke {
                    dateToSet?.let {
                        state.hour = it.hour
                        state.minute = it.minute
                    }
                        ?:run {
                            state.addTime(unpackInt2(timeToAdd),unpackInt1(timeToAdd))
                        }
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
        }
    }
}