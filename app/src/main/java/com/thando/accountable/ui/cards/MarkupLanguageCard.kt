package com.thando.accountable.ui.cards

import android.content.Context
import android.view.View
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.thando.accountable.SpannedString
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.database.tables.SpecialCharacters.State
import com.thando.accountable.recyclerviewadapters.MarkupLanguageCardAdapter.Companion.EXAMPLE_PARAGRAPH
import com.thando.accountable.recyclerviewadapters.MarkupLanguageCardAdapter.Companion.EXAMPLE_TEXT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class MarkupLanguageCard(
    val markupLanguageName: String,
    val tag: MarkupLanguage.Tag,
    val exampleText: String
) {
    val spannedString = SpannedString(exampleText,tag)
    val valueEditTextVisibility: Boolean
    val colourButtonVisibility: Boolean

    val duplicateErrorMessage = MutableStateFlow("")
    val errorMessageVisibility: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val colourButtonEnabled = MutableStateFlow(false)
    private var duplicateList : List<String>? = null
    val backgroundColour = MutableStateFlow(Color.Gray.toArgb())

    private var isProcessing = false
    var clickableSpan: (()->Unit)? = null

    init {
        when(tag.spanType){
            MarkupLanguage.TagType.FUNCTION_INT,
            MarkupLanguage.TagType.FUNCTION_FLOAT,
            MarkupLanguage.TagType.FUNCTION_URL -> {
                valueEditTextVisibility = true
                colourButtonVisibility = false
            }
            MarkupLanguage.TagType.FUNCTION_CLICKABLE,
            MarkupLanguage.TagType.FUNCTION,
            MarkupLanguage.TagType.FUNCTION_IMAGE_URI->{
                valueEditTextVisibility = false
                colourButtonVisibility = false
            }
            MarkupLanguage.TagType.FUNCTION_COLOUR,
            MarkupLanguage.TagType.FUNCTION_STRING-> {
                colourButtonVisibility = true
                valueEditTextVisibility = false
            }
        }
    }

    private fun updateState(stateInput: State){
        when(stateInput){
            State.EMPTY->{
                backgroundColour.value = Color.Gray.toArgb()
                errorMessageVisibility.update { false }
            }
            State.VALID->{
                backgroundColour.value = Color.Green.toArgb()
                errorMessageVisibility.update { false }
            }
            State.DUPLICATE->{
                backgroundColour.value = Color.Red.toArgb()
                val errorMessage = StringBuilder("Character The Same As:\n")
                duplicateList?.forEach { if (it != tag.spanName) errorMessage.append("\n$it") }
                duplicateErrorMessage.value = errorMessage.toString()
                errorMessageVisibility.update { true }
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
        return tag.spanCharValue.first.text.isNotEmpty()
    }

    private fun getSpanCharValueConversionSet():Boolean {
        val spanValueSet = MarkupLanguage.getStringValueSet(tag.spanCharValue.second.text.toString())
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

    suspend fun processText(
        markupLanguage: MarkupLanguage?,
        context: Context,
        updateStates: ()->Unit
    ) {
        val span = tag.spanCharValue.first.text.toString()
        val value = tag.spanCharValue.second.text.toString()
        if (isProcessing) return
        else isProcessing = true
        withContext(Dispatchers.IO) {
            if (span.isNotEmpty()) {
                if (value == "0") {
                    tag.spanCharValue.second.setTextAndPlaceCursorAtEnd("")
                } else {
                    getExampleAndSpanIndex(
                        markupLanguage,
                        this@MarkupLanguageCard,
                        variablesSet(context, clickableSpan)
                    ) { text, range ->
                        spannedString.setText(text, context)
                        if (range != null) {
                            tag.applyTag(
                                range,
                                spannedString.spannableStringBuilder,
                                context,
                                clickable = clickableSpan
                            )
                        }
                    }
                    colourButtonEnabled.value = true
                }
            } else {
                spannedString.setText("", context)
                colourButtonEnabled.value = false
            }
            updateStates()
            /*if (item.getSpanType() == MarkupLanguage.TagType.FUNCTION_URL || item.getSpanType() == MarkupLanguage.TagType.FUNCTION_CLICKABLE) {
                binding.markupLanguageCardCharExampleTextview.movementMethod =
                    LinkMovementMethod.getInstance()
            }*/
            isProcessing = false
        }
    }

    suspend fun getExampleAndSpanIndex(
        markupLanguage: MarkupLanguage?,
        card: MarkupLanguageCard,
        variablesSet:Boolean,
        function: suspend (String,IntRange?)->Unit
    )
    {
        var range: IntRange? = null
        val spanStart = 8
        val spanEnd = 15
        val builder = StringBuilder()
        if (
            markupLanguage?.opening?.text?.isNotEmpty() == true &&
            markupLanguage.closing.text.isNotEmpty() &&
            card.getSpanIdentifierSet() &&
            variablesSet
        )
        {
            markupLanguage.opening.text.toString().let { opening->
                markupLanguage.closing.text.toString().let { closing ->
                    if (!card.tag.isParagraph) {
                        var spanAddition =
                            EXAMPLE_TEXT.length + 1 + opening.length * 2 + card.tag.spanCharValue.first.text.length * 2 + closing.length * 2 + 1
                        builder.append(EXAMPLE_TEXT.take(spanStart))
                            .append(opening)
                            .append(card.tag.spanCharValue.first.text.toString())

                        if (card.tag.spanCharValue.second.text.isNotEmpty()) {
                            builder.append(MarkupLanguage.VALUE_OPENING).append(card.tag.spanCharValue.second.text.toString()).append(MarkupLanguage.VALUE_CLOSING)
                            spanAddition += 3 + card.tag.spanCharValue.second.text.length
                        }

                        builder.append(closing)
                            .append(EXAMPLE_TEXT.substring(spanStart, spanEnd))
                            .append(opening)
                            .append(MarkupLanguage.CLOSING_INDICATOR)
                            .append(card.tag.spanCharValue.first.text.toString())
                            .append(closing)
                            .append(EXAMPLE_TEXT.substring(spanEnd, EXAMPLE_TEXT.length))
                            .append('\n')
                            .append(EXAMPLE_TEXT)

                        range = IntRange(
                            spanAddition + spanStart,
                            spanAddition + spanEnd
                        )
                    } else{
                        builder.append(StringBuilder(opening))
                            .append(card.tag.spanCharValue.first.text.toString())

                        if (card.tag.spanCharValue.second.text.isNotEmpty()) {
                            builder.append(MarkupLanguage.VALUE_OPENING).append(card.tag.spanCharValue.second.text.toString()).append(MarkupLanguage.VALUE_CLOSING)
                        }

                        builder.append(closing)
                            .append('\n')
                            .append(EXAMPLE_PARAGRAPH)
                            .append('\n')
                            .append(opening)
                            .append(MarkupLanguage.CLOSING_INDICATOR)
                            .append(card.tag.spanCharValue.first.text.toString())
                            .append(closing).append("\n\n")
                            .append(EXAMPLE_PARAGRAPH)
                            .toString()

                        val builderString = builder.toString()
                        range = IntRange(builderString.lastIndexOf(EXAMPLE_PARAGRAPH),builderString.length-1)
                    }
                }
            }
        }
        if (range != null) function(builder.toString(),range)
        else function("",null)
    }
}