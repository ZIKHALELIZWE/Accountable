package com.thando.accountable.database.tables

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.util.packInts
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivity
import com.thando.accountable.R
import com.thando.accountable.database.Converters
import com.thando.accountable.database.dataaccessobjects.RepositoryDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import java.time.LocalDateTime

@Entity(tableName = "goal_table")
data class Goal(
    @PrimaryKey (autoGenerate = true)
    var id: Long? = null,

    @ColumnInfo (name = "goal_parent")
    var parent: Long,

    @ColumnInfo (name = "goal_date_time")
    var initialDateTime: Long = Converters().fromLocalDateTime(LocalDateTime.now()),

    @ColumnInfo (name = "goal_position")
    var position: Long = 0L,

    @ColumnInfo (name = "goal_scroll_position")
    var scrollPosition: Long = packInts(0,0),

    @ColumnInfo (name = "goal_size")
    var size: Float = 0F,

    @ColumnInfo (name = "goal_num_images")
    var numImages: Int = 0,

    @ColumnInfo (name = "goal_num_videos")
    var numVideos: Int = 0,

    @ColumnInfo (name = "goal_num_audios")
    var numAudios: Int = 0,

    @ColumnInfo (name = "goal_num_documents")
    var numDocuments: Int = 0,

    @ColumnInfo (name = "goal_num_scripts")
    var numScripts: Int = 0,

    @ColumnInfo (name = "goal_goal")
    var goal : String = "",

    @ColumnInfo (name = "goal_date_of_completion")
    var dateOfCompletion : Long = Converters().fromLocalDateTime(LocalDateTime.now()),

    @ColumnInfo (name = "goal_end_date")
    var endDateTime: Long = Converters().fromLocalDateTime(LocalDateTime.now()),

    @ColumnInfo (name = "goal_end_type")
    var endType: String = GoalEndType.UNDEFINED.name,

    @ColumnInfo (name = "goal_picture")
    private var goalPicture : String? = null,

    @ColumnInfo (name = "goal_status")
    var status : String = Status.PENDING.name,

    @ColumnInfo (name = "goal_colour") // -1 For no colour selected
    var colour: Int = -1,

    @ColumnInfo (name = "goal_location")
    var location: String = "",

    @ColumnInfo (name = "goal_selected_tab")
    var selectedTab: String = GoalTab.TASKS.name,

    @ColumnInfo (name = "goal_tab_list_state")
    var tabListState: Long = packInts(0,0),

    @ColumnInfo (name = "goal_clone_id")
    var cloneId: Long? = null

) {
    companion object {
        @Ignore
        private const val GOAL_IMAGE_PREFIX = "Goal_"
    }

    enum class GoalEndType {
        UNDEFINED, DATE, DELIVERABLE
    }

    enum class GoalTab(val stringRes:Int, val addStringRes:Int) {
        TASKS(R.string.tasks, R.string.add_task),
        DELIVERABLES(R.string.deliverables, R.string.add_deliverable),
        MARKERS(R.string.markers, R.string.add_marker)
    }

    enum class Status{
        OVERDUE, PENDING, PAUSED, BREAK, COMPLETED, FAILED, FUTURE
    }

    enum class TimeBlockType(val stringRes:Int){
        ONCE(R.string.once),
        DAILY(R.string.daily),
        WEEKLY(R.string.weekly),
        MONTHLY(R.string.monthly),
        YEARLY(R.string.yearly)
    }

    @Ignore
    private val timesState: MutableStateFlow<Flow<List<GoalTaskDeliverableTime>>?> = MutableStateFlow(null)
    @OptIn(ExperimentalCoroutinesApi::class)
    @Ignore
    val times = timesState.flatMapLatest {
            it ?: flowOf(emptyList())
    }.flowOn(MainActivity.IO)

    @Ignore
    private val goalDeliverablesState: MutableStateFlow<Flow<List<Deliverable>>?> = MutableStateFlow(null)
    @OptIn(ExperimentalCoroutinesApi::class)
    @Ignore
    val goalDeliverables = goalDeliverablesState.flatMapLatest {
            it ?: flowOf(emptyList())
    }.flowOn(MainActivity.IO)

    @Ignore
    private val goalTasksState: MutableStateFlow<Flow<List<Task>>?> = MutableStateFlow(null)
    @OptIn(ExperimentalCoroutinesApi::class)
    @Ignore
    val goalTasks = goalTasksState.flatMapLatest { tasksListFlow ->
            tasksListFlow?:MutableStateFlow(emptyList())
    }.flowOn(MainActivity.IO)

    @Ignore
    private val goalMarkersState: MutableStateFlow<Flow<List<Marker>>?> = MutableStateFlow(null)
    @OptIn(ExperimentalCoroutinesApi::class)
    @Ignore
    val goalMarkers = goalMarkersState.flatMapLatest { markersListFlow ->
            markersListFlow?:MutableStateFlow(emptyList())
    }.flowOn(MainActivity.IO)

    @Ignore
    private val selectedGoalDeliverablesState: MutableStateFlow<Flow<List<Deliverable>>?> = MutableStateFlow(null)
    @OptIn(ExperimentalCoroutinesApi::class)
    @Ignore
    val selectedGoalDeliverables = selectedGoalDeliverablesState.flatMapLatest {
            it ?: flowOf(emptyList())
    }.flowOn(MainActivity.IO)

    @Ignore
    private val notSelectedGoalDeliverablesState: MutableStateFlow<Flow<List<Deliverable>>?> = MutableStateFlow(null)
    @OptIn(ExperimentalCoroutinesApi::class)
    @Ignore
    val notSelectedGoalDeliverables = notSelectedGoalDeliverablesState.flatMapLatest {
            it ?: flowOf(emptyList())
    }.flowOn(MainActivity.IO)

    @Ignore
    val imageResource = AppResources.ImageResource(goalPicture?:"")

    fun loadGoalTimes(dao: RepositoryDao){
        timesState.value = dao.getTimes(id, GoalTaskDeliverableTime.TimesType.GOAL)
    }

    fun loadDeliverables(dao: RepositoryDao) {
        selectedGoalDeliverablesState.value = dao.getGoalDeliverables(id)
        notSelectedGoalDeliverablesState.value = dao.getNotGoalDeliverables(id)
        goalDeliverablesState.value = dao.getDeliverables(id)
    }

    fun loadTasks(dao: RepositoryDao) {
        goalTasksState.value = dao.getTasks(id, Task.TaskParentType.GOAL).map { tasks ->
            tasks.forEach { task ->
                task.loadTimes(dao)
                task.loadDeliverable(dao)
            }
            tasks
        }
    }

    fun loadMarkers(dao: RepositoryDao) {
        goalMarkersState.value = dao.getMarkers(id)
    }

    fun getGoalPicture(): String? { return goalPicture }

    fun saveImage(context: Context, inputUri: Uri?){
        goalPicture = imageResource.saveFile(context, inputUri, GOAL_IMAGE_PREFIX, id)
    }

    fun deleteFile(context: Context) {
        if (imageResource.deleteFile(context)) {
            goalPicture = null
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getImageBitmap(context: Context): Flow<ImageBitmap?> = imageResource.getUri(context).mapLatest { scriptUri ->
        scriptUri?.let {
            AppResources.getBitmapFromUri(context, it)?.asImageBitmap()
        }
    }.flowOn(MainActivity.IO)
}