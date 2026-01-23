package com.thando.accountable.fragments.viewmodels

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.thando.accountable.AccountableRepository
import com.thando.accountable.R
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import com.thando.accountable.ui.cards.ColourPickerDialog
import kotlinx.coroutines.flow.MutableStateFlow

class EditGoalViewModel(
    private val repository: AccountableRepository
): ViewModel() {

    val newGoal = repository.getNewGoal()

    val triedToSave = MutableStateFlow(false)
    val showErrorMessage = mutableStateOf(false)
    val errorMessage = mutableStateOf("")
    val goalFocusRequester = FocusRequester()
    val locationFocusRequester = FocusRequester()
    val colourFocusRequester = FocusRequester()
    val colourPickerDialog = ColourPickerDialog()

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
        repository.addNewGoalTimeBlock()
    }

    suspend fun deleteTimeBlock(timeBlock: GoalTaskDeliverableTime){
        repository.deleteNewGoalTimeBlock(timeBlock)
    }

    suspend fun closeGoal(){
        repository.clearNewGoal()
        repository.goBackToGoalsFromEditGoal()
    }

    private fun showError(message: String, focusRequester: FocusRequester){
        errorMessage.value = message
        showErrorMessage.value = true
        focusRequester.requestFocus()
    }

    suspend fun saveAndCloseGoal(context: Context){
        triedToSave.value = true
        if (newGoal.value?.goal?.text?.isEmpty() == true) {
            showError(
                context.getString(R.string.please_enter_a_goal),
                goalFocusRequester
            )
            return
        }
        if (newGoal.value?.location?.text?.isEmpty() == true) {
            showError(
                context.getString(R.string.please_enter_a_location),
                locationFocusRequester
            )
            return
        }
        if (newGoal.value?.colour?.value == -1) {
            showError(
                context.getString(R.string.please_select_a_colour),
                colourFocusRequester
            )
            return
        }
        newGoal.value?.times?.forEach { time ->
            if (time.duration.value.hour == 0 && time.duration.value.minute == 0) {
                showError(
                    context.getString(R.string.please_select_a_duration),
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