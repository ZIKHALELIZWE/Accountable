package com.thando.accountable.database.tables

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.thando.accountable.database.tables.Goal.TimeBlockType
import java.time.LocalDateTime

@Entity(tableName = "times_table")
data class GoalTaskDeliverableTime (
    @PrimaryKey (autoGenerate = true)
    var id: Long? = null,

    @ColumnInfo(name = "times_goal")
    val goal: MutableState<Long> = mutableLongStateOf(-1),

    @ColumnInfo(name = "times_task")
    val task: MutableState<Long> = mutableLongStateOf(-1),

    @ColumnInfo(name = "times_deliverable")
    val deliverable: MutableState<Long> = mutableLongStateOf(-1),

    @ColumnInfo(name = "times_time_block_type")
    val timeBlockType: MutableState<TimeBlockType> = mutableStateOf(TimeBlockType.ONCE),

    @ColumnInfo(name = "times_start")
    val start: MutableState<LocalDateTime> = mutableStateOf(LocalDateTime.now()),

    @ColumnInfo(name = "times_duration")
    val duration: MutableState<LocalDateTime> = mutableStateOf(LocalDateTime.now().withHour(0).withMinute(0))
){
    @Ignore
    val durationPickerFocusRequester = FocusRequester()

    @Ignore
    var cloneId: Long? = null
}