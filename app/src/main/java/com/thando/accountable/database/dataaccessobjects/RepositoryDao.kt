package com.thando.accountable.database.dataaccessobjects

import android.content.Context
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivity
import com.thando.accountable.database.tables.AppSettings
import com.thando.accountable.database.tables.Content
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
import com.thando.accountable.fragments.viewmodels.SearchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Dao
interface RepositoryDao {

    @Insert
    suspend fun insert(folder: Folder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goalTaskDeliverableTime: GoalTaskDeliverableTime): Long

    @Insert
    suspend fun insert(task: Task): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deliverable: Deliverable): Long

    @Insert
    suspend fun insert(marker: Marker): Long

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

    @Update
    suspend fun update(task: Task)

    @Update
    suspend fun update(deliverable: Deliverable)

    @Update
    suspend fun update(marker: Marker)

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

    @Delete
    suspend fun delete(task: Task)

    @Delete
    suspend fun delete(deliverable: Deliverable)

    @Delete
    suspend fun delete(marker: Marker)

    @Transaction
    suspend fun deleteGoal(goalId: Long?, context: Context){
        val goal = getGoal(goalId).first()?:return
        goal.deleteFile(context)
        val deliverables = getDeliverables(goalId).first()
        deliverables.forEach { deliverable -> deleteDeliverable(deliverable) }
        val tasks = getTasks(goalId, Task.TaskParentType.GOAL).first()
        tasks.forEach { task -> deleteTask(task) }
        val markers = getMarkers(goalId).first()
        markers.forEach { marker -> deleteMarker(marker) }
        val times = getTimes(goalId, GoalTaskDeliverableTime.TimesType.GOAL).first()
        times.forEach { time -> delete(time) }

        // Fix goal Positions
        val goals = getGoals(goal.parent).first()
        var passedGoal = false
        goals.forEach {
            if (!passedGoal){
                if (it.id == goal.id) passedGoal = true
            } else{
                it.position -= 1
                update(it)
            }
        }
        delete(goal)
    }

    @Transaction
    suspend fun deleteDeliverable(deliverable: Deliverable){
        val times = getTimes(deliverable.id, GoalTaskDeliverableTime.TimesType.GOAL).first()
        times.forEach { time -> delete(time) }
        delete(deliverable)
    }

    @Transaction
    suspend fun deleteTask(task: Task) {
        val times = getTimes(task.id, GoalTaskDeliverableTime.TimesType.TASK).first()
        times.forEach { time -> delete(time) }
        // Fix task Positions
        val tasks = getTasks(task.parent, Task.TaskParentType.GOAL).first()
        var passedTask = false
        tasks.forEach {
            if (!passedTask){
                if (it.id == task.id) passedTask = true
            } else{
                it.position -= 1
                update(it)
            }
        }
        delete(task)
    }

    @Transaction
    suspend fun deleteMarker(marker: Marker) {
        // Fix marker Positions
        val markers = getMarkers(marker.parent).first()
        var passedMarker = false
        markers.forEach {
            if (!passedMarker){
                if (it.id == marker.id) passedMarker = true
            } else{
                it.position -= 1
                update(it)
            }
        }
        delete(marker)
    }

    @Transaction
    suspend fun deleteFolder(folderId: Long?, context: Context){
        val folder = getFolder(folderId).first() ?: return
        when(folder.folderType){
            Folder.FolderType.SCRIPTS -> {
                val scripts = getScripts(folder.folderId).first()
                scripts.forEach { deleteScript(it.scriptId, context) }
            }
            Folder.FolderType.GOALS -> {
                val goals = getGoals(folder.folderId).first()
                goals.forEach { deleteGoal(it.id, context) }
            }
        }
        val folders = getFolders(folder.folderId,folder.folderType).first()
        folders.forEach { deleteFolder(it.folderId, context) }

        folder.deleteFile(context)
        // Fix folder Positions
        val parentFolders = getFolders(folder.folderParent,folder.folderType).first()
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
                val scripts = getScripts(script.scriptParent).first()
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
                if (content.content.text.isNotEmpty()) {
                    val image = AppResources.ImageResource(content.content.text.toString())
                    image.deleteFile(context)
                }
                delete(content)
            }

            Content.ContentType.AUDIO -> {
                if (content.content.text.isNotEmpty()) {
                    val audio = AppResources.AudioResource(content.content.text.toString())
                    audio.deleteFile(context)
                }
                delete(content)
            }

            Content.ContentType.VIDEO -> {
                if (content.content.text.isNotEmpty()) {
                    val video = AppResources.VideoResource(content.content.text.toString())
                    video.deleteFile(context)
                }
                delete(content)
            }

            Content.ContentType.DOCUMENT -> {
                if (content.content.text.isNotEmpty()) {
                    val document = AppResources.DocumentResource(content.content.text.toString())
                    document.deleteFile(context)
                }
                delete(content)
            }

            Content.ContentType.SCRIPT -> {
                if (content.content.text.isNotEmpty()) {
                    deleteScript(content.content.text.toString().toLongOrNull(),context)
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
    suspend fun upsert(task: Task): Long {
        if (task.id!=null){
            val existingEntity = getTask(task.id).first()
            if (existingEntity!=null){
                update(task)
            }
            else{
                task.id = insert(task)
            }
        } else task.id = insert(task)
        return task.id!!
    }

    @Transaction
    suspend fun upsert(deliverable: Deliverable): Long {
        if (deliverable.id!=null){
            val existingEntity = getDeliverable(deliverable.id).first()
            if (existingEntity!=null){
                update(deliverable)
            }
            else{
                deliverable.id = insert(deliverable)
            }
        }
        else deliverable.id = insert(deliverable)
        return deliverable.id!!
    }

    @Transaction
    suspend fun upsert(marker: Marker): Long {
        if (marker.id!=null){
            val existingEntity = getMarker(marker.id).first()
            if (existingEntity!=null){
                update(marker)
                return marker.id!!
            }
            else{
                marker.id = null
                return insert(marker)
            }
        }
        return insert(marker)
    }

    @Transaction
    suspend fun upsert(goalTaskDeliverableTime: GoalTaskDeliverableTime): Long {
        if (goalTaskDeliverableTime.id!=null){
            val existingEntity = getGoalTaskDeliverableTime(goalTaskDeliverableTime.id).first()
            if (existingEntity!=null){
                update(goalTaskDeliverableTime)
                return goalTaskDeliverableTime.id!!
            }
            else{
                goalTaskDeliverableTime.id = null
                return insert(goalTaskDeliverableTime)
            }
        }
        return insert(goalTaskDeliverableTime)
    }

    @Transaction
    suspend fun upsert(goal: Goal): Long {
        if (goal.id!=null){
            val existingEntity = getGoal(goal.id).first()
            if (existingEntity!=null){
                update(goal)
                return goal.id!!
            }
            else{
                goal.id = null
                return insert(goal)
            }
        }
        return insert(goal)
    }

    @Query("SELECT * FROM folder_table WHERE folderId = :folderId")
    fun getFolder(folderId:Long?): Flow<Folder?>

    @Query("SELECT * FROM goal_table WHERE id = :goalId")
    fun getGoal(goalId: Long?): Flow<Goal?>

    @Query("SELECT * FROM times_table WHERE times_parent =:parentId AND times_type =:type")
    fun getTimes(parentId: Long?, type: GoalTaskDeliverableTime.TimesType): Flow<List<GoalTaskDeliverableTime>>

    @Query("SELECT * FROM folder_table WHERE folder_parent = :parent AND folder_type = :folderType ORDER BY folder_position ASC")
    fun getFolders(parent:Long?, folderType: Folder.FolderType?): Flow<List<Folder>>

    @Query("SELECT * FROM folder_table WHERE folder_parent = :parent AND folder_type = :folderType ORDER BY folder_position DESC")
    fun getFoldersDESC(parent:Long?, folderType: Folder.FolderType?): Flow<List<Folder>>

    @Query("SELECT * FROM goal_table WHERE goal_parent = :parent ORDER BY goal_position ASC")
    fun getGoals(parent: Long?): Flow<List<Goal>>

    @Query("SELECT * FROM goal_table WHERE goal_parent = :parent ORDER BY goal_position DESC")
    fun getGoalsDESC(parent: Long?): Flow<List<Goal>>

    @Query("SELECT * FROM content_table WHERE content_script = :scriptId ORDER BY content_position ASC")
    suspend fun getContentList(scriptId:Long?): List<Content>

    @Query("SELECT * FROM content_table WHERE content_script = :scriptId ORDER BY content_position ASC")
    suspend fun getContentListNow(scriptId:Long?): List<Content>

    @Query("SELECT * FROM app_settings_table WHERE appSettingId = 1")
    fun getAppSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM script_table WHERE scriptId = :scriptId")
    suspend fun getScriptNow(scriptId:Long?): Script?

    @Query("DELETE FROM special_characters WHERE teleprompter_settings_id = :teleprompterSettingsId")
    fun deleteSpecialCharacters(teleprompterSettingsId: Long?)

    @Query("SELECT * FROM special_characters WHERE teleprompter_settings_id = :teleprompterSettingsId AND character = :character")
    suspend fun getSpecialCharacter(teleprompterSettingsId: Long?, character: String): SpecialCharacters?

    @Query("SELECT * FROM special_characters WHERE teleprompter_settings_id = :teleprompterSettingsId")
    suspend fun getScriptSpecialCharacters(teleprompterSettingsId:Long?): List<SpecialCharacters>

    @Query("SELECT * FROM markup_language_table WHERE name = :markupLanguageName")
    suspend fun getMarkupLanguage(markupLanguageName:String?): MarkupLanguage?

    @Query("SELECT * FROM markup_language_table WHERE name = :markupLanguageName")
    fun getMarkupLanguageQuick(markupLanguageName:String?): MarkupLanguage?

    @Query("SELECT * FROM markup_language_table")
    suspend fun getMarkupLanguages(): List<MarkupLanguage>

    @Query("SELECT * FROM teleprompter_settings_table")
    suspend fun getTeleprompterSettings(): List<TeleprompterSettings>

    @Query("SELECT * FROM teleprompter_settings_table WHERE id = :teleprompterSettingsId")
    fun getTeleprompterSettings(teleprompterSettingsId: Long?): TeleprompterSettings?

    @Query("SELECT * FROM script_table WHERE script_parent = :parent ORDER BY script_position ASC")
    fun getScripts(parent:Long?): Flow<List<Script>>

    @Query("SELECT * FROM script_table WHERE script_parent = :parent ORDER BY script_position DESC")
    fun getScriptsDESC(parent:Long?): Flow<List<Script>>

    @Query("SELECT * FROM task_table WHERE task_parent = :parentId AND task_parent_type = :parentType")
    fun getTasks(parentId:Long?, parentType: Task.TaskParentType): Flow<List<Task>>

    @Query("SELECT * FROM deliverable_table WHERE deliverable_parent = :goalId")
    fun getDeliverables(goalId:Long?): Flow<List<Deliverable>>

    @Query("SELECT * FROM marker_table WHERE marker_parent = :goalId")
    fun getMarkers(goalId:Long?): Flow<List<Marker>>

    @Query("SELECT * FROM task_table WHERE id = :taskId")
    fun getTask(taskId: Long?): Flow<Task?>

    @Query("SELECT * FROM deliverable_table WHERE id = :deliverableId")
    fun getDeliverable(deliverableId: Long?): Flow<Deliverable?>

    @Query("SELECT * FROM deliverable_table WHERE deliverable_task_id = :taskId")
    fun getTaskDeliverable(taskId: Long?): Flow<Deliverable?>

    @Query("SELECT * FROM marker_table WHERE id = :markerId")
    fun getMarker(markerId: Long?): Flow<Marker?>

    @Query("SELECT * FROM times_table WHERE id = :goalTaskDeliverableTimeId")
    fun getGoalTaskDeliverableTime(goalTaskDeliverableTimeId: Long?): Flow<GoalTaskDeliverableTime?>

    @Query("SELECT * FROM deliverable_table WHERE deliverable_goal_id = :goalId AND deliverable_parent = :goalId")
    fun getGoalDeliverables(goalId: Long?): Flow<List<Deliverable>>

    @Query("SELECT * FROM deliverable_table WHERE deliverable_goal_id IS NULL AND deliverable_parent = :goalId")
    fun getNotGoalDeliverables(goalId: Long?): Flow<List<Deliverable>>

    @Transaction
    suspend fun appSettings(): AppSettings {
        var appSettings = getAppSettings().first()
        if (appSettings==null){
            insert(AppSettings())
            appSettings = getAppSettings().first()
        }
        return appSettings!!
    }

    @Transaction
    suspend fun searchFolderScripts(
        context: Context,
        id:Long,
        title:String,
        searchString:String,
        matchCaseCheck:Boolean,
        wordCheck:Boolean,
        searchScriptsList: SnapshotStateList<SearchViewModel.ScriptSearch>,
        searchOccurrences: MutableStateFlow<Int>,
        searchNumScripts: MutableStateFlow<Int>,
        appendedUnit:(()->Unit)?=null
    ){
        searchOccurrences.value = 0
        searchNumScripts.value = 0
        val scriptsList = getScripts(id).first()
        for (script in scriptsList) {
            if (script.scriptId == null) continue
            val scriptSearch = SearchViewModel.ScriptSearch(id, title, script)
            val list = searchScriptForString(
                script.scriptId!!,
                searchString,
                matchCaseCheck,
                wordCheck,
                script.scriptTitle.text.toString()
            )
            var hasContent = false
            if (list.isNotEmpty()) {
                list.forEach { withContext(MainActivity.Main) { searchOccurrences.value += it.second.size } }
                scriptSearch.addRanges(context,list)
                hasContent = true
            }
            if (hasContent) {
                searchScriptsList.add(scriptSearch)
                withContext(MainActivity.Main) { searchNumScripts.value += 1 }
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
                deferredList.add(CoroutineScope(currentCoroutineContext()).async{
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
                if (!isValid || (title!=null && list.isEmpty())){
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
                CoroutineScope(currentCoroutineContext()).async {
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
                            CoroutineScope(currentCoroutineContext()).async{
                                val list: MutableList<IntRange>? = searchForStringInString(
                                    searchString, content.content.text.toString(), matchCaseCheck, wordCheck
                                )
                                if (list.isNullOrEmpty()) null
                                else Triple(content.content.text.toString(), list, content)
                            },
                            null
                        ))
                    }
                    Content.ContentType.IMAGE,
                    Content.ContentType.VIDEO,
                    Content.ContentType.DOCUMENT,
                    Content.ContentType.AUDIO -> {
                        deferredList.add(Pair(
                            CoroutineScope(currentCoroutineContext()).async{
                                val list: MutableList<IntRange>? = searchForStringInString(
                                    searchString, content.description.text.toString(), matchCaseCheck, wordCheck
                                )
                                if (list.isNullOrEmpty()) null
                                else Triple(content.description.text.toString(), list, content)
                            },
                            null
                        ))
                    }

                    Content.ContentType.SCRIPT -> {
                        content.content.text.toString().toLongOrNull()?.let {
                            deferredList.add(Pair(
                                null,
                                CoroutineScope(currentCoroutineContext()).async{
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