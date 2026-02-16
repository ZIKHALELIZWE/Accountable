package com.thando.accountable

import android.app.Activity
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.thando.accountable.AccountableNavigationController.AccountableFragment
import com.thando.accountable.database.AccountableDatabase
import com.thando.accountable.database.dataaccessobjects.RepositoryDao
import com.thando.accountable.database.tables.AppSettings
import com.thando.accountable.database.tables.Content
import com.thando.accountable.database.tables.Content.ContentType
import com.thando.accountable.database.tables.Deliverable
import com.thando.accountable.database.tables.Folder
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import com.thando.accountable.database.tables.Marker
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.database.tables.Script
import com.thando.accountable.database.tables.SpecialCharacters
import com.thando.accountable.database.tables.Task
import com.thando.accountable.database.tables.TeleprompterSettings
import com.thando.accountable.fragments.ContentPosition
import com.thando.accountable.fragments.viewmodels.BooksViewModel.Companion.INITIAL_FOLDER_ID
import com.thando.accountable.fragments.viewmodels.SearchViewModel
import com.thando.accountable.player.AccountablePlayer
import com.thando.accountable.ui.AccountableNotification
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference


@OptIn(ExperimentalCoroutinesApi::class)
class AccountableRepository(val application: Application): AutoCloseable {
    var dao: RepositoryDao = AccountableDatabase.getInstance(application).repositoryDao
    private val repositoryJob = Job()
    private val repositoryScope = CoroutineScope(MainActivity.Main + repositoryJob)
    private var loadFoldersListJob: CompletableJob? = null
    private var loadScriptsListJob: CompletableJob? = null

    private val folder = MutableStateFlow<Folder?>(null)
    val showScripts = MutableStateFlow<MutableStateFlow<Boolean>?>(null)
    val folderOrder = MutableStateFlow<MutableStateFlow<Boolean>?>(null)
    val listLoaded = MutableStateFlow(false)
    val scrollStateParent = MutableStateFlow<LazyListState?>(null)
    val listShown = MutableStateFlow(getListShown(false))

    private val appSettings = MutableStateFlow<AppSettings?>(null)
    private val foldersList = MutableStateFlow<Flow<List<Folder>>?>(null)
    private val scriptsList = MutableStateFlow<Flow<List<Script>>?>(null)
    private val goalsList = MutableStateFlow<Flow<List<Goal>>?>(null)
    var intentString: String? = null

    private val editFolder = MutableStateFlow<Folder?>(null)
    private val newEditFolder = MutableStateFlow<Folder?>(null)

    private val goal = MutableStateFlow<Flow<Goal?>?>(null)
    private val editGoal = MutableStateFlow<Goal?>(null)
    private var newGoal = MutableStateFlow<Flow<Goal?>?>(null)

    private val scriptsOrGoalsFolderType = MutableStateFlow(Folder.FolderType.SCRIPTS)

    private val direction = MutableStateFlow<AccountableFragment?>(null)
    private val currentFragment: MutableStateFlow<AccountableFragment?> = MutableStateFlow(null)

    private val script = MutableStateFlow<Script?>(null)
    private val scriptContentList = mutableStateListOf<Content>()
    private val scriptMarkupLanguage = MutableStateFlow<MarkupLanguage?>(null)
    private val isEditingScript = MutableStateFlow(false)

    private val markupLanguagesList = mutableStateListOf<MarkupLanguage>()
    private val defaultMarkupLanguage = MutableStateFlow(MarkupLanguage(
        name = MutableStateFlow(application.getString(R.string.new_markup_language))
    ))
    private val markupLanguageSelectedIndex = MutableStateFlow(-1)

    private val scriptTeleprompterSetting = MutableStateFlow<TeleprompterSettings?>(null)
    private val teleprompterSettingsList = mutableStateListOf<TeleprompterSettings>()
    private val defaultTeleprompterSetting = MutableStateFlow(TeleprompterSettings(
        name = MutableStateFlow(
            application.getString(R.string.default_settings)
        )
    ))
    private val teleprompterSettingsSelectedIndex = MutableStateFlow(-1)

    private val searchScrollPosition: LazyListState = LazyListState()
    private val searchString = TextFieldState("")
    private val matchCaseCheck: MutableState<Boolean> = mutableStateOf(false)
    private val wordCheck: MutableState<Boolean> = mutableStateOf(false)
    private val searchJob: MutableStateFlow<Job?> = MutableStateFlow(null)
    private val searchOccurrences: MutableStateFlow<Int> = MutableStateFlow(0)
    private val searchNumScripts: MutableStateFlow<Int> = MutableStateFlow(0)
    private val searchMenuOpen: MutableState<Boolean> = mutableStateOf(true)
    private val searchScriptsList: SnapshotStateList<SearchViewModel.ScriptSearch> =
        mutableStateListOf()
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
        if (MainActivity.IO == Dispatchers.IO) {
            // If not running tests then init. Will not run tests otherwise
            accountablePlayer.init(application)
        }
        repositoryScope.launch {
            appSettings.value = dao.appSettings()

            if (appSettings.value!!.initialFragment == AccountableFragment.HomeFragment){
                changeFragment(
                    AccountableFragment.HomeFragment,
                    AccountableFragment.AppSettingsFragment
                )
            }
            else {
                changeFragment(
                    appSettings.value!!.initialFragment,
                    AccountableFragment.HomeFragment
                )
            }
        }
    }

    override fun close() {
        accountablePlayer.onDestroy()
    }

    fun getFolder(): StateFlow<Folder?> {
        return folder
    }

    fun getShowScripts(): Flow<Boolean> {
        return showScripts.flatMapLatest {
                it ?: MutableStateFlow(false)
        }.flowOn(MainActivity.IO)
    }

    fun getFolderOrder(): Flow<Boolean> {
        return folderOrder.flatMapLatest {
                it ?: MutableStateFlow(false)
        }.flowOn(MainActivity.IO)
    }

    fun getListLoaded(): StateFlow<Boolean> {
        return listLoaded
    }

    fun getScrollStateParent(): StateFlow<LazyListState?> {
        return scrollStateParent
    }

    fun getListShown(): StateFlow<Folder.FolderListType> {
        return listShown
    }

    fun getAppSettings(): StateFlow<AppSettings?> {
        return appSettings
    }

    fun getFoldersList(): Flow<List<Folder>> {
        return foldersList.flatMapLatest {
                it ?: MutableStateFlow(emptyList())
        }.flowOn(MainActivity.IO)
    }

    fun getScriptsList(): Flow<List<Script>> {
        return scriptsList.flatMapLatest {
                it?: MutableStateFlow(emptyList())
        }.flowOn(MainActivity.IO)
    }

    fun getGoalsList(): Flow<List<Goal>> {
        return goalsList.flatMapLatest {
                it?:MutableStateFlow(emptyList())
        }.flowOn(MainActivity.IO)
    }

    fun getEditFolder(): StateFlow<Folder?> {
        return editFolder
    }

    fun getNewEditFolder(): StateFlow<Folder?> {
        return newEditFolder
    }

    fun getNewGoal(): Flow<Goal?> {
        return newGoal.flatMapLatest {
                it ?: flowOf(null)
        }.flowOn(MainActivity.IO)
    }

    fun getEditGoal(): StateFlow<Goal?> {
        return editGoal
    }

    fun getGoal(): Flow<Goal?> {
        return goal.flatMapLatest {
                it?:MutableStateFlow(null)
        }.flowOn(MainActivity.IO)
    }

    fun getDirection(): MutableStateFlow<AccountableFragment?> {
        return direction
    }

    fun getScriptsOrGoalsFolderType(): StateFlow<Folder.FolderType?> {
        return scriptsOrGoalsFolderType
    }

    fun getCurrentFragment(): StateFlow<AccountableFragment?> {
        return currentFragment
    }

    fun getScript(): StateFlow<Script?> {
        return script
    }

    fun getScriptContentList(): SnapshotStateList<Content> {
        return scriptContentList
    }

    fun getScriptMarkupLanguage(): StateFlow<MarkupLanguage?> {
        return scriptMarkupLanguage
    }

    fun getIsEditingScript(): MutableStateFlow<Boolean> {
        return isEditingScript
    }

    fun getMarkupLanguagesList(): SnapshotStateList<MarkupLanguage> {
        return markupLanguagesList
    }

    fun getDefaultMarkupLanguage(): StateFlow<MarkupLanguage> {
        return defaultMarkupLanguage
    }

    fun getMarkupLanguageSelectedIndex(): StateFlow<Int> {
        return markupLanguageSelectedIndex
    }

    fun getScriptTeleprompterSetting(): StateFlow<TeleprompterSettings?> {
        return scriptTeleprompterSetting
    }

    fun getTeleprompterSettingsList(): SnapshotStateList<TeleprompterSettings> {
        return teleprompterSettingsList
    }

    fun getDefaultTeleprompterSettings(): StateFlow<TeleprompterSettings> {
        return defaultTeleprompterSetting
    }

    fun getTeleprompterSettingsSelectedIndex(): StateFlow<Int> {
        return teleprompterSettingsSelectedIndex
    }

    fun getSearchScrollPosition(): LazyListState {
        return searchScrollPosition
    }

    fun getSearchString(): TextFieldState {
        return searchString
    }

    fun getMatchCaseCheck(): MutableState<Boolean> {
        return matchCaseCheck
    }

    fun getWordCheck(): MutableState<Boolean> {
        return wordCheck
    }

    fun getSearchJob(): StateFlow<Job?> {
        return searchJob
    }

    fun getSearchOccurrences(): StateFlow<Int> {
        return searchOccurrences
    }

    fun getSearchNumScripts(): StateFlow<Int> {
        return searchNumScripts
    }

    fun getSearchMenuOpen(): MutableState<Boolean> {
        return searchMenuOpen
    }

    fun getSearchScriptsList(): SnapshotStateList<SearchViewModel.ScriptSearch> {
        return searchScriptsList
    }

    fun clearFolderLists() {
        scriptsList.value = null
        foldersList.value = null
        goalsList.value = null
    }

    fun loadScriptsList(
        ascendingOrder: Boolean,
        folderNotAppSettings: Boolean,
        appendedUnit: (() -> Unit)? = null
    ) {
        loadScriptsListJob?.cancel()
        loadScriptsListJob = Job()
        CoroutineScope(MainActivity.IO + loadScriptsListJob!!).launch {
            val id = if (folderNotAppSettings) folder.value?.folderId else INITIAL_FOLDER_ID
            if (id != null) {
                withContext(MainActivity.Main) {
                    if (ascendingOrder) {
                        scriptsList.value = dao.getScripts(id)
                    } else {
                        scriptsList.value = dao.getScriptsDESC(id)
                    }
                }
            }
            withContext(MainActivity.Main) { appendedUnit?.invoke() }
            loadScriptsListJob?.complete()
            loadScriptsListJob = null
        }
    }

    fun loadGoalsList(
        ascendingOrder: Boolean,
        folderNotAppSettings: Boolean,
        appendedUnit: (() -> Unit)? = null
    ) {
        val id = if (folderNotAppSettings) folder.value?.folderId else INITIAL_FOLDER_ID
        if (id != null) {
            if (ascendingOrder) {
                goalsList.value = getGoals(id)
            } else {
                goalsList.value = getGoalsDESC(id)
            }
        }
        appendedUnit?.invoke()
    }

    fun loadFoldersList(
        ascendingOrder: Boolean,
        folderNotAppSettings: Boolean,
        folderTypeScriptsNotGoals: Boolean,
        appendedUnit: (() -> Unit)? = null
    ) {
        loadFoldersListJob?.cancel()
        loadFoldersListJob = Job()
        CoroutineScope(MainActivity.IO + loadFoldersListJob!!).launch {
            val id = if (folderNotAppSettings) folder.value?.folderId else INITIAL_FOLDER_ID
            val type =
                if (folderTypeScriptsNotGoals)
                    Folder.FolderType.SCRIPTS
                else Folder.FolderType.GOALS
            withContext(MainActivity.Main) {
                if (ascendingOrder) {
                    foldersList.value = dao.getFolders(id, type)
                } else {
                    foldersList.value = dao.getFoldersDESC(id, type)
                }
                appendedUnit?.invoke()
            }
            loadFoldersListJob?.complete()
            loadFoldersListJob = null
        }
    }

    suspend fun loadFolder(folderIdInput: Long? = null) {
        val folderId: Long =
            folderIdInput ?: (folder.value?.folderId ?: INITIAL_FOLDER_ID)
        folder.update {
            if (folderId != INITIAL_FOLDER_ID) dao.getFolder(folderId).first()
            else null
        }
        if (folder.value != null) {
            showScripts.update { folder.value!!.folderShowScripts }
            scrollStateParent.update { folder.value!!.folderScrollPosition }
        } else {
            if (scriptsOrGoalsFolderType.value == Folder.FolderType.SCRIPTS) showScripts.update { appSettings.value?.showScripts }
            else if (scriptsOrGoalsFolderType.value == Folder.FolderType.GOALS) showScripts.update { appSettings.value?.showGoals }
            scrollStateParent.update { appSettings.value?.scrollPosition }
        }
        loadFolderData()
    }

    fun folderIsScripts(): Boolean {
        return scriptsOrGoalsFolderType.value == Folder.FolderType.SCRIPTS
    }

    private fun getListShown(showScripts: Boolean): Folder.FolderListType {
        return if (showScripts) {
            if (folderIsScripts()) Folder.FolderListType.SCRIPTS
            else Folder.FolderListType.GOALS
        } else Folder.FolderListType.FOLDERS
    }

    fun loadFolderData() {
        clearFolderLists()
        showScripts.value?.let { showScripts -> listShown.update { getListShown(showScripts.value) } }
        if (folder.value != null) {
            if (folder.value?.folderShowScripts?.value == true) folderOrder.update {
                folder.value?.folderScriptsOrder
            }
            else folderOrder.update {
                folder.value?.folderFoldersOrder
            }
            folderOrder.value?.value?.let { loadContent(it) }
        } else if (appSettings.value != null) {
            if (scriptsOrGoalsFolderType.value == Folder.FolderType.SCRIPTS) {
                if (appSettings.value!!.showScripts.value)
                    folderOrder.update {
                        appSettings.value!!.scriptsOrder
                    }
                else folderOrder.update {
                    appSettings.value!!.foldersOrder
                }
                folderOrder.value?.value?.let { loadContent(it) }
            } else if (scriptsOrGoalsFolderType.value == Folder.FolderType.GOALS) {
                if (appSettings.value!!.showGoals.value)
                    folderOrder.update {
                        appSettings.value!!.goalScriptsOrder
                    }
                else folderOrder.update {
                    appSettings.value!!.goalFoldersOrder
                }
                folderOrder.value?.value?.let { loadContent(it) }
            }
        }
    }

    fun loadContent(ascendingOrder: Boolean) {
        listLoaded.update { false }
        val folderNotAppSettings = folder.value != null
        val folderTypeScriptsNotGoals = scriptsOrGoalsFolderType.value == Folder.FolderType.SCRIPTS
        if (showScripts.value?.value == true) {
            if (folderTypeScriptsNotGoals) {
                loadScriptsList(
                    ascendingOrder = ascendingOrder,
                    folderNotAppSettings = folderNotAppSettings
                )
            } else {
                loadGoalsList(
                    ascendingOrder = ascendingOrder,
                    folderNotAppSettings = folderNotAppSettings
                )
            }
        } else {
            loadFoldersList(
                ascendingOrder = ascendingOrder,
                folderNotAppSettings = folderNotAppSettings,
                folderTypeScriptsNotGoals = folderTypeScriptsNotGoals
            )
        }
    }

    fun updateAppSettings() {
        repositoryScope.launch {
            withContext(MainActivity.IO) {
                appSettings.value?.let { dao.update(it) }
            }
        }
    }

    fun saveCustomAppSettingsImage(imageUri: Uri?) {
        repositoryScope.launch {
            withContext(MainActivity.IO) {
                appSettings.value?.saveImage(application, imageUri)
                // Update database
                updateAppSettings()
            }
        }
    }

    fun restoreDefaultCustomAppSettingsImage() {
        repositoryScope.launch {
            withContext(MainActivity.IO) {
                appSettings.value?.restoreDefaultFile(application)
                updateAppSettings()
            }
        }
    }

    fun updateScriptsOrGoalsFolderScrollPosition(
        index: Int,
        offset: Int,
        appendedUnit: (() -> Unit)?
    ) {
        repositoryScope.launch {
            withContext(MainActivity.IO) {
                if (folder.value == null) {
                    // Save to settings table
                    appSettings.value?.scrollPosition?.requestScrollToItem(index, offset)
                    appSettings.value?.let { dao.update(it) }
                } else {
                    // Saved in Folders table
                    folder.value?.folderScrollPosition?.requestScrollToItem(index, offset)
                    appSettings.value?.let { dao.update(it) }
                }
                withContext(MainActivity.Main) {
                    appendedUnit?.invoke()
                }
            }
        }
    }

    fun updateFolderShowScripts(appendedUnit: (suspend () -> Unit)?) {
        repositoryScope.launch {
            withContext(MainActivity.IO) {
                if (folder.value == null) {
                    // Save to settings table
                    appSettings.value?.let { dao.update(it) }
                } else {
                    // Saved in Folders table
                    folder.value?.let { dao.update(it) }
                }
                if (appendedUnit != null) {
                    withContext(MainActivity.Main) { appendedUnit() }
                }
            }
        }
    }

    // Navigation Code
    fun changeFragment(
        newFragment: AccountableFragment, initializingFragment: AccountableFragment?=null
    ) {
        val currentFragmentInScope = initializingFragment?:currentFragment.value
        currentFragmentInScope?.let { currentFragmentInScope ->
            val (validatedFragment, navArgs) = AccountableNavigationController.getFragmentDirections(
                currentFragmentInScope,
                newFragment
            )
            if (validatedFragment == null) return
            if (initializingFragment!=null) currentFragment.value = validatedFragment
            if (navArgs.isDrawerFragment) {
                appSettings.value?.initialFragment = newFragment
                repositoryScope.launch {
                    withContext(MainActivity.IO) {
                        appSettings.value?.let {
                            dao.update(
                                it
                            )
                        }
                    }
                }
            }

            repositoryScope.launch {
                navArgs.scriptsOrGoalsFolderType?.let { scriptsOrGoalsFolderType.value = it }
                navArgs.scriptsOrGoalsFolderId?.let { loadFolder(it) }
                currentFragment.emit(validatedFragment)
                direction.emit(validatedFragment)
            }
        }
    }

    suspend fun directionChanged() {
        direction.emit(null)
    }

    data class NavigationArguments(var isValidDir: Boolean = false) {
        var isDrawerFragment = false
        var scriptsOrGoalsFolderId: Long? = null
        var scriptsOrGoalsFolderType: Folder.FolderType? = null
    }

    fun loadEditFolder(id: Long?) {
        repositoryScope.launch {
            withContext(MainActivity.IO) {
                withContext(MainActivity.Main) {
                    newEditFolder.update { null }
                    editFolder.value = withContext(MainActivity.IO) {
                        if (id != INITIAL_FOLDER_ID) dao.getFolder(id).first()
                        else null
                    }
                }
                changeFragment(AccountableFragment.EditFolderFragment)
            }
        }
    }

    suspend fun deleteFolder(id: Long?) {
        withContext(MainActivity.IO) {
            id?.let {
                dao.deleteFolder(it, application)
            }
        }
    }

    suspend fun deleteGoal(id: Long?) {
        withContext(MainActivity.IO) {
            id?.let {
                dao.deleteGoal(it, application)
            }
        }
    }

    suspend fun deleteScript(id: Long?) {
        withContext(MainActivity.IO) {
            id?.let {
                dao.deleteScript(it, application)
            }
        }
    }

    suspend fun setNewEditFolder(editFolder: Folder?) {
        if (newEditFolder.value != null) return
        newEditFolder.value = withContext(MainActivity.IO) {
            clearNewEditFolder(runAsync = false, resetEditFolder = false)
            val newFolder = Folder(
                folderType = scriptsOrGoalsFolderType.value,
                folderParent = folder.value?.folderId ?: INITIAL_FOLDER_ID
            )

            newFolder.folderId = dao.insert(newFolder)

            if (editFolder != null) {
                copyFolder(editFolder, newFolder)
            } else {
                if (folder.value == null) {
                    // Update AppSettings Table
                    newFolder.folderPosition = if (appSettings.value != null) dao.getFolders(
                        INITIAL_FOLDER_ID,
                        scriptsOrGoalsFolderType.value
                    ).first().size.toLong()
                    else 0.toLong()
                } else {
                    // Update Folders Table
                    newFolder.folderPosition = dao.getFolders(
                        folder.value!!.folderId,
                        folder.value!!.folderType
                    ).first().size.toLong()
                }
            }
            newFolder
        }
    }

    private suspend fun copyFolder(from: Folder, to: Folder) {
        if (to.folderId == from.folderId) return
        to.folderName.setTextAndPlaceCursorAtEnd(from.folderName.text.toString())
        to.folderPosition = from.folderPosition
        to.folderScrollPosition.requestScrollToItem(from.folderScrollPosition.firstVisibleItemIndex)
        to.folderShowScripts.value = from.folderShowScripts.value
        to.saveImage(application, from.imageResource.getUriFromStorage(application))
        dao.update(to)
    }

    suspend fun setNewEditFolderImage(imageUri: Uri? = null) {
        withContext(MainActivity.IO) {
            if (imageUri != null) newEditFolder.value?.saveImage(application, imageUri)
            dao.update(newEditFolder.value!!)
        }
    }

    suspend fun clearNewEditFolder(runAsync: Boolean = true, resetEditFolder: Boolean = true) {
        val unit = suspend {
            if (newEditFolder.value != null) {
                newEditFolder.value!!.imageResource.deleteFile(application)
                dao.delete(newEditFolder.value!!)
            }
            if (resetEditFolder) editFolder.value = null
            null
        }
        if (runAsync) newEditFolder.value = withContext(MainActivity.IO) {
            unit()
        }
        else {
            newEditFolder.value = unit()
        }
    }

    suspend fun saveNewEditFolderToNewFolder() {
        if (editFolder.value == null) {
            newEditFolder.value?.let { dao.update(it) }
        } else {
            newEditFolder.value?.let {
                copyFolder(it, editFolder.value!!)
                it.deleteFile(application)
                dao.delete(it)
            }
        }
        withContext(MainActivity.Main) {
            editFolder.value = null
            newEditFolder.value = null
        }
        loadFolder()
    }

    suspend fun deleteNewEditFolderImage() {
        withContext(MainActivity.IO) {
            newEditFolder.value?.deleteFile(application)
        }
    }

    fun appendIntentStringToScript(scriptId: Long, activity: Activity?) {
        repositoryScope.launch {
            withContext(MainActivity.IO) {
                if (scriptId != INITIAL_FOLDER_ID) {
                    withContext(MainActivity.Main) {
                        isEditingScript.value = false
                        script.value = withContext(MainActivity.IO) {
                            val tempScript = dao.getScriptNow(scriptId)
                            withContext(MainActivity.Main) {
                                scriptMarkupLanguage.value = withContext(MainActivity.IO) {
                                    tempScript!!.scriptMarkupLanguage.let { dao.getMarkupLanguage(it) }
                                }
                            }
                            tempScript
                        }
                        scriptContentList.clear()
                        scriptContentList.addAll(withContext(MainActivity.IO) {
                            dao.getContentList(scriptId)
                        })
                    }
                }
                if (script.value == null) {
                    Toast.makeText(
                        application,
                        application.getString(R.string.script_does_not_exist),
                        Toast.LENGTH_LONG
                    ).show()
                } else if (!intentString.isNullOrEmpty()) {
                    if (scriptContentList.lastIndex == -1 || scriptContentList.last().type != ContentType.TEXT) {
                        // Make a new TextProcessor and add the content's content
                        if (script.value != null && script.value!!.scriptId != null) {
                            scriptContentList.add(
                                Content(
                                    type = ContentType.TEXT,
                                    script = script.value!!.scriptId!!,
                                    position = scriptContentList.size.toLong()
                                )
                            )
                        }
                    }
                    withContext(MainActivity.Main) {
                        scriptContentList.last().content.edit { append("\n$intentString") }
                    }
                    saveScript {
                        activity?.finishAndRemoveTask()
                    }
                }
                intentString = null
                withContext(MainActivity.Main) {
                    activity?.finishAndRemoveTask()
                }
            }
        }
    }

    fun loadAndOpenGoal(goalId: Long) {
        repositoryScope.launch {
            withContext(MainActivity.Main) {
                goal.value = withContext(MainActivity.IO) {
                    getGoal(goalId)
                }
            }
            withContext(MainActivity.IO) {
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

    suspend fun loadAndOpenScript(scriptId: Long) {
        withContext(MainActivity.IO) {
            if (scriptId == INITIAL_FOLDER_ID) {
                val parentId = folder.value?.folderId ?: INITIAL_FOLDER_ID
                val scripts = dao.getScripts(parentId).first()
                withContext(MainActivity.Main) {
                    script.value = Script(
                        scriptParentType = Script.ScriptParentType.FOLDER,
                        scriptParent = parentId,
                        scriptPosition = scripts.size.toLong(),
                    )
                    scriptMarkupLanguage.value = null
                    isEditingScript.value = true
                    script.value!!.scriptId =
                        withContext(MainActivity.IO) { dao.insert(script.value!!) }
                    scriptContentList.clear()
                    appendTextFieldIfNeeded()
                }
            } else {
                withContext(MainActivity.Main) {
                    isEditingScript.value = false
                    script.value = withContext(MainActivity.IO) {
                        val tempScript = dao.getScriptNow(scriptId)
                        withContext(MainActivity.Main) {
                            scriptMarkupLanguage.value = withContext(MainActivity.IO) {
                                tempScript!!.scriptMarkupLanguage.let { dao.getMarkupLanguage(it) }
                            }
                        }
                        tempScript
                    }
                    withContext(MainActivity.IO) {
                        scriptContentList.clear()
                        scriptContentList.addAll(dao.getContentList(scriptId))
                        appendTextFieldIfNeeded()
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

    private fun appendTextFieldIfNeeded() {
        repositoryScope.launch {
            if (scriptContentList.lastIndex == -1 || scriptContentList.last().type != ContentType.TEXT) {
                // Make a new TextProcessor and add the content's content
                if (script.value != null && script.value!!.scriptId != null) {
                    scriptContentList.add(
                        Content(
                            type = ContentType.TEXT,
                            script = script.value!!.scriptId!!,
                            position = scriptContentList.size.toLong()
                        )
                    )
                }
            }
        }
    }

    fun deleteScriptImage() {
        repositoryScope.launch {
            withContext(MainActivity.IO) {
                script.value?.deleteFile(application)
                script.value?.let { dao.update(it) }
            }
        }
    }

    fun saveScriptImage(uri: Uri?) {
        if (uri != null) {
            repositoryScope.launch {
                withContext(MainActivity.IO) {
                    if (script.value?.scriptId == null) script.value?.let {
                        it.scriptId = dao.insert(it)
                    }
                    script.value?.saveImage(application, uri)
                    script.value?.let { dao.update(it) }
                }
            }
        }
    }

    fun saveScript(loadFolder: Boolean = false, appendedUnit: (() -> Unit)? = null) {
        repositoryScope.launch {
            withContext(MainActivity.IO) {
                if (script.value?.scriptId == null) script.value?.let {
                    it.scriptId = dao.insert(it)
                }
                else script.value?.let { dao.update(it) }

                // save content in the list
                var position = 1L
                scriptContentList.forEach { content ->
                    when (content.type) {
                        ContentType.TEXT -> {
                            if (content.content.text.isNotEmpty()) {
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

    private suspend fun saveContent(content: Content, position: Long): Long {
        content.position = position
        if (content.id == null) content.id = dao.insert(content)
        else dao.update(content)
        return position + 1
    }

    fun addContent(
        multipleContentList: List<Uri>?,
        contentType: ContentType,
        contentPosition: ContentPosition,
        item: Content,
        cursorPosition: Int?
    ) {
        repositoryScope.launch {
            withContext(MainActivity.IO) {
                val inputIndex = when (contentPosition) {
                    ContentPosition.ABOVE ->
                        scriptContentList.indexOf(item)

                    ContentPosition.AT_CURSOR_POINT -> {
                        val inputIndex = scriptContentList.indexOf(item) + 1
                        if (cursorPosition != null) {
                            // split the string
                            val text = item.content.text.toString()

                            // Split the string
                            val topString = text.take(cursorPosition)
                            val bottomString =
                                text.substring(cursorPosition, text.length)

                            item.content.edit { replace(0, length, topString) }
                            val newContent = Content(
                                type = ContentType.TEXT,
                                script = script.value!!.scriptId!!,
                                position = inputIndex.toLong(),
                                content = TextFieldState(bottomString)
                            )
                            scriptContentList.add(inputIndex, newContent)
                            newContent.id = dao.insert(newContent)
                        } else return@withContext
                        inputIndex
                    }

                    ContentPosition.BELOW -> scriptContentList.indexOf(item) + 1
                }
                multipleContentList?.forEach {
                    val newContent = Content(
                        type = contentType,
                        script = script.value!!.scriptId!!,
                        position = inputIndex.toLong(),
                        filename = TextFieldState(it.lastPathSegment ?: "")
                    )
                    scriptContentList.add(inputIndex, newContent)
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
                    scriptContentList.add(inputIndex, newContent)
                    newContent.id = dao.insert(newContent)
                }
                withContext(MainActivity.Main) {
                    appendTextFieldIfNeeded()
                }
            }
        }
    }

    fun deleteContent(content: Content) {
        repositoryScope.launch {
            withContext(MainActivity.IO) {
                if (accountablePlayer.contains(content)) accountablePlayer.close(content)
                scriptContentList.remove(content)
                content.deleteFile(application)
                dao.delete(content)
                withContext(MainActivity.Main) {
                    appendTextFieldIfNeeded()
                }
            }
        }
    }

    fun printScriptEntry() {
        repositoryScope.launch {
            withContext(MainActivity.IO) {
                script.value?.scriptTitle?.let { title ->
                    val outputStringBuilder = StringBuilder()
                    scriptContentList.forEach { content ->
                        if (content.type == ContentType.TEXT) {
                            outputStringBuilder.append(content.content.text.toString())
                        }
                    }

                    val contentValues = ContentValues().apply {
                        put(
                            MediaStore.Files.FileColumns.DISPLAY_NAME,
                            title.text.toString()
                        )
                        put(
                            MediaStore.Files.FileColumns.MIME_TYPE,
                            "text/plain"
                        )
                        put(
                            MediaStore.Files.FileColumns.RELATIVE_PATH,
                            createFolderInDocuments(application.getString(R.string.app_name))
                        )
                    }

                    val resolver = application.contentResolver
                    val uri: Uri? = resolver.insert(
                        MediaStore.Files.getContentUri("external"),
                        contentValues
                    )
                    uri?.let {
                        val outputStream: OutputStream? = resolver.openOutputStream(it)
                        outputStream?.use { stream ->
                            stream.write(outputStringBuilder.toString().toByteArray())
                            stream.flush()
                        }
                    }
                }
            }
        }
    }

    fun appendFileToScript(uri: Uri) {
        repositoryScope.launch {
            withContext(MainActivity.IO) {
                script.value?.let {
                    appendTextFieldIfNeeded()
                    scriptContentList.last().appendFile(uri, application.contentResolver)
                }
            }
        }
    }

    private fun createFolderInDocuments(folderName: String): String {
        // Get the Documents directory
        val documentsDir = Environment.DIRECTORY_DOCUMENTS

        // Create the new folder
        val newFolder = File(documentsDir, folderName)

        // Check if the folder already exists, if not, create it
        if (!newFolder.exists()) {
            newFolder.mkdirs()
        }
        return documentsDir + File.separator + folderName
    }

    fun makeAccountableBackup(
        data: Intent?,
        pushNotificationPermissionLauncher: ActivityResultLauncher<String>,
        pushNotificationUnit: AtomicReference<(() -> Unit)?>
    ) {
        repositoryScope.launch {
            withContext(MainActivity.IO) {
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
                    saveFile?.createFile("application/x-sqlite3", dbFile.name + ".db")
                        ?.let { outputFile ->
                            application.contentResolver.openOutputStream(outputFile.uri)
                                .use { output ->
                                    application.contentResolver.openInputStream(dbFile.toUri())
                                        .use { input ->
                                            if (output != null) {
                                                input?.copyTo(output)
                                            }
                                        }
                                }
                        }

                    val imageFile =
                        DocumentFile.fromFile(File(application.filesDir.toString() + File.separator + AppResources.ImageResource.DESTINATION_FOLDER))
                    val videoFile =
                        DocumentFile.fromFile(File(application.filesDir.toString() + File.separator + AppResources.VideoResource.DESTINATION_FOLDER))
                    val audioFile =
                        DocumentFile.fromFile(File(application.filesDir.toString() + File.separator + AppResources.AudioResource.DESTINATION_FOLDER))
                    val documentFile =
                        DocumentFile.fromFile(File(application.filesDir.toString() + File.separator + AppResources.DocumentResource.DESTINATION_FOLDER))
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
                            withContext(MainActivity.IO) {
                                copyAppMediaToExternalFolder(
                                    AppResources.ImageResource.DESTINATION_FOLDER,
                                    imageFile,
                                    saveFile,
                                    notification,
                                    itemsSaved,
                                    totalItems
                                )
                                copyAppMediaToExternalFolder(
                                    AppResources.VideoResource.DESTINATION_FOLDER,
                                    videoFile,
                                    saveFile,
                                    notification,
                                    itemsSaved,
                                    totalItems
                                )
                                copyAppMediaToExternalFolder(
                                    AppResources.AudioResource.DESTINATION_FOLDER,
                                    audioFile,
                                    saveFile,
                                    notification,
                                    itemsSaved,
                                    totalItems
                                )
                                copyAppMediaToExternalFolder(
                                    AppResources.DocumentResource.DESTINATION_FOLDER,
                                    documentFile,
                                    saveFile,
                                    notification,
                                    itemsSaved,
                                    totalItems
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun restoreAccountableDataFromBackup(
        data: Intent?,
        pushNotificationPermissionLauncher: ActivityResultLauncher<String>,
        pushNotificationUnit: AtomicReference<(() -> Unit)?>
    ) {
        repositoryScope.launch {
            withContext(MainActivity.IO) {
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
                                if (output != null) {
                                    input?.copyTo(output)
                                }
                            }
                        }
                    }

                    val saveFile = DocumentFile.fromFile(application.filesDir)
                    val imageFile = docFile.findFile(AppResources.ImageResource.DESTINATION_FOLDER)
                    val videoFile = docFile.findFile(AppResources.VideoResource.DESTINATION_FOLDER)
                    val audioFile = docFile.findFile(AppResources.AudioResource.DESTINATION_FOLDER)
                    val documentFile =
                        docFile.findFile(AppResources.DocumentResource.DESTINATION_FOLDER)
                    val itemsSaved = AtomicInteger(0)
                    val totalItems = (
                            imageFile?.listFiles()?.size?.plus(
                                videoFile?.listFiles()?.size?.plus(
                                    audioFile?.listFiles()?.size?.plus(
                                        documentFile?.listFiles()?.size!!
                                    ) ?: 0
                                ) ?: 0
                            ) ?: 0)
                    AccountableNotification.createProgressNotification(
                        application,
                        "Restoring Back Up",
                        pushNotificationPermissionLauncher,
                        pushNotificationUnit
                    ) { notification ->
                        repositoryScope.launch {
                            withContext(MainActivity.IO) {
                                copyAppMediaToExternalFolder(
                                    AppResources.ImageResource.DESTINATION_FOLDER,
                                    imageFile,
                                    saveFile,
                                    notification,
                                    itemsSaved,
                                    totalItems
                                )
                                copyAppMediaToExternalFolder(
                                    AppResources.VideoResource.DESTINATION_FOLDER,
                                    videoFile,
                                    saveFile,
                                    notification,
                                    itemsSaved,
                                    totalItems
                                )
                                copyAppMediaToExternalFolder(
                                    AppResources.AudioResource.DESTINATION_FOLDER,
                                    audioFile,
                                    saveFile,
                                    notification,
                                    itemsSaved,
                                    totalItems
                                )
                                copyAppMediaToExternalFolder(
                                    AppResources.DocumentResource.DESTINATION_FOLDER,
                                    documentFile,
                                    saveFile,
                                    notification,
                                    itemsSaved,
                                    totalItems
                                )
                                dao = AccountableDatabase.getInstance(application).repositoryDao
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun copyAppMediaToExternalFolder(
        inputFolder: String,
        inputDocumentFile: DocumentFile?,
        outputDocumentFile: DocumentFile?,
        notification: AccountableNotification,
        progress: AtomicInteger,
        total: Int
    ) {
        outputDocumentFile?.findFile(inputFolder)?.delete()
        outputDocumentFile?.createDirectory(inputFolder).let { imageDocFile ->
            inputDocumentFile?.listFiles()?.forEach { inputImage ->
                application.contentResolver.openInputStream(inputImage.uri).use { input ->
                    inputImage.name.let { name ->
                        if (name != null) {
                            imageDocFile?.createFile("", name)?.let { outputFile ->
                                application.contentResolver.openOutputStream(outputFile.uri)
                                    .use { output ->
                                        if (output != null) {
                                            input?.copyTo(output)
                                            withContext(MainActivity.Main) {
                                                notification.updateNotification(
                                                    progress.incrementAndGet(),
                                                    total
                                                )
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
        val simpleDateFormat =
            SimpleDateFormat("dd-MM-yyyy-hh:mm", Locale.getDefault()).format(Date())
        return simpleDateFormat.format(System.currentTimeMillis())
    }

    fun setAppSettingsTextSize(textSize: Int) {
        repositoryScope.launch {
            withContext(MainActivity.IO) {
                if (appSettings.value != null) {
                    appSettings.value!!.textSize.update { textSize }
                    dao.update(appSettings.value!!)
                }
            }
        }
    }

    suspend fun getMarkupLanguages() {
        markupLanguagesList.clear()
        markupLanguagesList.addAll(withContext(MainActivity.IO) {
            val arrayList = dao.getMarkupLanguages().toMutableList()
            if (script.value != null && script.value!!.scriptMarkupLanguage != null) {
                var exist = false
                for (markupLanguage in arrayList) {
                    if (markupLanguage.name.value == script.value!!.scriptMarkupLanguage) {
                        exist = true
                        break
                    }
                }
                if (!exist) {
                    script.value!!.scriptMarkupLanguage = null
                    dao.update(script.value!!)
                }
            }

            var exist = false
            for (markupLanguage in arrayList) {
                if (markupLanguage.name.value == defaultMarkupLanguage.value.name.value) {
                    exist = true
                    break
                }
            }
            if (!exist) arrayList.add(defaultMarkupLanguage.value)
            arrayList
        })
    }

    suspend fun deleteMarkupLanguage(
        markupLanguage: MarkupLanguage,
        appendedUnit: (suspend () -> Unit)? = null
    ) {
        withContext(MainActivity.IO) {
            dao.delete(markupLanguage)
            withContext(MainActivity.Main) {
                appendedUnit?.invoke()
            }
        }
    }

    suspend fun saveMarkupLanguage(similarList: List<String>, appendedUnit: (() -> Unit)?) {
        withContext(MainActivity.IO) {
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

    fun setMarkupLanguageToScript(set: Boolean, appendedUnit: (suspend () -> Unit)? = null) {
        repositoryScope.launch {
            withContext(MainActivity.IO) {
                script.value?.let { script ->
                    if (set) {
                        scriptMarkupLanguage.value?.name?.value.let {
                            script.scriptMarkupLanguage = it
                            dao.update(script)
                        }
                    } else {
                        script.scriptMarkupLanguage = null
                        dao.update(script)
                    }
                }
                appendedUnit?.invoke()
            }
        }
    }

    suspend fun deleteDefaultMarkupLanguage(appendedUnit: (suspend () -> Unit)? = null) {
        deleteMarkupLanguage(defaultMarkupLanguage.value, appendedUnit)
    }

    fun setRepositoryMarkupLanguage(markupLanguage: MarkupLanguage?) {
        scriptMarkupLanguage.value = markupLanguage
    }

    fun spansNotSimilarAndNameUnique(
        similarList: List<String>,
        process: (isValid: Boolean, similarList: List<String>, nameUniqueErrorMessage: String) -> Unit
    ) {
        val index = markupLanguageSelectedIndex.value
        if (index == -1 || index >= markupLanguagesList.size) return
        val indexName = markupLanguagesList[index].name.value
        var nameUniqueErrorMessage = ""
        if (indexName.isEmpty() || indexName == application.getString(R.string.new_markup_language)) {
            nameUniqueErrorMessage =
                application.getString(R.string.name_is_not_allowed, indexName)
        } else {
            for ((i, mLanguage) in markupLanguagesList.withIndex()) {
                if (i != index && mLanguage.name.value == indexName) {
                    nameUniqueErrorMessage = application.getString(
                        R.string.name_is_not_unique,
                        indexName
                    )
                }
            }
        }
        process(
            similarList.isEmpty() && nameUniqueErrorMessage.isEmpty(),
            similarList,
            nameUniqueErrorMessage
        )
    }

    suspend fun resetDefaultMarkupLanguage(appendedUnit: (suspend () -> Unit)? = null) {
        defaultMarkupLanguage.update { MarkupLanguage(
            name = MutableStateFlow(application.getString(R.string.new_markup_language))
        ) }
        markupLanguagesList.clear()
        markupLanguageSelectedIndex.value = -1
        scriptMarkupLanguage.value = null
        appendedUnit?.invoke()
    }

    fun setMarkupLanguageSelectedIndex(selection: Int) {
        markupLanguageSelectedIndex.value = selection
    }

    suspend fun loadTeleprompterSettingsList() {
        teleprompterSettingsList.clear()
        teleprompterSettingsList.addAll(withContext(MainActivity.IO) {
            val arrayList = ArrayList<TeleprompterSettings>(dao.getTeleprompterSettings())
            if (script.value != null && script.value!!.scriptTeleprompterSettings == null) {
                var exist = false
                for (teleprompterSetting in arrayList) {
                    if (teleprompterSetting.id == script.value!!.scriptTeleprompterSettings) {
                        exist = true
                        break
                    }
                }
                if (!exist) {
                    script.value!!.scriptTeleprompterSettings = null
                    dao.update(script.value!!)
                }
            }

            var exist = false
            for (teleprompterSetting in arrayList) {
                if (teleprompterSetting.id == defaultTeleprompterSetting.value.id) {
                    exist = true
                    break
                }
            }
            if (!exist) arrayList.add(defaultTeleprompterSetting.value)
            arrayList.toList()
        })
    }

    suspend fun setRepositoryTeleprompterSetting(teleprompterSettings: TeleprompterSettings?) {
        scriptTeleprompterSetting.value = teleprompterSettings
        loadTeleprompterSpecialCharacters(teleprompterSettings)
    }

    suspend fun resetDefaultTeleprompterSetting(appendedUnit: (suspend () -> Unit)? = null) {
        defaultTeleprompterSetting.value = TeleprompterSettings(
            name = MutableStateFlow(
                application.getString(R.string.default_settings)
            )
        )
        teleprompterSettingsList.clear()
        teleprompterSettingsSelectedIndex.value = -1
        appendedUnit?.invoke()
    }

    fun setTeleprompterSettingsSelectedIndex(selection: Int) {
        teleprompterSettingsSelectedIndex.value = selection
    }

    suspend fun saveTeleprompterSettings(appendedUnit: (suspend (settingsId: Long?) -> Unit)? = null) {
        scriptTeleprompterSetting.value?.let { teleprompterSettings ->
            val newId = dao.upsert(teleprompterSettings)
            teleprompterSettings.id = newId
            saveSpecialCharactersToDatabase(newId)
            appendedUnit?.invoke(newId)
        } ?: run {
            appendedUnit?.invoke(null)
        }
    }

    private suspend fun saveSpecialCharactersToDatabase(newId: Long) {
        scriptTeleprompterSetting.value?.specialCharactersList?.forEach {
            if (it.canUpdateList()) {
                it.teleprompterSettingsId = newId
                dao.upsert(it)
            }
        }
    }

    suspend fun setTeleprompterSettingToScript(
        set: Boolean,
        appendedUnit: (suspend () -> Unit)? = null
    ) {
        script.value?.let { script ->
            if (set) {
                scriptTeleprompterSetting.value?.id.let {
                    script.scriptTeleprompterSettings = it
                    dao.update(script)
                }
            } else {
                scriptTeleprompterSetting.value = null
                script.scriptTeleprompterSettings = null
                dao.update(script)
            }
        }
        appendedUnit?.invoke()
    }

    suspend fun deleteTeleprompterSetting(
        teleprompterSettings: TeleprompterSettings,
        deleteSpecialCharacters: Boolean = true,
        operationsAfterDelete: suspend () -> Unit
    ) {
        withContext(MainActivity.IO) {
            if (deleteSpecialCharacters) dao.deleteSpecialCharacters(
                teleprompterSettings.id
            )
            dao.delete(teleprompterSettings)
        }

        withContext(MainActivity.Main) {
            operationsAfterDelete()
        }
    }

    suspend fun loadTeleprompterSpecialCharacters(teleprompterSettings: TeleprompterSettings?) {
        teleprompterSettings?.let { teleprompterSettings ->
            teleprompterSettings.specialCharactersList.clear()
            teleprompterSettings.specialCharactersList.addAll(
                withContext(MainActivity.IO) {
                    dao.getScriptSpecialCharacters(
                        teleprompterSettings.id
                    )
                }
            )
        }
    }

    fun deleteTeleprompterSettingSpecialCharacter(
        teleprompterSettings: TeleprompterSettings,
        operationsAfterDelete: () -> Unit
    ) {
        repositoryScope.launch {
            withContext(MainActivity.IO) {
                dao.deleteSpecialCharacters(teleprompterSettings.id)
                withContext(MainActivity.Main) {
                    operationsAfterDelete()
                }
            }
        }
    }

    fun addSpecialCharacter() {
        scriptTeleprompterSetting.value?.let { scriptTeleprompterSetting ->
            repositoryScope.launch {
                withContext(MainActivity.IO) {
                    val id = scriptTeleprompterSetting.id
                    if (id == null) saveTeleprompterSettings { savedId ->
                        val specialCharactersInput = SpecialCharacters(savedId!!)
                        scriptTeleprompterSetting.specialCharactersList.add(specialCharactersInput)
                    }
                    else {
                        val specialCharactersInput = SpecialCharacters(id)
                        scriptTeleprompterSetting.specialCharactersList.add(specialCharactersInput)
                    }
                }
            }
        }
    }

    fun deleteSpecialCharacter(specialCharacter: SpecialCharacters) {
        repositoryScope.launch {
            withContext(MainActivity.IO) {
                scriptTeleprompterSetting.value?.specialCharactersList?.removeIf { it == specialCharacter }
                dao.delete(specialCharacter)
            }
        }
    }

    fun setSearchScrollPosition(index: Int = 0, offset: Int = 0) {
        searchScrollPosition.requestScrollToItem(index, offset)
    }

    suspend fun getContentListNow(scriptId: Long?): List<Content> {
        return dao.getContentListNow(scriptId)
    }

    inner class GoalContentPreview(
        val id: Long
    ) {
        private var numAudios: MutableStateFlow<Int> = MutableStateFlow(0)
        private var numImages: MutableStateFlow<Int> = MutableStateFlow(0)
        private var numVideos: MutableStateFlow<Int> = MutableStateFlow(0)
        private var numScripts: MutableStateFlow<Int> = MutableStateFlow(0)
        private var numDocuments: MutableStateFlow<Int> = MutableStateFlow(0)
        private var description: MutableStateFlow<String> = MutableStateFlow("")
        private var displayImage: MutableStateFlow<Uri?> = MutableStateFlow(null)

        fun init(finished: (() -> Unit)? = null): Job {
            return repositoryScope.launch {
                /*withContext(MainActivity.IO) {
                    val contentList: List<Content> = getContentListNow(id)
                    val builder = StringBuilder("")
                    contentList.forEach { content ->
                        when (content.type) {
                            ContentType.TEXT -> {
                                if (content.content.text.isNotEmpty()) builder.append(content.content.text.toString())
                            }

                            ContentType.IMAGE -> {
                                withContext(MainActivity.Main) { numImages.value += 1 }
                                if (content.description.text.isNotEmpty()) builder.append(content.description.text.toString())
                                if (displayImage.value == null) {
                                    val file = File(
                                        application.filesDir.toString() + "/" + AppResources.ImageResource.DESTINATION_FOLDER,
                                        content.content.text.toString()
                                    )
                                    withContext(MainActivity.Main) {
                                        displayImage.value = if (file.exists()) {
                                            withContext(MainActivity.IO) { Uri.fromFile(file) }
                                        } else {
                                            null
                                        }
                                    }
                                }
                            }

                            ContentType.AUDIO -> {
                                withContext(MainActivity.Main) { numAudios.value += 1 }
                                if (content.description.text.isNotEmpty()) builder.append(content.description.text.toString())
                            }

                            ContentType.VIDEO -> {
                                withContext(MainActivity.Main) { numVideos.value += 1 }
                                if (content.description.text.isNotEmpty()) builder.append(content.description.text.toString())
                            }

                            ContentType.DOCUMENT -> {
                                withContext(MainActivity.Main) { numDocuments.value += 1 }
                                if (content.description.text.isNotEmpty()) builder.append(content.description.text.toString())
                            }

                            ContentType.SCRIPT -> {
                                val scriptContentPreview =
                                    content.id?.let { it1 -> ContentPreview(it1) }
                                if (scriptContentPreview != null) {
                                    scriptContentPreview.init()
                                    withContext(MainActivity.Main) {
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
                                withContext(MainActivity.Main) { numScripts.value += 1 }
                            }
                        }
                    }
                    withContext(MainActivity.Main) {
                        description.value = builder.toString()
                        finished?.invoke()
                    }
                }*/
            }
        }

        fun getNumAudios(): StateFlow<Int> {
            return numAudios
        }

        fun getNumImages(): StateFlow<Int> {
            return numImages
        }

        fun getNumVideos(): StateFlow<Int> {
            return numVideos
        }

        fun getNumScripts(): StateFlow<Int> {
            return numScripts
        }

        fun getNumDocuments(): StateFlow<Int> {
            return numDocuments
        }

        fun getDescription(): StateFlow<String> {
            return description
        }

        fun getDisplayImage(context: Context): Flow<ImageBitmap?> {
            return displayImage.mapLatest { scriptUri ->
                scriptUri?.let { AppResources.getBitmapFromUri(context, it)?.asImageBitmap() }
            }.flowOn(MainActivity.IO)
        }
    }

    inner class ContentPreview(
        val id: Long
    ) {
        private var numAudios: MutableStateFlow<Int> = MutableStateFlow(0)
        private var numImages: MutableStateFlow<Int> = MutableStateFlow(0)
        private var numVideos: MutableStateFlow<Int> = MutableStateFlow(0)
        private var numScripts: MutableStateFlow<Int> = MutableStateFlow(0)
        private var numDocuments: MutableStateFlow<Int> = MutableStateFlow(0)
        private var description: MutableStateFlow<String> = MutableStateFlow("")
        private var displayImage: MutableStateFlow<Uri?> = MutableStateFlow(null)

        fun init(finished: (() -> Unit)? = null): Job {
            return repositoryScope.launch {
                withContext(MainActivity.IO) {
                    val contentList: List<Content> = getContentListNow(id)
                    val builder = StringBuilder("")
                    contentList.forEach { content ->
                        when (content.type) {
                            ContentType.TEXT -> {
                                if (content.content.text.isNotEmpty()) builder.append(content.content.text.toString())
                            }

                            ContentType.IMAGE -> {
                                withContext(MainActivity.Main) { numImages.value += 1 }
                                if (content.description.text.isNotEmpty()) builder.append(content.description.text.toString())
                                if (displayImage.value == null) {
                                    val file = File(
                                        application.filesDir.toString() + "/" + AppResources.ImageResource.DESTINATION_FOLDER,
                                        content.content.text.toString()
                                    )
                                    withContext(MainActivity.Main) {
                                        displayImage.value = if (file.exists()) {
                                            withContext(MainActivity.IO) { Uri.fromFile(file) }
                                        } else {
                                            null
                                        }
                                    }
                                }
                            }

                            ContentType.AUDIO -> {
                                withContext(MainActivity.Main) { numAudios.value += 1 }
                                if (content.description.text.isNotEmpty()) builder.append(content.description.text.toString())
                            }

                            ContentType.VIDEO -> {
                                withContext(MainActivity.Main) { numVideos.value += 1 }
                                if (content.description.text.isNotEmpty()) builder.append(content.description.text.toString())
                            }

                            ContentType.DOCUMENT -> {
                                withContext(MainActivity.Main) { numDocuments.value += 1 }
                                if (content.description.text.isNotEmpty()) builder.append(content.description.text.toString())
                            }

                            ContentType.SCRIPT -> {
                                val scriptContentPreview =
                                    content.id?.let { it1 -> ContentPreview(it1) }
                                if (scriptContentPreview != null) {
                                    scriptContentPreview.init()
                                    withContext(MainActivity.Main) {
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
                                withContext(MainActivity.Main) { numScripts.value += 1 }
                            }
                        }
                    }
                    withContext(MainActivity.Main) {
                        description.value = builder.toString()
                        finished?.invoke()
                    }
                }
            }
        }

        fun getNumAudios(): StateFlow<Int> {
            return numAudios
        }

        fun getNumImages(): StateFlow<Int> {
            return numImages
        }

        fun getNumVideos(): StateFlow<Int> {
            return numVideos
        }

        fun getNumScripts(): StateFlow<Int> {
            return numScripts
        }

        fun getNumDocuments(): StateFlow<Int> {
            return numDocuments
        }

        fun getDescription(): StateFlow<String> {
            return description
        }

        fun getDisplayImage(): StateFlow<Uri?> {
            return displayImage
        }
    }

    suspend fun getFolderFolderNum(
        folder: Folder
    ) {
        withContext(MainActivity.IO) {
            dao.getFolders(folder.folderId, folder.folderType).first().let { folders ->
                folder.numFolders.update { folders.size }
            }

        }
    }

    suspend fun getFolderScriptGoalNum(
        folder: Folder
    ) {
        withContext(MainActivity.IO) {
            when (folder.folderType) {
                Folder.FolderType.SCRIPTS -> {
                    dao.getScripts(folder.folderId).first().let { scripts ->
                        folder.numScripts.update { scripts.size }
                    }
                }

                Folder.FolderType.GOALS -> {
                    getGoals(folder.folderId).first().let { goals ->
                        folder.numGoals.update { goals.size }
                    }
                }
            }
        }
    }

    fun searchFragmentSearch(
        context: Context,
        searchComplete: (() -> Unit)? = null
    ) {
        searchJob.value?.cancel()
        searchScriptsList.clear()
        searchJob.value = repositoryScope.launch {
            if (searchString.text.isEmpty()) {
                searchJob.value?.cancel()
                searchJob.value = null
                return@launch
            }
            withContext(MainActivity.IO) {
                val title: String
                val id: Long = if (folder.value != null) {
                    title = folder.value!!.folderName.text.toString()
                    folder.value!!.folderId ?: return@withContext
                } else {
                    title = application.getString(R.string.books)
                    appSettings.value?.appSettingId ?: return@withContext
                }
                dao.searchFolderScripts(
                    context,
                    id,
                    title,
                    searchString.text.toString().trim(),
                    matchCaseCheck.value,
                    wordCheck.value,
                    searchScriptsList,
                    searchOccurrences,
                    searchNumScripts
                ) {
                    searchJob.value = null
                    searchComplete?.invoke()
                }
            }
        }
    }

    suspend fun setIsFromSearchFolderToTrue(appendedUnit: (suspend () -> Unit)? = null) {
        isFromSearchFolder = true
        appendedUnit?.invoke()
    }

    fun isFromSearchFragment(): Boolean {
        val result = isFromSearchFolder
        isFromSearchFolder = false
        return result
    }

    fun resetSearchData(appendedUnit: (() -> Unit)? = null) {
        searchMenuOpen.value = true
        searchScrollPosition.requestScrollToItem(0, 0)
        searchString.clearText()
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

    fun toggleShowScripts(){
        showScripts.value?.value = showScripts.value?.value?.not() == true
    }

    fun toggleFolderOrder(){
        folderOrder.value?.value = folderOrder.value?.value?.not() == true
    }

    suspend fun loadGoals() {
        withContext(MainActivity.IO) {
            changeFragment(AccountableFragment.GoalsFragment)
        }
    }

    suspend fun loadEditGoal(id: Long? = null) {
        if (id != null && id != INITIAL_FOLDER_ID) {
            // Load Existing Goal
            val tempEditGoal = MutableStateFlow<Goal?>(
                getGoal(id).map { goal ->
                    goal?.loadGoalTimes(dao)
                    goal?.loadTasks(dao)
                    goal?.loadMarkers(dao)
                    goal?.loadDeliverables(dao)
                    goal
                }.first()
                    ?: return
            )
            val tempNewGoal = MutableStateFlow<Goal?>(
                Goal(
                    parent = tempEditGoal.value!!.parent
                )
            )
            cloneGoalTo(tempEditGoal.value, tempNewGoal, true)
            tempNewGoal.value?.let { newGoal ->
                dao.update(newGoal)
            }
            editGoal.value = tempEditGoal.value
            newGoal.value = getGoal(tempNewGoal.value?.id)
        } else {
            // Make New Goal
            editGoal.value = null
            newGoal.update {
                val tempGoal = Goal(
                    parent = folder.value?.folderId ?: INITIAL_FOLDER_ID,
                    position = getGoals(
                        folder.value?.folderId ?: INITIAL_FOLDER_ID
                    ).first().size.toLong()
                )
                tempGoal.id = dao.insert(tempGoal)
                getGoal(tempGoal.id)
            }
        }
        changeFragment(AccountableFragment.EditGoalFragment)
    }

    suspend fun setNewGoalImage(imageUri: Uri? = null) {
        newGoal.value?.first()?.let { newGoal ->
            deleteNewGoalImage()
            if (newGoal.id == null) newGoal.id = dao.insert(newGoal)
            if (imageUri != null) newGoal.saveImage(application, imageUri)
            dao.update(newGoal)
        }
    }

    suspend fun deleteNewGoalImage() {
        newGoal.value?.first()?.let { newGoal ->
            newGoal.deleteFile(application)
            dao.update(newGoal)
        }
    }

    suspend fun addNewGoalTimeBlock() {
        newGoal.value?.first()?.let { newGoal ->
            if (newGoal.id == null) newGoal.id = dao.insert(newGoal)

            newGoal.id?.let {
                val newTime = GoalTaskDeliverableTime(
                    parent = it,
                    type = GoalTaskDeliverableTime.TimesType.GOAL.name
                )
                newTime.id = dao.upsert(newTime)
            }
        }
    }

    suspend fun deleteNewGoalTimeBlock(timeBlock: GoalTaskDeliverableTime) {
        dao.delete(timeBlock)
    }

    suspend fun saveNewGoal(process: suspend () -> Unit) {
        newGoal.value?.first()?.let { newGoal ->
            if (editGoal.value == null) {
                editGoal.value = Goal(parent = newGoal.parent)
            }
            if (editGoal.value!!.id == null) {
                editGoal.value!!.id = dao.insert(editGoal.value!!)
            }
            cloneGoalTo(newGoal, editGoal, false)
            editGoal.value?.let { editGoal ->
                dao.update(editGoal)
                withContext(MainActivity.Main) {
                    process()
                }
            }
        }
    }

    suspend fun cloneGoalTo(
        from: Goal?,
        to: MutableStateFlow<Goal?>,
        setCloneId: Boolean
    ) {
        from?.let { from ->
            to.value?.let { to ->
                to.parent = from.parent
                to.goalCategory = from.goalCategory
                to.initialDateTime = from.initialDateTime
                to.position = from.position
                to.scrollPosition = from.scrollPosition
                to.size = from.size
                to.numImages = from.numImages
                to.numVideos = from.numVideos
                to.numAudios = from.numAudios
                to.numDocuments = from.numDocuments
                to.numScripts = from.numScripts
                to.goal = from.goal
                to.dateOfCompletion = from.dateOfCompletion
                to.endDateTime = from.endDateTime
                to.endType = from.endType
                to.status = from.status
                to.colour = from.colour
                to.location = from.location
                to.selectedTab = from.selectedTab
                to.tabListState = from.tabListState

                if (setCloneId) to.cloneId = from.id

                if (to.id == null) to.id = saveGoal(to)

                to.saveImage(
                    application,
                    from.imageResource.getUriFromStorage(application)
                )

                cloneTimesTo(
                    to.id,
                    from.times,
                    to.times,
                    GoalTaskDeliverableTime.TimesType.GOAL,
                    setCloneId
                )

                from.goalDeliverables.first().let { fromDeliverables ->
                    to.goalDeliverables.first().forEach { toDeliverable ->
                        // Delete the ones that are not in from (originally in to)
                        if (
                            !fromDeliverables.any { fromDeliverable ->
                                toDeliverable.id == fromDeliverable.cloneId
                            }
                        ) {
                            dao.delete(toDeliverable)
                        }
                    }

                    for (deliverable in fromDeliverables) {
                        to.id?.let { id ->
                            var newDeliverable = to.goalDeliverables.first()
                                .find { toDeliverable -> toDeliverable.id == deliverable.cloneId }
                            if (newDeliverable == null) {
                                newDeliverable = Deliverable(
                                    parent = id,
                                    position = deliverable.position
                                )
                                newDeliverable.id = saveDeliverable(newDeliverable)
                            } else {
                                newDeliverable.parent = id
                                newDeliverable.position = deliverable.position
                            }

                            val fromMutable: MutableStateFlow<Flow<Deliverable?>?> = MutableStateFlow(getDeliverable(deliverable.id))
                            val toMutable: MutableStateFlow<Flow<Deliverable?>?> = MutableStateFlow(getDeliverable(newDeliverable.id))
                            cloneDeliverableTo(
                                fromMutable,
                                toMutable,
                                setCloneId
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun cloneTimesTo(
        toId: Long?,
        fromTimes : Flow<List<GoalTaskDeliverableTime>>,
        toTimes : Flow<List<GoalTaskDeliverableTime>>,
        timesType : GoalTaskDeliverableTime.TimesType,
        setCloneId: Boolean
    ) {
        fromTimes.first().let { fromTimes ->
            toTimes.first().forEach { toTime ->
                // Delete the ones that are not in from (originally in to)
                if (
                    !fromTimes.any { fromTime ->
                        toTime.id == fromTime.cloneId
                    }
                ) {
                    dao.delete(toTime)
                }
            }

            for (time in fromTimes) {
                toId?.let { id ->
                    var newTime = toTimes.first()
                        .find { toTime -> toTime.id == time.cloneId }
                    if (newTime == null) {
                        newTime = GoalTaskDeliverableTime(
                            parent = id,
                            type = timesType.name
                        )
                    } else {
                        newTime.parent = id
                        newTime.type = timesType.name
                    }

                    newTime.timeBlockType = time.timeBlockType
                    newTime.start = time.start
                    newTime.duration = time.duration
                    if (setCloneId) {
                        newTime.cloneId = time.id
                    }
                    newTime.id = saveGoalTaskDeliverableTime(newTime)
                }
            }
        }
    }

    suspend fun clearNewGoal() {
        withContext(MainActivity.IO) {
            newGoal.value?.first()?.let {
                it.deleteFile(application)
                it.times.let { flowList -> for (time in flowList.first()) dao.delete(time) }
                dao.delete(it)
                newGoal.value = null
            }
            editGoal.value = null
        }
    }

    suspend fun goBackToGoalsFromEditGoal() {
        withContext(MainActivity.IO) {
            loadFolder()
            changeFragment(AccountableFragment.GoalsFragment)
        }
    }

    suspend fun goBackToGoalsFromTasks() {
        withContext(MainActivity.IO) {
            loadFolder()
            changeFragment(AccountableFragment.GoalsFragment)
        }
    }

    suspend fun cloneTaskTo(
        from: MutableStateFlow<Flow<Task?>?>,
        to: MutableStateFlow<Flow<Task?>?>,
        setCloneId: Boolean
    ) {
        to.value?.first()?.let { to ->
            from.value?.first()?.let { from ->
                to.parent = from.parent
                to.parentType = from.parentType
                to.position = from.position
                to.initialDateTime = from.initialDateTime
                to.endDateTime = from.endDateTime
                to.endType = from.endType
                to.scrollPosition = from.scrollPosition
                to.task = from.task
                to.type = from.type
                to.quantity = from.quantity
                to.time = from.time
                to.status = from.status
                to.colour = from.colour
                to.location = from.location
                to.size = from.size
                to.numImages = from.numImages
                to.numVideos = from.numVideos
                to.numAudios = from.numAudios
                to.numDocuments = from.numDocuments
                to.numScripts = from.numScripts

                if (setCloneId) to.cloneId = from.id

                if (to.id == null) to.id = saveTask(to)
                cloneTimesTo(
                    to.id,
                    from.times,
                    to.times,
                    GoalTaskDeliverableTime.TimesType.TASK,
                    setCloneId
                )
            }
        }
    }

    suspend fun getTaskClone(taskFlow: Flow<Task?>, setCloneId: Boolean): Flow<Task?>? {
        taskFlow.first()?.let { task ->
            val mutableOriginal: MutableStateFlow<Flow<Task?>?> = MutableStateFlow(taskFlow)
            val mutableReturn: MutableStateFlow<Flow<Task?>?> = MutableStateFlow(
                getTask(
                    insert(
                        Task(
                            parent = task.parent,
                            parentType = task.parentType,
                            position = task.position,
                            type = task.type
                        )
                    )
                )
            )
            cloneTaskTo(mutableOriginal, mutableReturn, setCloneId)
            return mutableReturn.value
        }
        return null
    }

    suspend fun cloneDeliverableTo(
        from: MutableStateFlow<Flow<Deliverable?>?>,
        to: MutableStateFlow<Flow<Deliverable?>?>,
        setCloneId:Boolean
    ) {
        from.value?.first()?.let { from ->
            to.value?.first()?.let { to ->
                to.parent = from.parent
                to.position = from.position
                to.initialDateTime = from.initialDateTime
                to.endDateTime = from.endDateTime
                to.endType = from.endType
                to.scrollPosition = from.scrollPosition
                to.deliverable = from.deliverable
                to.status = from.status
                to.location = from.location
                to.size = from.size
                to.numImages = from.numImages
                to.numVideos = from.numVideos
                to.numAudios = from.numAudios
                to.numDocuments = from.numDocuments
                to.numScripts = from.numScripts

                if (setCloneId) to.cloneId = from.id

                if (to.id == null) to.id = saveDeliverable(to)
                to.goalId = if (from.goalId!=null) to.id else null

                cloneTimesTo(
                    to.id,
                    from.times,
                    to.times,
                    GoalTaskDeliverableTime.TimesType.DELIVERABLE,
                    setCloneId
                )

                saveDeliverable(to)
            }
        }
    }

    suspend fun getDeliverableClone(deliverableFlow: Flow<Deliverable?>, setCloneId: Boolean): Flow<Deliverable?>? {
        deliverableFlow.first()?.let { deliverable ->
            val mutableOriginal: MutableStateFlow<Flow<Deliverable?>?> =
                MutableStateFlow(deliverableFlow)
            val mutableReturn: MutableStateFlow<Flow<Deliverable?>?> = MutableStateFlow(
                getDeliverable(
                    insert(
                        Deliverable(
                            parent = deliverable.parent,
                            position = deliverable.position
                        )
                    )
                )
            )
            cloneDeliverableTo(mutableOriginal, mutableReturn, setCloneId)
            return mutableReturn.value
        }
        return null
    }

    suspend fun cloneMarkerTo(
        from: MutableStateFlow<Flow<Marker?>?>,
        to: MutableStateFlow<Flow<Marker?>?>,
        setCloneId: Boolean
    ) {
        from.value?.first()?.let { from ->
            to.value?.first()?.let { to ->
                to.parent = from.parent
                to.position = from.position
                to.dateTime = from.dateTime
                to.scrollPosition = from.scrollPosition
                to.marker = from.marker

                if (setCloneId) to.cloneId = from.id

                if (to.id == null) to.id = saveMarker(to)
            }
        }
    }

    suspend fun getMarkerClone(markerFlow: Flow<Marker?>, setCloneId: Boolean): Flow<Marker?>? {
        markerFlow.first()?.let { marker ->
            val mutableOriginal: MutableStateFlow<Flow<Marker?>?> = MutableStateFlow(markerFlow)
            val mutableReturn: MutableStateFlow<Flow<Marker?>?> = MutableStateFlow(
                getMarker(
                    insert(
                        Marker(
                            parent = marker.parent,
                            position = marker.position
                        )
                    )
                )
            )
            cloneMarkerTo(mutableOriginal, mutableReturn, setCloneId)
            return mutableReturn.value
        }
        return null
    }

    suspend fun saveGoal(goal: Goal): Long {
        return dao.upsert(goal)
    }

    suspend fun saveTask(task: Task): Long {
        return dao.upsert(task)
    }

    suspend fun saveDeliverable(deliverable: Deliverable): Long {
        return dao.upsert(deliverable)
    }

    suspend fun saveMarker(marker: Marker): Long {
        return dao.upsert(marker)
    }

    suspend fun saveGoalTaskDeliverableTime(goalTaskDeliverableTime: GoalTaskDeliverableTime): Long {
        return dao.upsert(goalTaskDeliverableTime)
    }

    suspend fun insert(goal: Goal): Long = dao.insert(goal)
    suspend fun insert(task: Task): Long = dao.insert(task)
    suspend fun insert(marker: Marker): Long = dao.insert(marker)
    suspend fun insert(deliverable: Deliverable): Long = dao.insert(deliverable)
    suspend fun update(goal: Goal) = dao.update(goal)
    suspend fun update(task: Task) = dao.update(task)
    suspend fun update(marker: Marker) = dao.update(marker)
    suspend fun update(timeBlock: GoalTaskDeliverableTime) = dao.update(timeBlock)
    suspend fun update(deliverable: Deliverable) = dao.update(deliverable)

    suspend fun delete(deliverable: Deliverable) = dao.delete(deliverable)

    suspend fun deleteTask(task: Task) {
        dao.delete(task)
    }

    suspend fun deleteDeliverable(deliverable: Deliverable) {
        dao.delete(deliverable)
    }

    suspend fun deleteMarker(marker: Marker) {
        dao.delete(marker)
    }

    suspend fun deleteGoalTaskDeliverableTime(goalTaskDeliverableTime: GoalTaskDeliverableTime) {
        dao.delete(goalTaskDeliverableTime)
    }
    fun getGoal(goalId: Long?): Flow<Goal?> = dao.getGoal(goalId).map { goal ->
        goal?.loadGoalTimes(dao)
        goal?.loadTasks(dao)
        goal?.loadMarkers(dao)
        goal?.loadDeliverables(dao)
        goal
    }

    fun getTask(taskId: Long?): Flow<Task?> = dao.getTask(taskId).map { task ->
        task?.loadTimes(dao)
        task
    }

    fun getMarker(markerId: Long?): Flow<Marker?> = dao.getMarker(markerId)
    fun getGoals(parent: Long?): Flow<List<Goal>> = dao.getGoals(parent).map { goals ->
        goals.forEach { goal ->
            goal.loadGoalTimes(dao)
            goal.loadTasks(dao)
            goal.loadMarkers(dao)
            goal.loadDeliverables(dao)
        }
        goals
    }

    fun getGoalsDESC(parent: Long?): Flow<List<Goal>> = dao.getGoalsDESC(parent).map { goals ->
        goals.forEach { goal ->
            goal.loadGoalTimes(dao)
            goal.loadTasks(dao)
            goal.loadMarkers(dao)
            goal.loadDeliverables(dao)
        }
        goals
    }

    fun getDeliverable(deliverableId: Long?): Flow<Deliverable?> =
        dao.getDeliverable(deliverableId).map { deliverable ->
            deliverable?.loadTimes(dao)
            deliverable
        }

    fun getGoalDeliverables(goalId: Long?): Flow<List<Deliverable>> =
        dao.getGoalDeliverables(goalId).map { deliverables ->
            deliverables.forEach { deliverable -> deliverable.loadTimes(dao) }
            deliverables
        }

    fun getTasks(parentId: Long, parentType: Task.TaskParentType): Flow<List<Task>> {
        return dao.getTasks(parentId, parentType).map { tasks ->
            tasks.forEach { task ->
                task.loadTimes(dao)
            }
            tasks
        }
    }

    fun getDeliverables(parentId: Long): Flow<List<Deliverable>> {
        return dao.getDeliverables(parentId).map { deliverables ->
            deliverables.forEach { deliverable ->
                deliverable.loadTimes(dao)
            }
            deliverables
        }
    }

    fun getMarkers(parentId: Long): Flow<List<Marker>> {
        return dao.getMarkers(parentId)
    }
}