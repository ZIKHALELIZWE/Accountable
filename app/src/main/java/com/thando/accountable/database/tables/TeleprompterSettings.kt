package com.thando.accountable.database.tables

import android.graphics.Color
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.thando.accountable.MainActivity
import com.thando.accountable.R
import kotlinx.coroutines.flow.MutableStateFlow

@Entity(tableName = "teleprompter_settings_table")
data class TeleprompterSettings(
    @PrimaryKey (autoGenerate = true)
    var id: Long? = null,

    @ColumnInfo (name = "name")
    var name: MutableStateFlow<String>,

    @ColumnInfo (name = "text_size")
    var textSize: MutableStateFlow<Int> = MutableStateFlow(24),

    @ColumnInfo (name = "scroll_speed")
    var scrollSpeed: MutableStateFlow<Int> = MutableStateFlow(4),

    @ColumnInfo (name = "text_colour")
    var textColour: MutableStateFlow<Int> = MutableStateFlow(Color.BLACK),

    @ColumnInfo (name = "background_colour")
    var backgroundColour: MutableStateFlow<Int> = MutableStateFlow(Color.WHITE),

    @ColumnInfo (name = "controls_position_bottom")
    var controlsPositionBottom: MutableStateFlow<Boolean> = MutableStateFlow(false),

    @ColumnInfo (name = "start_countdown")
    var startCountDown: MutableStateFlow<Int> = MutableStateFlow(0),

    @ColumnInfo (name = "scroll_countdown")
    var scrollCountDown: MutableStateFlow<Int> = MutableStateFlow(0),

    @ColumnInfo (name = "skip_size")
    var skipSize: MutableStateFlow<Int> = MutableStateFlow(0)
){
    @Ignore
    val specialCharactersList = mutableStateListOf<SpecialCharacters>()

    fun equalsContent(teleprompterSettings: TeleprompterSettings): Boolean {
        return (teleprompterSettings.name.value == name.value
                && teleprompterSettings.textSize.value == textSize.value
                && teleprompterSettings.scrollSpeed.value == scrollSpeed.value
                && teleprompterSettings.textColour.value == textColour.value
                && teleprompterSettings.backgroundColour.value == backgroundColour.value
                && teleprompterSettings.controlsPositionBottom.value == controlsPositionBottom.value
                && teleprompterSettings.startCountDown.value == startCountDown.value
                && teleprompterSettings.scrollCountDown.value == scrollCountDown.value
                && teleprompterSettings.skipSize.value == skipSize.value)
    }

    fun setValues(teleprompterSettings: TeleprompterSettings){
        name.value  = teleprompterSettings.name.value
        textSize.value  = teleprompterSettings.textSize.value
        scrollSpeed.value  = teleprompterSettings.scrollSpeed.value
        textColour.value  = teleprompterSettings.textColour.value
        backgroundColour.value  = teleprompterSettings.backgroundColour.value
        controlsPositionBottom.value  = teleprompterSettings.controlsPositionBottom.value
        startCountDown.value  = teleprompterSettings.startCountDown.value
        scrollCountDown.value  = teleprompterSettings.scrollCountDown.value
        skipSize.value  = teleprompterSettings.skipSize.value
    }

    override fun toString(): String {
        return name.value
    }
}