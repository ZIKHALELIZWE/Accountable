package com.thando.accountable.database.tables

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.util.packInts
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.thando.accountable.database.Converters
import java.time.LocalDateTime
import java.time.ZoneOffset

@Entity(tableName = "marker_table")
data class Marker(
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null,

    @ColumnInfo(name = "marker_parent")
    var parent: Long,

    @ColumnInfo (name = "marker_position")
    var position: Long,

    @ColumnInfo (name = "marker_date_time")
    var dateTime: Long = Converters().fromLocalDateTime(LocalDateTime.now()),

    @ColumnInfo (name = "marker_edit_scroll_position")
    var scrollPosition: Long = packInts(0,0),

    @ColumnInfo (name = "marker_marker")
    var marker : String = "",

    @ColumnInfo (name = "marker_clone_id")
    var cloneId : Long? = null
) {
    @Ignore
    val markerTextFocusRequester = FocusRequester()
}