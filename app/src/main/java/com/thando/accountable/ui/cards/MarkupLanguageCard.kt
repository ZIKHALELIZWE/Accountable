package com.thando.accountable.ui.cards

import android.content.Context
import android.view.View
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.thando.accountable.SpannedString
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.database.tables.SpecialCharacters.State
import kotlinx.coroutines.flow.MutableStateFlow

data class MarkupLanguageCard(
    val markupLanguageName: String,
    val tag: MarkupLanguage.Tag,
    val exampleText: String
) {
    val spannedString = SpannedString(exampleText,tag)
    val valueEditTextVisibility: Int
    val colourButtonVisibility: Int

    val duplicateErrorMessage = MutableStateFlow("")
    val errorMessageVisibility: MutableStateFlow<Int> = MutableStateFlow(View.GONE)
    val colourButtonEnabled = MutableStateFlow(false)
    private var duplicateList : List<String>? = null
    val backgroundColour = MutableStateFlow(Color.Gray.toArgb())

    init {
        when(tag.spanType){
            MarkupLanguage.TagType.FUNCTION_INT,
            MarkupLanguage.TagType.FUNCTION_FLOAT,
            MarkupLanguage.TagType.FUNCTION_URL -> {
                valueEditTextVisibility = View.VISIBLE
                colourButtonVisibility = View.GONE
            }
            MarkupLanguage.TagType.FUNCTION_CLICKABLE,
            MarkupLanguage.TagType.FUNCTION,
            MarkupLanguage.TagType.FUNCTION_IMAGE_URI->{
                valueEditTextVisibility = View.GONE
                colourButtonVisibility = View.GONE
            }
            MarkupLanguage.TagType.FUNCTION_COLOUR,
            MarkupLanguage.TagType.FUNCTION_STRING-> {
                colourButtonVisibility = View.VISIBLE
                valueEditTextVisibility = View.GONE
            }
        }
    }

    private fun updateState(stateInput: State){
        when(stateInput){
            State.EMPTY->{
                backgroundColour.value = Color.Gray.toArgb()
                errorMessageVisibility.value = View.GONE
            }
            State.VALID->{
                backgroundColour.value = Color.Green.toArgb()
                errorMessageVisibility.value = View.GONE
            }
            State.DUPLICATE->{
                backgroundColour.value = Color.Red.toArgb()
                val errorMessage = StringBuilder("Character The Same As:\n")
                duplicateList?.forEach { if (it != tag.spanName) errorMessage.append("\n$it") }
                duplicateErrorMessage.value = errorMessage.toString()
                errorMessageVisibility.value = View.VISIBLE
            }
        }
    }

    fun setDuplicateIndexes(list: List<String>){
        duplicateList = null
        if (list.isEmpty()) updateState(State.EMPTY)
        else if (list.size == 1) updateState(State.VALID)
        else{
            duplicateList = list
            updateState(State.DUPLICATE)
        }
    }

    fun hasValue():Boolean{ return tag.hasValue }

    fun getSpanType(): MarkupLanguage.TagType { return tag.spanType }

    fun getString():String { return spannedString.getString() }

    fun getSpanIdentifierSet(): Boolean{
        return tag.spanCharValue.first.value.isNotEmpty()
    }

    private fun getSpanCharValueConversionSet():Boolean {
        val spanValueSet = MarkupLanguage.getStringValueSet(tag.spanCharValue.second.value)
        return when(getSpanType()) {
            MarkupLanguage.TagType.FUNCTION -> true
            MarkupLanguage.TagType.FUNCTION_INT -> spanValueSet && MarkupLanguage.getInt(tag.spanCharValue.second)!=null
            MarkupLanguage.TagType.FUNCTION_FLOAT -> spanValueSet && MarkupLanguage.getFloat(tag.spanCharValue.second)!=null
            MarkupLanguage.TagType.FUNCTION_COLOUR -> spanValueSet && MarkupLanguage.getColour(tag.spanCharValue.second)!=null
            MarkupLanguage.TagType.FUNCTION_IMAGE_URI -> spanValueSet
            MarkupLanguage.TagType.FUNCTION_URL -> spanValueSet
            MarkupLanguage.TagType.FUNCTION_CLICKABLE -> true
            MarkupLanguage.TagType.FUNCTION_STRING -> spanValueSet
        }
    }

    fun variablesSet(context: Context?=null, clickable: (()->Unit)?=null): Boolean{
        return when(getSpanType()) {
            MarkupLanguage.TagType.FUNCTION -> getSpanCharValueConversionSet()
            MarkupLanguage.TagType.FUNCTION_INT -> getSpanCharValueConversionSet()
            MarkupLanguage.TagType.FUNCTION_FLOAT -> getSpanCharValueConversionSet()
            MarkupLanguage.TagType.FUNCTION_COLOUR -> getSpanCharValueConversionSet()
            MarkupLanguage.TagType.FUNCTION_URL -> getSpanCharValueConversionSet()
            MarkupLanguage.TagType.FUNCTION_STRING -> getSpanCharValueConversionSet()
            MarkupLanguage.TagType.FUNCTION_IMAGE_URI -> context != null
            MarkupLanguage.TagType.FUNCTION_CLICKABLE -> clickable!=null
        }
    }
}