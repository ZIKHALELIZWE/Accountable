package com.thando.accountable.database.tables

import android.view.View
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Entity (tableName = "special_characters", primaryKeys = ["teleprompter_settings_id","character"])
data class SpecialCharacters(
    @ColumnInfo (name = "teleprompter_settings_id")
    var teleprompterSettingsId: Long,

    @ColumnInfo (name = "character")
    var character: TextFieldState = TextFieldState(""),

    @ColumnInfo (name = "editing_after_char")
    var editingAfterChar: TextFieldState = TextFieldState("")
){
    enum class State{
        EMPTY, VALID, DUPLICATE
    }

    @Ignore
    val oldCharacter = character.text.toString()

    @Ignore
    val backgroundColour = MutableStateFlow(Color.Gray.toArgb())

    @Ignore
    var state = MutableStateFlow(State.EMPTY)

    @Ignore
    var position : Int? = null

    @Ignore
    var duplicateList : List<Int>? = null

    @Ignore
    val duplicateErrorMessage = MutableStateFlow("")

    @Ignore
    val errorMessageButtonVisibility = MutableStateFlow(false)

    private fun isValid():Boolean{
        return state.value == State.VALID
    }

    fun canUpdateList(): Boolean{
        return isValid() && editingAfterChar.text.isNotEmpty()
    }

    private fun setAndUpdateState(inputState:State){
        state.update { inputState }
        when(state.value){
            State.EMPTY->{
                backgroundColour.value = Color.Gray.toArgb()
                errorMessageButtonVisibility.value = false
            }
            State.VALID->{
                backgroundColour.value = Color.Green.toArgb()
                errorMessageButtonVisibility.value = false
            }
            State.DUPLICATE->{
                backgroundColour.value = Color.Red.toArgb()
                val errorMessage = StringBuilder("Character The Same As:\n")
                duplicateList?.forEach { if (it != position) errorMessage.append("\nItem $it") }
                duplicateErrorMessage.value = errorMessage.toString()
                errorMessageButtonVisibility.value = true
            }
        }
    }

    fun setDuplicateIndexes(list: List<Int>, positionInput:Int){
        duplicateList = null
        position = positionInput
        if (list.isEmpty()) {
            setAndUpdateState(State.EMPTY)
        }
        else if (list.size == 1) {
            setAndUpdateState(State.VALID)
        }
        else{
            duplicateList = list
            setAndUpdateState(State.DUPLICATE)
        }
    }
}