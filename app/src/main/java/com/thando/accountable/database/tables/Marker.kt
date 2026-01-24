package com.thando.accountable.database.tables

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.MutableState
import androidx.compose.ui.focus.FocusRequester
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.thando.accountable.AppResources
import java.util.Calendar

@Entity(tableName = "marker_table")
data class Marker(
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null,

    @ColumnInfo(name = "marker_parent")
    val parent: MutableState<Long>,

    @ColumnInfo (name = "marker_position")
    val position: MutableState<Long>,

    @ColumnInfo (name = "marker_date_time")
    var dateTime: AppResources.CalendarResource = AppResources.CalendarResource(
        Calendar.getInstance()
    ),

    @ColumnInfo (name = "marker_edit_scroll_position")
    val scrollPosition: LazyListState = LazyListState(0,0),

    @ColumnInfo (name = "marker_marker")
    val marker : TextFieldState = TextFieldState(""),
) {
    @Ignore
    val markerTextFocusRequester = FocusRequester()
}