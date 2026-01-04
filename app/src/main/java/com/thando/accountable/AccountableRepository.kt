package com.thando.accountable

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentActivity
import com.thando.accountable.AccountableNavigationController.AccountableFragment
import com.thando.accountable.database.AccountableDatabase
import com.thando.accountable.database.dataaccessobjects.RepositoryDao
import com.thando.accountable.database.tables.AppSettings
import com.thando.accountable.database.tables.Content
import com.thando.accountable.database.tables.Content.ContentType
import com.thando.accountable.database.tables.Folder
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.database.tables.Script
import com.thando.accountable.database.tables.SpecialCharacters
import com.thando.accountable.database.tables.TeleprompterSettings
import com.thando.accountable.databinding.TextItemBinding
import com.thando.accountable.fragments.FoldersAndScriptsFragment.FoldersAndScriptsLists
import com.thando.accountable.fragments.ScriptFragment
import com.thando.accountable.fragments.dialogs.AddMediaDialog
import com.thando.accountable.fragments.viewmodels.FoldersAndScriptsViewModel.Companion.INITIAL_FOLDER_ID
import com.thando.accountable.fragments.viewmodels.SearchViewModel
import com.thando.accountable.player.AccountablePlayer
import com.thando.accountable.ui.AccountableNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference


class AccountableRepository(val application: Application): AutoCloseable {
    var dao: RepositoryDao = AccountableDatabase.getInstance(application).repositoryDao
    private val repositoryJob = Job()
    private val repositoryScope = CoroutineScope(Dispatchers.Main + repositoryJob)

    private val folder = MutableStateFlow<Folder?>(null)
    val showScripts = MutableStateFlow<MutableStateFlow<Boolean>?>(null)
    val folderOrder = MutableStateFlow<MutableStateFlow<Boolean>?>(null)
    val listLoaded = MutableStateFlow( false)
    val scrollStateParent = MutableStateFlow<ScrollState?>(null)
    var saveScrollPosition: Int? = null
    val listShown = MutableStateFlow(getListShown(false))

    private val appSettings = MutableStateFlow<AppSettings?>(null)
    private val foldersList = mutableStateOf(mutableListOf<Folder>())
    private val scriptsList = mutableStateOf(mutableListOf<Script>())
    private val goalsList = mutableStateOf(mutableListOf<Goal>())
    var intentString: String? = null

    private val editFolder = MutableStateFlow<Folder?>(null)
    private val newEditFolder = MutableStateFlow<Folder?>(null)

    private val goal = MutableStateFlow<Goal?>(null)
    private val editGoal: MutableState<Goal?> = mutableStateOf(null)
    private val newGoal: MutableState<Goal?> = mutableStateOf(null)

    private val scriptsOrGoalsFolderType = MutableStateFlow(Folder.FolderType.SCRIPTS)

    private val direction = MutableSharedFlow<AccountableFragment?>()
    private val currentFragment = MutableStateFlow<AccountableFragment?>(null)

    private val script = MutableStateFlow<Script?>(null)
    private val scriptContentList = MutableStateFlow<MutableList<Content>>(mutableListOf())
    private val scriptMarkupLanguage = MutableStateFlow<MarkupLanguage?>(null)
    private val isEditingScript = MutableStateFlow(false)

    private val markupLanguagesList: MutableStateFlow<List<MarkupLanguage>> = MutableStateFlow(listOf())
    private val defaultMarkupLanguage = MutableStateFlow(MarkupLanguage())
    private val markupLanguageSelectedIndex = MutableStateFlow(-1)

    private val scriptTeleprompterSetting = MutableStateFlow<TeleprompterSettings?>(null)
    private val teleprompterSettingsList: MutableStateFlow<List<TeleprompterSettings>> = MutableStateFlow(listOf())
    private val defaultTeleprompterSetting = MutableStateFlow(TeleprompterSettings())
    private val teleprompterSettingsSelectedIndex = MutableStateFlow(-1)
    private val specialCharactersList: MutableStateFlow<MutableList<SpecialCharacters>> = MutableStateFlow(mutableListOf())

    private val searchScrollPosition: MutableStateFlow<Int> = MutableStateFlow(0)
    private val searchString: MutableStateFlow<String> = MutableStateFlow("")
    private val matchCaseCheck: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val wordCheck: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val searchJob: MutableStateFlow<Job?> = MutableStateFlow(null)
    private val searchOccurrences: MutableStateFlow<Int> = MutableStateFlow(0)
    private val searchNumScripts: MutableStateFlow<Int> = MutableStateFlow(0)
    private val searchScriptsList: MutableList<SearchViewModel.ScriptSearch> = mutableListOf()
    private var isFromSearchFolder: Boolean = false


    companion object {
        @Volatile
        private var INSTANCE: AccountableRepository? = null

        fun getInstance(application: Application): AccountableRepository {
            synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = AccountableRepository(application)
                }
                return INSTANCE!!
            }
        }

        val accountablePlayer = AccountablePlayer()
    }

    init {
        accountablePlayer.init(application)
        loadAppSettings()
    }

    override fun close() {
        accountablePlayer.onDestroy()
    }

    fun getFolder(): StateFlow<Folder?>{ return folder }
    fun getShowScripts(): StateFlow<MutableStateFlow<Boolean>?>{ return showScripts}
    fun getFolderOrder(): StateFlow<MutableStateFlow<Boolean>?>{ return folderOrder }
    fun getListLoaded(): StateFlow<Boolean>{ return listLoaded }
    fun getScrollStateParent(): StateFlow<ScrollState?>{ return scrollStateParent }
    fun getListShown() : StateFlow<FoldersAndScriptsLists.FASListType>{ return listShown}

    fun getAppSettings(): StateFlow<AppSettings?> { return appSettings }
    fun getFoldersList(): MutableState<MutableList<Folder>> { return foldersList }
    fun getScriptsList(): MutableState<MutableList<Script>> { return scriptsList }
    fun getGoalsList(): MutableState<MutableList<Goal>> { return goalsList }
    fun getEditFolder(): StateFlow<Folder?>{ return editFolder }
    fun getNewEditFolder(): StateFlow<Folder?>{ return newEditFolder }
    fun getNewGoal():MutableState<Goal?> { return newGoal }
    fun getDirection(): SharedFlow<AccountableFragment?>{ return direction }
    fun getScriptsOrGoalsFolderType(): StateFlow<Folder.FolderType?> { return scriptsOrGoalsFolderType }
    fun getCurrentFragment(): SharedFlow<AccountableFragment?>{ return currentFragment }

    fun getScript(): StateFlow<Script?>{ return script }
    fun getScriptContentList(): StateFlow<MutableList<Content>>{ return scriptContentList }
    fun getScriptMarkupLanguage(): StateFlow<MarkupLanguage?>{ return scriptMarkupLanguage }
    fun getIsEditingScript(): MutableStateFlow<Boolean>{ return isEditingScript }

    fun getMarkupLanguagesList(): StateFlow<List<MarkupLanguage>> { return markupLanguagesList }
    fun getDefaultMarkupLanguage(): StateFlow<MarkupLanguage> { return defaultMarkupLanguage }
    fun getMarkupLanguageSelectedIndex(): StateFlow<Int> { return markupLanguageSelectedIndex }

    fun getScriptTeleprompterSetting(): StateFlow<TeleprompterSettings?> { return scriptTeleprompterSetting }
    fun getTeleprompterSettingsList(): StateFlow<List<TeleprompterSettings>> { return teleprompterSettingsList }
    fun getDefaultTeleprompterSettings(): StateFlow<TeleprompterSettings> { return defaultTeleprompterSetting }
    fun getTeleprompterSettingsSelectedIndex(): StateFlow<Int> { return teleprompterSettingsSelectedIndex }
    fun getSpecialCharactersList(): StateFlow<MutableList<SpecialCharacters>> { return specialCharactersList }

    fun getSearchScrollPosition(): StateFlow<Int> { return searchScrollPosition }
    fun getSearchString(): MutableStateFlow<String> { return searchString }
    fun getMatchCaseCheck(): MutableStateFlow<Boolean> { return matchCaseCheck }
    fun getWordCheck(): MutableStateFlow<Boolean> { return wordCheck }
    fun getSearchJob(): StateFlow<Job?> { return searchJob }
    fun getSearchOccurrences(): StateFlow<Int> { return searchOccurrences }
    fun getSearchNumScripts(): StateFlow<Int> { return searchNumScripts }
    fun getSearchScriptsList(): MutableList<SearchViewModel.ScriptSearch> { return searchScriptsList }

    fun clearFolderLists(){
        scriptsList.value = mutableListOf()
        foldersList.value = mutableListOf()
        goalsList.value = mutableListOf()
    }

    fun loadScriptsList(ascendingOrder:Boolean, folderNotAppSettings:Boolean,
                        appendedUnit: (() -> Unit)? = null){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                val id = if (folderNotAppSettings) folder.value?.folderId else INITIAL_FOLDER_ID
                if (id != null) {
                    withContext(Dispatchers.Main) {
                        if (ascendingOrder) scriptsList.value = scriptsList.value.toMutableList().apply {
                            clear()
                            addAll(
                            withContext(Dispatchers.IO) { dao.getScriptsNow(id).toMutableList() }
                            )
                        }
                        else scriptsList.value = scriptsList.value.toMutableList().apply {
                            clear()
                            addAll(
                                withContext(Dispatchers.IO) {
                                    dao.getScriptsNowDESC(id)
                                        .toMutableList()
                                }
                            )
                        }
                    }
                }
                withContext(Dispatchers.Main){ appendedUnit?.invoke() }
            }
        }
    }

    fun loadGoalsList(ascendingOrder:Boolean, folderNotAppSettings:Boolean,
                      appendedUnit: (() -> Unit)? = null){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                val id = if (folderNotAppSettings) folder.value?.folderId else INITIAL_FOLDER_ID
                if (id != null) {
                    withContext(Dispatchers.Main) {
                        if (ascendingOrder) {
                            goalsList.value = goalsList.value.toMutableList().apply {
                                clear()
                                addAll(
                                    withContext(Dispatchers.IO) {
                                        dao.getGoalsNow(id).toMutableList()
                                    }
                                )
                            }
                        }
                        else {
                            goalsList.value = goalsList.value.toMutableList().apply {
                                clear()
                                addAll(
                                    withContext(Dispatchers.IO) {
                                        dao.getGoalsNowDESC(id).toMutableList()
                                    }
                                )
                            }
                        }
                    }
                }
                withContext(Dispatchers.Main){ appendedUnit?.invoke() }
            }
        }
    }

    fun loadFoldersList(ascendingOrder:Boolean,
                        folderNotAppSettings:Boolean,
                        folderTypeScriptsNotGoals:Boolean,
                        appendedUnit: (() -> Unit)? = null){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                val id = if (folderNotAppSettings) folder.value?.folderId else INITIAL_FOLDER_ID
                val type =
                    if (folderTypeScriptsNotGoals) Folder.FolderType.SCRIPTS else Folder.FolderType.GOALS
                withContext(Dispatchers.Main) {
                    if (ascendingOrder){
                        foldersList.value = foldersList.value.toMutableList().apply {
                            clear()
                            addAll(
                                withContext(Dispatchers.IO) {
                                    val list = dao.getFoldersNow(id, type).toMutableList()
                                    list.forEachIndexed { index, folder ->
                                        folder.folderPosition = index.toLong()
                                        dao.update(folder)
                                    }
                                    list
                                }
                            )
                        }
                    }
                    else{
                        foldersList.value = foldersList.value.toMutableList().apply {
                            clear()
                            addAll(
                                withContext(Dispatchers.IO) {
                                    val list = dao.getFoldersNowDESC(id, type).toMutableList()
                                    var position = list.size.toLong() - 1L
                                    list.forEach { folder ->
                                        folder.folderPosition = position
                                        dao.update(folder)
                                        --position
                                    }
                                    list
                                }
                            )
                        }
                    }
                }
                withContext(Dispatchers.Main){ appendedUnit?.invoke() }
            }
        }
    }

    fun loadFolder(folderIdInput: Long?=null){
        val folderId:Long =
            folderIdInput ?: (folder.value?.folderId ?: INITIAL_FOLDER_ID)
        repositoryScope.launch {
            folder.update {
                    if (folderId != INITIAL_FOLDER_ID) dao.getFolderNow(folderId)
                    else null
            }
            if (folder.value!=null){
                showScripts.update { folder.value!!.folderShowScripts }
            }
            else{
                if (scriptsOrGoalsFolderType.value == Folder.FolderType.SCRIPTS) showScripts.update { appSettings.value?.showScripts }
                else if (scriptsOrGoalsFolderType.value == Folder.FolderType.GOALS) showScripts.update { appSettings.value?.showGoals }
            }
            loadFolderData()
        }
    }

    fun folderIsScripts(): Boolean { return scriptsOrGoalsFolderType.value == Folder.FolderType.SCRIPTS }

    private fun getListShown(showScripts:Boolean):FoldersAndScriptsLists.FASListType{
        return if (showScripts){
            if (folderIsScripts()) FoldersAndScriptsLists.FASListType.SCRIPTS
            else FoldersAndScriptsLists.FASListType.GOALS
        }
        else FoldersAndScriptsLists.FASListType.FOLDERS
    }

    fun loadFolderData(){
        clearFolderLists()
        showScripts.value?.let { showScripts -> listShown.update { getListShown(showScripts.value) } }
        if (folder.value !=null) {
            if (folder.value?.folderShowScripts?.value == true) folderOrder.update {
                folder.value?.folderScriptsOrder
            }
            else folderOrder.update {
                folder.value?.folderFoldersOrder
            }
            folderOrder.value?.value?.let { loadContent(it) }
        }
        else if ( appSettings.value != null){
            if (scriptsOrGoalsFolderType.value == Folder.FolderType.SCRIPTS){
                if (appSettings.value!!.showScripts.value)
                    folderOrder.update {
                        appSettings.value!!.scriptsOrder
                    }
                else folderOrder.update {
                    appSettings.value!!.foldersOrder
                }
                folderOrder.value?.value?.let { loadContent(it) }
            }
            else if(scriptsOrGoalsFolderType.value == Folder.FolderType.GOALS){ // Folder.FolderType.GOALS
                if (appSettings.value!!.showGoals.value)
                    folderOrder.update {
                        appSettings.value!!.scriptsOrder
                    }
                else folderOrder.update {
                    appSettings.value!!.foldersOrder
                }
                folderOrder.value?.value?.let { loadContent(it) }
            }
        }
    }

    fun loadContent(ascendingOrder: Boolean){
        listLoaded.update { false }
        val folderNotAppSettings = folder.value != null
        val folderTypeScriptsNotGoals = scriptsOrGoalsFolderType.value == Folder.FolderType.SCRIPTS
        if (showScripts.value?.value == true){
            if (folderTypeScriptsNotGoals) {
                loadScriptsList(
                    ascendingOrder = ascendingOrder,
                    folderNotAppSettings = folderNotAppSettings
                ){
                    scroll()
                }
            }
            else {
                loadGoalsList(
                    ascendingOrder = ascendingOrder,
                    folderNotAppSettings = folderNotAppSettings
                ){
                    scroll()
                }
            }
        }
        else{
            loadFoldersList(ascendingOrder = ascendingOrder,
                folderNotAppSettings = folderNotAppSettings,
                folderTypeScriptsNotGoals = folderTypeScriptsNotGoals){
                scroll()
            }
        }
    }

    private fun scroll() {
        repositoryScope.launch {
            val returnScrollPosition = if (folder.value == null) {
                // Get from settings table
                appSettings.value?.scrollPosition ?: ScrollState(0)
            } else {
                // Get from folder table
                folder.value?.folderScrollPosition ?: ScrollState(0)
            }
            saveScrollPosition?.let {
                updateScriptsOrGoalsFolderScrollPosition(it) {
                    saveScrollPosition = null
                }
            }
            scrollStateParent.update { returnScrollPosition }
        }
    }

    private fun loadAppSettings() {
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                appSettings.update { dao.appSettings() }

                val loadInitialFolder:(inputFolderType:Folder.FolderType) -> Unit = { type ->
                    scriptsOrGoalsFolderType.update { type }
                    loadFolder(INITIAL_FOLDER_ID)
                }
                when (currentFragment.value) {
                    null -> {
                        if (appSettings.value?.initialFragment == AccountableFragment.BooksFragment) {
                            loadInitialFolder(Folder.FolderType.SCRIPTS)
                        } else if (appSettings.value?.initialFragment == AccountableFragment.GoalsFragment) {
                            loadInitialFolder(Folder.FolderType.GOALS)
                        }
                        currentFragment.update { appSettings.value?.initialFragment }
                    }

                    AccountableFragment.BooksFragment -> {
                        loadInitialFolder(Folder.FolderType.SCRIPTS)
                    }

                    AccountableFragment.GoalsFragment -> {
                        loadInitialFolder(Folder.FolderType.GOALS)
                    }

                    else -> {}
                }
            }
        }
    }

    fun updateAppSettings(){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                appSettings.value?.let { dao.update(it) }
            }
        }
    }

    fun saveCustomAppSettingsImage(imageUri: Uri?){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                appSettings.value?.saveImage(application, imageUri)
                // Update database
                updateAppSettings()
            }
        }
    }

    fun restoreDefaultCustomAppSettingsImage(){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                appSettings.value?.restoreDefaultFile(application)
                updateAppSettings()
            }
        }
    }

    fun updateScriptsOrGoalsFolderScrollPosition(scrollPosition: Int,appendedUnit:(()->Unit)?){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                if (folder.value == null) {
                    // Save to settings table
                    appSettings.value?.scrollPosition?.scrollTo(scrollPosition)
                    appSettings.value?.let { dao.update(it) }
                } else {
                    // Saved in Folders table
                    folder.value?.folderScrollPosition?.scrollTo(scrollPosition)
                    appSettings.value?.let { dao.update(it) }
                }
                appendedUnit?.invoke()
            }
        }
    }

    fun updateFolderShowScripts(appendedUnit : (()->Unit)?){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                if (folder.value == null) {
                    // Save to settings table
                    appSettings.value?.let { dao.update(it) }
                } else {
                    // Saved in Folders table
                    folder.value?.let { dao.update(it) }
                }
                if (appendedUnit != null) {
                    withContext(Dispatchers.Main) { appendedUnit() }
                }
            }
        }
    }

    // Navigation Code
    fun changeFragment(newFragment: AccountableFragment){
        currentFragment.value?.let { currentFragmentInScope ->
            val (validatedFragment, navArgs) = AccountableNavigationController.getFragmentDirections(
                currentFragmentInScope,
                newFragment
            )
            if (validatedFragment == null) return

            if (navArgs.isDrawerFragment) {
                appSettings.value?.initialFragment = newFragment
                repositoryScope.launch {
                    withContext(Dispatchers.IO) {
                        appSettings.value?.let {
                            dao.update(
                                it
                            )
                        }
                    }
                }
                if (navArgs.scriptsOrGoalsFolderId != null) {
                    navArgs.scriptsOrGoalsFolderType?.let { scriptsOrGoalsFolderType.value = it }
                    loadFolder(navArgs.scriptsOrGoalsFolderId!!)
                }
            }

            repositoryScope.launch {
                currentFragment.emit(validatedFragment)
                direction.emit(validatedFragment)
            }
        }
    }

    data class NavigationArguments(var isValidDir:Boolean = false){
        var isDrawerFragment = false
        var scriptsOrGoalsFolderId:Long? = null
        var scriptsOrGoalsFolderType:Folder.FolderType? = null
    }

    fun loadEditFolder(id:Long?){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    editFolder.value = withContext(Dispatchers.IO) {
                        if (id != -1L) dao.getFolderNow(id)
                        else null
                    }
                }
                changeFragment(AccountableFragment.EditFolderFragment)
            }
        }
    }

    fun deleteFolder(id:Long?){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                for(i in 0 until foldersList.value.size){
                    if (foldersList.value[i].folderId == id){
                        foldersList.value = foldersList.value.toMutableList().apply { removeAt(i) }
                        break
                    }
                }
                id?.let { dao.deleteFolder(it, application) }
            }
        }
    }

    fun deleteGoal(id:Long?){
        repositoryScope.launch {
            withContext(Dispatchers.IO){
                id?.let { dao.deleteGoal(it, application) }
            }
        }
    }

    fun deleteScript(id:Long?){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                id?.let { dao.deleteScript(it, application) }
            }
        }
    }

    fun setNewEditFolder(editFolder:Folder?){
        if (newEditFolder.value!=null) return
        repositoryScope.launch {
            newEditFolder.value = withContext(Dispatchers.IO){
                clearNewEditFolder(runAsync = false, resetEditFolder = false)
                val newFolder = Folder(
                    folderType = scriptsOrGoalsFolderType.value,
                    folderParent = folder.value?.folderId?: INITIAL_FOLDER_ID
                )

                newFolder.folderId = dao.insert(newFolder)

                if (editFolder != null) {
                    copyFolder(editFolder, newFolder)
                } else {
                    if (folder.value == null) {
                        // Update AppSettings Table
                        newFolder.folderPosition = (appSettings.value?.let {
                            dao.getFoldersNow(
                                INITIAL_FOLDER_ID,
                                scriptsOrGoalsFolderType.value
                            ).size
                        } ?: 0).toLong()
                    } else {
                        // Update Folders Table
                        newFolder.folderPosition =
                            (folder.value?.let {
                                dao.getFoldersNow(
                                    it.folderId,
                                    it.folderType
                                ).size
                            } ?: 0).toLong()
                    }
                }
                newFolder
            }
        }
    }

    private suspend fun copyFolder(from:Folder,to:Folder){
        if (to.folderId == from.folderId) return
        to.folderName.value = from.folderName.value
        to.folderPosition = from.folderPosition
        to.folderScrollPosition.scrollTo(from.folderScrollPosition.value)
        to.folderShowScripts.value = from.folderShowScripts.value
        to.saveImage(application, from.imageResource.getUriFromStorage(application))
        dao.update(to)
    }

    fun setNewEditFolderImage(imageUri: Uri?=null){
        repositoryScope.launch {
            withContext(Dispatchers.IO){
                if (imageUri!=null) newEditFolder.value?.saveImage(application, imageUri)
                dao.update(newEditFolder.value!!)
            }
        }
    }

    fun clearNewEditFolder(runAsync:Boolean = true, resetEditFolder:Boolean = true){
        repositoryScope.launch {
            val unit = suspend {
                if (newEditFolder.value != null) {
                    newEditFolder.value!!.imageResource.deleteFile(application)
                    dao.delete(newEditFolder.value!!)
                }
                if (resetEditFolder) editFolder.value = null
                null
            }
            if (runAsync)  newEditFolder.value = withContext(Dispatchers.IO){
                unit()
            }
            else{
                newEditFolder.value = unit()
            }
        }
    }

    fun saveNewEditFolderToNewFolder(){
        repositoryScope.launch {
            if (editFolder.value==null){
                newEditFolder.value?.let { dao.update(it) }
            }
            else{
                newEditFolder.value?.let {
                    copyFolder(it, editFolder.value!!)
                    it.deleteFile(application)
                    dao.delete(it)
                }
            }
            withContext(Dispatchers.Main) {
                editFolder.value = null
                newEditFolder.value = null
            }
            loadFolder()
        }
    }

    fun deleteNewEditFolderImage(){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                newEditFolder.value?.deleteFile(application)
            }
        }
    }

    fun appendIntentStringToScript(scriptId:Long, activity: FragmentActivity?){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                if (scriptId != INITIAL_FOLDER_ID) {
                    withContext(Dispatchers.Main) {
                        isEditingScript.value = false
                        script.value = withContext(Dispatchers.IO) {
                            val tempScript = dao.getScriptNow(scriptId)
                            withContext(Dispatchers.Main){
                                scriptMarkupLanguage.value = withContext(Dispatchers.IO) {
                                    tempScript!!.scriptMarkupLanguage.let { dao.getMarkupLanguage(it) }
                                }
                            }
                            tempScript
                        }
                        scriptContentList.value =
                            withContext(Dispatchers.IO) { dao.getContentList(scriptId).toMutableList() }
                    }
                }
                if (script.value == null) {
                    Toast.makeText(
                        application,
                        application.getString(R.string.script_does_not_exist),
                        Toast.LENGTH_LONG
                    ).show()
                } else if (!intentString.isNullOrEmpty()) {
                    if (scriptContentList.value.lastIndex == -1 || scriptContentList.value.last().type!= ContentType.TEXT){
                        // Make a new TextProcessor and add the content's content
                        if(script.value!=null && script.value!!.scriptId!=null) {
                            scriptContentList.value.add(
                                Content(
                                    type = ContentType.TEXT,
                                    script = script.value!!.scriptId!!,
                                    position = scriptContentList.value.size.toLong()
                                )
                            )
                        }
                    }
                    withContext(Dispatchers.Main){
                        scriptContentList.value.last().content.value += "\n$intentString"
                    }
                    saveScript {
                        activity?.finishAndRemoveTask()
                    }
                }
                intentString = null
                withContext(Dispatchers.Main){
                    activity?.finishAndRemoveTask()
                }
            }
        }
    }

    fun loadAndOpenGoal(goalId: Long){
        repositoryScope.launch {
            withContext(Dispatchers.Main) {
                goal.value = withContext(Dispatchers.IO) {
                    dao.getGoalWithTimes(goalId)
                }
            }
            withContext(Dispatchers.IO) {
                if (goal.value == null) {
                    Toast.makeText(
                        application,
                        application.getString(R.string.goal_does_not_exist),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    changeFragment(AccountableFragment.TaskFragment)
                }
            }
        }
    }

    fun loadAndOpenScript(scriptId: Long){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                if (scriptId == INITIAL_FOLDER_ID) {
                    val parentId = folder.value?.folderId ?: INITIAL_FOLDER_ID
                    val scripts = dao.getScriptsNow(parentId)
                    withContext(Dispatchers.Main) {
                        script.value = Script(
                            scriptParentType = Script.ScriptParentType.FOLDER,
                            scriptParent = parentId,
                            scriptDateTime = AppResources.CalendarResource(Calendar.getInstance()),
                            scriptPosition = scripts.size.toLong(),
                        )
                        scriptContentList.value = appendTextFieldIfNeeded(mutableListOf())
                        scriptMarkupLanguage.value = null
                        isEditingScript.value = true
                        script.value!!.scriptId = withContext(Dispatchers.IO) { dao.insert(script.value!!) }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isEditingScript.value = false
                        script.value = withContext(Dispatchers.IO) {
                            val tempScript = dao.getScriptNow(scriptId)
                            withContext(Dispatchers.Main){
                                scriptMarkupLanguage.value = withContext(Dispatchers.IO) {
                                    tempScript!!.scriptMarkupLanguage.let { dao.getMarkupLanguage(it) }
                                }
                            }
                            tempScript
                        }
                        scriptContentList.value =
                            withContext(Dispatchers.IO) {
                                appendTextFieldIfNeeded(dao.getContentList(scriptId).toMutableList())
                            }
                    }
                }
                if (script.value == null) {
                    Toast.makeText(
                        application,
                        application.getString(R.string.script_does_not_exist),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    changeFragment(AccountableFragment.ScriptFragment)
                }
            }
        }
    }

    private fun appendTextFieldIfNeeded(list: MutableList<Content>? = null):MutableList<Content>{
        if (list != null){
            if (list.lastIndex == -1 || list.last().type!= ContentType.TEXT){
                // Make a new TextProcessor and add the content's content
                if(script.value!=null && script.value!!.scriptId!=null) {
                    list.add(
                        Content(
                            type = ContentType.TEXT,
                            script = script.value!!.scriptId!!,
                            position = list.size.toLong()
                        )
                    )
                }
            }
        }
        else {
            if (scriptContentList.value.lastIndex == -1 || scriptContentList.value.last().type!= ContentType.TEXT){
                // Make a new TextProcessor and add the content's content
                if(script.value!=null && script.value!!.scriptId!=null) {
                    scriptContentList.value.add(
                        Content(
                            type = ContentType.TEXT,
                            script = script.value!!.scriptId!!,
                            position = scriptContentList.value.size.toLong()
                        )
                    )
                }
            }
        }
        return list?:mutableListOf()
    }

    fun deleteScriptImage(){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                script.value?.deleteFile(application)
                script.value?.let { dao.update(it) }
            }
        }
    }

    fun saveScriptImage(uri: Uri?){
        if (uri!=null){
            repositoryScope.launch {
                withContext(Dispatchers.IO) {
                    if (script.value?.scriptId == null) script.value?.let {
                        it.scriptId = dao.insert(it)
                    }
                    script.value?.saveImage(application, uri)
                    script.value?.let { dao.update(it) }
                }
            }
        }
    }

    fun saveScript(loadFolder:Boolean = false, appendedUnit: (() -> Unit)? = null){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                if (script.value?.scriptId == null) script.value?.let {
                    it.scriptId = dao.insert(it)
                }
                else script.value?.let { dao.update(it) }

                // save content in the list
                var position = 1L
                scriptContentList.value.forEach { content ->
                    when (content.type) {
                        ContentType.TEXT -> {
                            if (content.content.value.isNotEmpty()) {
                                position = saveContent(content, position)
                            } else dao.delete(content)
                        }

                        ContentType.IMAGE -> {
                            if (content.imageResource.getUriFromStorage(application) == null) {
                                content.deleteFile(application)
                                dao.delete(content)
                            } else {
                                position = saveContent(content, position)
                            }
                        }

                        ContentType.SCRIPT -> TODO()
                        ContentType.VIDEO -> {
                            if (content.videoResource.getUriFromStorage(application) == null) {
                                content.deleteFile(application)
                                dao.delete(content)
                            } else {
                                position = saveContent(content, position)
                            }
                        }

                        ContentType.DOCUMENT -> {
                            if (content.documentResource.getUriFromStorage(application) == null) {
                                content.deleteFile(application)
                                dao.delete(content)
                            } else {
                                position = saveContent(content, position)
                            }
                        }

                        ContentType.AUDIO -> {
                            if (content.audioResource.getUriFromStorage(application) == null) {
                                content.deleteFile(application)
                                dao.delete(content)
                            } else {
                                position = saveContent(content, position)
                            }
                        }
                    }
                }

                if (loadFolder) loadFolder()
                appendedUnit?.invoke()
            }
        }
    }

    private suspend fun saveContent(content: Content, position:Long):Long{
        content.position = position
        if (content.id==null) content.id = dao.insert(content)
        else dao.update(content)
        return position+1
    }

    fun addContent(multipleContentList:List<Uri>?, contentType: ContentType, contentPosition: ScriptFragment.ContentPosition, item: Content, cursorPosition:Int?){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                scriptContentList.value = scriptContentList.value.toMutableList().apply {
                    val inputIndex = when (contentPosition) {
                        ScriptFragment.ContentPosition.ABOVE ->
                            indexOf(item)

                        ScriptFragment.ContentPosition.AT_CURSOR_POINT -> {
                            val inputIndex = indexOf(item) + 1
                            if (cursorPosition != null) {
                                // split the string
                                val text = item.content.value

                                // Split the string
                                val topString = text.take(cursorPosition)
                                val bottomString =
                                    text.substring(cursorPosition, text.length)

                                item.content.value = topString
                                val newContent = Content(
                                    type = ContentType.TEXT,
                                    script = script.value!!.scriptId!!,
                                    position = inputIndex.toLong(),
                                    content = MutableStateFlow(bottomString)
                                )
                                add(inputIndex, newContent)
                                newContent.id = dao.insert(newContent)
                            } else return@withContext
                            inputIndex
                        }

                        ScriptFragment.ContentPosition.BELOW -> indexOf(item) + 1
                    }
                    multipleContentList?.forEach {
                        val newContent = Content(
                            type = contentType,
                            script = script.value!!.scriptId!!,
                            position = inputIndex.toLong(),
                            filename = MutableStateFlow(it.lastPathSegment ?: "")
                        )
                        add(inputIndex, newContent)
                        newContent.id = dao.insert(newContent)
                        when (contentType) {
                            ContentType.TEXT,
                            ContentType.SCRIPT -> {
                            }

                            ContentType.IMAGE,
                            ContentType.VIDEO,
                            ContentType.DOCUMENT,
                            ContentType.AUDIO -> newContent.saveFile(application, it)
                        }
                    }
                    if (multipleContentList == null && (contentType == ContentType.TEXT || contentType == ContentType.SCRIPT)) {
                        val newContent = Content(
                            type = contentType,
                            script = script.value!!.scriptId!!,
                            position = inputIndex.toLong()
                        )
                        add(inputIndex, newContent)
                        newContent.id = dao.insert(newContent)
                    }
                }
                withContext(Dispatchers.Main) {
                    appendTextFieldIfNeeded()
                }
            }
        }
    }

    fun deleteContent(content: Content){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                scriptContentList.value = scriptContentList.value.toMutableList().apply { remove(content) }
                content.deleteFile(application)
                dao.delete(content)
                withContext(Dispatchers.Main) {
                    appendTextFieldIfNeeded()
                }
            }
        }
    }

    fun makeAccountableBackup(data: Intent?,
                              pushNotificationPermissionLauncher: ActivityResultLauncher<String>,
                              pushNotificationUnit: AtomicReference<(() -> Unit)?>
    ){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                data?.data?.let { uri ->
                    val docFile = DocumentFile.fromTreeUri(application, uri) ?: run {
                        Toast.makeText(
                            application,
                            "Can't access Accountable directory!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@let
                    }
                    if (!docFile.canRead()) {
                        Toast.makeText(
                            application,
                            "Can't access Accountable directory!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@let
                    }

                    val dbFile: File = application.getDatabasePath("accountable_database")
                    val fileName = "Backup (${getDateTimeFromMillis()})"
                    var saveFile = docFile.findFile(fileName)
                    saveFile?.delete()
                    saveFile = docFile.createDirectory(fileName)
                    saveFile?.createFile("application/x-sqlite3", dbFile.name+".db")?.let { outputFile ->
                        application.contentResolver.openOutputStream(outputFile.uri ).use { output ->
                            application.contentResolver.openInputStream(dbFile.toUri()).use { input ->
                                if (output != null) {
                                    input?.copyTo(output)
                                }
                            }
                        }
                    }

                    val imageFile = DocumentFile.fromFile(File(application.filesDir.toString()+ File.separator + AppResources.ImageResource.DESTINATION_FOLDER))
                    val videoFile = DocumentFile.fromFile(File(application.filesDir.toString()+ File.separator + AppResources.VideoResource.DESTINATION_FOLDER))
                    val audioFile = DocumentFile.fromFile(File(application.filesDir.toString()+ File.separator + AppResources.AudioResource.DESTINATION_FOLDER))
                    val documentFile = DocumentFile.fromFile(File(application.filesDir.toString()+ File.separator + AppResources.DocumentResource.DESTINATION_FOLDER))
                    val itemsSaved = AtomicInteger(0)
                    val totalItems = imageFile.listFiles().size.plus(
                        videoFile.listFiles().size.plus(
                            audioFile.listFiles().size.plus(
                                documentFile.listFiles().size
                            )
                        )
                    )
                    AccountableNotification.createProgressNotification(
                        application,
                        "Backing Up",
                        pushNotificationPermissionLauncher,
                        pushNotificationUnit
                    ) { notification ->
                        repositoryScope.launch {
                            withContext(Dispatchers.IO) {
                                copyAppMediaToExternalFolder(AppResources.ImageResource.DESTINATION_FOLDER, imageFile, saveFile, notification, itemsSaved, totalItems)
                                copyAppMediaToExternalFolder(AppResources.VideoResource.DESTINATION_FOLDER, videoFile, saveFile, notification, itemsSaved, totalItems)
                                copyAppMediaToExternalFolder(AppResources.AudioResource.DESTINATION_FOLDER, audioFile, saveFile, notification, itemsSaved, totalItems)
                                copyAppMediaToExternalFolder(AppResources.DocumentResource.DESTINATION_FOLDER, documentFile, saveFile, notification, itemsSaved, totalItems)
                            }
                        }
                    }
                }
            }
        }
    }

    fun restoreAccountableDataFromBackup(data: Intent?,
                                         pushNotificationPermissionLauncher: ActivityResultLauncher<String>,
                                         pushNotificationUnit: AtomicReference<(() -> Unit)?>
    ){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                data?.data?.let { uri ->
                    val docFile = DocumentFile.fromTreeUri(application, uri) ?: run {
                        Toast.makeText(
                            application,
                            "Can't access Accountable directory!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@let
                    }
                    if (!docFile.canRead()) {
                        Toast.makeText(
                            application,
                            "Can't access Accountable directory!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@let
                    }

                    AccountableDatabase.closeDatabase()
                    val inputDbFile = docFile.findFile("accountable_database.db")
                    val dbFile: File = application.getDatabasePath("accountable_database")
                    application.contentResolver.openOutputStream(dbFile.toUri()).use { output ->
                        inputDbFile?.uri?.let { inputFile ->
                            application.contentResolver.openInputStream(inputFile).use { input ->
                                if (output != null){
                                    input?.copyTo(output)
                                }
                            }
                        }
                    }

                    val saveFile = DocumentFile.fromFile(application.filesDir)
                    val imageFile = docFile.findFile(AppResources.ImageResource.DESTINATION_FOLDER)
                    val videoFile = docFile.findFile(AppResources.VideoResource.DESTINATION_FOLDER)
                    val audioFile = docFile.findFile(AppResources.AudioResource.DESTINATION_FOLDER)
                    val documentFile = docFile.findFile(AppResources.DocumentResource.DESTINATION_FOLDER)
                    val itemsSaved = AtomicInteger(0)
                    val totalItems = (
                            imageFile?.listFiles()?.size?.plus(
                                videoFile?.listFiles()?.size?.plus(
                                    audioFile?.listFiles()?.size?.plus(
                                        documentFile?.listFiles()?.size!!
                                    )?: 0
                                ) ?: 0
                            ) ?: 0)
                    AccountableNotification.createProgressNotification(
                        application,
                        "Restoring Back Up",
                        pushNotificationPermissionLauncher,
                        pushNotificationUnit
                    ) { notification ->
                        repositoryScope.launch {
                            withContext(Dispatchers.IO) {
                                copyAppMediaToExternalFolder(AppResources.ImageResource.DESTINATION_FOLDER, imageFile, saveFile, notification, itemsSaved, totalItems)
                                copyAppMediaToExternalFolder(AppResources.VideoResource.DESTINATION_FOLDER, videoFile, saveFile, notification, itemsSaved, totalItems)
                                copyAppMediaToExternalFolder(AppResources.AudioResource.DESTINATION_FOLDER, audioFile, saveFile, notification, itemsSaved, totalItems)
                                copyAppMediaToExternalFolder(AppResources.DocumentResource.DESTINATION_FOLDER, documentFile, saveFile, notification, itemsSaved, totalItems)
                                dao = AccountableDatabase.getInstance(application).repositoryDao
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun copyAppMediaToExternalFolder(inputFolder:String,
                                                     inputDocumentFile: DocumentFile?,
                                                     outputDocumentFile: DocumentFile?,
                                                     notification: AccountableNotification,
                                                     progress: AtomicInteger,
                                                     total: Int
    ){
        outputDocumentFile?.findFile(inputFolder)?.delete()
        outputDocumentFile?.createDirectory(inputFolder).let { imageDocFile ->
            inputDocumentFile?.listFiles()?.forEach { inputImage ->
                application.contentResolver.openInputStream(inputImage.uri).use { input ->
                    inputImage.name.let { name ->
                        if (name != null) {
                            imageDocFile?.createFile("", name)?.let { outputFile ->
                                application.contentResolver.openOutputStream(outputFile.uri).use { output ->
                                    if (output != null) {
                                        input?.copyTo(output)
                                        withContext(Dispatchers.Main) {
                                            notification.updateNotification(progress.incrementAndGet(),total)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getDateTimeFromMillis(): String {
        val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy-hh:mm", Locale.getDefault()).format(Date())
        return simpleDateFormat.format(System.currentTimeMillis())
    }

    fun setAppSettingsTextSize(textSize:Int){
        repositoryScope.launch {
            withContext(Dispatchers.IO){
                if (appSettings.value != null) {
                    appSettings.value!!.textSize = textSize
                    dao.update(appSettings.value!!)
                }
            }
        }
    }

    fun getMarkupLanguages(){
        repositoryScope.launch{
            markupLanguagesList.value = withContext(Dispatchers.IO){
                val arrayList = ArrayList<MarkupLanguage>(dao.getMarkupLanguages())
                if (script.value != null && script.value!!.scriptMarkupLanguage == null){
                    var exist = false
                    for (markupLanguage in arrayList) {
                        if (markupLanguage.name.value == script.value!!.scriptMarkupLanguage){
                            exist = true
                            break
                        }
                    }
                    if (!exist){
                        script.value!!.scriptMarkupLanguage = null
                        dao.update(script.value!!)
                    }
                }

                var exist = false
                for (markupLanguage in arrayList) {
                    if (markupLanguage.name.value == defaultMarkupLanguage.value.name.value){
                        exist = true
                        break
                    }
                }
                if (!exist) arrayList.add(defaultMarkupLanguage.value)
                arrayList.toList()
            }
        }
    }

    fun deleteMarkupLanguage(
        markupLanguage: MarkupLanguage,
        appendedUnit: (() -> Unit)? = null
    ) {
        repositoryScope.launch{
            withContext(Dispatchers.IO) {
                dao.delete(markupLanguage)
                withContext(Dispatchers.Main){
                    appendedUnit?.invoke()
                }
            }
        }
    }

    fun saveMarkupLanguage(similarList: List<String>, appendedUnit: (() -> Unit)?){
        repositoryScope.launch{
            withContext(Dispatchers.IO) {
                // skip the save if there are conflicting identifiers
                var isValid = false
                spansNotSimilarAndNameUnique(similarList) { isValidInput, _, _ ->
                    isValid = isValidInput
                }
                if (isValid) {
                    scriptMarkupLanguage.value?.let { dao.upsert(it) }
                }
                appendedUnit?.invoke()
            }
        }
    }

    fun setMarkupLanguageToScript(set:Boolean, appendedUnit: (suspend () -> Unit)? = null){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                script.value?.let { script ->
                    if (set) {
                        scriptMarkupLanguage.value?.name?.value.let {
                            script.scriptMarkupLanguage = it
                            dao.update(script)
                        }
                    }
                    else{
                        scriptMarkupLanguage.value = null
                        script.scriptMarkupLanguage = null
                        dao.update(script)
                    }
                }
                appendedUnit?.invoke()
            }
        }
    }

    fun deleteDefaultMarkupLanguage(appendedUnit: (() -> Unit)? = null){
        deleteMarkupLanguage(defaultMarkupLanguage.value,appendedUnit)
    }

    fun setRepositoryMarkupLanguage(markupLanguage: MarkupLanguage?){
        scriptMarkupLanguage.value = markupLanguage
    }

    fun spansNotSimilarAndNameUnique(similarList: List<String>, process:(isValid:Boolean, similarList:List<String>, nameUniqueErrorMessage:String)->Unit){
        val index = markupLanguageSelectedIndex.value
        if (index>=markupLanguagesList.value.size) return
        val indexName = markupLanguagesList.value[index].name.value
        var nameUniqueErrorMessage = ""
        if (indexName.isEmpty() || indexName == MainActivity.ResourceProvider.getString(R.string.new_markup_language)) {
            nameUniqueErrorMessage = MainActivity.ResourceProvider.getString(R.string.name_is_not_allowed, indexName)
        }
        else {
            for ((i, mLanguage) in markupLanguagesList.value.withIndex()) {
                if (i != index && mLanguage.name.value == indexName) {
                    nameUniqueErrorMessage = MainActivity.ResourceProvider.getString(
                        R.string.name_is_not_unique,
                        indexName
                    )
                }
            }
        }
        process(similarList.isEmpty() && nameUniqueErrorMessage.isEmpty(),similarList,nameUniqueErrorMessage)
    }

    fun resetDefaultMarkupLanguage(appendedUnit: (() -> Unit)? = null){
        defaultMarkupLanguage.value = MarkupLanguage()
        markupLanguagesList.value = arrayListOf()
        markupLanguageSelectedIndex.value = -1
        appendedUnit?.invoke()
    }

    fun setMarkupLanguageSelectedIndex(selection:Int){
        markupLanguageSelectedIndex.value = selection
    }

    fun loadTeleprompterSettingsList(){
        repositoryScope.launch{
            teleprompterSettingsList.value = withContext(Dispatchers.IO){
                val arrayList = ArrayList<TeleprompterSettings>(dao.getTeleprompterSettings())
                if (script.value != null && script.value!!.scriptTeleprompterSettings == null){
                    var exist = false
                    for (teleprompterSetting in arrayList) {
                        if (teleprompterSetting.id == script.value!!.scriptTeleprompterSettings){
                            exist = true
                            break
                        }
                    }
                    if (!exist){
                        script.value!!.scriptTeleprompterSettings = null
                        dao.update(script.value!!)
                    }
                }

                var exist = false
                for (teleprompterSetting in arrayList) {
                    if (teleprompterSetting.id == defaultTeleprompterSetting.value.id){
                        exist = true
                        break
                    }
                }
                if (!exist) arrayList.add(defaultTeleprompterSetting.value)
                arrayList.toList()
            }
        }
    }

    fun setRepositoryTeleprompterSetting(teleprompterSettings: TeleprompterSettings?){
        scriptTeleprompterSetting.value = teleprompterSettings
    }

    fun resetDefaultTeleprompterSetting(appendedUnit: (() -> Unit)? = null){
        defaultTeleprompterSetting.value = TeleprompterSettings()
        teleprompterSettingsList.value = arrayListOf()
        teleprompterSettingsSelectedIndex.value = -1
        appendedUnit?.invoke()
    }

    fun setTeleprompterSettingsSelectedIndex(selection:Int){
        teleprompterSettingsSelectedIndex.value = selection
    }

    fun saveTeleprompterSettings(appendedUnit: ((settingsId:Long?) -> Unit)? = null){
        repositoryScope.launch {
            scriptTeleprompterSetting.value?.let { teleprompterSettings ->
                val newId = dao.upsert(teleprompterSettings)
                teleprompterSettings.id = newId
                saveSpecialCharactersToDatabase(newId)
                appendedUnit?.invoke(newId)
            }?:run {
                appendedUnit?.invoke(null)
            }
        }
    }

    private fun saveSpecialCharactersToDatabase(newId:Long){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                specialCharactersList.value.forEach {
                    if (it.canUpdateList()) {
                        it.teleprompterSettingsId = newId
                        dao.upsert(it)
                    }
                }
            }
        }
    }

    fun setTeleprompterSettingToScript(set:Boolean, appendedUnit: (suspend () -> Unit)? = null){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                script.value?.let { script ->
                    if (set) {
                        scriptTeleprompterSetting.value?.id.let {
                            script.scriptTeleprompterSettings = it
                            dao.update(script)
                        }
                    }
                    else{
                        scriptTeleprompterSetting.value = null
                        script.scriptTeleprompterSettings = null
                        dao.update(script)
                    }
                }
                appendedUnit?.invoke()
            }
        }
    }

    fun deleteTeleprompterSetting(
        teleprompterSettings: TeleprompterSettings,
        deleteSpecialCharacters: Boolean = true,
        operationsAfterDelete:()->Unit
    ){
        repositoryScope.launch {
            withContext(Dispatchers.IO){
                if (deleteSpecialCharacters) dao.deleteSpecialCharacters(teleprompterSettings.id)
                dao.delete(teleprompterSettings)

                withContext(Dispatchers.Main) {
                    operationsAfterDelete()
                }
            }
        }
    }

    fun loadTeleprompterSpecialCharacters(){
        scriptTeleprompterSetting.value?.let { teleprompterSettings ->
            repositoryScope.launch {
                specialCharactersList.value = withContext(Dispatchers.IO) {
                    dao.getScriptSpecialCharacters(teleprompterSettings.id).toMutableList()
                }
            }
        }?:run {
            specialCharactersList.value = mutableListOf()
        }
    }

    fun deleteTeleprompterSettingSpecialCharacter(teleprompterSettings: TeleprompterSettings,
                                                  operationsAfterDelete: () -> Unit){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                dao.deleteSpecialCharacters(teleprompterSettings.id)
                withContext(Dispatchers.Main) {
                    operationsAfterDelete()
                }
            }
        }
    }

    fun addSpecialCharacter(emitAddSpecialCharacterUpdate:MutableSharedFlow<Int>){
        scriptTeleprompterSetting.value?.let { scriptTeleprompterSetting ->
            repositoryScope.launch {
                withContext(Dispatchers.IO) {
                    val id = scriptTeleprompterSetting.id
                    if (id == null) saveTeleprompterSettings { savedId ->
                        val specialCharactersInput = SpecialCharacters(savedId!!)
                        specialCharactersList.value.add(specialCharactersInput)
                        repositoryScope.launch {
                            emitAddSpecialCharacterUpdate.emit(specialCharactersList.value.indexOf(specialCharactersInput))
                        }
                    }
                    else {
                        val specialCharactersInput = SpecialCharacters(id)
                        specialCharactersList.value.add(specialCharactersInput)
                        emitAddSpecialCharacterUpdate.emit(specialCharactersList.value.indexOf(specialCharactersInput))
                    }
                }
            }
        }
    }

    fun deleteSpecialCharacter(specialCharacter: SpecialCharacters){
        repositoryScope.launch {
            withContext(Dispatchers.IO){
                dao.delete(specialCharacter)
            }
        }
    }

    fun setSearchScrollPosition(inputScrollPosition:Int){
        searchScrollPosition.value = inputScrollPosition
    }

    suspend fun getContentListNow(scriptId: Long?): List<Content> {
        return dao.getContentListNow(scriptId)
    }

    inner class ContentPreview(
        val id:Long
    ) {
        private var numAudios: MutableStateFlow<Int> = MutableStateFlow(0)
        private var numImages: MutableStateFlow<Int> = MutableStateFlow(0)
        private var numVideos: MutableStateFlow<Int> = MutableStateFlow(0)
        private var numScripts: MutableStateFlow<Int> = MutableStateFlow(0)
        private var numDocuments: MutableStateFlow<Int> = MutableStateFlow(0)
        private var description: MutableStateFlow<String> = MutableStateFlow("")
        private var displayImage: MutableStateFlow<Uri?> = MutableStateFlow(null)

        fun init(finished:(()->Unit)?=null): Job {
            return repositoryScope.launch {
                withContext(Dispatchers.IO) {
                    val contentList: List<Content> = getContentListNow(id)
                    val builder = StringBuilder("")
                    contentList.forEach { content ->
                        when (content.type) {
                            ContentType.TEXT -> {
                                if (content.content.value.isNotEmpty()) builder.append(content.content.value)
                            }

                            ContentType.IMAGE -> {
                                withContext(Dispatchers.Main) { numImages.value += 1 }
                                if (content.description.value.isNotEmpty()) builder.append(content.description.value)
                                if (displayImage.value == null) {
                                    val file = File(
                                        application.filesDir.toString() + "/" + AppResources.ImageResource.DESTINATION_FOLDER,
                                        content.content.value
                                    )
                                    withContext(Dispatchers.Main) {
                                        displayImage.value = if (file.exists()) {
                                            withContext(Dispatchers.IO) { Uri.fromFile(file) }
                                        } else {
                                            null
                                        }
                                    }
                                }
                            }

                            ContentType.AUDIO -> {
                                withContext(Dispatchers.Main) { numAudios.value += 1 }
                                if (content.description.value.isNotEmpty()) builder.append(content.description.value)
                            }

                            ContentType.VIDEO -> {
                                withContext(Dispatchers.Main) { numVideos.value += 1 }
                                if (content.description.value.isNotEmpty()) builder.append(content.description.value)
                            }

                            ContentType.DOCUMENT -> {
                                withContext(Dispatchers.Main) { numDocuments.value += 1 }
                                if (content.description.value.isNotEmpty()) builder.append(content.description.value)
                            }

                            ContentType.SCRIPT -> {
                                val scriptContentPreview =
                                    content.id?.let { it1 -> ContentPreview(it1) }
                                if (scriptContentPreview != null) {
                                    scriptContentPreview.init()
                                    withContext(Dispatchers.Main) {
                                        numAudios.value = scriptContentPreview.numAudios.value
                                        numImages.value = scriptContentPreview.numImages.value
                                        numVideos.value = scriptContentPreview.numVideos.value
                                        numDocuments.value = scriptContentPreview.numDocuments.value
                                        numScripts.value = scriptContentPreview.numScripts.value
                                        if (scriptContentPreview.description.value.isNotEmpty()) builder.append(
                                            scriptContentPreview.description.value
                                        )
                                        if (displayImage.value == null && scriptContentPreview.displayImage.value != null) displayImage.value =
                                            scriptContentPreview.displayImage.value
                                    }
                                }
                                withContext(Dispatchers.Main) { numScripts.value += 1 }
                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        description.value = builder.toString()
                        finished?.invoke()
                    }
                }
            }
        }

        fun getNumAudios(): StateFlow<Int> { return numAudios }
        fun getNumImages(): StateFlow<Int> { return numImages }
        fun getNumVideos(): StateFlow<Int> { return numVideos }
        fun getNumScripts(): StateFlow<Int> { return numScripts }
        fun getNumDocuments(): StateFlow<Int> { return numDocuments }
        fun getDescription(): StateFlow<String> { return description }
        fun getDisplayImage(): StateFlow<Uri?> { return displayImage }
    }

    suspend fun getFolderContentPreview(
        folder:Folder
    ){
        withContext(Dispatchers.IO){
            val folders = dao.getFoldersNow(folder.folderId, folder.folderType)
            withContext(Dispatchers.Main) {
                folder.numFolders.update { folders.size }
            }

            when (folder.folderType) {
                Folder.FolderType.SCRIPTS -> {
                    val scripts = dao.getScriptsNow(folder.folderId)
                    withContext(Dispatchers.Main) {
                        folder.numScripts.update { scripts.size }
                    }
                }

                Folder.FolderType.GOALS -> {
                    /*val goals = dao.getGoalsNow(folder.folderId)
                    withContext(Dispatchers.Main) {
                        folder.numGoals.update { goals.size }
                    }*/
                }
            }
        }
    }

    fun searchFragmentSearch(
        onScriptClick:(scriptId:Long)->Unit,
        notifyListCleared:MutableSharedFlow<Unit>,
        searchComplete:(()->Unit)?=null
    ){
        searchJob.value?.cancel()
        searchScriptsList.clear()
        searchJob.value = repositoryScope.launch {
            notifyListCleared.emit(Unit)
            if (searchString.value.isEmpty()) {
                searchJob.value?.cancel()
                searchJob.value = null
                return@launch
            }
            withContext(Dispatchers.IO){
                val title:String
                val id: Long = if (folder.value != null) {
                    title = folder.value!!.folderName.value
                    folder.value!!.folderId?:return@withContext
                } else {
                    title = application.getString(R.string.books)
                    appSettings.value?.appSettingId?:return@withContext
                }
                dao.searchFolderScripts(
                    id,
                    title,
                    searchString.value.trim(),
                    matchCaseCheck.value,
                    wordCheck.value,
                    searchScriptsList,
                    searchOccurrences,
                    searchNumScripts,
                    onScriptClick
                ){
                    searchJob.value = null
                    searchComplete?.invoke()
                }
            }
        }
    }

    fun setIsFromSearchFolderToTrue(appendedUnit: (() -> Unit)?=null){
        isFromSearchFolder = true
        appendedUnit?.invoke()
    }

    fun isFromSearchFragment():Boolean{
        val result = isFromSearchFolder
        isFromSearchFolder = false
        return result
    }

    fun resetSearchData(appendedUnit: (() -> Unit)?=null){
        searchScrollPosition.value = 0
        searchString.value = ""
        matchCaseCheck.value = false
        wordCheck.value = false
        searchJob.value?.cancel()
        searchJob.value = null
        searchOccurrences.value = 0
        searchNumScripts.value = 0
        searchScriptsList.clear()
        isFromSearchFolder = false
        appendedUnit?.invoke()
    }

    fun loadGoals(){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                changeFragment(AccountableFragment.GoalsFragment)
            }
        }
    }

    fun goBackToHomeFromGoals(){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                changeFragment(
                    AccountableFragment.HomeFragment
                )
            }
        }
    }

    fun loadEditGoal(id: Long? = null){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                    id?.let { id ->
                        // Load Existing Goal
                        val tempEditGoal = dao.getGoalWithTimes(id)
                        val tempNewGoal = Goal()
                        tempNewGoal.let { newGoal ->
                            tempEditGoal?.let { editGoal ->
                                newGoal.id = dao.insert(newGoal)

                                newGoal.goalCategory.value = editGoal.goalCategory.value
                                newGoal.initialDateTime = AppResources.CalendarResource(editGoal.initialDateTime.getCalendar())
                                newGoal.position.value = editGoal.position.value
                                newGoal.scrollPosition.scrollTo(editGoal.scrollPosition.value)
                                newGoal.size.value = editGoal.size.value
                                newGoal.numImages.value = editGoal.numImages.value
                                newGoal.numVideos.value = editGoal.numVideos.value
                                newGoal.numAudios.value = editGoal.numAudios.value
                                newGoal.numDocuments.value = editGoal.numDocuments.value
                                newGoal.numScripts.value = editGoal.numScripts.value
                                newGoal.goal.value = editGoal.goal.value
                                newGoal.dateOfCompletion = editGoal.dateOfCompletion?.let{ AppResources.CalendarResource(it.getCalendar()) }
                                newGoal.status.value = editGoal.status.value
                                newGoal.colour.value = editGoal.colour.value
                                newGoal.location.value = editGoal.location.value

                                newGoal.saveImage(application,editGoal.imageResource.getUriFromStorage(application))
                                for (time in editGoal.times) {
                                    newGoal.id?.let {
                                        val newTime = GoalTaskDeliverableTime()

                                        newTime.goal.value = it
                                        newTime.task.value = time.task.value
                                        newTime.deliverable.value = time.deliverable.value
                                        newTime.timeBlockType.value = time.timeBlockType.value
                                        newTime.start.value = time.start.value
                                        newTime.duration.value = time.duration.value
                                        newTime.id = dao.insert(newTime)
                                        newGoal.times.add(newTime)
                                    }
                                }
                                dao.update(newGoal)
                            }
                        }
                        withContext(Dispatchers.Main){
                            editGoal.value = tempEditGoal
                            newGoal.value = tempNewGoal
                        }
                    } ?: run {
                        // Make New Goal
                        withContext(Dispatchers.Main) {
                            editGoal.value = null
                            newGoal.value = withContext(Dispatchers.IO) {
                                val tempGoal = Goal()
                                tempGoal.id = dao.insert(tempGoal)
                                tempGoal
                            }
                        }
                    }
            }
            withContext(Dispatchers.IO){
                changeFragment(AccountableFragment.EditGoalFragment)
            }
        }
    }

    fun setNewGoalImage(imageUri: Uri?=null){
        repositoryScope.launch {
            withContext(Dispatchers.IO){
                if (imageUri!=null) newGoal.value?.saveImage(application, imageUri)
                newGoal.value?.let { dao.update(it) }
            }
        }
    }

    fun deleteNewGoalImage(){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                newGoal.value?.deleteFile(application)
            }
        }
    }

    fun addNewGoalTimeBlock(){
        repositoryScope.launch {
            withContext(Dispatchers.IO){
                newGoal.value?.id?.let {
                    val newTime = GoalTaskDeliverableTime()
                    newTime.goal.value = it
                    newTime.id = dao.insert(newTime)
                    withContext(Dispatchers.Main) {
                        newGoal.value?.times?.add(newTime)
                    }
                }
            }
        }
    }

    fun deleteNewGoalTimeBlock(timeBlock: GoalTaskDeliverableTime) {
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                dao.delete(timeBlock)
                withContext(Dispatchers.Main) {
                    newGoal.value?.times?.remove(timeBlock)
                }
            }
        }
    }

    fun saveNewGoal(process:()->Unit){
        repositoryScope.launch {
            if (editGoal.value == null) {
                editGoal.value = Goal()
                editGoal.value!!.id = dao.insert(editGoal.value!!)
            }
            editGoal.value?.let { editGoal ->
                newGoal.value?.let { newGoal ->
                    editGoal.goalCategory.value = newGoal.goalCategory.value
                    editGoal.initialDateTime =
                        AppResources.CalendarResource(newGoal.initialDateTime.getCalendar())
                    editGoal.position.value = newGoal.position.value
                    editGoal.scrollPosition.scrollTo(newGoal.scrollPosition.value)
                    editGoal.size.value = newGoal.size.value
                    editGoal.numImages.value = newGoal.numImages.value
                    editGoal.numVideos.value = newGoal.numVideos.value
                    editGoal.numAudios.value = newGoal.numAudios.value
                    editGoal.numDocuments.value = newGoal.numDocuments.value
                    editGoal.numScripts.value = newGoal.numScripts.value
                    editGoal.goal.value = newGoal.goal.value
                    editGoal.dateOfCompletion =
                        newGoal.dateOfCompletion?.let{ AppResources.CalendarResource(it.getCalendar()) }
                    editGoal.status.value = newGoal.status.value
                    editGoal.colour.value = newGoal.colour.value
                    editGoal.location.value = newGoal.location.value

                    editGoal.saveImage(
                        application,
                        newGoal.imageResource.getUriFromStorage(application)
                    )
                    for (time in editGoal.times) {
                        dao.delete(time)
                    }
                    editGoal.times.clear()
                    for (time in newGoal.times) {
                        editGoal.id?.let { id ->
                            val newTime = GoalTaskDeliverableTime()

                            newTime.goal.value = id
                            newTime.task.value = time.task.value
                            newTime.deliverable.value = time.deliverable.value
                            newTime.timeBlockType.value = time.timeBlockType.value
                            newTime.start.value = time.start.value
                            newTime.duration.value = time.duration.value
                            newTime.id = dao.insert(newTime)
                            editGoal.times.add(newTime)
                        }
                    }
                    dao.update(editGoal)

                    withContext(Dispatchers.Main){
                        process()
                    }
                }
            }
        }
    }

    fun clearNewGoal(){
        repositoryScope.launch {
            withContext(Dispatchers.IO){
                newGoal.value?.let {
                    it.deleteFile(application)
                    for (time in it.times) dao.delete(time)
                    dao.delete(it)
                    newGoal.value = null
                }
                editGoal.value = null
            }
        }
    }

    fun goBackToGoalsFromEditGoal(){
        repositoryScope.launch {
            withContext(Dispatchers.IO) {
                changeFragment(AccountableFragment.GoalsFragment)
            }
        }
    }
}