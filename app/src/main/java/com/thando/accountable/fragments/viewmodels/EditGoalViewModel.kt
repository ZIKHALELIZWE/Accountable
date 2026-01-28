package com.thando.accountable.fragments.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableIntStateOf
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
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.addDeliverableCompanionObject
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.addTimeBlockCompanionObject
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.canSaveDeliverable
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.deleteClickedDeliverable
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.deleteDeliverableClickedCompanionObject
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.deleteTimeBlockCompanionObject
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.dismissBottomSheetCompanionObject
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.editClickedDeliverable
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.loadTaskDeliverableMarkerLists
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.processBottomSheetAddCompanionObject
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.saveClickedDeliverable
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.showBottomSheetCompanionObject
import com.thando.accountable.fragments.viewmodels.TaskViewModel.Companion.showInputError
import com.thando.accountable.ui.cards.ColourPickerDialog
import kotlinx.coroutines.flow.MutableStateFlow

class EditGoalViewModel(
    private val repository: AccountableRepository
): ViewModel() {
    val newGoal = repository.getNewGoal()
    val triedToSave = MutableStateFlow(false)
    val showErrorMessage = mutableStateOf(false)
    val errorMessage = mutableIntStateOf(-1)
    val goalFocusRequester = FocusRequester()
    val locationFocusRequester = FocusRequester()
    val colourFocusRequester = FocusRequester()
    val colourPickerDialog = ColourPickerDialog()
    val deliverable: MutableStateFlow<Deliverable?> = MutableStateFlow(null)
    val originalDeliverable = MutableStateFlow<Deliverable?>(null)
    val deliverablesList: SnapshotStateList<Deliverable> = mutableStateListOf()
    val triedToSaveBottomSheet = MutableStateFlow(false)
    val bottomSheetType = mutableStateOf<Goal.GoalTab?>(null)

    suspend fun loadLists() {
        loadTaskDeliverableMarkerLists(
            repository = repository,
            goalId = newGoal.value?.id,
            task = null,
            tasksList = null,
            deliverable = deliverable,
            deliverablesList = deliverablesList,
            marker = null,
            markersList = null
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

    suspend fun setImage(uri:Uri?){
        uri?.let {
            repository.setNewGoalImage(uri)
        }
    }

    suspend fun removeImage(){
        repository.deleteNewGoalImage()
    }

    fun pickColour(originalColour: Color? = null){
        colourPickerDialog.pickColour(originalColour){ selectedColour: Int ->
            newGoal.value?.colour?.value = selectedColour
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

    suspend fun closeGoal(){
        if (bottomSheetType.value != null){
            dismissBottomSheet()
        }
        else {
            repository.clearNewGoal()
            repository.goBackToGoalsFromEditGoal()
        }
    }

    suspend fun editDeliverable(originalDeliverableInput: Deliverable){
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
            goal = newGoal.value,
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
            deliverablesList,
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
            tasksList = null,
            deleteTask = null,
            canSaveTask = null,
            saveTask = null,
            deliverable = deliverable,
            originalDeliverable = originalDeliverable,
            deliverablesList = deliverablesList,
            deleteDeliverable = ::deleteDeliverable,
            saveDeliverable = ::saveDeliverable,
            marker = null,
            originalMarker = null,
            markersList = null,
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
            tasksList = null,
            deleteTask = null,
            deliverable = deliverable,
            deliverablesList = deliverablesList,
            deleteDeliverable = ::deleteDeliverable,
            marker = null,
            markersList = null,
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
        if (newGoal.value?.goal?.text?.isEmpty() == true) {
            showError(
                R.string.please_enter_a_goal,
                goalFocusRequester
            )
            return
        }
        if (newGoal.value?.location?.text?.isEmpty() == true) {
            showError(
                R.string.please_enter_a_location,
                locationFocusRequester
            )
            return
        }
        if (newGoal.value?.colour?.value == -1) {
            showError(
                R.string.please_select_a_colour,
                colourFocusRequester
            )
            return
        }
        newGoal.value?.times?.forEach { time ->
            if (time.duration.value.hour == 0 && time.duration.value.minute == 0) {
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