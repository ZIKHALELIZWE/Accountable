package com.thando.accountable.database.tables

import android.content.Context
import android.net.Uri
import androidx.compose.ui.util.packInts
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivity
import com.thando.accountable.database.dataaccessobjects.RepositoryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.ZoneOffset

@Entity(tableName = "goal_table")
data class Goal(
    @PrimaryKey (autoGenerate = true)
    var id: Long? = null,

    @ColumnInfo (name = "goal_parent")
    var parent: Long,

    @ColumnInfo (name = "goal_category")
    var goalCategory: String = "",

    @ColumnInfo (name = "goal_date_time")
    var initialDateTime: Long = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli(),

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
    var dateOfCompletion : Long = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli(),

    @ColumnInfo (name = "goal_end_date")
    var endDateTime: Long = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli(),

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
    var tabListState: Long = packInts(0,0)

) {
    companion object {
        @Ignore
        private const val GOAL_IMAGE_PREFIX = "Goal_"
    }

    enum class GoalEndType {
        UNDEFINED, DATE, DELIVERABLE
    }

    enum class GoalTab{
        TASKS, DELIVERABLES, MARKERS
    }

    enum class Status{
        OVERDUE, PENDING, PAUSED, BREAK, COMPLETED, FAILED, FUTURE
    }

    enum class TimeBlockType{
        ONCE, DAILY, WEEKLY, MONTHLY, YEARLY
    }

    @Ignore
    val times: MutableStateFlow<Flow<List<GoalTaskDeliverableTime>>?> = MutableStateFlow(null)

    @Ignore
    val goalDeliverables: MutableStateFlow<Flow<List<Deliverable>>?> = MutableStateFlow(null)

    @Ignore
    val goalTasks: MutableStateFlow<Flow<List<Task>>?> = MutableStateFlow(null)

    @Ignore
    val goalMarkers: MutableStateFlow<Flow<List<Marker>>?> = MutableStateFlow(null)

    @Ignore
    val selectedGoalDeliverables: MutableStateFlow<Flow<List<Deliverable>>?> = MutableStateFlow(null)

    @Ignore
    val imageResource = AppResources.ImageResource(goalPicture?:"")

    fun loadGoalTimes(dao: RepositoryDao){
        times.value = dao.getTimes(id, GoalTaskDeliverableTime.TimesType.GOAL)
    }

    fun loadDeliverables(dao: RepositoryDao) {
        selectedGoalDeliverables.value = dao.getGoalDeliverables(id)
        goalDeliverables.value = dao.getDeliverables(id)
    }

    fun loadTasks(dao: RepositoryDao) {
        goalTasks.value = dao.getTasks(id, Task.TaskParentType.GOAL).map { tasks ->
            tasks.forEach { task ->
                task.loadTimes(dao)
            }
            tasks
        }
    }

    fun loadMarkers(dao: RepositoryDao) {
        goalMarkers.value = dao.getMarkers(id)
    }

    fun getGoalPicture(): String? { return goalPicture }

    suspend fun saveImage(context: Context, inputUri: Uri?){
        goalPicture = imageResource.saveFile(context, inputUri, GOAL_IMAGE_PREFIX, id)
    }

    suspend fun deleteFile(context: Context) {
        if (imageResource.deleteFile(context)) {
            goalPicture = null
        }
    }

    fun getUri(context: Context): StateFlow<Uri?> = imageResource.getUri(context)
}