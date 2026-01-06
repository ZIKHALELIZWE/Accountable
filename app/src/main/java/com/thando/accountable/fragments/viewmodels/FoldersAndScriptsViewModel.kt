package com.thando.accountable.fragments.viewmodels

import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.thando.accountable.AccountableNavigationController
import com.thando.accountable.AccountableRepository
import com.thando.accountable.MainActivity
import com.thando.accountable.database.tables.Folder
import com.thando.accountable.recyclerviewadapters.GoalItemAdapter
import com.thando.accountable.recyclerviewadapters.ScriptItemAdapter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class FoldersAndScriptsViewModel(private val repository: AccountableRepository): ViewModel() {
    // Data
    val intentString = repository.intentString
    val folder = repository.getFolder()
    val appSettings = repository.getAppSettings()

    val foldersList = repository.getFoldersList()
    val scriptsList = repository.getScriptsList()
    val goalsList = repository.getGoalsList()
    val folderType = repository.getScriptsOrGoalsFolderType()

    private val _scrollTo = MutableSharedFlow<Int>()
    val scrollTo: SharedFlow<Int> get() {
        _collapseAppBar.emit()
        return _scrollTo
    }
    private val _collapseAppBar = MutableSharedFlow<Unit>()
    val collapseAppBar: SharedFlow<Unit> get() = _collapseAppBar

    // View States
    val scrollStateParent = repository.getScrollStateParent()
    val showScripts = repository.getShowScripts()
    val folderOrder = repository.getFolderOrder()
    val listLoaded = repository.getListLoaded()
    val listShown = repository.getListShown()

    val bottomSheetListeners = MutableStateFlow<BottomSheetListeners?>(null)

    data class BottomSheetListeners(
        val displayView: @Composable () -> Unit,
        val onEditClickListener: (() -> Unit)?,
        val onDeleteClickListener: () -> Unit
    )

    private fun MutableSharedFlow<Unit>.emit() = run { viewModelScope.launch { emit(Unit) } }

    fun getScriptContentPreview(scriptId: Long): AccountableRepository.ContentPreview{
        return repository.ContentPreview(scriptId)
    }

    fun getFolderContentPreview(folder: Folder) {
        repository.getFolderFolderNum(folder)
        repository.getFolderScriptGoalNum(folder)
    }

    fun setOnLongClick():Boolean = repository.intentString==null

    fun getScriptAdapter(viewModelLifecycleOwner: LifecycleOwner,childFragmentManager: FragmentManager): ScriptItemAdapter{
        return ScriptItemAdapter(
            repository.intentString==null,
            viewModelLifecycleOwner,
            childFragmentManager,
            { script -> onScriptClick(script) },
            { script -> onDeleteScript(script) },
            this
        )
    }

    fun getGoalAdapter(
        viewModelLifecycleOwner: LifecycleOwner,
        childFragmentManager: FragmentManager
    ): GoalItemAdapter{
        return GoalItemAdapter(
            repository.intentString==null,
            viewModelLifecycleOwner,
            childFragmentManager,
            { goal -> onGoalClick(goal) },
            { goalId -> onGoalEdit(goalId) },
            { goalId -> onDeleteGoal(goalId) }
        )
    }

    fun onFolderClick(folderId: Long?) {
        viewModelScope.launch {
            folderId?.let { id ->
                updateFolderScrollPosition(scrollStateParent.value?.firstVisibleItemIndex?:0){
                    updateFolderShowScripts {
                        loadFolder(id)
                    }
                }
            }
        }
    }

    fun onFolderEdit(folderId: Long?) {
        repository.loadEditFolder(folderId)
    }

    fun onDeleteFolder(folderId: Long?){
        repository.deleteFolder(folderId)
    }

    private fun onDeleteGoal(goalId: Long?){
        repository.deleteGoal(goalId)
    }

    fun onDeleteScript(scriptId:Long?){
        repository.deleteScript(scriptId)
    }

    private fun onGoalEdit(goalId: Long?) {
        repository.loadEditGoal(goalId)
    }

    fun onScriptClick(scriptId: Long) {
        viewModelScope.launch {
            updateFolderScrollPosition(scrollStateParent.value?.firstVisibleItemIndex?:0){
                updateFolderShowScripts {
                    loadAndOpenScript(scriptId,null/*activity*/)
                }
            }
        }
    }

    private fun onGoalClick(goalId: Long) {
        viewModelScope.launch {
            updateFolderScrollPosition(scrollStateParent.value?.firstVisibleItemIndex?:0){
                updateFolderShowScripts {
                    repository.loadAndOpenGoal(goalId)
                }
            }
        }
    }

    fun loadAndOpenScript(scriptId: Long, activity: FragmentActivity?) {
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
        repository.saveScrollPosition = scrollStateParent.value?.firstVisibleItemIndex?:0
        if (showScripts.value!=null){
            showScripts.value!!.update { showScripts.value!!.value.not() }
            updateFolderShowScripts()
            repository.loadFolderData()
        }
    }

    fun switchFolderOrder(){
        folderOrder.value?.update {
            folderOrder.value?.value?.not() == true
        }
        folderOrder.value?.value?.let { repository.loadContent(it) }
    }

    fun addFolderScript(){
        val id = INITIAL_FOLDER_ID
        showScripts.value?.let {
            if (it.value) {
                // Add a script
                if (repository.folderIsScripts()) onScriptClick(id)
                else onGoalEdit(id)
            } else {
                // Add a folder
                onFolderEdit(id)
            }
        }
    }

    fun onBackPressed():Boolean {
        if (folder.value !=null) {
            //_collapseAppBar.emit()
            viewModelScope.launch { resetScrollPosition() }
            updateFolderShowScripts {
                folder.value?.folderParent?.let { loadFolder(it) }
            }
            return true
        }
        else return false
    }

    fun loadFolder(folderId: Long) {
        repository.loadFolder(folderId)
    }

    private suspend fun resetScrollPosition(){
        if (folder.value ==null) {
            // Reset the settings table
            appSettings.value?.scrollPosition?.scrollToItem(0,0)
        }
        else{
            // Resets the folders table
            folder.value?.folderScrollPosition?.scrollToItem(0,0)
        }
    }

    fun getFolderId(): Long?{
        return folder.value?.folderId?: appSettings.value?.appSettingId
    }

    fun folderIsScripts(): Boolean { return repository.folderIsScripts() }

    fun getFolderType(): Folder.FolderType{ return folderType.value!! }

    fun updateFolderScrollPosition(scrollPosition: Int,appendedUnit:(()->Unit)? = null){
        repository.updateScriptsOrGoalsFolderScrollPosition(scrollPosition,appendedUnit)
    }

    fun updateFolderShowScripts(appendedUnit : (()->Unit)? = null){
        repository.updateFolderShowScripts(appendedUnit)
    }

    fun prepareToClose( appendedUnit: () -> Unit?){
        updateFolderShowScripts()
        updateFolderScrollPosition(scrollStateParent.value?.firstVisibleItemIndex?:0)
        appendedUnit()
    }

    companion object {
        const val INITIAL_FOLDER_ID: Long = -1L
        const val FOLDER_ID_BUNDLE: String = "folder_id_bundle"
        const val FOLDER_TYPE_BUNDLE: String = "folder_type_bundle"
        const val GOAL_ID_BUNDLE: String = "goal_id_bundle"
        const val GOAL_PARENT_ID_BUNDLE = "goal_parent_id_bundle"

        val Factory : ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>,
                                                extras: CreationExtras): T {
                // Get the Application object from extras
                val application = checkNotNull(extras[APPLICATION_KEY])

                val accountableRepository = AccountableRepository.getInstance(application)

                return FoldersAndScriptsViewModel(
                    accountableRepository
                ) as T
            }
        }
    }
}