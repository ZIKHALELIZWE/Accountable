package com.thando.accountable.fragments.viewmodels

import android.net.Uri
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.thando.accountable.AccountableRepository
import com.thando.accountable.MainActivity
import com.thando.accountable.R
import com.thando.accountable.database.Converters
import com.thando.accountable.database.tables.Deliverable
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import com.thando.accountable.database.tables.Marker
import com.thando.accountable.database.tables.Task
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.addDeliverableCompanionObject
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.addTimeBlockCompanionObject
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.deleteClickedDeliverable
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.deleteDeliverableClickedCompanionObject
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.deleteTimeBlockCompanionObject
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.dismissBottomSheetCompanionObject
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.editClickedDeliverable
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.processBottomSheetAddCompanionObject
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.saveClickedDeliverable
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.showBottomSheetCompanionObject
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.showInputError
import com.thando.accountable.ui.MenuItemData
import com.thando.accountable.ui.cards.ColourPickerDialog
import com.thando.accountable.ui.cards.DeliverableAdderDialog
import com.thando.accountable.ui.cards.DeliverablePickerDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class EditGoalViewModel(
    private val repository: AccountableRepository
): ViewModel() {
    val editGoal = repository.getEditGoal()
    val newGoal = repository.getNewGoal()
    val triedToSave = MutableStateFlow(false)
    val showErrorMessage = mutableStateOf(false)
    val errorMessage = mutableIntStateOf(-1)
    val goalFocusRequester = FocusRequester()
    val locationFocusRequester = FocusRequester()
    val colourFocusRequester = FocusRequester()
    val colourPickerDialog = ColourPickerDialog()
    val addDeliverableDialog = DeliverableAdderDialog()
    val pickDeliverableDialog = DeliverablePickerDialog()
    private val deliverableState: MutableStateFlow<Flow<Deliverable?>?> = MutableStateFlow(null)
    val deliverable = deliverableState.flatMapLatest { it?:flowOf(null) }
    private val originalDeliverableState = MutableStateFlow<Flow<Deliverable?>?>(null)
    val originalDeliverable = originalDeliverableState.flatMapLatest { it?: flowOf(null) }
    val triedToSaveBottomSheet = MutableStateFlow(false)
    val bottomSheetType = MutableStateFlow<Goal.GoalTab?>(null)

    val showEndTypeOptions = MutableStateFlow(false)
    val buttonDatePick = MutableStateFlow(false)
    val buttonTimePick = MutableStateFlow(false)
    val endTypeOptions = MutableStateFlow(listOf<MenuItemData>())
    val selectDeliverableDialog = MutableStateFlow(false)

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

    suspend fun setImage(uri:Uri?){
        uri?.let {
            repository.setNewGoalImage(uri)
        }
    }

    suspend fun removeImage(){
        repository.deleteNewGoalImage()
    }

    suspend fun updateGoalString(goal: String) {
        newGoal.first()?.let{ newGoal ->
            repository.update(newGoal.copy(goal = goal))
        }
    }

    suspend fun updateScrollPosition(scrollPosition: Long) {
        newGoal.first()?.let { newGoal ->
            repository.update(newGoal.copy(scrollPosition = scrollPosition))
        }
    }

    suspend fun updateLocation(location: String) {
        newGoal.first()?.let{ newGoal ->
            repository.update(newGoal.copy(location = location))
        }
    }

    suspend fun updatePickedDate(pickedDate: LocalDateTime) {
        newGoal.first()?.let{ newGoal ->
            repository.update(newGoal.copy(
                endDateTime = Converters().fromLocalDateTime(pickedDate)
            ))
        }
    }

    suspend fun updateEndType( endType: Goal.GoalEndType) {
        newGoal.first()?.let{ newGoal ->
            repository.update(newGoal.copy(
                endType = endType.name
            ))
        }
    }

    fun pickColour(originalColour: Color? = null){
        colourPickerDialog.pickColour(originalColour){ selectedColour: Int ->
            newGoal.first()?.let { goal -> repository.update(goal.copy(colour = selectedColour)) }
        }
    }

    suspend fun addTimeBlock(){
        if (bottomSheetType.value == null) repository.addNewGoalTimeBlock()
        else addTimeBlockCompanionObject(
            bottomSheetType = bottomSheetType,
            saveTime = ::saveTime,
            task = null,
            saveTask = null,
            deliverable = deliverable,
            saveDeliverable = ::saveDeliverable
        )
    }

    suspend fun saveTime(time: GoalTaskDeliverableTime): Long {
        return repository.saveGoalTaskDeliverableTime(time)
    }

    suspend fun deleteTimeBlock(timeBlock: GoalTaskDeliverableTime){
        if (bottomSheetType.value == null) repository.deleteNewGoalTimeBlock(timeBlock)
        else deleteTimeBlockCompanionObject(
            repository = repository,
            bottomSheetType = bottomSheetType,
            timeBlock = timeBlock,
            task = null,
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

    suspend fun closeGoal(){
        if (bottomSheetType.value != null){
            dismissBottomSheet()
        }
        else {
            repository.clearNewGoal()
            repository.goBackToGoalsFromEditGoal()
        }
    }

    suspend fun editDeliverable(originalDeliverableInput: Deliverable) {
        originalDeliverableInput.deliverableId?.let { id ->
            editDeliverable(repository.getDeliverable(id))
        }
    }

    suspend fun editDeliverable(originalDeliverableInput: Flow<Deliverable?>){
        editClickedDeliverable(
            originalDeliverableInput = originalDeliverableInput,
            repository = repository,
            originalTask = null,
            originalDeliverable = originalDeliverableState,
            originalMarker = null,
            deliverable = deliverableState,
            saveDeliverable = ::saveDeliverable,
            showBottomSheet = ::showBottomSheet
        )
    }

    suspend fun addDeliverable() {
        addDeliverableCompanionObject(
            repository = repository,
            goal = newGoal.first(),
            saveDeliverable = ::saveDeliverable,
            showBottomSheet = ::showBottomSheet,
            deliverable = deliverableState,
            originalTask = null,
            originalDeliverable = originalDeliverableState,
            originalMarker = null
        )
    }

    fun selectDeliverable() {
        selectDeliverableDialog.value = true
    }

    fun closeSelectDeliverableDialog() {
        selectDeliverableDialog.value = false
    }

    suspend fun saveDeliverable(){
        saveClickedDeliverable( repository, deliverableState)
    }

    suspend fun saveDeliverable(deliverable: Deliverable) {
        repository.saveDeliverable(deliverable)
    }

    suspend fun deleteDeliverable() {
        deleteClickedDeliverable(
            repository,
            deliverableState
        )
    }

    suspend fun deleteDeliverableClicked() {
        deleteDeliverableClickedCompanionObject(
            deliverableState,
            originalDeliverableState,
            ::deleteDeliverable,
            ::dismissBottomSheet
        )
    }

    suspend fun processBottomSheetAdd(){
        processBottomSheetAddCompanionObject(
            repository = repository,
            triedToSave = triedToSaveBottomSheet,
            bottomSheetType = bottomSheetType,
            task = null,
            originalTask = null,
            deleteTask = null,
            canSaveTask = null,
            saveTask = null,
            deliverable = deliverableState,
            originalDeliverable = originalDeliverableState,
            deleteDeliverable = ::deleteDeliverable,
            saveDeliverable = ::saveDeliverable,
            marker = null,
            originalMarker = null,
            deleteMarker = null,
            canSaveMarker = null,
            saveMarker = null,
            showError = ::showError,
            dismissBottomSheet = ::dismissBottomSheet
        )
    }

    suspend fun dismissBottomSheet() {
        dismissBottomSheetCompanionObject(
            triedToSave = triedToSaveBottomSheet,
            bottomSheetType = bottomSheetType,
            task = null,
            deleteTask = null,
            deliverable = deliverableState,
            deleteDeliverable = ::deleteDeliverable,
            marker = null,
            deleteMarker = null,
            originalTask = null,
            originalDeliverable = originalDeliverableState,
            originalMarker = null
        )
    }

    fun showBottomSheet(sheetType: Goal.GoalTab) {
        showBottomSheetCompanionObject(
            sheetType,
            triedToSaveBottomSheet,
            bottomSheetType
        )
    }

    suspend fun saveAndCloseGoal(){
        triedToSave.value = true
        if (newGoal.first()?.goal?.isEmpty() == true) {
            showError(
                R.string.please_enter_a_goal,
                goalFocusRequester
            )
            return
        }
        if (newGoal.first()?.location?.isEmpty() == true) {
            showError(
                R.string.please_enter_a_location,
                locationFocusRequester
            )
            return
        }
        if (newGoal.first()?.colour == -1) {
            showError(
                R.string.please_select_a_colour,
                colourFocusRequester
            )
            return
        }
        newGoal.first()?.times?.first()?.forEach { time ->
            val duration = Converters().toLocalDateTime(
                    time.duration
                ).value
            if (duration.hour == 0 && duration.minute == 0) {
                showError(
                    R.string.please_select_a_duration,
                    time.durationPickerFocusRequester
                )
                return
            }
        }
        repository.saveNewGoal { closeGoal() }
    }

    // Define ViewModel factory in a companion object
    companion object {
        val Factory: ViewModelProvider.Factory = MainActivity.getEditGoalViewModelFactory()
    }
}