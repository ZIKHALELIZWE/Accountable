package com.thando.accountable.database.tables

import android.view.View
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Entity (tableName = "special_characters", primaryKeys = ["teleprompter_settings_id","character"])
data class SpecialCharacters(
    @ColumnInfo (name = "teleprompter_settings_id")
    var teleprompterSettingsId: Long,

    @ColumnInfo (name = "character")
    var character: MutableStateFlow<String> = MutableStateFlow(""),

    @ColumnInfo (name = "editing_after_char")
    var editingAfterChar: MutableStateFlow<String> = MutableStateFlow("")
){
    enum class State{
        EMPTY, VALID, DUPLICATE
    }

    @Ignore
    val oldCharacter = character.value

    @Ignore
    val deleteClicked = MutableSharedFlow<Boolean>()

    @Ignore
    val backgroundColour = MutableStateFlow(Color.Gray.toArgb())

    @Ignore
    var state = State.EMPTY

    @Ignore
    var position : Int? = null

    @Ignore
    var duplicateList : List<Int>? = null

    @Ignore
    val duplicateErrorMessage = MutableStateFlow("")

    @Ignore
    val errorMessageButtonVisibility = MutableStateFlow(View.GONE)

    fun delete(viewLifecycleOwner: LifecycleOwner){
        viewLifecycleOwner.lifecycleScope.launch {
            deleteClicked.emit(true)
        }
    }

    private fun isValid():Boolean{
        return state == State.VALID
    }

    fun canUpdateList(): Boolean{
        return isValid() && editingAfterChar.value.isNotEmpty()
    }

    private fun setAndUpdateState(inputState:State){
        state = inputState
        when(state){
            State.EMPTY->{
                backgroundColour.value = Color.Gray.toArgb()
                errorMessageButtonVisibility.value = View.GONE
            }
            State.VALID->{
                backgroundColour.value = Color.Green.toArgb()
                errorMessageButtonVisibility.value = View.GONE
            }
            State.DUPLICATE->{
                backgroundColour.value = Color.Red.toArgb()
                val errorMessage = StringBuilder("Character The Same As:\n")
                duplicateList?.forEach { if (it != position) errorMessage.append("\nItem $it") }
                duplicateErrorMessage.value = errorMessage.toString()
                errorMessageButtonVisibility.value = View.VISIBLE
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