package com.thando.accountable.database.tables

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.thando.accountable.AppResources
import com.thando.accountable.database.dataaccessobjects.RepositoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.Calendar

@Entity(tableName = "goal_table")
data class Goal(
    @PrimaryKey (autoGenerate = true)
    var id: Long? = null,

    @ColumnInfo (name = "goal_parent")
    val parent: MutableState<Long> = mutableLongStateOf(1L),

    @ColumnInfo (name = "goal_category")
    var goalCategory: MutableState<String> = mutableStateOf(""),

    @ColumnInfo (name = "goal_date_time")
    var initialDateTime: AppResources.CalendarResource = AppResources.CalendarResource(Calendar.getInstance()),

    @ColumnInfo (name = "goal_position")
    val position: MutableState<Long> = mutableLongStateOf(0L),

    @ColumnInfo (name = "goal_scroll_position")
    val scrollPosition: LazyListState = LazyListState(0,0),

    @ColumnInfo (name = "goal_size")
    val size: MutableState<Float> = mutableFloatStateOf(0F),

    @ColumnInfo (name = "goal_num_images")
    val numImages: MutableState<Int> = mutableIntStateOf(0),

    @ColumnInfo (name = "goal_num_videos")
    val numVideos: MutableState<Int> = mutableIntStateOf(0),

    @ColumnInfo (name = "goal_num_audios")
    val numAudios: MutableState<Int> = mutableIntStateOf(0),

    @ColumnInfo (name = "goal_num_documents")
    val numDocuments: MutableState<Int> = mutableIntStateOf(0),

    @ColumnInfo (name = "goal_num_scripts")
    val numScripts: MutableState<Int> = mutableIntStateOf(0),

    @ColumnInfo (name = "goal_goal")
    val goal : TextFieldState = TextFieldState(""),

    @ColumnInfo (name = "goal_date_of_completion")
    var dateOfCompletion : AppResources.CalendarResource? = null,

    @ColumnInfo (name = "goal_picture")
    private var goalPicture : String? = null,

    @ColumnInfo (name = "goal_status")
    val status : MutableState<Status> = mutableStateOf(Status.PENDING),

    @ColumnInfo (name = "goal_colour") // -1 For no colour selected
    val colour: MutableState<Int> = mutableIntStateOf(-1),

    @ColumnInfo (name = "goal_location")
    val location: TextFieldState = TextFieldState(""),

    @ColumnInfo (name = "goal_selected_tab")
    val selectedTab: MutableState<GoalTab> = mutableStateOf(GoalTab.TASKS),

    @ColumnInfo (name = "goal_tab_list_state")
    val tabListState: LazyListState = LazyListState()

) {
    companion object {
        @Ignore
        private const val GOAL_IMAGE_PREFIX = "Goal_"
    }

    enum class GoalTab{
        TASKS, DELIVERABLES, MARKERS
    }

    enum class Status{
        OVERDUE, PENDING, PAUSED, BREAK, COMPLETED, FUTURE
    }

    enum class TimeBlockType{
        ONCE, DAILY, WEEKLY, MONTHLY, YEARLY
    }

    @Ignore
    val times: SnapshotStateList<GoalTaskDeliverableTime> = mutableStateListOf()

    @Ignore
    val imageResource = AppResources.ImageResource(goalPicture?:"")

    suspend fun loadGoalTimes(dao: RepositoryDao){
        times.clear()
        times.addAll(
            withContext(Dispatchers.IO){
                dao.getGoalTimes(id)
            }
        )
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