package com.thando.accountable.database.tables

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.focus.FocusRequester
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.thando.accountable.database.dataaccessobjects.RepositoryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDateTime

@Entity(tableName = "task_table")
data class Task(
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null,

    @ColumnInfo(name = "task_parent")
    val parent: MutableState<Long>,

    @ColumnInfo(name = "task_parent_type")
    val parentType: MutableState<TaskParentType>,

    @ColumnInfo (name = "task_position")
    val position: MutableState<Long>,

    @ColumnInfo (name = "task_initial_date")
    var initialDateTime: MutableState<LocalDateTime> = mutableStateOf(LocalDateTime.now()),

    @ColumnInfo (name = "task_end_date")
    var endDateTime: MutableState<LocalDateTime> = mutableStateOf(LocalDateTime.now()),

    @ColumnInfo (name = "task_end_type")
    val endType: MutableState<TaskEndType> = mutableStateOf(TaskEndType.UNDEFINED),

    @ColumnInfo (name = "task_edit_scroll_position")
    val scrollPosition: LazyListState = LazyListState(0,0),

    @ColumnInfo (name = "task_task")
    val task : TextFieldState = TextFieldState(""),

    @ColumnInfo (name = "task_type")
    val type: MutableState<TaskType>,

    @ColumnInfo (name = "task_quantity")
    val quantity: MutableState<Long> = mutableLongStateOf(0L),

    @ColumnInfo (name = "task_time")
    val time: MutableState<LocalDateTime> = mutableStateOf(LocalDateTime.now()),

    @ColumnInfo (name = "task_status")
    val status : MutableState<Goal.Status> = mutableStateOf(Goal.Status.PENDING),

    @ColumnInfo (name = "task_colour") // -1 For no colour selected
    val colour: MutableState<Int> = mutableIntStateOf(-1),

    @ColumnInfo (name = "task_location")
    val location: TextFieldState = TextFieldState(""),

    @ColumnInfo (name = "task_size")
    val size: MutableState<Float> = mutableFloatStateOf(0F),

    @ColumnInfo (name = "task_num_images")
    val numImages: MutableState<Int> = mutableIntStateOf(0),

    @ColumnInfo (name = "task_num_videos")
    val numVideos: MutableState<Int> = mutableIntStateOf(0),

    @ColumnInfo (name = "task_num_audios")
    val numAudios: MutableState<Int> = mutableIntStateOf(0),

    @ColumnInfo (name = "task_num_documents")
    val numDocuments: MutableState<Int> = mutableIntStateOf(0),

    @ColumnInfo (name = "task_num_scripts")
    val numScripts: MutableState<Int> = mutableIntStateOf(0),

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