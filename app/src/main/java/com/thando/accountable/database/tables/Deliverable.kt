package com.thando.accountable.database.tables

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.util.packInts
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.thando.accountable.R
import com.thando.accountable.database.Converters
import java.time.LocalDateTime

@Entity(tableName = "deliverable_table")
data class Deliverable (
    @PrimaryKey(autoGenerate = true)
    var deliverableId: Long? = null,

    @ColumnInfo(name = "deliverable_parent")
    var parent: Long,

    @ColumnInfo (name = "deliverable_position")
    var position: Long,

    @ColumnInfo (name = "deliverable_initial_date")
    var initialDateTime: Long = Converters().fromLocalDateTime(LocalDateTime.now()),

    @ColumnInfo (name = "deliverable_end_date")
    var endDateTime: Long? = null,

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
    var cloneId: Long? = null,

    @ColumnInfo (name = "deliverable_work_type")
    var workType: String = TaskDeliverable.WorkType.CompleteTasks.name,

    @ColumnInfo (name = "deliverable_should_complete_work")
    var shouldCompleteWork: Boolean = true,

    @ColumnInfo (name = "deliverable_type")
    var type: String //TaskType
) {
    enum class DeliverableEndType(val resString: Int) {
        UNDEFINED(R.string.undefined),
        DATE(R.string.date),
        GOAL(R.string.goal),
        WORK(R.string.work)
    }

    @Ignore
    val deliverableTextFocusRequester = FocusRequester()

    @Ignore
    val locationFocusRequester = FocusRequester()
}

data class DeliverableWithLists(
    @Embedded val deliverable: Deliverable,

    @Relation(
        entity = TaskDeliverable::class,
        parentColumn = "deliverableId",
        entityColumn = "deliverableId"
    )
    val taskDeliverables: MutableList<TaskDeliverable> = mutableListOf(),

    @Relation(
        entity = GoalTaskDeliverableTime::class,
        parentColumn = "deliverableId",
        entityColumn = "times_parent"
    )
    private val timesRaw: List<GoalTaskDeliverableTime> = emptyList()
) {
    @Ignore
    val times = timesRaw.filter { it.type == GoalTaskDeliverableTime.TimesType.DELIVERABLE.name }
}