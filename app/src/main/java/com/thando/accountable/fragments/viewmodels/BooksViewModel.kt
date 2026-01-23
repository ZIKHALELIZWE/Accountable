package com.thando.accountable.fragments.viewmodels

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.thando.accountable.AccountableNavigationController
import com.thando.accountable.AccountableRepository
import com.thando.accountable.MainActivity
import com.thando.accountable.database.tables.Folder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BooksViewModel(
    private val repository: AccountableRepository
): ViewModel() {
    // Data
    val intentString = repository.intentString
    val folder = repository.getFolder()
    val appSettings = repository.getAppSettings()

    val foldersList = repository.getFoldersList()
    val scriptsList = repository.getScriptsList()
    val goalsList = repository.getGoalsList()
    val folderType = repository.getScriptsOrGoalsFolderType()

    // View States
    val scrollStateParent = repository.getScrollStateParent()
    val showScripts = repository.getShowScripts()
    val folderOrder = repository.getFolderOrder()
    val listLoaded = repository.getListLoaded()
    val listShown = repository.getListShown()

    val bottomSheetListeners = MutableStateFlow<BottomSheetListeners?>(null)
    var initialized = mutableStateOf(false)

    data class BottomSheetListeners(
        val displayView: @Composable () -> Unit,
        val onEditClickListener: (() -> Unit)?,
        val onDeleteClickListener: () -> Unit
    )

    fun getScriptContentPreview(scriptId: Long): AccountableRepository.ContentPreview{
        return repository.ContentPreview(scriptId)
    }

    fun getGoalContentPreview(goalId: Long): AccountableRepository.GoalContentPreview{
        return repository.GoalContentPreview(goalId)
    }

    suspend fun getFolderContentPreview(folder: Folder) {
        repository.getFolderFolderNum(folder)
        repository.getFolderScriptGoalNum(folder)
    }

    fun setOnLongClick():Boolean = repository.intentString==null

    fun initialized(){
        initialized.value = true
    }

    fun onFolderClick(folderId: Long?) {
        viewModelScope.launch {
            folderId?.let { id ->
                updateFolderScrollPosition {
                    updateFolderShowScripts {
                        initialized.value = false
                        loadFolder(id)
                    }
                }
            }
        }
    }

    fun onFolderEdit(folderId: Long?) {
        repository.loadEditFolder(folderId)
    }

    suspend fun onDeleteFolder(folderId: Long?){
        repository.deleteFolder(folderId)
    }

    suspend fun onDeleteGoal(goalId: Long?){
        repository.deleteGoal(goalId)
    }

    suspend fun onDeleteScript(scriptId:Long?){
        repository.deleteScript(scriptId)
    }

    private suspend fun onGoalEdit(goalId: Long?) {
        repository.loadEditGoal(goalId)
    }

    fun onScriptClick(scriptId: Long, activity: Activity?) {
        viewModelScope.launch {
            updateFolderScrollPosition{
                updateFolderShowScripts {
                    loadAndOpenScript( scriptId, activity)
                }
            }
        }
    }

    fun onGoalClick(goalId: Long) {
        viewModelScope.launch {
            updateFolderScrollPosition{
                updateFolderShowScripts {
                    repository.loadAndOpenGoal(goalId)
                }
            }
        }
    }

    fun loadAndOpenScript(scriptId: Long, activity: Activity?) {
        if (repository.intentString==null) {
            repository.loadAndOpenScript(scriptId)
        }
        else{
            repository.appendIntentStringToScript(scriptId,activity)
        }
    }

    fun search(){
        prepareToClose{
            repository.changeFragment(AccountableNavigationController.AccountableFragment.SearchFragment)
        }
    }

    fun switchFolderScript(){
        scrollStateParent.value?.requestScrollToItem(0,0)
        if (showScripts.value!=null){
            showScripts.value!!.update { showScripts.value!!.value.not() }
            updateFolderShowScripts()
            repository.loadFolderData()
            initialized.value = false
        }
    }

    fun switchFolderOrder(){
        folderOrder.value?.update {
            folderOrder.value?.value?.not() == true
        }
        scrollStateParent.value?.requestScrollToItem(0,0)
        folderOrder.value?.value?.let { repository.loadContent(it) }
        initialized.value = false
    }

    suspend fun addFolderScript(){
        val id = INITIAL_FOLDER_ID
        showScripts.value?.let {
            if (it.value) {
                // Add a script
                if (repository.folderIsScripts()) onScriptClick(id, null)
                else onGoalEdit(id)
            } else {
                // Add a folder
                onFolderEdit(id)
            }
        }
    }

    fun onBackPressed():Boolean {
        if (folder.value !=null) {
            viewModelScope.launch {
                if (folder.value ==null) {
                    // Reset the settings table
                    appSettings.value?.scrollPosition?.requestScrollToItem(0,0)
                }
                else{
                    // Resets the folders table
                    folder.value?.folderScrollPosition?.requestScrollToItem(0,0)
                }
            }
            updateFolderShowScripts {
                folder.value?.folderParent?.let { loadFolder(it) }
            }
            initialized.value = false
            return true
        }
        else return false
    }

    suspend fun loadFolder(folderId: Long) {
        repository.loadFolder(folderId)
    }

    fun folderIsScripts(): Boolean { return repository.folderIsScripts() }

    fun updateFolderScrollPosition(appendedUnit:(()->Unit)? = null){
        repository.updateScriptsOrGoalsFolderScrollPosition(
            scrollStateParent.value?.firstVisibleItemIndex?:0,
            scrollStateParent.value?.firstVisibleItemScrollOffset?:0,
            appendedUnit
        )
    }

    fun updateFolderShowScripts(appendedUnit : (suspend ()->Unit)? = null){
        repository.updateFolderShowScripts(appendedUnit)
    }

    fun prepareToClose( appendedUnit: () -> Unit?){
        updateFolderShowScripts()
        updateFolderScrollPosition()
        appendedUnit()
    }

    fun navigateToHome(){
        repository.changeFragment(
            AccountableNavigationController.AccountableFragment.HomeFragment
        )
    }

    companion object {
        const val INITIAL_FOLDER_ID: Long = -1L

        val Factory : ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                // Get the Application object from extras
                val application = checkNotNull(extras[APPLICATION_KEY])

                val accountableRepository = AccountableRepository.getInstance(application)

                return BooksViewModel(
                    accountableRepository
                ) as T
            }
        }
    }
}