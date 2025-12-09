package com.thando.accountable.fragments.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.thando.accountable.AccountableRepository

class GoalsViewModel(val repository: AccountableRepository) : ViewModel() {

    val appSettings = repository.getAppSettings()
    var folder = repository.getFolder()
    val goals = repository.getGoalsList()

    fun loadEditGoal(){
        repository.loadEditGoal()
    }

    fun closeGoals(){
        repository.goBackToHomeFromGoals()
    }

    companion object {
        val Factory : ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T
            {
                // Get the Application object from extras
                val application = checkNotNull(extras[APPLICATION_KEY])

                val accountableRepository = AccountableRepository.getInstance(application)

                return GoalsViewModel(
                    accountableRepository
                ) as T
            }
        }
    }
}