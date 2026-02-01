package com.thando.accountable.database.tables

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.util.packInts
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.thando.accountable.database.dataaccessobjects.RepositoryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDateTime
import java.time.ZoneOffset

@Entity(tableName = "deliverable_table")
data class Deliverable (
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null,

    @ColumnInfo(name = "deliverable_parent")
    var parent: Long,

    @ColumnInfo (name = "deliverable_position")
    var position: Long,

    @ColumnInfo (name = "deliverable_initial_date")
    var initialDateTime: Long = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli(),

    @ColumnInfo (name = "deliverable_end_date")
    var endDateTime: Long = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli(),

    @ColumnInfo (name = "deliverable_end_type")
    var endType: String = DeliverableEndType.UNDEFINED.name,

    @ColumnInfo (name = "deliverable_edit_scroll_position")
    var scrollPosition: Long = packInts(0,0),

    @ColumnInfo (name = "deliverable_deliverable")
    var deliverable : String = "",

    @ColumnInfo (name = "deliverable_status")
    var status : String = Goal.Status.PENDING.name,

    @ColumnInfo (name = "deliverable_location")
    var location: String = "",

    @ColumnInfo (name = "deliverable_goal_id")
    val goalId: Long? = null,

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
) {
    enum class DeliverableEndType {
        UNDEFINED, DATE, GOAL, WORK
    }

    @Ignore
    val times = MutableStateFlow<Flow<List<GoalTaskDeliverableTime>>?>(null)

    @Ignore
    val deliverableTextFocusRequester = FocusRequester()

    @Ignore
    val locationFocusRequester = FocusRequester()

    fun loadTimes(dao: RepositoryDao) {
        times.value = dao.getTimes(id, GoalTaskDeliverableTime.TimesType.DELIVERABLE)
    }
}