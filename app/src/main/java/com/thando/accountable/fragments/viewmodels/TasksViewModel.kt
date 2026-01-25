package com.thando.accountable.fragments.viewmodels

import androidx.compose.foundation.text.input.TextFieldState
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
        tasksList.clear()
        tasksList.addAll(
                repository.getTasks(goal.value?.id?:return,
                    Task.TaskParentType.GOAL
                )
        )
        tasksList.removeIf { taskInstance ->
            taskInstance.id != null && taskInstance.id == task.value?.id
        }
        deliverablesList.clear()
        deliverablesList.addAll(
            repository.getDeliverables(goal.value?.id?:return)
        )
        deliverablesList.removeIf { deliverableInstance ->
            deliverableInstance.id != null && deliverableInstance.id == deliverable.value?.id
        }
        markersList.clear()
        markersList.addAll(
            repository.getMarkers(goal.value?.id?:return)
        )
        markersList.removeIf { markerInstance ->
            markerInstance.id != null && markerInstance.id == marker.value?.id
        }
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

    fun canSaveDeliverable(): Boolean{
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

    private fun showError(message: Int, focusRequester: FocusRequester){
        errorMessage.intValue = message
        showErrorMessage.value = true
        focusRequester.requestFocus()
    }

    fun pickColour(originalColour: Color? = null){
        task.value?.let { task ->
            colourPickerDialog.pickColour(originalColour) { selectedColour: Int ->
                task.colour.value = selectedColour
            }
        }
    }

    suspend fun processBottomSheetAdd(){
        triedToSave.value = true
        when (bottomSheetType.value) {
            Goal.GoalTab.TASKS
                if task.value != null
                -> {
                if (!canSaveTask()) return
                if (originalTask.value != null) {
                    deleteTask()
                    repository.cloneTaskTo(task, originalTask)
                    task.value = originalTask.value
                    saveTask()
                } else {
                    saveTask()
                    tasksList.add(task.value!!)
                }
                task.value = null
            }

            Goal.GoalTab.DELIVERABLES
                if deliverable.value != null
                -> {
                if (!canSaveDeliverable()) return
                if (originalDeliverable.value != null) {
                    deleteDeliverable()
                    repository.cloneDeliverableTo(deliverable, originalDeliverable)
                    deliverable.value = originalDeliverable.value
                    saveDeliverable()
                } else {
                    saveDeliverable()
                    deliverablesList.add(deliverable.value!!)
                }
                deliverable.value = null
            }

            Goal.GoalTab.MARKERS
                if marker.value != null
                -> {
                if (!canSaveMarker()) return
                if (originalMarker.value != null) {
                    deleteMarker()
                    repository.cloneMarkerTo(marker, originalMarker)
                    marker.value = originalMarker.value
                    saveMarker()
                } else {
                    saveMarker()
                    markersList.add(marker.value!!)
                }
                marker.value = null
            }

            else -> {}
        }
        dismissBottomSheet()
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
        originalTask.value = null
        originalDeliverable.value = null
        originalMarker.value = null
        goal.value?.let { goal ->
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
        originalTask.value = null
        originalDeliverable.value = originalDeliverableInput
        originalMarker.value = null
        deliverable.value = repository.getDeliverableClone(originalDeliverableInput)?:return
        saveDeliverable()
        showBottomSheet(Goal.GoalTab.DELIVERABLES)
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
        originalDeliverable.value?.let { deliverableToDelete ->
            deliverablesList.remove(deliverableToDelete)
            deleteDeliverable()
            deliverable.value = deliverableToDelete
        }
        dismissBottomSheet()
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
        deliverable.value?.let { deliverable ->
            deliverable.id = repository.saveDeliverable(deliverable)
        }
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
        deliverable.value?.let { deliverable ->
            repository.deleteDeliverable(deliverable)
            deliverable.times.forEach {
                repository.deleteGoalTaskDeliverableTime(it)
            }
        }
    }

    suspend fun deleteMarker() {
        marker.value?.let { marker ->
            repository.deleteMarker(marker)
        }
    }

    suspend fun addTimeBlock(){
        withContext(Dispatchers.IO){
            if (
                bottomSheetType.value == Goal.GoalTab.TASKS
                && task.value != null
            ){
                if (task.value?.id == null) saveTask()
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
                && deliverable.value != null
            ){
                if (deliverable.value?.id == null) saveDeliverable()
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

    suspend fun deleteTimeBlock(timeBlock: GoalTaskDeliverableTime){
        withContext(Dispatchers.IO) {
            if (
                bottomSheetType.value == Goal.GoalTab.TASKS
                && task.value != null
            ){
                repository.deleteGoalTaskDeliverableTime(timeBlock)
                withContext(Dispatchers.Main) {
                    task.value?.times?.remove(timeBlock)
                }
            }
            else if (
                bottomSheetType.value == Goal.GoalTab.DELIVERABLES
                && deliverable.value != null
            ){
                repository.deleteGoalTaskDeliverableTime(timeBlock)
                withContext(Dispatchers.Main) {
                    deliverable.value?.times?.remove(timeBlock)
                }
            }
        }
    }

    suspend fun saveTime(time: GoalTaskDeliverableTime): Long {
        return repository.saveGoalTaskDeliverableTime(time)
    }

    suspend fun dismissBottomSheet() {
        task.value?.let {
            deleteTask()
            tasksList.remove(task.value)
            task.value = null
        }
        deliverable.value?.let {
            deleteDeliverable()
            deliverablesList.remove(deliverable.value)
            deliverable.value = null
        }
        marker.value?.let {
            deleteMarker()
            markersList.remove(marker.value)
            marker.value = null
        }
        triedToSave.value = false
        bottomSheetType.value = null
    }

    fun showBottomSheet(sheetType: Goal.GoalTab) {
        triedToSave.value = false
        bottomSheetType.value = sheetType
    }

    suspend fun closeTasks(){
        if (bottomSheetType.value != null){
            dismissBottomSheet()
        }
        else repository.goBackToGoalsFromTasks()
    }

    companion object{
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