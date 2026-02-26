package com.thando.accountable.database.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.thando.accountable.R
import com.thando.accountable.database.Converters
import java.time.LocalDateTime

@Entity(
    primaryKeys = ["taskId","deliverableId"],
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["taskId"],
            childColumns = ["taskId"]
        ),
        ForeignKey(
            entity = Deliverable::class,
            parentColumns = ["deliverableId"],
            childColumns = ["deliverableId"]
        )
    ],
)
data class TaskDeliverable (
    val taskId: Long,
    val deliverableId:Long,

    @ColumnInfo(name = "percentage")
    var percentage: Float? = null,

    @ColumnInfo(name = "startDate")
    var startDate: Long = Converters().fromLocalDateTime(LocalDateTime.now()),

    @ColumnInfo(name = "streak")
    var streak: Long? = null,

    @ColumnInfo(name = "workType")
    var workType: String = WorkType.RepeatingTaskTimes.name
) {
    enum class WorkType(val resString: Int) {
        RepeatingTaskTimes(R.string.task_accumulation),
        QuantityTaskOnce(R.string.quantity_accumulation),
        TimeTaskOnce(R.string.time_accumulation),
        CompleteTasks(R.string.complete_tasks)
    }
}
