package com.thando.accountable.fragments.viewmodels

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.thando.accountable.AccountableRepository
import com.thando.accountable.R
import com.thando.accountable.database.Converters
import com.thando.accountable.database.tables.Deliverable
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import com.thando.accountable.database.tables.Marker
import com.thando.accountable.database.tables.Task
import com.thando.accountable.ui.cards.ColourPickerDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(val repository: AccountableRepository) : ViewModel() {

    val goal = repository.getGoal()

    val bottomSheetType = MutableStateFlow<Goal.GoalTab?>(null)
    val triedToSave = MutableStateFlow(false)
    val showErrorMessage = mutableStateOf(false)
    val errorMessage = mutableIntStateOf(-1)
    val colourPickerDialog = ColourPickerDialog()

    val originalTask = MutableStateFlow<Flow<Task?>?>(null)
    private val originalDeliverableState = MutableStateFlow<Flow<Deliverable?>?>(null)
    val originalDeliverable = originalDeliverableState.flatMapLatest { it?:flowOf(null) }
    val originalMarker = MutableStateFlow<Flow<Marker?>?>(null)
    private val taskState: MutableStateFlow<Flow<Task?>?> = MutableStateFlow(null)
    val task = taskState.flatMapLatest { it?:flowOf(null) }
    private val deliverableState: MutableStateFlow<Flow<Deliverable?>?> = MutableStateFlow(null)
    val deliverable = deliverableState.flatMapLatest { it?: flowOf(null) }
    private val markerState: MutableStateFlow<Flow<Marker?>?> = MutableStateFlow(null)
    val marker = markerState.flatMapLatest { it?:flowOf(null) }

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
        taskState.value?.first()?.let { task ->
            if (task.task.isEmpty()) {
                showError(
                    R.string.please_enter_a_task,
                    task.taskTextFocusRequester
                )
                return false
            }
            if (task.location.isEmpty()) {
                showError(
                    R.string.please_enter_a_location,
                    task.locationFocusRequester
                )
                return false
            }
            if (task.colour == -1) {
                showError(
                    R.string.please_select_a_colour,
                    task.colourFocusRequester
                )
                return false
            }
            task.times.first().forEach { time ->
                val duration = Converters().toLocalDateTime(
                    time.duration
                ).value
                if (duration.hour == 0 && duration.minute == 0) {
                    showError(
                        R.string.please_select_a_duration,
                        time.durationPickerFocusRequester
                    )
                    return false
                }
            }
            return true
        }
        return false
    }

    suspend fun canSaveMarker(): Boolean{
        markerState.value?.first()?.let { marker ->
            if (marker.marker.isEmpty()) {
                showError(
                    R.string.please_enter_a_marker,
                    marker.markerTextFocusRequester
                )
                return false
            }
            return true
        }
        return false
    }

    fun pickColour(originalColour: Color? = null){
        colourPickerDialog.pickColour(originalColour) { selectedColour: Int ->
            taskState.value?.first()?.let { task -> repository.update(task.copy(colour = selectedColour)) }
        }
    }

    suspend fun processBottomSheetAdd(){
        processBottomSheetAddCompanionObject(
            repository,
            triedToSave,
            bottomSheetType,
            taskState,
            originalTask,
            ::deleteTask,
            ::canSaveTask,
            ::saveTask,
            deliverableState,
            originalDeliverableState,
            ::deleteDeliverable,
            ::saveDeliverable,
            markerState,
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
        originalDeliverableState.value = null
        originalMarker.value = null
        goal.first()?.let { goal ->
            taskState.value = repository.getTask(repository.insert(Task(
                parent = goal.id ?:return,
                parentType = Task.TaskParentType.GOAL.name,
                position = repository.getTasks(
                    goal.id?:return,
                    Task.TaskParentType.GOAL
                ).first().size.toLong(),
                colour = goal.colour,
                location = goal.location,
                type = Task.TaskType.NORMAL.name
            )))
        }?:return
        saveTask()
        showBottomSheet(Goal.GoalTab.TASKS )
    }

    suspend fun addDeliverable(){
        addDeliverableCompanionObject(
            repository = repository,
            goal = goal.first(),
            saveDeliverable = ::saveDeliverable,
            showBottomSheet = ::showBottomSheet,
            deliverable = deliverableState,
            originalTask = originalTask,
            originalDeliverable = originalDeliverableState,
            originalMarker = originalMarker
        )
    }

    suspend fun addMarker(){
        originalTask.value = null
        originalDeliverableState.value = null
        originalMarker.value = null
        goal.first()?.let { goal ->
            markerState.value = repository.getMarker(repository.insert(Marker(
                parent = goal.id ?:return,
                position = repository.getMarkers(
                    goal.id?:return
                ).first().size.toLong()
            )))
        }?:return
        saveMarker()
        showBottomSheet(Goal.GoalTab.MARKERS)
    }

    suspend fun editTask(originalTaskInput: Task){
        originalTaskInput.id?.let { originalTaskInputId ->
            originalTask.value = repository.getTask(originalTaskInputId)
            originalDeliverableState.value = null
            originalMarker.value = null
            taskState.value = repository.getTaskClone(originalTask.value?:return, true)?:return
        }
        saveTask()
        showBottomSheet(Goal.GoalTab.TASKS)
    }

    suspend fun editDeliverable(originalDeliverableInput: Deliverable) {
        originalDeliverableInput.id?.let { id ->
            editDeliverable(repository.getDeliverable(id))
        }
    }

    suspend fun editDeliverable(originalDeliverableInput: Flow<Deliverable?>){
        editClickedDeliverable(
            originalDeliverableInput = originalDeliverableInput,
            repository = repository,
            originalTask = originalTask,
            originalDeliverable = originalDeliverableState,
            originalMarker = originalMarker,
            deliverable = deliverableState,
            saveDeliverable = ::saveDeliverable,
            showBottomSheet = ::showBottomSheet
        )
    }

    suspend fun editMarker(originalMarkerInput: Marker){
        originalMarkerInput.id?.let { markerId ->
            originalTask.value = null
            originalDeliverableState.value = null
            originalMarker.value = repository.getMarker(markerId)
            markerState.value = repository.getMarkerClone(originalMarker.value?:return, true) ?: return
        }
        saveMarker()
        showBottomSheet(Goal.GoalTab.MARKERS)
    }

    suspend fun deleteTaskClicked() {
        originalTask.value?.let { taskToDelete ->
            deleteTask()
            taskState.value = taskToDelete
        }
        dismissBottomSheet()
    }

    suspend fun deleteDeliverableClicked() {
        deleteDeliverableClickedCompanionObject(
            deliverableState,
            originalDeliverableState,
            ::deleteDeliverable,
            ::dismissBottomSheet
        )
    }

    suspend fun deleteMarkerClicked() {
        originalMarker.value?.let { markerToDelete ->
            deleteMarker()
            markerState.value = markerToDelete
        }
        dismissBottomSheet()
    }

    suspend fun saveTask() {
        taskState.value?.first()?.let { task ->
            task.id = repository.saveTask(task)
            task.times.first().forEach { saveTime(it) }
        }
    }

    suspend fun saveDeliverable() {
        saveClickedDeliverable(repository, deliverableState)
    }

    suspend fun saveMarker() {
        markerState.value?.first()?.let { marker ->
            marker.id = repository.saveMarker(marker)
        }
    }

    suspend fun deleteTask(){
        taskState.value?.first()?.let { task ->
            repository.deleteTask(task)
            task.times.first().forEach {
                repository.deleteGoalTaskDeliverableTime(it)
            }
        }
    }

    suspend fun deleteDeliverable(){
        deleteClickedDeliverable(repository, deliverableState)
    }

    suspend fun deleteMarker() {
        markerState.value?.first()?.let { marker ->
            repository.deleteMarker(marker)
        }
    }

    suspend fun addTimeBlock(){
        addTimeBlockCompanionObject(
            bottomSheetType,
            ::saveTime,
            taskState,
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
            task = taskState,
            deliverable = deliverableState
        )
    }

    suspend fun updateTimeBlock(timeBlock: GoalTaskDeliverableTime) {
        repository.update(timeBlock)
    }

    suspend fun updateDeliverable(deliverable: Deliverable) {
        repository.update(deliverable)
    }

    suspend fun updateTask(task: Task) {
        repository.update(task)
    }

    suspend fun updateMarker(marker: Marker) {
        repository.update(marker)
    }

    suspend fun updateGoal(goal: Goal) {
        repository.update(goal)
    }

    suspend fun saveTime(time: GoalTaskDeliverableTime): Long {
        return repository.saveGoalTaskDeliverableTime(time)
    }

    suspend fun dismissBottomSheet() {
        dismissBottomSheetCompanionObject(
            triedToSave,
            bottomSheetType,
            taskState,
            ::deleteTask,
            deliverableState,
            ::deleteDeliverable,
            markerState,
            ::deleteMarker,
            originalTask,
            originalDeliverableState,
            originalMarker
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
        suspend fun canSaveDeliverable(deliverable: MutableStateFlow<Flow<Deliverable?>?>, showError:(Int, FocusRequester)->Unit): Boolean{
            deliverable.value?.first()?.let { deliverable ->
                if (deliverable.deliverable.isEmpty()) {
                    showError(
                        R.string.please_enter_a_deliverable,
                        deliverable.deliverableTextFocusRequester
                    )
                    return false
                }
                if (deliverable.location.isEmpty()) {
                    showError(
                        R.string.please_enter_a_location,
                        deliverable.locationFocusRequester
                    )
                    return false
                }
                when (Deliverable.DeliverableEndType.valueOf(deliverable.endType)){
                    Deliverable.DeliverableEndType.UNDEFINED -> {
                        // Do nothing
                    }
                    Deliverable.DeliverableEndType.DATE -> {
                        // I do not think there is anything to do here as well
                        // Do I allow all dates (even past ones)?
                    }
                    Deliverable.DeliverableEndType.GOAL -> {
                        // Do nothing (whenever the goal ends it ends as well)
                    }
                    Deliverable.DeliverableEndType.WORK -> {
                        // Can complete a task a number of times
                        // Can have a cumulative amount for the quantity/time
                    }
                }
                deliverable.times.first().forEach { time ->
                    val duration = Converters().toLocalDateTime(
                        time.duration
                    ).value
                    if (duration.hour == 0 && duration.minute == 0) {
                        showError(
                            R.string.please_select_a_duration,
                            time.durationPickerFocusRequester
                        )
                        return false
                    }
                }
                return true
            }
            return false
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
            originalDeliverableInput: Flow<Deliverable?>,
            repository: AccountableRepository,
            originalTask: MutableStateFlow<Flow<Task?>?>?=null,
            originalDeliverable: MutableStateFlow<Flow<Deliverable?>?>,
            originalMarker: MutableStateFlow<Flow<Marker?>?>?=null,
            deliverable: MutableStateFlow<Flow<Deliverable?>?>,
            saveDeliverable: suspend () -> Unit,
            showBottomSheet: suspend (Goal.GoalTab) -> Unit
        ){
            originalTask?.value = null
            originalDeliverable.value = originalDeliverableInput
            originalMarker?.value = null
            deliverable.value = repository.getDeliverableClone(originalDeliverableInput, true)?:return
            saveDeliverable()
            showBottomSheet(Goal.GoalTab.DELIVERABLES)
        }

        suspend fun saveClickedDeliverable(
            repository: AccountableRepository,
            deliverable: MutableStateFlow<Flow<Deliverable?>?>
        ) {
            deliverable.value?.first()?.let { deliverable ->
                deliverable.id = repository.saveDeliverable(deliverable)
            }
        }

        suspend fun deleteClickedDeliverable(
            repository: AccountableRepository,
            deliverable: MutableStateFlow<Flow<Deliverable?>?>
        ) {
            deliverable.value?.first()?.let { deliverable ->
                deliverable.times.first().forEach {
                    repository.deleteGoalTaskDeliverableTime(it)
                }
                repository.deleteDeliverable(deliverable)
            }
        }

        suspend fun deleteDeliverableClickedCompanionObject(
            deliverable: MutableStateFlow<Flow<Deliverable?>?>,
            originalDeliverable: MutableStateFlow<Flow<Deliverable?>?>,
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
            bottomSheetType: MutableStateFlow<Goal.GoalTab?>,
            task: MutableStateFlow<Flow<Task?>?>?=null,
            deleteTask: (suspend () -> Unit)?=null,
            deliverable: MutableStateFlow<Flow<Deliverable?>?>?=null,
            deleteDeliverable: (suspend () -> Unit)?=null,
            marker: MutableStateFlow<Flow<Marker?>?>?=null,
            deleteMarker: (suspend () -> Unit)?=null,
            originalTask: MutableStateFlow<Flow<Task?>?>?=null,
            originalDeliverable: MutableStateFlow<Flow<Deliverable?>?>?=null,
            originalMarker: MutableStateFlow<Flow<Marker?>?>?=null,
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
            originalTask?.value?.let {
                originalTask.value = null
            }
            originalDeliverable?.value?.let {
                originalDeliverable.value = null
            }
            originalMarker?.value?.let {
                originalMarker.value = null
            }
            triedToSave.value = false
            bottomSheetType.value = null
        }

        fun showBottomSheetCompanionObject(
                sheetType: Goal.GoalTab,
                triedToSave: MutableStateFlow<Boolean>,
                bottomSheetType: MutableStateFlow<Goal.GoalTab?>
        ) {
            triedToSave.value = false
            bottomSheetType.value = sheetType
        }

        suspend fun processBottomSheetAddCompanionObject(
            repository: AccountableRepository,
            triedToSave: MutableStateFlow<Boolean>,
            bottomSheetType: MutableStateFlow<Goal.GoalTab?>,
            task: MutableStateFlow<Flow<Task?>?>?=null,
            originalTask: MutableStateFlow<Flow<Task?>?>?=null,
            deleteTask: (suspend () -> Unit)?=null,
            canSaveTask: (suspend () -> Boolean)?=null,
            saveTask: (suspend () -> Unit)?=null,
            deliverable: MutableStateFlow<Flow<Deliverable?>?>?=null,
            originalDeliverable: MutableStateFlow<Flow<Deliverable?>?>?=null,
            deleteDeliverable: (suspend () -> Unit)?=null,
            saveDeliverable: (suspend () -> Unit)?=null,
            marker: MutableStateFlow<Flow<Marker?>?>?=null,
            originalMarker: MutableStateFlow<Flow<Marker?>?>?=null,
            deleteMarker: (suspend () -> Unit)?=null,
            canSaveMarker: (suspend () -> Boolean)?=null,
            saveMarker: (suspend () -> Unit)?=null,
            showError: (Int, FocusRequester) -> Unit,
            dismissBottomSheet: (suspend () -> Unit)
        ) {
            triedToSave.value = true
            when (bottomSheetType.value) {
                Goal.GoalTab.TASKS -> {
                    if (task?.value != null) {
                        if (!(canSaveTask?.invoke() ?: return)) return
                        if (originalTask?.value != null) {
                            repository.cloneTaskTo(task, originalTask, false)
                            deleteTask?.invoke()
                            task.value = originalTask.value
                            saveTask?.invoke()
                        } else {
                            saveTask?.invoke()
                        }
                        task.value = null
                    }
                }

                Goal.GoalTab.DELIVERABLES -> {
                    if (deliverable?.value != null) {
                        if (!canSaveDeliverable(
                                deliverable,
                                showError
                            )
                        ) return
                        if (originalDeliverable?.value != null) {
                            repository.cloneDeliverableTo(deliverable, originalDeliverable, false)
                            deleteDeliverable?.invoke()
                            deliverable.value = originalDeliverable.value
                            saveDeliverable?.invoke()
                        } else {
                            saveDeliverable?.invoke()
                        }
                        deliverable.value = null
                    }
                }

                Goal.GoalTab.MARKERS -> {
                    if (marker?.value != null) {
                        if (!(canSaveMarker?.invoke() ?: return)) return
                        if (originalMarker?.value != null) {
                            repository.cloneMarkerTo(marker, originalMarker, false)
                            deleteMarker?.invoke()
                            marker.value = originalMarker.value
                            saveMarker?.invoke()
                        } else {
                            saveMarker?.invoke()
                        }
                        marker.value = null
                    }
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
            deliverable: MutableStateFlow<Flow<Deliverable?>?>,
            originalTask: MutableStateFlow<Flow<Task?>?>?=null,
            originalDeliverable: MutableStateFlow<Flow<Deliverable?>?>?=null,
            originalMarker: MutableStateFlow<Flow<Marker?>?>?=null
        ) {
            originalTask?.value = null
            originalDeliverable?.value = null
            originalMarker?.value = null
            goal?.let { goal ->
                deliverable.value = repository.getDeliverable(repository.insert(Deliverable(
                    parent = goal.id ?:return,
                    position = repository.getDeliverables(
                        goal.id?:return
                    ).first().size.toLong(),
                    location = goal.location
                )))
            }?:return
            saveDeliverable()
            showBottomSheet(Goal.GoalTab.DELIVERABLES)
        }

        suspend fun addTimeBlockCompanionObject(
            bottomSheetType: MutableStateFlow<Goal.GoalTab?>,
            saveTime: (suspend (GoalTaskDeliverableTime) -> Long),
            task: MutableStateFlow<Flow<Task?>?>?=null,
            saveTask: (suspend () -> Unit)?=null,
            deliverable: Flow<Deliverable?>?=null,
            saveDeliverable: (suspend () -> Unit)?=null,
        ) {
            if (
                bottomSheetType.value == Goal.GoalTab.TASKS
                && task?.value?.first() != null
            ){
                if (task.value?.first()?.id == null) saveTask?.invoke()
                task.value?.first()?.id?.let {
                    val newTime = GoalTaskDeliverableTime(
                        parent = it,
                        type = GoalTaskDeliverableTime.TimesType.TASK.name
                    )
                    newTime.id = saveTime(newTime)
                }
            }
            else if (
                bottomSheetType.value == Goal.GoalTab.DELIVERABLES
                && deliverable?.first() != null
            ){
                if (deliverable.first()?.id == null) saveDeliverable?.invoke()
                deliverable.first()?.id?.let {
                    val newTime = GoalTaskDeliverableTime(
                        parent = it,
                        type = GoalTaskDeliverableTime.TimesType.DELIVERABLE.name
                    )
                    newTime.id = saveTime(newTime)
                }
            }
        }

        suspend fun deleteTimeBlockCompanionObject(
            repository: AccountableRepository,
            bottomSheetType: MutableStateFlow<Goal.GoalTab?>,
            timeBlock: GoalTaskDeliverableTime,
            task: MutableStateFlow<Flow<Task?>?>?=null,
            deliverable: MutableStateFlow<Flow<Deliverable?>?>?=null,
        ) {
            if (
                bottomSheetType.value == Goal.GoalTab.TASKS
                && task?.value != null
            ){
                repository.deleteGoalTaskDeliverableTime(timeBlock)
            }
            else if (
                bottomSheetType.value == Goal.GoalTab.DELIVERABLES
                && deliverable?.value?.first() != null
            ){
                repository.deleteGoalTaskDeliverableTime(timeBlock)
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