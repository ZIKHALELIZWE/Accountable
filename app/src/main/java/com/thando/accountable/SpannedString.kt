package com.thando.accountable

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.thando.accountable.MainActivity.Companion.collectFlow
import com.thando.accountable.database.tables.MarkupLanguage
import kotlinx.coroutines.flow.MutableStateFlow


class SpannedString(inputText: String) {
    private var tagsList: List<MarkupLanguage.Tag>?=null
    var spannableStringBuilder = MutableStateFlow(SpannableStringBuilder(inputText))
    val spannableAnnotatedString = MutableStateFlow(buildAnnotatedString {
        append(inputText)
    })

    constructor(
        inputText: String,
        tag: MarkupLanguage.Tag
    ):this(inputText){
        tagsList = listOf(tag)
    }

    fun getString():String{ return spannableStringBuilder.value.toString() }

    fun setText(inputText: String?, context: Context?, markupLanguage: MarkupLanguage? = null) {
        spannableStringBuilder.value.clear()
        spannableStringBuilder.value.clearSpans()
        spannableStringBuilder.value = processString(inputText, markupLanguage, context)

        spannableAnnotatedString.value = processAnnotatedString(inputText, markupLanguage, context)
    }

    private fun processString(
        inputString: String?,
        markupLanguage: MarkupLanguage?,
        context: Context?
    ): SpannableStringBuilder {
        return if (markupLanguage != null && inputString != null) {
            val (tags, noTagString) = markupLanguage.getTagRanges(inputString, true)
            val temp = SpannedString(noTagString)
            tagsList = tags
            tags.forEach { it.applyTag(temp.spannableStringBuilder, context) }
            temp.spannableStringBuilder.value
        } else if (inputString != null) {
            SpannedString(inputString).spannableStringBuilder.value
        } else {
            SpannedString("").spannableStringBuilder.value
        }
    }

    private fun processAnnotatedString(
        inputString: String?,
        markupLanguage: MarkupLanguage?,
        context: Context?
    ): AnnotatedString {
        return if (markupLanguage != null && inputString != null) {
            val (tags, noTagString) = markupLanguage.getTagRanges(inputString, true)
            val temp = MutableStateFlow(buildAnnotatedString { append(noTagString) } )
            tagsList = tags
            tags.forEach { it.applyTagAnnotated(temp, context) }
            temp.value
        } else if (inputString != null) {
            buildAnnotatedString {
                append(inputString)
            }
        } else {
            buildAnnotatedString {
                append("")
            }
        }
    }
}