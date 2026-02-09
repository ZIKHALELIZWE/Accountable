package com.thando.accountable.fragments.viewmodels

import android.net.Uri
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneOffset

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
    val deliverable: MutableStateFlow<Flow<Deliverable?>?> = MutableStateFlow(null)
    val originalDeliverable = MutableStateFlow<Flow<Deliverable?>?>(null)
    val triedToSaveBottomSheet = MutableStateFlow(false)
    val bottomSheetType = MutableStateFlow<Goal.GoalTab?>(null)

    val showEndTypeOptions = MutableStateFlow(false)
    val buttonDatePick = MutableStateFlow(false)
    val buttonTimePick = MutableStateFlow(false)
    val endTypeOptions = MutableStateFlow(listOf<MenuItemData>())

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
        newGoal.value?.first()?.let{ newGoal ->
            repository.update(newGoal.copy(goal = goal))
        }
    }

    suspend fun updateLocation(location: String) {
        newGoal.value?.first()?.let{ newGoal ->
            repository.update(newGoal.copy(location = location))
        }
    }

    suspend fun updatePickedDate(pickedDate: LocalDateTime) {
        newGoal.value?.first()?.let{ newGoal ->
            repository.update(newGoal.copy(
                endDateTime = pickedDate.toInstant(ZoneOffset.UTC).toEpochMilli()
            ))
        }
    }

    suspend fun updateEndType( endType: Goal.GoalEndType) {
        newGoal.value?.first()?.let{ newGoal ->
            repository.update(newGoal.copy(
                endType = endType.name
            ))
        }
    }

    fun pickColour(originalColour: Color? = null){
        colourPickerDialog.pickColour(originalColour){ selectedColour: Int ->
            newGoal.value?.first()?.let { goal -> repository.update(goal.copy(colour = selectedColour)) }
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
            deliverable = deliverable
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
        originalDeliverableInput.id?.let { id ->
            editDeliverable(repository.getDeliverable(id))
        }
    }

    suspend fun editDeliverable(originalDeliverableInput: Flow<Deliverable?>){
        editClickedDeliverable(
            originalDeliverableInput = originalDeliverableInput,
            repository = repository,
            originalTask = null,
            originalDeliverable = originalDeliverable,
            originalMarker = null,
            deliverable = deliverable,
            saveDeliverable = ::saveDeliverable,
            showBottomSheet = ::showBottomSheet
        )
    }

    suspend fun addDeliverable() {
        addDeliverableCompanionObject(
            repository = repository,
            goal = newGoal.value?.first(),
            saveDeliverable = ::saveDeliverable,
            showBottomSheet = ::showBottomSheet,
            deliverable = deliverable,
            originalTask = null,
            originalDeliverable = originalDeliverable,
            originalMarker = null
        )
    }

    suspend fun selectDeliverable() {

    }

    suspend fun saveDeliverable(){
        saveClickedDeliverable( repository, deliverable)
    }

    suspend fun deleteDeliverable() {
        deleteClickedDeliverable(
            repository,
            deliverable
        )
    }

    suspend fun deleteDeliverableClicked() {
        deleteDeliverableClickedCompanionObject(
            deliverable,
            originalDeliverable,
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
            deliverable = deliverable,
            originalDeliverable = originalDeliverable,
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
            deliverable = deliverable,
            deleteDeliverable = ::deleteDeliverable,
            marker = null,
            deleteMarker = null
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
        if (newGoal.value?.first()?.goal?.isEmpty() == true) {
            showError(
                R.string.please_enter_a_goal,
                goalFocusRequester
            )
            return
        }
        if (newGoal.value?.first()?.location?.isEmpty() == true) {
            showError(
                R.string.please_enter_a_location,
                locationFocusRequester
            )
            return
        }
        if (newGoal.value?.first()?.colour == -1) {
            showError(
                R.string.please_select_a_colour,
                colourFocusRequester
            )
            return
        }
        newGoal.value?.first()?.times?.value?.first()?.forEach { time ->
            val duration = LocalDateTime.ofEpochSecond(time.duration/1000,0, ZoneOffset.UTC)
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
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                // Get the Application object from extras
                val application = checkNotNull(extras[APPLICATION_KEY])

                val accountableRepository = AccountableRepository.getInstance(application)

                return EditGoalViewModel(
                    accountableRepository
                ) as T
            }
        }
    }
}