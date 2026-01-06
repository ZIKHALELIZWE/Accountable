package com.thando.accountable.database.dataaccessobjects

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.thando.accountable.AppResources
import com.thando.accountable.database.tables.AppSettings
import com.thando.accountable.database.tables.Content
import com.thando.accountable.database.tables.Folder
import com.thando.accountable.database.tables.Goal
import com.thando.accountable.database.tables.GoalTaskDeliverableTime
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.database.tables.Script
import com.thando.accountable.database.tables.SpecialCharacters
import com.thando.accountable.database.tables.TeleprompterSettings
import com.thando.accountable.fragments.viewmodels.SearchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

@Dao
interface RepositoryDao {

    @Insert
    suspend fun insert(folder: Folder): Long

    @Insert
    suspend fun insert(goal: Goal): Long

    @Insert
    suspend fun insert(script: Script): Long

    @Insert
    suspend fun insert(appSettings: AppSettings): Long

    @Insert
    suspend fun insert(content: Content): Long

    @Insert
    suspend fun insert(markupLanguage: MarkupLanguage): Long

    @Insert
    suspend fun insert(teleprompterSettings: TeleprompterSettings): Long

    @Insert
    suspend fun insert(specialCharacters: SpecialCharacters): Long

    @Insert
    suspend fun insert(goalTaskDeliverableTime: GoalTaskDeliverableTime): Long

    @Update
    suspend fun update(folder: Folder)

    @Update
    suspend fun update(goal: Goal)

    @Update
    suspend fun update(script: Script)

    @Update
    suspend fun update(appSettings: AppSettings)

    @Update
    suspend fun update(content: Content)

    @Update
    suspend fun update(markupLanguage: MarkupLanguage)

    @Update
    suspend fun update(teleprompterSettings: TeleprompterSettings)

    @Update
    suspend fun update(specialCharacters: SpecialCharacters)

    @Update
    suspend fun update(goalTaskDeliverableTime: GoalTaskDeliverableTime)

    @Delete
    suspend fun delete(folder: Folder)

    @Delete
    suspend fun delete(goal: Goal)

    @Delete
    suspend fun delete(script: Script)

    @Delete
    suspend fun delete(content: Content)

    @Delete
    suspend fun delete(markupLanguage: MarkupLanguage)

    @Delete
    suspend fun delete(specialCharacters: SpecialCharacters)

    @Delete
    suspend fun delete(teleprompterSettings: TeleprompterSettings)

    @Delete
    suspend fun delete(goalTaskDeliverableTime: GoalTaskDeliverableTime)

    @Transaction
    suspend fun deleteGoal(goalId: Long?, context: Context){
        val goal = getGoalNow(goalId) ?: return
        /* todo this is where you delete whatever is inside goal

        val contentList = getContentListNow(script.scriptId)
        contentList?.forEach {
            deleteContent(it,context)
        }*/
        goal.deleteFile(context)
        // Fix goal Positions
        /*val goals = getGoalsNow(goal.parent)
        var passedGoal = false
        goals.forEach {
            if (!passedGoal){
                if (it.id == goal.id) passedGoal = true
            } else{
                it.position -= 1
                update(it)
            }
        }*/
        delete(goal)
    }

    @Transaction
    suspend fun deleteFolder(folderId: Long?, context: Context){
        val folder = getFolderNow(folderId) ?: return
        when(folder.folderType){
            Folder.FolderType.SCRIPTS -> {
                val scripts = getScriptsNow(folder.folderId)
                scripts.forEach { deleteScript(it.scriptId, context) }
            }
            Folder.FolderType.GOALS -> {
                val goals = getGoalsNow(folder.folderId)
                goals.forEach { deleteGoal(it.id, context) }
            }
        }
        val folders = getFoldersNow(folder.folderId,folder.folderType)
        folders.forEach { deleteFolder(it.folderId, context) }

        folder.deleteFile(context)
        // Fix folder Positions
        val parentFolders = getFoldersNow(folder.folderParent,folder.folderType)
        var passedFolder = false
        parentFolders.forEach {
            if (!passedFolder) {
                if (it.folderId == folder.folderId) passedFolder = true
            } else {
                it.folderPosition -= 1
                update(it)
            }
        }
        delete(folder)
    }

    @Transaction
    suspend fun deleteScript(scriptId: Long?, context: Context){
        val script = getScriptNow(scriptId) ?: return
        val contentList = getContentListNow(script.scriptId)
        contentList.forEach {
            deleteContent(it,context)
        }
        script.deleteFile(context)
        // Fix script Positions
        when(script.scriptParentType){
            Script.ScriptParentType.SCRIPT, // Stored in content. Content will update its own positions
            Script.ScriptParentType.TASK -> {}//Order does not matter because it is not stored in a list
            Script.ScriptParentType.FOLDER -> {
                val scripts = getScriptsNow(script.scriptParent)
                var passedScript = false
                scripts.forEach {
                    if (!passedScript) {
                        if (it.scriptId == script.scriptId) passedScript = true
                    } else {
                        it.scriptPosition -= 1
                        update(it)
                    }
                }
            }
        }
        delete(script)
    }

    @Transaction
    suspend fun deleteContent(content: Content, context: Context){
        // todo Add notification for long deletes. Can take more than 5 minutes
        // Fix script Positions
        val contentList = getContentListNow(content.script)
        var passedContent = false
        contentList.forEach {
            if (!passedContent){
                if (it.id == content.id) passedContent = true
            } else{
                it.position -= 1
                update(it)
            }
        }
        when (content.type) {
            Content.ContentType.TEXT -> {
                delete(content)
            }

            Content.ContentType.IMAGE -> {
                if (content.content.value.isNotEmpty()) {
                    val image = AppResources.ImageResource(content.content.value)
                    image.deleteFile(context)
                }
                delete(content)
            }

            Content.ContentType.AUDIO -> {
                if (content.content.value.isNotEmpty()) {
                    val audio = AppResources.AudioResource(content.content.value)
                    audio.deleteFile(context)
                }
                delete(content)
            }

            Content.ContentType.VIDEO -> {
                if (content.content.value.isNotEmpty()) {
                    val video = AppResources.VideoResource(content.content.value)
                    video.deleteFile(context)
                }
                delete(content)
            }

            Content.ContentType.DOCUMENT -> {
                if (content.content.value.isNotEmpty()) {
                    val document = AppResources.DocumentResource(content.content.value)
                    document.deleteFile(context)
                }
                delete(content)
            }

            Content.ContentType.SCRIPT -> {
                if (content.content.value.isNotEmpty()) {
                    deleteScript(content.content.value.toLongOrNull(),context)
                }
                delete(content)
            }
        }
    }

    @Transaction
    suspend fun upsert(specialCharacters: SpecialCharacters) {
        val searchCharacter = specialCharacters.oldCharacter
        val existingEntity = getSpecialCharacter(specialCharacters.teleprompterSettingsId,
            searchCharacter
        )
        if (existingEntity != null)
        {
            delete(existingEntity)
        }
        insert(specialCharacters)
    }

    @Transaction
    suspend fun upsert(teleprompterSettings: TeleprompterSettings): Long {
        if (teleprompterSettings.id!=null){
            val existingEntity = getTeleprompterSettings(teleprompterSettings.id)
            if (existingEntity!=null){
                update(teleprompterSettings)
                return teleprompterSettings.id!!
            }
        }
        return insert(teleprompterSettings)
    }

    @Transaction
    suspend fun upsert(markupLanguage: MarkupLanguage){
        if (markupLanguage.name.value.isNotEmpty()){
            val existingEntity = getMarkupLanguageQuick(markupLanguage.name.value)
            if (existingEntity!=null){
                update(markupLanguage)
            }
            else insert(markupLanguage)
        }
    }

    @Transaction
    suspend fun getGoalWithTimes(goalId:Long?) = getGoalNow(goalId).apply {
        this?.times?.clear()
        this?.times?.addAll(getGoalTimes(goalId))
    }

    @Query("SELECT * FROM folder_table WHERE folderId = :folderId")
    fun getFolder(folderId:Long?): LiveData<Folder>

    @Query("SELECT * FROM folder_table WHERE folderId = :folderId")
    suspend fun getFolderNow(folderId:Long?): Folder?

    @Query("SELECT * FROM goal_table WHERE id = :goalId")
    fun getGoal(goalId: Long?): LiveData<Goal>

    @Query("SELECT * FROM goal_table WHERE id = :goalId")
    suspend fun getGoalNow(goalId: Long?): Goal?

    @Query("SELECT * FROM times_table WHERE times_goal =:goalId")
    fun getGoalTimes(goalId: Long?): MutableList<GoalTaskDeliverableTime>

    @Query("SELECT * FROM folder_table WHERE folder_parent = :parent AND folder_type = :folderType ORDER BY folder_position ASC")
    fun getFolders(parent:Long?, folderType: Folder.FolderType): LiveData<List<Folder>>

    @Query("SELECT * FROM folder_table WHERE folder_parent = :parent AND folder_type = :folderType ORDER BY folder_position ASC")
    suspend fun getFoldersNow(parent:Long?, folderType: Folder.FolderType?): List<Folder>

    @Query("SELECT * FROM folder_table WHERE folder_parent = :parent AND folder_type = :folderType ORDER BY folder_position DESC")
    suspend fun getFoldersNowDESC(parent:Long?, folderType: Folder.FolderType?): List<Folder>

    @Query("SELECT * FROM script_table WHERE script_parent = :parent ORDER BY script_position ASC")
    fun getScripts(parent:Long?): LiveData<List<Script>>

    @Query("SELECT * FROM goal_table WHERE goal_parent = :parent ORDER BY goal_position ASC")
    fun getGoals(parent: Long?): LiveData<List<Goal>>

    @Query("SELECT * FROM goal_table WHERE goal_parent = :parent ORDER BY goal_position ASC")
    suspend fun getGoalsNow(parent: Long?): List<Goal>

    @Query("SELECT * FROM goal_table WHERE goal_parent = :parent ORDER BY goal_position DESC")
    suspend fun getGoalsNowDESC(parent: Long?): List<Goal>

    @Query("SELECT * FROM content_table WHERE content_script = :scriptId ORDER BY content_position ASC")
    suspend fun getContentList(scriptId:Long?): List<Content>

    @Query("SELECT * FROM content_table WHERE content_script = :scriptId ORDER BY content_position ASC")
    suspend fun getContentListNow(scriptId:Long?): List<Content>

    @Query("SELECT * FROM app_settings_table WHERE appSettingId = 1")
    fun getAppSettings(): LiveData<AppSettings>

    @Query("SELECT * FROM app_settings_table WHERE appSettingId = 1")
    suspend fun getAppSettingsNow(): AppSettings?

    @Query("SELECT * FROM script_table WHERE scriptId = :scriptId")
    suspend fun getScriptNow(scriptId:Long?): Script?

    @Query("DELETE FROM special_characters WHERE teleprompter_settings_id = :teleprompterSettingsId")
    fun deleteSpecialCharacters(teleprompterSettingsId: Long?)

    @Query("SELECT * FROM special_characters WHERE teleprompter_settings_id = :teleprompterSettingsId AND character = :character")
    suspend fun getSpecialCharacter(teleprompterSettingsId: Long?, character: String): SpecialCharacters?

    @Query("SELECT * FROM script_table WHERE scriptId = :scriptId")
    fun getScript(scriptId:Long?): LiveData<Script>

    @Query("SELECT * FROM content_table WHERE id = :contentId")
    fun getContent(contentId:Long?): LiveData<Content>

    @Query("SELECT * FROM special_characters WHERE teleprompter_settings_id = :teleprompterSettingsId")
    suspend fun getScriptSpecialCharacters(teleprompterSettingsId:Long?): List<SpecialCharacters>

    @Query("DELETE FROM content_table WHERE id = :contentId")
    fun deleteContent(contentId: Long?)

    @Query("SELECT * FROM markup_language_table WHERE name = :markupLanguageName")
    suspend fun getMarkupLanguage(markupLanguageName:String?): MarkupLanguage?

    @Query("SELECT * FROM markup_language_table WHERE name = :markupLanguageName")
    fun getMarkupLanguageQuick(markupLanguageName:String?): MarkupLanguage?

    @Query("SELECT * FROM markup_language_table")
    suspend fun getMarkupLanguages(): List<MarkupLanguage>

    @Query("SELECT * FROM teleprompter_settings_table")
    suspend fun getTeleprompterSettings(): List<TeleprompterSettings>

    @Query("UPDATE script_table SET script_markup_language = :markupLanguageName WHERE scriptId = :scriptId")
    suspend fun setScriptMarkupLanguage( scriptId: Long?, markupLanguageName: String?)

    @Query("SELECT script_markup_language FROM script_table WHERE scriptId = :scriptId")
    fun getScriptMarkupLanguage(scriptId: Long?): LiveData<String?>

    @Query("SELECT * FROM teleprompter_settings_table WHERE id = :teleprompterSettingsId")
    fun getScriptTeleprompterSettings(teleprompterSettingsId: Long?): LiveData<TeleprompterSettings?>

    @Query("SELECT * FROM teleprompter_settings_table WHERE id = :teleprompterSettingsId")
    fun getTeleprompterSettings(teleprompterSettingsId: Long?): TeleprompterSettings?

    @Query("SELECT * FROM script_table WHERE script_parent = :parent ORDER BY script_position ASC")
    suspend fun getScriptsNow(parent:Long?): List<Script>

    @Query("SELECT * FROM script_table WHERE script_parent = :parent ORDER BY script_position DESC")
    suspend fun getScriptsNowDESC(parent:Long?): List<Script>

    @Transaction
    suspend fun appSettings(): AppSettings {
        var appSettings = getAppSettingsNow()
        if (appSettings==null){
            insert(AppSettings())
            appSettings = getAppSettingsNow()
        }
        return appSettings!!
    }

    @Transaction
    suspend fun searchFolderScripts(
        id:Long,
        title:String,
        searchString:String,
        matchCaseCheck:Boolean,
        wordCheck:Boolean,
        searchScriptsList: MutableList<SearchViewModel.ScriptSearch>,
        searchOccurrences: MutableStateFlow<Int>,
        searchNumScripts: MutableStateFlow<Int>,
        onScriptClick:(scriptId:Long)->Unit,
        appendedUnit:(()->Unit)?=null
    ){
        searchOccurrences.value = 0
        searchNumScripts.value = 0
        val scriptsList = getScriptsNow(id)
        for (script in scriptsList) {
            if (script.scriptId == null) continue
            val scriptSearch = SearchViewModel.ScriptSearch(id, title, script, onScriptClick)
            val list = searchScriptForString(
                script.scriptId!!,
                searchString,
                matchCaseCheck,
                wordCheck,
                script.scriptTitle.value
            )
            var hasContent = false
            if (list.isNotEmpty()) {
                list.forEach { withContext(Dispatchers.Main) { searchOccurrences.value += it.second.size } }
                scriptSearch.addRanges(list)
                hasContent = true
            }
            if (hasContent) {
                searchScriptsList.add(scriptSearch)
                withContext(Dispatchers.Main) { searchNumScripts.value += 1 }
            }
        }
        appendedUnit?.invoke()
    }

    private suspend fun searchScriptForString(
        id:Long,
        searchString: String,
        matchCaseCheck: Boolean,
        wordCheck: Boolean,
        title:String? = null
    ): ArrayList<Triple<String,MutableList<IntRange>,Content?>>{
        val ranges = arrayListOf<Triple<String,MutableList<IntRange>,Content?>>()
        val words = searchString.split(" ")
        if (words.size>1){
            val deferredList = arrayListOf<Deferred<ArrayList<Triple<String,MutableList<IntRange>,Content?>>>>()
            words.forEach {
                deferredList.add(CoroutineScope(coroutineContext).async{
                    val deferredArrayList = arrayListOf<Triple<String,MutableList<IntRange>,Content?>>()
                    if (title!=null){
                        val list = searchForStringInString(it,title,matchCaseCheck, wordCheck)
                        if (!list.isNullOrEmpty()) deferredArrayList.add(Triple(title, list,null))
                    }
                    deferredArrayList.addAll(searchScriptForString(id,it,matchCaseCheck,wordCheck))
                    deferredArrayList
                })
            }

            var isValid = true
            deferredList.forEach {
                val list = it.await()
                if (!isValid || (title!=null && list.size == 0)){
                    isValid = false
                    ranges.clear()
                    deferredList.forEach { dList-> dList.cancel() }
                    return@forEach
                }
                else ranges.addAll(list)
            }
        }
        else if (words.size==1) {
            val contentList = getContentListNow(id)
            val deferredList = arrayListOf<
                    Pair<
                            Deferred<Triple<String, MutableList<IntRange>, Content?>?>?,
                            Deferred<ArrayList<Triple<String, MutableList<IntRange>, Content?>>>?
                            >
                    >()
            if (title!=null) deferredList.add(Pair(
                CoroutineScope(coroutineContext).async {
                    val list = searchForStringInString(searchString, title, matchCaseCheck, wordCheck)
                    if (list.isNullOrEmpty()) null
                    else Triple(title, list, null)
                },
                null
            ))
            for (content in contentList) {
                when (content.type) {
                    Content.ContentType.TEXT -> {
                        deferredList.add(Pair(
                            CoroutineScope(coroutineContext).async{
                                val list: MutableList<IntRange>? = searchForStringInString(
                                    searchString, content.content.value, matchCaseCheck, wordCheck
                                )
                                if (list.isNullOrEmpty()) null
                                else Triple(content.content.value, list, content)
                            },
                            null
                        ))
                    }
                    Content.ContentType.IMAGE,
                    Content.ContentType.VIDEO,
                    Content.ContentType.DOCUMENT,
                    Content.ContentType.AUDIO -> {
                        deferredList.add(Pair(
                            CoroutineScope(coroutineContext).async{
                                val list: MutableList<IntRange>? = searchForStringInString(
                                    searchString, content.description.value, matchCaseCheck, wordCheck
                                )
                                if (list.isNullOrEmpty()) null
                                else Triple(content.description.value, list, content)
                            },
                            null
                        ))
                    }

                    Content.ContentType.SCRIPT -> {
                        content.content.value.toLongOrNull()?.let {
                            deferredList.add(Pair(
                                null,
                                CoroutineScope(coroutineContext).async{
                                    searchScriptForString(
                                        it,
                                        searchString,
                                        matchCaseCheck,
                                        wordCheck
                                    )
                                }
                            ))
                        }
                    }
                }
            }
            deferredList.forEach { pair ->
                if (pair.first!=null){
                    val triple = pair.first!!.await()
                    triple?.let {
                        if (triple.second.isNotEmpty()) ranges.add(triple)
                    }
                }
                else if (pair.second!=null){
                    val list = pair.second!!.await()
                    if (list.isNotEmpty()) ranges.addAll(list)
                }
            }
        }
        return ranges
    }

    private fun searchForStringInString(
        searchString: String,
        contentString: String,
        matchCase:Boolean,
        onlyWord:Boolean
    ): MutableList<IntRange>? {
        val occurrences = mutableListOf<IntRange>()
        var index = contentString.indexOf(searchString, ignoreCase = !matchCase)
        while (index >= 0) {
            val range = IntRange(index,index + searchString.length-1)
            var isWord = true
            if (onlyWord){
                isWord =
                    !((
                            contentString.getOrNull(range.first-1)!=null &&
                                    contentString.getOrNull(range.first-1)!!.isLetterOrDigit()
                            ) ||
                            (
                                    contentString.getOrNull(range.last+1)!=null &&
                                            contentString.getOrNull(range.last+1)!!.isLetterOrDigit()
                                    ))
            }
            if (isWord) occurrences.add(range)
            index = contentString.indexOf(searchString, index + 1, ignoreCase = !matchCase)
        }
        return occurrences.ifEmpty { null }
    }
}