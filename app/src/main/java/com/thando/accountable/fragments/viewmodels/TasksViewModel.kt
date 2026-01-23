package com.thando.accountable.fragments.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.thando.accountable.AccountableRepository
import com.thando.accountable.database.tables.Goal
import kotlinx.coroutines.flow.MutableStateFlow

class TaskViewModel(val repository: AccountableRepository) : ViewModel() {

    val goal = repository.getGoal()

    val bottomSheetType = mutableStateOf<Goal.GoalTab?>(null)
    val sheetListState = MutableStateFlow<LazyListState?>(null)

    fun processBottomSheetAdd(){

        dismissBottomSheet()
    }

    fun addTask(){

        showBottomSheet(Goal.GoalTab.TASKS)
    }

    fun addDeliverable(){

        showBottomSheet(Goal.GoalTab.DELIVERABLES)
    }

    fun dismissBottomSheet() {
        bottomSheetType.value = null
        sheetListState.value = null
    }

    fun showBottomSheet(sheetType: Goal.GoalTab) {
        sheetListState.value = LazyListState()
        bottomSheetType.value = sheetType
    }

    suspend fun closeTasks(){
        repository.goBackToGoalsFromTasks()
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