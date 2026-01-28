package com.thando.accountable.fragments.viewmodels

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import kotlinx.coroutines.withContext

class TaskViewModel(val repository: AccountableRepository) : ViewModel() {

    val goal = repository.getGoal()
    val tasksList: SnapshotStateList<Task> = mutableStateListOf()
    val deliverablesList: SnapshotStateList<Deliverable> = mutableStateListOf()
    val markersList: SnapshotStateList<Marker> = mutableStateListOf()

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

    suspend fun loadLists(){
        loadTaskDeliverableMarkerLists(
            repository = repository,
            goalId = goal.value?.id,
            task = task,
            tasksList = tasksList,
            deliverable = deliverable,
            deliverablesList = deliverablesList,
            marker = marker,
            markersList = markersList,
        )
    }

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

    fun canSaveTask(): Boolean {
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
            task.times.forEach { time ->
                if (time.duration.value.hour == 0 && time.duration.value.minute == 0) {
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
            tasksList,
            ::deleteTask,
            ::canSaveTask,
            ::saveTask,
            deliverable,
            originalDeliverable,
            deliverablesList,
            ::deleteDeliverable,
            ::saveDeliverable,
            marker,
            originalMarker,
            markersList,
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
        goal.value?.let { goal ->
            task.value = Task(
                parent = goal.id?.let { mutableLongStateOf(it) }?:return,
                parentType = mutableStateOf(Task.TaskParentType.GOAL),
                position = mutableLongStateOf(repository.getTasks(
                    goal.id?:return,
                    Task.TaskParentType.GOAL
                ).size.toLong()),
                colour = mutableIntStateOf(goal.colour.value),
                location = TextFieldState(goal.location.text.toString()),
                type = mutableStateOf(Task.TaskType.NORMAL)
            )
        }?:return
        saveTask()
        showBottomSheet(Goal.GoalTab.TASKS )
    }

    suspend fun addDeliverable(){
        addDeliverableCompanionObject(
            repository = repository,
            goal = goal.value,
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
        goal.value?.let { goal ->
            marker.value = Marker(
                parent = goal.id?.let { mutableLongStateOf(it) }?:return,
                position = mutableLongStateOf(repository.getMarkers(
                    goal.id?:return
                ).size.toLong())
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
            tasksList.remove(taskToDelete)
            deleteTask()
            task.value = taskToDelete
        }
        dismissBottomSheet()
    }

    suspend fun deleteDeliverableClicked() {
        deleteDeliverableClickedCompanionObject(
            deliverable,
            originalDeliverable,
            deliverablesList,
            ::deleteDeliverable,
            ::dismissBottomSheet
        )
    }

    suspend fun deleteMarkerClicked() {
        originalMarker.value?.let { markerToDelete ->
            markersList.remove(markerToDelete)
            deleteMarker()
            marker.value = markerToDelete
        }
        dismissBottomSheet()
    }

    suspend fun saveTask() {
        task.value?.let { task ->
            task.id = repository.saveTask(task)
            task.times.forEach { saveTime(it) }
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
            task.times.forEach {
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

    suspend fun saveTime(time: GoalTaskDeliverableTime): Long {
        return repository.saveGoalTaskDeliverableTime(time)
    }

    suspend fun dismissBottomSheet() {
        dismissBottomSheetCompanionObject(
            triedToSave,
            bottomSheetType,
            task,
            tasksList,
            ::deleteTask,
            deliverable,
            deliverablesList,
            ::deleteDeliverable,
            marker,
            markersList,
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
        suspend fun loadTaskDeliverableMarkerLists(
            repository: AccountableRepository,
            goalId: Long?,
            task: MutableStateFlow<Task?>?=null,
            tasksList: SnapshotStateList<Task>?=null,
            deliverable: MutableStateFlow<Deliverable?>?=null,
            deliverablesList: SnapshotStateList<Deliverable>?=null,
            marker: MutableStateFlow<Marker?>?=null,
            markersList: SnapshotStateList<Marker>?=null
        ){
            tasksList?.clear()
            tasksList?.addAll(
                repository.getTasks(goalId?:return,
                    Task.TaskParentType.GOAL
                )
            )
            tasksList?.removeIf { taskInstance ->
                taskInstance.id != null && taskInstance.id == task?.value?.id
            }
            deliverablesList?.clear()
            deliverablesList?.addAll(
                repository.getDeliverables(goalId?:return)
            )
            deliverablesList?.removeIf { deliverableInstance ->
                deliverableInstance.id != null && deliverableInstance.id == deliverable?.value?.id
            }
            markersList?.clear()
            markersList?.addAll(
                repository.getMarkers(goalId?:return)
            )
            markersList?.removeIf { markerInstance ->
                markerInstance.id != null && markerInstance.id == marker?.value?.id
            }
        }

        fun canSaveDeliverable(deliverable: MutableStateFlow<Deliverable?>, showError:(Int, FocusRequester)->Unit): Boolean{
            deliverable.value?.let { deliverable ->
                if (deliverable.deliverable.text.isEmpty()) {
                    showError(
                        R.string.please_enter_a_deliverable,
                        deliverable.deliverableTextFocusRequester
                    )
                    return false
                }
                deliverable.times.forEach { time ->
                    if (time.duration.value.hour == 0 && time.duration.value.minute == 0) {
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
                deliverable.times.forEach {
                    repository.deleteGoalTaskDeliverableTime(it)
                }
            }
        }

        suspend fun deleteDeliverableClickedCompanionObject(
            deliverable: MutableStateFlow<Deliverable?>,
            originalDeliverable: MutableStateFlow<Deliverable?>,
            deliverablesList: SnapshotStateList<Deliverable>,
            deleteDeliverable: suspend () -> Unit,
            dismissBottomSheet: suspend () -> Unit
        ) {
            originalDeliverable.value?.let { deliverableToDelete ->
                deliverablesList.remove(deliverableToDelete)
                deleteDeliverable()
                deliverable.value = deliverableToDelete
            }
            dismissBottomSheet()
        }

        suspend fun dismissBottomSheetCompanionObject(
            triedToSave: MutableStateFlow<Boolean>,
            bottomSheetType: MutableState<Goal.GoalTab?>,
            task: MutableStateFlow<Task?>?=null,
            tasksList: SnapshotStateList<Task>?=null,
            deleteTask: (suspend () -> Unit)?=null,
            deliverable: MutableStateFlow<Deliverable?>?=null,
            deliverablesList: SnapshotStateList<Deliverable>?=null,
            deleteDeliverable: (suspend () -> Unit)?=null,
            marker: MutableStateFlow<Marker?>?=null,
            markersList: SnapshotStateList<Marker>?=null,
            deleteMarker: (suspend () -> Unit)?=null,
        ) {
            task?.value?.let {
                deleteTask?.invoke()
                tasksList?.remove(task.value)
                task.value = null
            }
            deliverable?.value?.let {
                deleteDeliverable?.invoke()
                deliverablesList?.remove(deliverable.value)
                deliverable.value = null
            }
            marker?.value?.let {
                deleteMarker?.invoke()
                markersList?.remove(marker.value)
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
            tasksList: SnapshotStateList<Task>?=null,
            deleteTask: (suspend () -> Unit)?=null,
            canSaveTask: (() -> Boolean)?=null,
            saveTask: (suspend () -> Unit)?=null,
            deliverable: MutableStateFlow<Deliverable?>?=null,
            originalDeliverable: MutableStateFlow<Deliverable?>?=null,
            deliverablesList: SnapshotStateList<Deliverable>?=null,
            deleteDeliverable: (suspend () -> Unit)?=null,
            saveDeliverable: (suspend () -> Unit)?=null,
            marker: MutableStateFlow<Marker?>?=null,
            originalMarker: MutableStateFlow<Marker?>?=null,
            markersList: SnapshotStateList<Marker>?=null,
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
                        tasksList?.add(task.value!!)
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
                        deliverablesList?.add(deliverable.value!!)
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
                        markersList?.add(marker.value!!)
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
                    parent = goal.id?.let { mutableLongStateOf(it) }?:return,
                    position = mutableLongStateOf(repository.getDeliverables(
                        goal.id?:return
                    ).size.toLong()),
                    location = TextFieldState(goal.location.text.toString())
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
                            parent = mutableLongStateOf(it),
                            type = mutableStateOf(GoalTaskDeliverableTime.TimesType.TASK)
                        )
                        newTime.id = saveTime(newTime)
                        withContext(Dispatchers.Main) {
                            task.value?.times?.add(newTime)
                        }
                    }
                }
                else if (
                    bottomSheetType.value == Goal.GoalTab.DELIVERABLES
                    && deliverable?.value != null
                ){
                    if (deliverable.value?.id == null) saveDeliverable?.invoke()
                    deliverable.value?.id?.let {
                        val newTime = GoalTaskDeliverableTime(
                            parent = mutableLongStateOf(it),
                            type = mutableStateOf(GoalTaskDeliverableTime.TimesType.DELIVERABLE)
                        )
                        newTime.id = saveTime(newTime)
                        withContext(Dispatchers.Main) {
                            deliverable.value?.times?.add(newTime)
                        }
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
                    withContext(Dispatchers.Main) {
                        task.value?.times?.remove(timeBlock)
                    }
                }
                else if (
                    bottomSheetType.value == Goal.GoalTab.DELIVERABLES
                    && deliverable?.value != null
                ){
                    repository.deleteGoalTaskDeliverableTime(timeBlock)
                    withContext(Dispatchers.Main) {
                        deliverable.value?.times?.remove(timeBlock)
                    }
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