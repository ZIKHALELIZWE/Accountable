package com.thando.accountable.fragments.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.thando.accountable.AccountableRepository
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import com.thando.accountable.ui.cards.Colour

class EditGoalViewModel(
    private val repository: AccountableRepository
): ViewModel() {

    val newGoal = repository.getNewGoal()

    fun setImage(uri:Uri?){
        uri?.let {
            repository.setNewGoalImage(uri)
        }
    }

    fun removeImage(){
        repository.deleteNewGoalImage()
    }

    fun pickColour(context: Context){
        Colour.showColorPickerDialog(context){ selectedColour: Int ->
            newGoal.value?.colour?.value = selectedColour
        }
    }

    fun addTimeBlock(){
        repository.addNewGoalTimeBlock()
    }

    fun deleteTimeBlock(timeBlock: GoalTaskDeliverableTime){
        repository.deleteNewGoalTimeBlock(timeBlock)
    }

    fun closeGoal(){
        repository.clearNewGoal()
        repository.goBackToGoalsFromEditGoal()
    }

    fun saveAndCloseGoal(){
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