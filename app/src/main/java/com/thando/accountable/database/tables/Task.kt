package com.thando.accountable.database.tables

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.util.packInts
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.thando.accountable.database.Converters
import com.thando.accountable.database.dataaccessobjects.RepositoryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDateTime
import java.time.ZoneOffset

@Entity(tableName = "task_table")
data class Task(
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null,

    @ColumnInfo(name = "task_parent")
    var parent: Long,

    @ColumnInfo(name = "task_parent_type")
    var parentType: String, //TaskParentType

    @ColumnInfo (name = "task_position")
    var position: Long,

    @ColumnInfo (name = "task_initial_date")
    var initialDateTime: Long = Converters().fromLocalDateTime(LocalDateTime.now()),

    @ColumnInfo (name = "task_end_date")
    var endDateTime: Long = Converters().fromLocalDateTime(LocalDateTime.now()),

    @ColumnInfo (name = "task_end_type")
    var endType: String = TaskEndType.UNDEFINED.name,

    @ColumnInfo (name = "task_edit_scroll_position")
    var scrollPosition: Long = packInts(0,0),

    @ColumnInfo (name = "task_task")
    var task : String = "",

    @ColumnInfo (name = "task_type")
    var type: String, //TaskType,

    @ColumnInfo (name = "task_quantity")
    var quantity: Long = 0L,

    @ColumnInfo (name = "task_time")
    var time: Long = Converters().fromLocalDateTime(LocalDateTime.now()),

    @ColumnInfo (name = "task_status")
    var status : String = Goal.Status.PENDING.name,

    @ColumnInfo (name = "task_colour") // -1 For no colour selected
    var colour: Int = -1,

    @ColumnInfo (name = "task_location")
    var location: String = "",

    @ColumnInfo (name = "task_size")
    var size: Float = 0F,

    @ColumnInfo (name = "task_num_images")
    var numImages: Int = 0,

    @ColumnInfo (name = "task_num_videos")
    var numVideos: Int = 0,

    @ColumnInfo (name = "task_num_audios")
    var numAudios: Int = 0,

    @ColumnInfo (name = "task_num_documents")
    var numDocuments: Int = 0,

    @ColumnInfo (name = "task_num_scripts")
    var numScripts: Int = 0,

) {
    enum class TaskParentType {
        GOAL, FOLDER
    }

    enum class TaskEndType {
        DATE, DELIVERABLE, GOAL, UNDEFINED, MARKER
    }

    enum class TaskType {
        NORMAL, QUANTITY, TIME,
    }

    @Ignore
    val times = MutableStateFlow<Flow<List<GoalTaskDeliverableTime>>?>(null)

    @Ignore
    val taskTextFocusRequester = FocusRequester()

    @Ignore
    val locationFocusRequester = FocusRequester()

    @Ignore
    val colourFocusRequester = FocusRequester()

    fun loadTimes(dao: RepositoryDao) {
        times.value = dao.getTimes(id, GoalTaskDeliverableTime.TimesType.TASK)
    }
}