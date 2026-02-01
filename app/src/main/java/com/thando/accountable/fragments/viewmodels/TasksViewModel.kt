package com.thando.accountable.fragments.viewmodels

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.thando.accountable.AccountableRepository
import com.thando.accountable.R
import com.thando.accountable.database.tables.Deliverable
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import com.thando.accountable.database.tables.Marker
import com.thando.accountable.database.tables.Task
import com.thando.accountable.ui.cards.ColourPickerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset

class TaskViewModel(val repository: AccountableRepository) : ViewModel() {

    val goal = repository.getGoal()

    val bottomSheetType = mutableStateOf<Goal.GoalTab?>(null)
    val triedToSave = MutableStateFlow(false)
    val showErrorMessage = mutableStateOf(false)
    val errorMessage = mutableIntStateOf(-1)
    val colourPickerDialog = ColourPickerDialog()

    val originalTask = MutableStateFlow<Task?>(null)
    val originalDeliverable = MutableStateFlow<Deliverable?>(null)
    val originalMarker = MutableStateFlow<Marker?>(null)
    val task: MutableStateFlow<Task?> = MutableStateFlow(null)
    val deliverable: MutableStateFlow<Deliverable?> = MutableStateFlow(null)
    val marker: MutableStateFlow<Marker?> = MutableStateFlow(null)

    private fun showError(
        message: Int,
        focusRequester: FocusRequester
    ){
        showInputError(
            errorMessage,
            showErrorMessage,
            message,
            focusRequester
        )
    }

    suspend fun canSaveTask(): Boolean {
        task.value?.let { task ->
            if (task.task.text.isEmpty()) {
                showError(
                    R.string.please_enter_a_task,
                    task.taskTextFocusRequester
                )
                return false
            }
            if (task.location.text.isEmpty()) {
                showError(
                    R.string.please_enter_a_location,
                    task.locationFocusRequester
                )
                return false
            }
            if (task.colour.value == -1) {
                showError(
                    R.string.please_select_a_colour,
                    task.colourFocusRequester
                )
                return false
            }
            task.times.value?.first()?.forEach { time ->
                val duration = LocalDateTime.ofEpochSecond(time.duration/1000,0, ZoneOffset.UTC)
                if (duration.hour == 0 && duration.minute == 0) {
                    showError(
                        R.string.please_select_a_duration,
                        time.durationPickerFocusRequester
                    )
                    return false
                }
            }
        }?:return false
        return true
    }

    fun canSaveMarker(): Boolean{
        marker.value?.let { marker ->
            if (marker.marker.text.isEmpty()) {
                showError(
                    R.string.please_enter_a_marker,
                    marker.markerTextFocusRequester
                )
                return false
            }
        }?:return false
        return true
    }

    fun pickColour(originalColour: Color? = null){
        task.value?.let { task ->
            colourPickerDialog.pickColour(originalColour) { selectedColour: Int ->
                task.colour.value = selectedColour
            }
        }
    }

    suspend fun processBottomSheetAdd(){
        processBottomSheetAddCompanionObject(
            repository,
            triedToSave,
            bottomSheetType,
            task,
            originalTask,
            ::deleteTask,
            ::canSaveTask,
            ::saveTask,
            deliverable,
            originalDeliverable,
            ::deleteDeliverable,
            ::saveDeliverable,
            marker,
            originalMarker,
            ::deleteMarker,
            ::canSaveMarker,
            ::saveMarker,
            ::showError,
            ::dismissBottomSheet
        )
    }

    suspend fun addTask(){
        originalTask.value = null
        originalDeliverable.value = null
        originalMarker.value = null
        goal.value?.first()?.let { goal ->
            task.value = Task(
                parent = goal.id?.let { mutableLongStateOf(it) }?:return,
                parentType = mutableStateOf(Task.TaskParentType.GOAL),
                position = mutableLongStateOf(repository.getTasks(
                    goal.id?:return,
                    Task.TaskParentType.GOAL
                ).first().size.toLong()),
                colour = mutableIntStateOf(goal.colour),
                location = TextFieldState(goal.location),
                type = mutableStateOf(Task.TaskType.NORMAL)
            )
        }?:return
        saveTask()
        showBottomSheet(Goal.GoalTab.TASKS )
    }

    suspend fun addDeliverable(){
        addDeliverableCompanionObject(
            repository = repository,
            goal = goal.value?.first(),
            saveDeliverable = ::saveDeliverable,
            showBottomSheet = ::showBottomSheet,
            deliverable = deliverable,
            originalTask = originalTask,
            originalDeliverable = originalDeliverable,
            originalMarker = originalMarker
        )
    }

    suspend fun addMarker(){
        originalTask.value = null
        originalDeliverable.value = null
        originalMarker.value = null
        goal.value?.first()?.let { goal ->
            marker.value = Marker(
                parent = goal.id?.let { mutableLongStateOf(it) }?:return,
                position = mutableLongStateOf(repository.getMarkers(
                    goal.id?:return
                ).first().size.toLong())
            )
        }?:return
        saveMarker()
        showBottomSheet(Goal.GoalTab.MARKERS)
    }

    suspend fun editTask(originalTaskInput: Task){
        originalTask.value = originalTaskInput
        originalDeliverable.value = null
        originalMarker.value = null
        task.value = repository.getTaskClone(originalTaskInput)?:return
        saveTask()
        showBottomSheet(Goal.GoalTab.TASKS)
    }

    suspend fun editDeliverable(originalDeliverableInput: Deliverable){
        editClickedDeliverable(
            originalDeliverableInput = originalDeliverableInput,
            repository = repository,
            originalTask = originalTask,
            originalDeliverable = originalDeliverable,
            originalMarker = originalMarker,
            deliverable = deliverable,
            saveDeliverable = ::saveDeliverable,
            showBottomSheet = ::showBottomSheet
        )
    }

    suspend fun editMarker(originalMarkerInput: Marker){
        originalTask.value = null
        originalDeliverable.value = null
        originalMarker.value = originalMarkerInput
        marker.value = repository.getMarkerClone(originalMarkerInput)?:return
        saveMarker()
        showBottomSheet(Goal.GoalTab.MARKERS)
    }

    suspend fun deleteTaskClicked() {
        originalTask.value?.let { taskToDelete ->
            deleteTask()
            task.value = taskToDelete
        }
        dismissBottomSheet()
    }

    suspend fun deleteDeliverableClicked() {
        deleteDeliverableClickedCompanionObject(
            deliverable,
            originalDeliverable,
            ::deleteDeliverable,
            ::dismissBottomSheet
        )
    }

    suspend fun deleteMarkerClicked() {
        originalMarker.value?.let { markerToDelete ->
            deleteMarker()
            marker.value = markerToDelete
        }
        dismissBottomSheet()
    }

    suspend fun saveTask() {
        task.value?.let { task ->
            task.id = repository.saveTask(task)
            task.times.value?.first()?.forEach { saveTime(it) }
        }
    }

    suspend fun saveDeliverable() {
        saveClickedDeliverable(repository, deliverable)
    }

    suspend fun saveMarker() {
        marker.value?.let { marker ->
            marker.id = repository.saveMarker(marker)
        }
    }

    suspend fun deleteTask(){
        task.value?.let { task ->
            repository.deleteTask(task)
            task.times.value?.first()?.forEach {
                repository.deleteGoalTaskDeliverableTime(it)
            }
        }
    }

    suspend fun deleteDeliverable(){
        deleteClickedDeliverable(repository, deliverable)
    }

    suspend fun deleteMarker() {
        marker.value?.let { marker ->
            repository.deleteMarker(marker)
        }
    }

    suspend fun addTimeBlock(){
        addTimeBlockCompanionObject(
            bottomSheetType,
            ::saveTime,
            task,
            ::saveTask,
            deliverable,
            ::saveDeliverable,
        )
    }

    suspend fun deleteTimeBlock(timeBlock: GoalTaskDeliverableTime){
        deleteTimeBlockCompanionObject(
            repository = repository,
            bottomSheetType = bottomSheetType,
            timeBlock = timeBlock,
            task = task,
            deliverable = deliverable
        )
    }

    suspend fun updateTimeBlock(timeBlock: GoalTaskDeliverableTime) {
        repository.update(timeBlock)
    }

    suspend fun saveTime(time: GoalTaskDeliverableTime): Long {
        return repository.saveGoalTaskDeliverableTime(time)
    }

    suspend fun dismissBottomSheet() {
        dismissBottomSheetCompanionObject(
            triedToSave,
            bottomSheetType,
            task,
            ::deleteTask,
            deliverable,
            ::deleteDeliverable,
            marker,
            ::deleteMarker,
        )
    }

    fun showBottomSheet(sheetType: Goal.GoalTab) {
        showBottomSheetCompanionObject(
            sheetType,
            triedToSave,
            bottomSheetType
        )
    }

    suspend fun closeTasks(){
        if (bottomSheetType.value != null){
            dismissBottomSheet()
        }
        else repository.goBackToGoalsFromTasks()
    }

    companion object{
        suspend fun canSaveDeliverable(deliverable: MutableStateFlow<Deliverable?>, showError:(Int, FocusRequester)->Unit): Boolean{
            deliverable.value?.let { deliverable ->
                if (deliverable.deliverable.isEmpty()) {
                    showError(
                        R.string.please_enter_a_deliverable,
                        deliverable.deliverableTextFocusRequester
                    )
                    return false
                }
                deliverable.times.value?.first()?.forEach { time ->
                    val duration = LocalDateTime.ofEpochSecond(time.duration/1000,0, ZoneOffset.UTC)
                    if (duration.hour == 0 && duration.minute == 0) {
                        showError(
                            R.string.please_select_a_duration,
                            time.durationPickerFocusRequester
                        )
                        return false
                    }
                }
            }?:return false
            return true
        }

        fun showInputError(
            errorMessage: MutableIntState,
            showErrorMessage: MutableState<Boolean>,
            message: Int,
            focusRequester: FocusRequester
        ){
            errorMessage.intValue = message
            showErrorMessage.value = true
            focusRequester.requestFocus()
        }

        suspend fun editClickedDeliverable(
            originalDeliverableInput: Deliverable,
            repository: AccountableRepository,
            originalTask: MutableStateFlow<Task?>?=null,
            originalDeliverable: MutableStateFlow<Deliverable?>,
            originalMarker: MutableStateFlow<Marker?>?=null,
            deliverable: MutableStateFlow<Deliverable?>,
            saveDeliverable: suspend () -> Unit,
            showBottomSheet: suspend (Goal.GoalTab) -> Unit
        ){
            originalTask?.value = null
            originalDeliverable.value = originalDeliverableInput
            originalMarker?.value = null
            deliverable.value = repository.getDeliverableClone(originalDeliverableInput)?:return
            saveDeliverable()
            showBottomSheet(Goal.GoalTab.DELIVERABLES)
        }

        suspend fun saveClickedDeliverable(
            repository: AccountableRepository,
            deliverable: MutableStateFlow<Deliverable?>
        ) {
            deliverable.value?.let { deliverable ->
                deliverable.id = repository.saveDeliverable(deliverable)
            }
        }

        suspend fun deleteClickedDeliverable(
            repository: AccountableRepository,
            deliverable: MutableStateFlow<Deliverable?>
        ) {
            deliverable.value?.let { deliverable ->
                repository.deleteDeliverable(deliverable)
                deliverable.times.value?.first()?.forEach {
                    repository.deleteGoalTaskDeliverableTime(it)
                }
            }
        }

        suspend fun deleteDeliverableClickedCompanionObject(
            deliverable: MutableStateFlow<Deliverable?>,
            originalDeliverable: MutableStateFlow<Deliverable?>,
            deleteDeliverable: suspend () -> Unit,
            dismissBottomSheet: suspend () -> Unit
        ) {
            originalDeliverable.value?.let { deliverableToDelete ->
                deleteDeliverable()
                deliverable.value = deliverableToDelete
            }
            dismissBottomSheet()
        }

        suspend fun dismissBottomSheetCompanionObject(
            triedToSave: MutableStateFlow<Boolean>,
            bottomSheetType: MutableState<Goal.GoalTab?>,
            task: MutableStateFlow<Task?>?=null,
            deleteTask: (suspend () -> Unit)?=null,
            deliverable: MutableStateFlow<Deliverable?>?=null,
            deleteDeliverable: (suspend () -> Unit)?=null,
            marker: MutableStateFlow<Marker?>?=null,
            deleteMarker: (suspend () -> Unit)?=null,
        ) {
            task?.value?.let {
                deleteTask?.invoke()
                task.value = null
            }
            deliverable?.value?.let {
                deleteDeliverable?.invoke()
                deliverable.value = null
            }
            marker?.value?.let {
                deleteMarker?.invoke()
                marker.value = null
            }
            triedToSave.value = false
            bottomSheetType.value = null
        }

        fun showBottomSheetCompanionObject(
                sheetType: Goal.GoalTab,
                triedToSave: MutableStateFlow<Boolean>,
                bottomSheetType: MutableState<Goal.GoalTab?>
        ) {
            triedToSave.value = false
            bottomSheetType.value = sheetType
        }

        suspend fun processBottomSheetAddCompanionObject(
            repository: AccountableRepository,
            triedToSave: MutableStateFlow<Boolean>,
            bottomSheetType: MutableState<Goal.GoalTab?>,
            task: MutableStateFlow<Task?>?=null,
            originalTask: MutableStateFlow<Task?>?=null,
            deleteTask: (suspend () -> Unit)?=null,
            canSaveTask: (suspend () -> Boolean)?=null,
            saveTask: (suspend () -> Unit)?=null,
            deliverable: MutableStateFlow<Deliverable?>?=null,
            originalDeliverable: MutableStateFlow<Deliverable?>?=null,
            deleteDeliverable: (suspend () -> Unit)?=null,
            saveDeliverable: (suspend () -> Unit)?=null,
            marker: MutableStateFlow<Marker?>?=null,
            originalMarker: MutableStateFlow<Marker?>?=null,
            deleteMarker: (suspend () -> Unit)?=null,
            canSaveMarker: (() -> Boolean)?=null,
            saveMarker: (suspend () -> Unit)?=null,
            showError: (Int, FocusRequester) -> Unit,
            dismissBottomSheet: (suspend () -> Unit)
        ) {
            triedToSave.value = true
            when (bottomSheetType.value) {
                Goal.GoalTab.TASKS
                    if task?.value != null
                    -> {
                    if (!(canSaveTask?.invoke()?:return)) return
                    if (originalTask?.value != null) {
                        deleteTask?.invoke()
                        repository.cloneTaskTo(task, originalTask)
                        task.value = originalTask.value
                        saveTask?.invoke()
                    } else {
                        saveTask?.invoke()
                    }
                    task.value = null
                }

                Goal.GoalTab.DELIVERABLES
                    if deliverable?.value != null
                    -> {
                    if (!canSaveDeliverable(
                            deliverable,
                            showError
                        )) return
                    if (originalDeliverable?.value != null) {
                        deleteDeliverable?.invoke()
                        repository.cloneDeliverableTo(deliverable, originalDeliverable)
                        deliverable.value = originalDeliverable.value
                        saveDeliverable?.invoke()
                    } else {
                        saveDeliverable?.invoke()
                    }
                    deliverable.value = null
                }

                Goal.GoalTab.MARKERS
                    if marker?.value != null
                    -> {
                    if (!(canSaveMarker?.invoke()?:return)) return
                    if (originalMarker?.value != null) {
                        deleteMarker?.invoke()
                        repository.cloneMarkerTo(marker, originalMarker)
                        marker.value = originalMarker.value
                        saveMarker?.invoke()
                    } else {
                        saveMarker?.invoke()
                    }
                    marker.value = null
                }

                else -> {}
            }
            dismissBottomSheet()
        }

        suspend fun addDeliverableCompanionObject(
            repository: AccountableRepository,
            goal: Goal?,
            saveDeliverable: (suspend () -> Unit),
            showBottomSheet: (Goal.GoalTab) -> Unit,
            deliverable: MutableStateFlow<Deliverable?>,
            originalTask: MutableStateFlow<Task?>?=null,
            originalDeliverable: MutableStateFlow<Deliverable?>?=null,
            originalMarker: MutableStateFlow<Marker?>?=null
        ) {
            originalTask?.value = null
            originalDeliverable?.value = null
            originalMarker?.value = null
            goal?.let { goal ->
                deliverable.value = Deliverable(
                    parent = goal.id ?:return,
                    position = repository.getDeliverablesWithTimes(
                        goal.id?:return
                    ).first().size.toLong(),
                    location = goal.location
                )
            }?:return
            saveDeliverable()
            showBottomSheet(Goal.GoalTab.DELIVERABLES)
        }

        suspend fun addTimeBlockCompanionObject(
            bottomSheetType: MutableState<Goal.GoalTab?>,
            saveTime: (suspend (GoalTaskDeliverableTime) -> Long),
            task: MutableStateFlow<Task?>?=null,
            saveTask: (suspend () -> Unit)?=null,
            deliverable: MutableStateFlow<Deliverable?>?=null,
            saveDeliverable: (suspend () -> Unit)?=null,
        ) {
            withContext(Dispatchers.IO){
                if (
                    bottomSheetType.value == Goal.GoalTab.TASKS
                    && task?.value != null
                ){
                    if (task.value?.id == null) saveTask?.invoke()
                    task.value?.id?.let {
                        val newTime = GoalTaskDeliverableTime(
                            parent = it,
                            type = GoalTaskDeliverableTime.TimesType.TASK.name
                        )
                        newTime.id = saveTime(newTime)
                    }
                }
                else if (
                    bottomSheetType.value == Goal.GoalTab.DELIVERABLES
                    && deliverable?.value != null
                ){
                    if (deliverable.value?.id == null) saveDeliverable?.invoke()
                    deliverable.value?.id?.let {
                        val newTime = GoalTaskDeliverableTime(
                            parent = it,
                            type = GoalTaskDeliverableTime.TimesType.DELIVERABLE.name
                        )
                        newTime.id = saveTime(newTime)
                    }
                }
            }
        }

        suspend fun deleteTimeBlockCompanionObject(
            repository: AccountableRepository,
            bottomSheetType: MutableState<Goal.GoalTab?>,
            timeBlock: GoalTaskDeliverableTime,
            task: MutableStateFlow<Task?>?=null,
            deliverable: MutableStateFlow<Deliverable?>?=null,
        ) {
            withContext(Dispatchers.IO) {
                if (
                    bottomSheetType.value == Goal.GoalTab.TASKS
                    && task?.value != null
                ){
                    repository.deleteGoalTaskDeliverableTime(timeBlock)
                }
                else if (
                    bottomSheetType.value == Goal.GoalTab.DELIVERABLES
                    && deliverable?.value != null
                ){
                    repository.deleteGoalTaskDeliverableTime(timeBlock)
                }
            }
        }

        val Factory : ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T
            {
                // Get the Application object from extras
                val application = checkNotNull(extras[APPLICATION_KEY])

                val accountableRepository = AccountableRepository.getInstance(application)

                return TaskViewModel(
                    accountableRepository
                ) as T
            }
        }
    }
}