package com.thando.accountable.database.tables

import androidx.compose.ui.focus.FocusRequester
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.thando.accountable.database.tables.Goal.TimeBlockType
import java.time.LocalDateTime
import java.time.ZoneOffset

@Entity(tableName = "times_table")
data class GoalTaskDeliverableTime (
    @PrimaryKey (autoGenerate = true)
    var id: Long? = null,

    @ColumnInfo(name = "times_parent")
    var parent: Long,

    @ColumnInfo(name = "times_type")
    var type: String,

    @ColumnInfo(name = "times_time_block_type")
    var timeBlockType: String = TimeBlockType.ONCE.name,

    @ColumnInfo(name = "times_start")
    var start: Long = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli(),

    @ColumnInfo(name = "times_duration")
    var duration: Long = LocalDateTime.now().withHour(0).withMinute(0).toInstant(ZoneOffset.UTC).toEpochMilli()
){
    enum class TimesType{
        GOAL, TASK, DELIVERABLE
    }
    @Ignore
    val durationPickerFocusRequester = FocusRequester()

    @Ignore
    var cloneId: Long? = null
}