package com.thando.accountable.database.tables

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.focus.FocusRequester
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.thando.accountable.AppResources
import java.util.Calendar

@Entity(tableName = "deliverable_table")
data class Deliverable (
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null,

    @ColumnInfo(name = "deliverable_parent")
    val parent: MutableState<Long>,

    @ColumnInfo (name = "deliverable_position")
    val position: MutableState<Long>,

    @ColumnInfo (name = "deliverable_initial_date")
    var initialDateTime: AppResources.CalendarResource = AppResources.CalendarResource(
        Calendar.getInstance()
    ),

    @ColumnInfo (name = "deliverable_end_date")
    var endDateTime: AppResources.CalendarResource = AppResources.CalendarResource(
        Calendar.getInstance()
    ),

    @ColumnInfo (name = "deliverable_end_type")
    val endType: MutableState<DeliverableEndType> = mutableStateOf(DeliverableEndType.UNDEFINED),

    @ColumnInfo (name = "deliverable_edit_scroll_position")
    val scrollPosition: LazyListState = LazyListState(0,0),

    @ColumnInfo (name = "deliverable_deliverable")
    val deliverable : TextFieldState = TextFieldState(""),

    @ColumnInfo (name = "deliverable_status")
    val status : MutableState<Goal.Status> = mutableStateOf(Goal.Status.PENDING),

    @ColumnInfo (name = "deliverable_location")
    val location: TextFieldState = TextFieldState(""),

    @ColumnInfo (name = "deliverable_size")
    val size: MutableState<Float> = mutableFloatStateOf(0F),

    @ColumnInfo (name = "deliverable_num_images")
    val numImages: MutableState<Int> = mutableIntStateOf(0),

    @ColumnInfo (name = "deliverable_num_videos")
    val numVideos: MutableState<Int> = mutableIntStateOf(0),

    @ColumnInfo (name = "deliverable_num_audios")
    val numAudios: MutableState<Int> = mutableIntStateOf(0),

    @ColumnInfo (name = "deliverable_num_documents")
    val numDocuments: MutableState<Int> = mutableIntStateOf(0),

    @ColumnInfo (name = "deliverable_num_scripts")
    val numScripts: MutableState<Int> = mutableIntStateOf(0),
) {
    enum class DeliverableEndType {
        UNDEFINED, DATE, GOAL, WORK
    }

    @Ignore
    val times: SnapshotStateList<GoalTaskDeliverableTime> = mutableStateListOf()

    @Ignore
    val deliverableTextFocusRequester = FocusRequester()

    @Ignore
    val locationFocusRequester = FocusRequester()
}