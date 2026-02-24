package com.thando.accountable.database.tables

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.util.packInts
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.thando.accountable.MainActivity
import com.thando.accountable.database.Converters
import com.thando.accountable.database.dataaccessobjects.RepositoryDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDateTime

@Entity(
    tableName = "deliverable_table",
    indices = [
        Index(value = ["deliverable_task_id"], unique = true) // Enforces uniqueness
    ]
)
data class Deliverable (
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null,

    @ColumnInfo(name = "deliverable_parent")
    var parent: Long,

    @ColumnInfo (name = "deliverable_position")
    var position: Long,

    @ColumnInfo (name = "deliverable_initial_date")
    var initialDateTime: Long = Converters().fromLocalDateTime(LocalDateTime.now()),

    @ColumnInfo (name = "deliverable_end_date")
    var endDateTime: Long = Converters().fromLocalDateTime(LocalDateTime.now()),

    @ColumnInfo (name = "deliverable_end_type")
    var endType: String = DeliverableEndType.UNDEFINED.name,

    @ColumnInfo (name = "deliverable_edit_scroll_position")
    var scrollPosition: Long = packInts(0,0),

    @ColumnInfo (name = "deliverable_deliverable")
    var deliverable : String = "",

    @ColumnInfo (name = "deliverable_quantity")
    var quantity: Long = 0L,

    @ColumnInfo (name = "deliverable_time")
    var time: Long = Converters().fromLocalDateTime(LocalDateTime.now()),

    @ColumnInfo (name = "deliverable_status")
    var status : String = Goal.Status.PENDING.name,

    @ColumnInfo (name = "deliverable_location")
    var location: String = "",

    @ColumnInfo (name = "deliverable_goal_id")
    var goalId: Long? = null,

    @ColumnInfo (name = "deliverable_task_id")
    var taskId: Long? = null,

    @ColumnInfo (name = "deliverable_size")
    var size: Float = 0F,

    @ColumnInfo (name = "deliverable_num_images")
    var numImages: Int = 0,

    @ColumnInfo (name = "deliverable_num_videos")
    var numVideos: Int = 0,

    @ColumnInfo (name = "deliverable_num_audios")
    var numAudios: Int = 0,

    @ColumnInfo (name = "deliverable_num_documents")
    var numDocuments: Int = 0,

    @ColumnInfo (name = "deliverable_num_scripts")
    var numScripts: Int = 0,

    @ColumnInfo (name = "deliverable_clone_id")
    var cloneId: Long? = null
) {
    enum class DeliverableEndType {
        UNDEFINED, DATE, GOAL, WORK
    }

    @Ignore
    val timesState = MutableStateFlow<Flow<List<GoalTaskDeliverableTime>>?>(null)
    @OptIn(ExperimentalCoroutinesApi::class)
    @Ignore
    val times = timesState.flatMapLatest { it?: MutableStateFlow(emptyList()) }
        .flowOn(MainActivity.IO)

    @Ignore
    val deliverableTextFocusRequester = FocusRequester()

    @Ignore
    val locationFocusRequester = FocusRequester()

    fun loadTimes(dao: RepositoryDao) {
        timesState.value = dao.getTimes(id, GoalTaskDeliverableTime.TimesType.DELIVERABLE)
    }
}