package com.thando.accountable.database.tables

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.BulletSpan
import android.text.style.ClickableSpan
import android.text.style.DrawableMarginSpan
import android.text.style.DynamicDrawableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.IconMarginSpan
import android.text.style.ImageSpan
import android.text.style.LeadingMarginSpan
import android.text.style.LineBackgroundSpan
import android.text.style.LineHeightSpan
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.ScaleXSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TabStopSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.util.Range
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Bullet
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.text.set
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivity
import com.thando.accountable.R
import com.thando.accountable.ui.cards.MarkupLanguageCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3
import kotlin.reflect.KFunction4

// we will use '=()' for Spans with values
@Entity(tableName = "markup_language_table")
data class MarkupLanguage(
    @PrimaryKey
    var name: MutableStateFlow<String>,

    @ColumnInfo(name = "opening")
    var opening: TextFieldState = TextFieldState(""),

    @ColumnInfo(name = "closing")
    var closing: TextFieldState = TextFieldState(""),

    @ColumnInfo(name = "relative_size")
    var relativeSize: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "absolute_size")
    var absoluteSize: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "scale_x")
    var scaleX: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "bold")
    var bold: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "italic")
    var italic: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "underline")
    var underline: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "strikethrough")
    var strikethrough: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "superscript")
    var superscript: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "subscript")
    var subscript: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "colour_text")
    var colourText: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "highlight")
    var highlight: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "url")
    var url: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "clickable")
    var clickable: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "dynamic_drawable")
    var dynamicDrawable: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "image")
    var image: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "alignment")
    var alignment: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "quote")
    var quote: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "bullet_point")
    var bulletPoint: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "bullet_point_colour")
    var bulletPointColour: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "line_height")
    var lineHeight: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "drawable_margin")
    var drawableMargin: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "icon_margin")
    var iconMargin: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "leading_margin")
    var leadingMargin: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "tab_stop")
    var tabStop: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState("")),

    @ColumnInfo(name = "line_background")
    var lineBackground: Pair<TextFieldState,TextFieldState> = Pair(TextFieldState(""),TextFieldState(""))
)
{
    companion object{
        const val VALUE_OPENING = "=("
        const val VALUE_CLOSING = ")"
        const val CLOSING_INDICATOR = "/"

        const val EXAMPLE_SPAN = "bi"
        const val EXAMPLE_TEXT = "This is example text."
        const val EXAMPLE_PARAGRAPH = "This\t is the first paragraph.\n\nThis\t is the second paragraph.\n\nThis\t is the third paragraph."


        fun getFloat(inputFloat: TextFieldState): Float? {
            return inputFloat.text.toString().toFloatOrNull()
        }

        fun getInt(inputInt: TextFieldState): Int? {
            return inputInt.text.toString().toIntOrNull()
        }

        fun getColour(inputColour: TextFieldState): Color? {
            val colourInt = getInt(inputColour) ?: return null
            return Color.valueOf(colourInt)
        }

        fun getStringValueSet(string:String?):Boolean { return !string.isNullOrEmpty() }

        fun findParagraphs(textValue: String, startIndexInput: Int, endIndex: Int):List<IntRange>{
            val paragraphs = arrayListOf<IntRange>()
            //val lines = textValue.substring(startIndexInput,endIndex).split("[\r\n]+".toRegex()).toTypedArray()
            val lines = textValue.substring(startIndexInput,endIndex).split("\n\n".toRegex()).toTypedArray()
            lines.forEach {
                if (it.isNotEmpty()) {
                    val index = textValue.substring(startIndexInput,endIndex).indexOf(it)
                    paragraphs.add(IntRange(index+startIndexInput,startIndexInput+ index + it.length))
                }
            }
            return paragraphs
        }
    }

    enum class TagType{
        FUNCTION, FUNCTION_INT, FUNCTION_FLOAT, FUNCTION_COLOUR, FUNCTION_IMAGE_URI,
        FUNCTION_URL, FUNCTION_CLICKABLE, FUNCTION_STRING, FUNCTION_RELATIVE_SIZE
    }

    @Ignore
    private val spans = mapOf(
        "Bold" to Tag("Bold", false, TagType.FUNCTION, bold, opening, closing, function = this::boldSpan, functionAnnotated = this::boldSpanAnnotated),
        "Italic" to Tag( "Italic",false, TagType.FUNCTION, italic, opening, closing, function = this::italicSpan, functionAnnotated = this::italicSpanAnnotated),
        "Underline" to Tag( "Underline",false, TagType.FUNCTION, underline, opening, closing, function = this::underlineSpan, functionAnnotated = this::underlineSpanAnnotated),
        "Strikethrough" to Tag( "Strikethrough", false, TagType.FUNCTION, strikethrough, opening, closing, function = this::strikethroughSpan, functionAnnotated = this::strikethroughSpanAnnotated),
        "Superscript" to Tag( "Superscript",false, TagType.FUNCTION, superscript, opening, closing, function = this::superscriptSpan, functionAnnotated = this::superscriptSpanAnnotated),
        "Subscript" to Tag( "Subscript",false, TagType.FUNCTION, subscript, opening, closing, function = this::subscriptSpan, functionAnnotated = this::subscriptSpanAnnotated),
        "Bullet Point" to Tag( "Bullet Point",true, TagType.FUNCTION, bulletPoint, opening, closing, function = this::bulletPointSpan, functionAnnotated = this::bulletPointSpanAnnotated),
        "Absolute Size" to Tag( "Absolute Size",false, TagType.FUNCTION_INT, absoluteSize, opening, closing, functionInt = this::absoluteSizeSpan, functionIntAnnotated = this::absoluteSizeSpanAnnotated),
        "Line Height" to Tag( "Line Height", true, TagType.FUNCTION_INT, lineHeight, opening, closing, functionInt = this::lineHeightSpan, functionIntAnnotated = this::lineHeightSpanAnnotated),
        "Leading Margin" to Tag( "Leading Margin", true, TagType.FUNCTION_INT, leadingMargin, opening, closing, functionInt = this::leadingMarginSpan, functionIntAnnotated = this::leadingMarginSpanAnnotated),
        "Tab Stop" to Tag( "Tab Stop", true, TagType.FUNCTION_INT, tabStop, opening, closing, functionInt = this::tabStopSpan, functionIntAnnotated = this::tabStopSpanAnnotated),
        "Relative Size" to Tag( "Relative Size", false, TagType.FUNCTION_RELATIVE_SIZE, relativeSize, opening, closing, functionRelativeSize = this::relativeSizeSpan, functionRelativeSizeAnnotated = this::relativeSizeSpanAnnotated),
        "Scale X" to Tag( "Scale X", false, TagType.FUNCTION_FLOAT, scaleX, opening, closing, functionFloat = this::scaleXSpan, functionFloatAnnotated = this::scaleXSpanAnnotated),
        "Colour Text" to Tag( "Colour Text", false, TagType.FUNCTION_COLOUR, colourText, opening, closing, functionColour = this::colourTextSpan, functionColourAnnotated = this::colourTextSpanAnnotated),
        "Highlight" to Tag( "Highlight", false, TagType.FUNCTION_COLOUR, highlight, opening, closing, functionColour = this::highlightSpan, functionColourAnnotated = this::highlightSpanAnnotated),
        "Quote" to Tag( "Quote", true, TagType.FUNCTION_COLOUR, quote, opening, closing, functionColour = this::quoteSpan, functionColourAnnotated = this::quoteSpanAnnotated),
        "Bullet Point Colour" to Tag( "Bullet Point Colour", true, TagType.FUNCTION_COLOUR, bulletPointColour, opening, closing, functionColour = this::bulletPointColourSpan, functionColourAnnotated = this::bulletPointColourSpanAnnotated),
        "Line Background" to Tag( "Line Background", true, TagType.FUNCTION_COLOUR, lineBackground, opening, closing, functionColour = this::lineBackgroundSpan, functionColourAnnotated = this::lineBackgroundSpanAnnotated),
        "Dynamic Drawable" to Tag( "Dynamic Drawable", false, TagType.FUNCTION_IMAGE_URI, dynamicDrawable, opening, closing, functionImageUri = this::dynamicDrawableSpan, functionImageUriAnnotated = this::dynamicDrawableSpanAnnotated),
        "Image" to Tag( "Image", false, TagType.FUNCTION_IMAGE_URI, image, opening, closing, functionImageUri = this::imageSpan, functionImageUriAnnotated = this::imageSpanAnnotated),
        "Drawable Margin" to Tag( "Drawable Margin", true, TagType.FUNCTION_IMAGE_URI, drawableMargin, opening, closing, functionImageUri = this::drawableMarginSpan, functionImageUriAnnotated = this::drawableMarginSpanAnnotated),
        "Icon Margin" to Tag( "Icon Margin", true, TagType.FUNCTION_IMAGE_URI, iconMargin, opening, closing, functionImageUri = this::iconMarginSpan, functionImageUriAnnotated = this::iconMarginSpanAnnotated),
        "Url" to Tag( "Url", false, TagType.FUNCTION_URL, url, opening, closing, functionUrl = this::urlSpan, functionUrlAnnotated = this::urlSpanAnnotated),
        "Clickable" to Tag( "Clickable", false, TagType.FUNCTION_CLICKABLE, clickable, opening, closing, functionClickable = this::clickableSpan, functionClickableAnnotated = this::clickableSpanAnnotated),
        "Alignment" to Tag( "Alignment", true, TagType.FUNCTION_STRING, alignment, opening, closing, functionString = this::alignmentSpan, functionStringAnnotated = this::alignmentSpanAnnotated)
    )

    data class Tag(
        val spanName: String,
        val isParagraph: Boolean,
        val spanType: TagType,
        var spanCharValue: Pair<TextFieldState, TextFieldState>,
        var openingChar: TextFieldState,
        var closingChar:TextFieldState,
        var function: KFunction2<SpannableStringBuilder, IntRange, SpannableStringBuilder>? = null,
        var functionInt: KFunction3<SpannableStringBuilder, Int, IntRange, SpannableStringBuilder>? = null,
        var functionFloat: KFunction3<SpannableStringBuilder, Float, IntRange, SpannableStringBuilder>? = null,
        var functionColour: KFunction3<SpannableStringBuilder, Color, IntRange, SpannableStringBuilder>? = null,
        var functionImageUri: KFunction4<SpannableStringBuilder, Context, Uri, IntRange, SpannableStringBuilder>? = null,
        var functionUrl: KFunction3<SpannableStringBuilder, String, IntRange, SpannableStringBuilder>? = null,
        var functionClickable: KFunction3<SpannableStringBuilder, () -> Unit, IntRange, SpannableStringBuilder>? = null,
        var functionString: KFunction3<SpannableStringBuilder, String, IntRange, SpannableStringBuilder>? = null,
        var functionRelativeSize: KFunction4<SpannableStringBuilder, Float, Float, IntRange, SpannableStringBuilder>? = null,
        var functionAnnotated: KFunction2<AnnotatedString, IntRange, AnnotatedString>? = null,
        var functionIntAnnotated: KFunction3<AnnotatedString, Int, IntRange, AnnotatedString>? = null,
        var functionFloatAnnotated: KFunction3<AnnotatedString, Float, IntRange, AnnotatedString>? = null,
        var functionColourAnnotated: KFunction3<AnnotatedString, Color, IntRange, AnnotatedString>? = null,
        var functionImageUriAnnotated: KFunction4<AnnotatedString, Context, Uri, IntRange, AnnotatedString>? = null,
        var functionUrlAnnotated: KFunction4<AnnotatedString, Context, String, IntRange, AnnotatedString>? = null,
        var functionClickableAnnotated: KFunction3<AnnotatedString, () -> Unit, IntRange, AnnotatedString>? = null,
        var functionStringAnnotated: KFunction3<AnnotatedString, String, IntRange, AnnotatedString>? = null,
        var functionRelativeSizeAnnotated: KFunction4<AnnotatedString, Float, Float, IntRange, AnnotatedString>? = null,
    ){
        private var openingTag:String = ""
        private var valueClosing:String = ""
        private var closingTag:String = ""
        var value:String = ""
        private var openingRange:Range<Int>? = null
        private var closingRange:Range<Int>? = null
        val hasValue: Boolean
        private var contentRange: IntRange? = null

        private var clickable:(()->Unit)? = null

        init{
            if (function==null){
                if (functionInt==null
                    && functionFloat==null
                    && functionColour==null
                    && functionImageUri==null
                    && functionUrl==null
                    && functionClickable==null
                    && functionString==null
                    && functionRelativeSize==null) throw IllegalArgumentException("No function passed")
                else if (functionInt!=null
                    && functionFloat!=null
                    && functionColour!=null
                    && functionImageUri!=null
                    && functionUrl!=null
                    && functionClickable!=null
                    && functionString!=null
                    && functionRelativeSize!=null) throw IllegalArgumentException("Too many functions passed")
            }
            if (spanCharValue.first.text.isEmpty().not()) {
                openingTag = openingChar.text.toString() + spanCharValue.first.text.toString()
                if (spanType != TagType.FUNCTION) {
                    openingTag += VALUE_OPENING
                    valueClosing = VALUE_CLOSING + closingChar.text.toString()
                } else openingTag += closingChar.text.toString()
                closingTag = openingChar.text.toString() + CLOSING_INDICATOR + spanCharValue.first.text.toString() + closingChar.text.toString()
            }
            hasValue = function==null
            when (spanType) {
                TagType.FUNCTION_IMAGE_URI if spanCharValue.second.text.isEmpty() ->
                    spanCharValue.second.setTextAndPlaceCursorAtEnd("app_picture")
                TagType.FUNCTION_COLOUR if spanCharValue.second.text.isEmpty() ->
                    spanCharValue.second.setTextAndPlaceCursorAtEnd(Color.YELLOW.toString())
                TagType.FUNCTION_STRING if spanCharValue.second.text.isEmpty() ->
                    spanCharValue.second.setTextAndPlaceCursorAtEnd("ALIGN_NORMAL")
                else -> {}
            }
        }

        fun spanIsValid():Boolean{
            return spanCharValue.first.text.isEmpty().not() &&
                    openingChar.text.isEmpty().not() &&
                    closingChar.text.isEmpty().not()
        }

        fun setOpeningRange(range: Range<Int>){
            openingRange = range
            updateContentRange()
        }

        fun setClosingRange(range: Range<Int>){
            closingRange = range
            updateContentRange()
        }

        private fun updateContentRange(){
            contentRange =
                if (openingRange!=null && closingRange != null)
                    IntRange(openingRange!!.lower,closingRange!!.lower)
                else
                    null
        }

        fun setClickable(clickable:(()->Unit)?){ this.clickable = clickable }

        fun getOpeningRange():Range<Int>? { return openingRange }
        fun getClosingRange():Range<Int>? { return closingRange }

        fun getOpeningTag():String { return openingTag }
        fun getValueClosing():String { return valueClosing }
        fun getClosingTag():String { return closingTag }

        fun overlapsTag(inputRange:Range<Int>):Boolean{
            return (openingRange?.contains(inputRange) == true || closingRange?.contains(inputRange) == true )
        }

        private fun shiftOpeningTag(range:Range<Int>){
            if (openingRange!=null) {
                if (!range.contains(openingRange) && !openingRange!!.contains(range)) {
                    if (openingRange!!.lower >= range.upper) {
                        val diff = range.upper-range.lower
                        setOpeningRange(Range(openingRange!!.lower-diff,openingRange!!.upper-diff))
                    }
                }
            }
        }

        fun shiftClosingTag(range:Range<Int>){
            if (closingRange!=null) {
                if (!range.contains(closingRange) && !closingRange!!.contains(range)) {
                    if (closingRange!!.lower >= range.upper) {
                        val diff = range.upper-range.lower
                        setClosingRange(Range(closingRange!!.lower-diff,closingRange!!.upper-diff))
                    }
                }
            }
        }

        fun shiftTags(range: Range<Int>){
            shiftOpeningTag(range)
            shiftClosingTag(range)
        }

        fun applyTag(
            range: IntRange,
            spannableStringBuilder: MutableStateFlow<SpannableStringBuilder>,
            textSize: Float,
            context: Context? = null,
            clickable: (() -> Unit)? = null
        ){
            val storedContentRange = contentRange
            contentRange = range
            applyTag(spannableStringBuilder, textSize, context, clickable)
            contentRange = storedContentRange
        }

        fun applyTag(
            spannableStringBuilder: MutableStateFlow<SpannableStringBuilder>,
            textSize: Float,
            context: Context? = null,
            clickable: (() -> Unit)? = null,
        ) {
            if (isParagraph) {
                if (contentRange != null) {
                    var stringBuilder = spannableStringBuilder.value
                    val storedContentRange = contentRange
                    findParagraphs(
                        spannableStringBuilder.value.toString(),
                        contentRange!!.first,
                        contentRange!!.last
                    ).forEach {
                        contentRange = it
                        stringBuilder = executeFunction(
                            stringBuilder,
                            textSize,
                            context,
                            clickable
                        )
                    }
                    contentRange = storedContentRange
                    spannableStringBuilder.value = stringBuilder
                }
            } else {
                spannableStringBuilder.value = executeFunction(
                    spannableStringBuilder.value,
                    textSize,
                    context,
                    clickable
                )
            }
        }

        fun applyTagAnnotated(
            range: IntRange,
            annotatedString: MutableStateFlow<AnnotatedString>,
            textSize: Float,
            context: Context? = null,
            clickable: (() -> Unit)? = null
        ){
            val storedContentRange = contentRange
            contentRange = range
            applyTagAnnotated(annotatedString, textSize, context, clickable)
            contentRange = storedContentRange
        }

        fun applyTagAnnotated(
            annotatedString: MutableStateFlow<AnnotatedString>,
            textSize: Float,
            context: Context? = null,
            clickable: (() -> Unit)? = null,
        ) {
            if (isParagraph) {
                if (contentRange != null) {
                    var stringBuilder = annotatedString.value
                    val storedContentRange = contentRange
                    findParagraphs(
                        annotatedString.value.text,
                        contentRange!!.first,
                        contentRange!!.last
                    ).forEach {
                        contentRange = it
                        stringBuilder = executeFunction(
                            stringBuilder,
                            textSize,
                            context,
                            clickable
                        )
                    }
                    contentRange = storedContentRange
                    annotatedString.update { stringBuilder }
                }
            } else {
                annotatedString.update {
                    executeFunction(
                        annotatedString.value,
                        textSize,
                        context,
                        clickable
                    )
                }
            }
        }

        private fun executeFunction(
            spannableStringBuilder: SpannableStringBuilder,
            textSize: Float,
            context: Context? = null,
            clickable: (() -> Unit)? = null
        ): SpannableStringBuilder {
            if (contentRange!=null && contentRange!!.last<spannableStringBuilder.length) {
                when (spanType) {
                    TagType.FUNCTION -> return function!!(
                        spannableStringBuilder,
                        contentRange!!
                    )

                    TagType.FUNCTION_INT -> return functionInt!!(
                        spannableStringBuilder,
                        getInt(spanCharValue.second)!!,
                        contentRange!!
                    )

                    TagType.FUNCTION_FLOAT -> return functionFloat!!(
                        spannableStringBuilder,
                        getFloat(spanCharValue.second)!!,
                        contentRange!!
                    )

                    TagType.FUNCTION_COLOUR -> return functionColour!!(
                        spannableStringBuilder,
                        getColour(spanCharValue.second)!!,
                        contentRange!!
                    )

                    TagType.FUNCTION_IMAGE_URI -> if (context != null){
                        return functionImageUri!!(
                            spannableStringBuilder,
                            context,
                            AppResources.ImageResource(spanCharValue.second.text.toString()).getAbsoluteUri(
                                context
                            ),
                            contentRange!!
                        )
                    }
                    TagType.FUNCTION_URL -> return functionUrl!!(
                        spannableStringBuilder,
                        spanCharValue.second.text.toString(),
                        contentRange!!
                    )

                    TagType.FUNCTION_CLICKABLE -> if (clickable != null) return functionClickable!!(
                            spannableStringBuilder,
                            clickable,
                            contentRange!!
                        )

                    TagType.FUNCTION_STRING -> return functionString!!(
                        spannableStringBuilder,
                        spanCharValue.second.text.toString(),
                        contentRange!!
                    )

                    TagType.FUNCTION_RELATIVE_SIZE -> return functionRelativeSize!!(
                        spannableStringBuilder,
                        getFloat(spanCharValue.second)!!,
                        textSize,
                        contentRange!!
                    )
                }
            }
            return spannableStringBuilder
        }

        private fun executeFunction(
            annotatedString: AnnotatedString,
            textSize: Float,
            context: Context? = null,
            clickable: (() -> Unit)? = null
        ): AnnotatedString {
            if (contentRange!=null && contentRange!!.last<annotatedString.length) {
                when (spanType) {
                    TagType.FUNCTION -> return functionAnnotated!!(
                        annotatedString,
                        contentRange!!
                    )

                    TagType.FUNCTION_INT -> return functionIntAnnotated!!(
                        annotatedString,
                        getInt(spanCharValue.second)!!,
                        contentRange!!
                    )

                    TagType.FUNCTION_FLOAT -> return functionFloatAnnotated!!(
                        annotatedString,
                        getFloat(spanCharValue.second)!!,
                        contentRange!!
                    )

                    TagType.FUNCTION_COLOUR -> return functionColourAnnotated!!(
                        annotatedString,
                        getColour(spanCharValue.second)!!,
                        contentRange!!
                    )

                    TagType.FUNCTION_IMAGE_URI -> if (context != null){
                        return functionImageUriAnnotated!!(
                            annotatedString,
                            context,
                            AppResources.ImageResource(spanCharValue.second.text.toString()).getAbsoluteUri(
                                context
                            ),
                            contentRange!!
                        )
                    }
                    TagType.FUNCTION_URL -> if (context != null){ return functionUrlAnnotated!!(
                        annotatedString,
                        context,
                        spanCharValue.second.text.toString(),
                        contentRange!!
                    )}

                    TagType.FUNCTION_CLICKABLE -> if (clickable != null) return functionClickableAnnotated!!(
                        annotatedString,
                        clickable,
                        contentRange!!
                    )

                    TagType.FUNCTION_STRING -> return functionStringAnnotated!!(
                        annotatedString,
                        spanCharValue.second.text.toString(),
                        contentRange!!
                    )

                    TagType.FUNCTION_RELATIVE_SIZE -> return functionRelativeSizeAnnotated!!(
                        annotatedString,
                        getFloat(spanCharValue.second)!!,
                        textSize,
                        contentRange!!
                    )
                }
            }
            return annotatedString
        }
    }

    override fun toString(): String {
        return name.value
    }

    private fun getTags(): List<Tag>{
        val tags:MutableList<Tag> = mutableListOf()
        spans.forEach { (_, tag) ->
            if (tag.spanIsValid()) tags.add(tag)
        }
        return tags
    }

    fun getTagRanges(string: String, removeTagsFromString:Boolean = false):Pair<MutableList<Tag>,String>{
        val ranges:MutableList<Tag> = mutableListOf()
        val newString = MutableStateFlow(string)
        var replacedString = string
        val tags = getTags()

        tags.forEach { tag->
            getTagRange(newString,tag){ openingRange, closingRange, value ->
                tagFunction(newString,tag,ranges,openingRange,closingRange, value)
            }
        }

        if (removeTagsFromString){
            ranges.forEach { tag ->
                replacedString = replacedString.removeRange(tag.getClosingRange()!!.lower,tag.getClosingRange()!!.upper)
                replacedString = replacedString.removeRange(tag.getOpeningRange()!!.lower,tag.getOpeningRange()!!.upper)
                tag.shiftClosingTag(tag.getOpeningRange()!!)
                ranges.forEach { innerTag ->
                    if (tag != innerTag) {
                        innerTag.shiftTags(tag.getOpeningRange()!!)
                        innerTag.shiftTags(tag.getClosingRange()!!)
                    }
                }
            }
        }

        return Pair(ranges, replacedString)
    }

    private fun tagFunction(
        stringInput:MutableStateFlow<String>,
        tagInput:Tag,
        ranges:MutableList<Tag>,
        openingRangeInput:Range<Int>,
        closingRangeInput:Range<Int>,
        valueInput:String
    ){
        tagInput.setOpeningRange(openingRangeInput)
        tagInput.setClosingRange(closingRangeInput)
        tagInput.value = valueInput
        ranges.add(tagInput)
        getTagRange(stringInput,tagInput,openingRangeInput.upper,closingRangeInput.upper){ openingRange, closingRange, value->
            tagFunction(stringInput,tagInput,ranges,openingRange,closingRange, value)
        }
    }

    private fun getTagRange(
        string:MutableStateFlow<String>,
        tag:Tag,
        openingIndexInput:Int = -1,
        closingIndexInput:Int = -1,
        resultFunction:(Range<Int>, Range<Int>, String)->Unit
    ){
        var index = string.value.indexOf(tag.getOpeningTag(), openingIndexInput + 1)
        if (index != -1) {
            val startIndex = index
            if (tag.getValueClosing().isNotEmpty()) {
                index = string.value.indexOf(tag.getValueClosing(), index + 1)
                index = index + tag.getValueClosing().length-1
                if (index != -1) index++
            } else {
                index += tag.getOpeningTag().length
            }
            if (index != -1 && index < string.value.length) {
                val openingTagRange = Range(startIndex, index)
                var value = ""
                if (tag.getValueClosing().isNotEmpty()) {
                    value = string.value.substring(startIndex + tag.getOpeningTag().length, index - 1)
                }

                index = string.value.indexOf(tag.getClosingTag(), closingIndexInput + 1)
                if (index != -1 && index > openingTagRange.upper) {
                    val closingStartIndex = index
                    index += tag.getClosingTag().length
                    val closingTagRange = Range(closingStartIndex, index)

                    if (index < string.value.length) resultFunction(
                        openingTagRange,
                        closingTagRange,
                        value
                    )
                }
            }
        }
    }

    fun getCards():ArrayList<MarkupLanguageCard>{
        val list = ArrayList<MarkupLanguageCard>()
        spans.forEach { (_, tag) ->
            var text = EXAMPLE_TEXT
            if (tag.isParagraph) text = EXAMPLE_PARAGRAPH
            list.add(
                MarkupLanguageCard(
                    name.value,
                    tag,
                    text
                )
            )
        }
        return list
    }

    fun relativeSizeSpan(spannableStringBuilder: SpannableStringBuilder, proportion:Float, textSize:Float, range:IntRange):SpannableStringBuilder{
        spannableStringBuilder[range] = RelativeSizeSpan(proportion)
        return spannableStringBuilder
    }

    fun absoluteSizeSpan(spannableStringBuilder: SpannableStringBuilder, size:Int, range:IntRange):SpannableStringBuilder{
        spannableStringBuilder[range] = AbsoluteSizeSpan(size)
        return spannableStringBuilder
    }

    fun scaleXSpan(spannableStringBuilder: SpannableStringBuilder, scale:Float, range:IntRange):SpannableStringBuilder{
        spannableStringBuilder[range] = ScaleXSpan(scale)
        return spannableStringBuilder
    }

    fun boldSpan(spannableStringBuilder: SpannableStringBuilder, range:IntRange):SpannableStringBuilder{
        // make text bold
        spannableStringBuilder[range] = StyleSpan(Typeface.BOLD)
        return spannableStringBuilder
    }

    fun italicSpan(spannableStringBuilder: SpannableStringBuilder, range:IntRange):SpannableStringBuilder{
        // make text italic
        spannableStringBuilder[range] = StyleSpan(Typeface.ITALIC)
        return spannableStringBuilder
    }

    fun underlineSpan(spannableStringBuilder: SpannableStringBuilder, range:IntRange):SpannableStringBuilder{
        // underline text
        spannableStringBuilder[range] = UnderlineSpan()
        return spannableStringBuilder
    }

    fun strikethroughSpan(spannableStringBuilder: SpannableStringBuilder, range:IntRange):SpannableStringBuilder{
        spannableStringBuilder[range] = StrikethroughSpan()
        return spannableStringBuilder
    }

    fun superscriptSpan(spannableStringBuilder: SpannableStringBuilder, range:IntRange):SpannableStringBuilder{
        // superscript
        spannableStringBuilder[range] = SuperscriptSpan()
        return spannableStringBuilder
    }

    fun subscriptSpan(spannableStringBuilder: SpannableStringBuilder, range:IntRange):SpannableStringBuilder{
        // subscript
        spannableStringBuilder[range] = SubscriptSpan()
        return spannableStringBuilder
    }

    fun colourTextSpan(spannableStringBuilder: SpannableStringBuilder, inputColour:Color, range:IntRange):SpannableStringBuilder{
        // change text color
        spannableStringBuilder[range] = ForegroundColorSpan(inputColour.toArgb())
        return spannableStringBuilder
    }

    fun highlightSpan(spannableStringBuilder: SpannableStringBuilder, inputColour: Color,range: IntRange):SpannableStringBuilder{
        // highlight text
        spannableStringBuilder[range] = BackgroundColorSpan(inputColour.toArgb())
        return spannableStringBuilder
    }

    fun urlSpan(spannableStringBuilder: SpannableStringBuilder, inputWebsite:String,range:IntRange):SpannableStringBuilder{
        // url
        var website = inputWebsite
        if (!website.contains("https://")) website = "https://$website"
        spannableStringBuilder[range] = URLSpan(website)
        return spannableStringBuilder
    }

    fun clickableSpan(spannableStringBuilder: SpannableStringBuilder, clickable: ()->Unit,range:IntRange):SpannableStringBuilder{
        val urlClickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                clickable()
            }
        }
        spannableStringBuilder[range] = urlClickableSpan
        return spannableStringBuilder
    }

    fun quoteSpan(spannableStringBuilder: SpannableStringBuilder, inputColour:Color, range:IntRange):SpannableStringBuilder{
        spannableStringBuilder[range] = QuoteSpan(inputColour.toArgb(),20,20)
        return spannableStringBuilder
    }

    fun bulletPointSpan(spannableStringBuilder: SpannableStringBuilder, range:IntRange):SpannableStringBuilder{
        spannableStringBuilder[range] = BulletSpan(20,Color.BLACK,20)
        return spannableStringBuilder
    }

    fun bulletPointColourSpan(spannableStringBuilder: SpannableStringBuilder, inputColour:Color,range:IntRange):SpannableStringBuilder{
        spannableStringBuilder[range] = BulletSpan(20,inputColour.toArgb(),20)
        return spannableStringBuilder
    }

    fun lineHeightSpan(spannableStringBuilder: SpannableStringBuilder, height:Int, range:IntRange):SpannableStringBuilder{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            spannableStringBuilder[range] = LineHeightSpan.Standard(height)
        }
        return spannableStringBuilder
    }

    fun drawableMarginSpan(spannableStringBuilder: SpannableStringBuilder, context: Context, uri: Uri, range:IntRange):SpannableStringBuilder{
        AppResources.getDrawableFromUri( context, uri)?.let {
            spannableStringBuilder[range] = DrawableMarginSpan(it)
        }
        return spannableStringBuilder
    }

    fun iconMarginSpan(spannableStringBuilder: SpannableStringBuilder, context: Context, uri: Uri, range:IntRange):SpannableStringBuilder{
        AppResources.getBitmapFromUri( context, uri)?.let {
            spannableStringBuilder[range] = IconMarginSpan(it)
        }
        return spannableStringBuilder
    }

    fun dynamicDrawableSpan(spannableStringBuilder: SpannableStringBuilder, context: Context, uri: Uri,range:IntRange):SpannableStringBuilder{
        AppResources.getDrawableFromUri( context, uri)?.let {
            spannableStringBuilder[range] = CustomDrawableSpan(it)
        }
        return spannableStringBuilder
    }

    fun imageSpan(spannableStringBuilder: SpannableStringBuilder, context: Context, uri: Uri, range:IntRange):SpannableStringBuilder{
        AppResources.getBitmapFromUri( context, uri)?.let {
            spannableStringBuilder[range] = ImageSpan(context,it)
        }
        return spannableStringBuilder
    }

    fun leadingMarginSpan(spannableStringBuilder: SpannableStringBuilder, lead:Int, range:IntRange):SpannableStringBuilder{
        spannableStringBuilder[range] = LeadingMarginSpan.Standard(lead,50)
        return spannableStringBuilder
    }

    fun tabStopSpan(spannableStringBuilder: SpannableStringBuilder, offset: Int, range:IntRange):SpannableStringBuilder{
        spannableStringBuilder[range] = TabStopSpan.Standard(offset)
        return spannableStringBuilder
    }

    fun lineBackgroundSpan(spannableStringBuilder: SpannableStringBuilder, colour: Color,range:IntRange):SpannableStringBuilder{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            spannableStringBuilder[range] = LineBackgroundSpan.Standard(colour.toArgb())
        }
        return spannableStringBuilder
    }

    fun alignmentSpan(spannableStringBuilder: SpannableStringBuilder, alignment: String, range:IntRange):SpannableStringBuilder{
        spannableStringBuilder[range] = AlignmentSpan.Standard(Layout.Alignment.valueOf(alignment))
        return spannableStringBuilder
    }

    class CustomDrawableSpan(private val drawable: Drawable) :
        DynamicDrawableSpan() {
        override fun getDrawable(): Drawable {
            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            return drawable
        }

        override fun draw(
            canvas: Canvas,
            text: CharSequence,
            start: Int,
            end: Int,
            x: Float,
            top: Int,
            y: Int,
            bottom: Int,
            paint: Paint
        ) {
            val drawable = getDrawable()
            canvas.save()
            val transY = bottom - drawable.bounds.bottom
            canvas.translate(x, transY.toFloat())
            drawable.draw(canvas)
            canvas.restore()
        }
    }

    fun relativeSizeSpanAnnotated(annotatedString: AnnotatedString, proportion:Float, textSize: Float, range:IntRange):AnnotatedString{
        val baseFontSize = TextUnit(textSize, TextUnitType.Sp)
        val builder = AnnotatedString.Builder(annotatedString)
        val style = SpanStyle(fontSize = baseFontSize * proportion)
        builder.addStyle(style, range.first, range.last)
        return builder.toAnnotatedString()
    }

    fun absoluteSizeSpanAnnotated(annotatedString: AnnotatedString, size:Int, range:IntRange):AnnotatedString{
        val builder = AnnotatedString.Builder(annotatedString)
        val style = SpanStyle( fontSize = size.sp)
        builder.addStyle(style, range.first, range.last)
        return builder.toAnnotatedString()
    }

    fun scaleXSpanAnnotated(annotatedString: AnnotatedString, scale:Float, range:IntRange):AnnotatedString{
        val builder = AnnotatedString.Builder(annotatedString)
        val style = SpanStyle(
            textGeometricTransform = TextGeometricTransform(scaleX = scale)
        )
        builder.addStyle(style, range.first, range.last)
        return builder.toAnnotatedString()
    }

    fun boldSpanAnnotated(annotatedString: AnnotatedString, range:IntRange):AnnotatedString{
        // make text bold
        val builder = AnnotatedString.Builder(annotatedString)
        val style = SpanStyle(fontWeight = FontWeight.Bold)
        builder.addStyle(style, range.first, range.last)
        return builder.toAnnotatedString()
    }

    fun italicSpanAnnotated(annotatedString: AnnotatedString, range:IntRange):AnnotatedString{
        // make text italic
        val builder = AnnotatedString.Builder(annotatedString)
        val style = SpanStyle(fontStyle = FontStyle.Italic)
        builder.addStyle(style, range.first, range.last)
        return builder.toAnnotatedString()
    }

    fun underlineSpanAnnotated(annotatedString: AnnotatedString, range:IntRange):AnnotatedString{
        // underline text
        val builder = AnnotatedString.Builder(annotatedString)
        val style = SpanStyle(textDecoration = TextDecoration.Underline)
        builder.addStyle(style, range.first, range.last)
        return builder.toAnnotatedString()
    }

    fun strikethroughSpanAnnotated(annotatedString: AnnotatedString, range:IntRange):AnnotatedString{
        val builder = AnnotatedString.Builder(annotatedString)
        val style = SpanStyle(textDecoration = TextDecoration.LineThrough)
        builder.addStyle(style, range.first, range.last)
        return builder.toAnnotatedString()
    }

    fun superscriptSpanAnnotated(annotatedString: AnnotatedString, range:IntRange):AnnotatedString{
        // superscript
        val builder = AnnotatedString.Builder(annotatedString)
        val style = SpanStyle( fontSize = 12.sp,
            baselineShift = BaselineShift.Superscript
        )
        builder.addStyle(style, range.first, range.last)
        return builder.toAnnotatedString()
    }

    fun subscriptSpanAnnotated(annotatedString: AnnotatedString, range:IntRange):AnnotatedString{
        // subscript
        val builder = AnnotatedString.Builder(annotatedString)
        val style = SpanStyle( fontSize = 12.sp,
            baselineShift = BaselineShift.Subscript
        )
        builder.addStyle(style, range.first, range.last)
        return builder.toAnnotatedString()
    }

    fun colourTextSpanAnnotated(annotatedString: AnnotatedString, inputColour:Color, range:IntRange):AnnotatedString{
        // change text color
        val builder = AnnotatedString.Builder(annotatedString)
        val style = SpanStyle(color = androidx.compose.ui.graphics.Color(inputColour.toArgb()))
        builder.addStyle(style, range.first, range.last)
        return builder.toAnnotatedString()
    }

    fun highlightSpanAnnotated(annotatedString: AnnotatedString, inputColour: Color,range: IntRange):AnnotatedString{
        // highlight text
        val builder = AnnotatedString.Builder(annotatedString)
        val style = SpanStyle(background = androidx.compose.ui.graphics.Color(inputColour.toArgb()))
        builder.addStyle(style, range.first, range.last)
        return builder.toAnnotatedString()
    }

    fun urlSpanAnnotated(annotatedString: AnnotatedString, context: Context, inputWebsite:String, range:IntRange):AnnotatedString{
        // url
        var website = inputWebsite
        if (!website.contains("https://")) website = "https://$website"

        val builder = AnnotatedString.Builder(annotatedString)
        val style = SpanStyle(
            color = androidx.compose.ui.graphics.Color.Blue,
            textDecoration = TextDecoration.Underline
        )
        builder.addLink(LinkAnnotation.Url(
            url = website,
            styles = TextLinkStyles(style = style),
            linkInteractionListener = {
                val intent = Intent(Intent.ACTION_VIEW, website.toUri())
                context.startActivity(intent)
            }
        ),
            range.first, range.last)
        return builder.toAnnotatedString()
    }

    fun clickableSpanAnnotated(annotatedString: AnnotatedString, clickable: ()->Unit,range:IntRange):AnnotatedString{
        val builder = AnnotatedString.Builder(annotatedString)
        val style = SpanStyle(
            color = androidx.compose.ui.graphics.Color.Red,
            textDecoration = TextDecoration.Underline
        )
        builder.addLink(LinkAnnotation.Clickable(
            tag = "CLICKABLE",
            styles = TextLinkStyles(style = style),
            linkInteractionListener = { clickable() }
        ),
            range.first, range.last)
        return builder.toAnnotatedString()
    }

    fun quoteSpanAnnotated(annotatedString: AnnotatedString, inputColour:Color, range:IntRange):AnnotatedString{
        val builder = AnnotatedString.Builder(annotatedString)
        val paragraphStyle = ParagraphStyle(
            textIndent = TextIndent(restLine = 16.sp)
        )
        val style = SpanStyle( background = androidx.compose.ui.graphics.Color(0xFFEFEFEF),
            color = androidx.compose.ui.graphics.Color(inputColour.toArgb())
        )
        builder.addStyle(style, range.first, range.last)
        builder.addStyle(paragraphStyle, range.first, range.last)
        return builder.toAnnotatedString()
    }

    fun bulletPointSpanAnnotated(annotatedString: AnnotatedString, range:IntRange):AnnotatedString{
        val builder = AnnotatedString.Builder(annotatedString)
        builder.addBullet(
            Bullet(
                shape = CircleShape,
                width = TextUnit(20f, TextUnitType.Sp),
                height = TextUnit(20f, TextUnitType.Sp),
                padding = TextUnit(5f, TextUnitType.Sp)
            ),
            TextUnit(20f, TextUnitType.Sp),
            range.first,
            range.last
        )
        return builder.toAnnotatedString()
    }

    fun bulletPointColourSpanAnnotated(annotatedString: AnnotatedString, inputColour:Color,range:IntRange):AnnotatedString{
        val builder = AnnotatedString.Builder(annotatedString)
        builder.addBullet(
            Bullet(
                shape = CircleShape,
                width = TextUnit(20f, TextUnitType.Sp),
                height = TextUnit(20f, TextUnitType.Sp),
                padding = TextUnit(5f, TextUnitType.Sp)
            ),
            TextUnit(20f, TextUnitType.Sp),
            range.first,
            range.last
        )
        return builder.toAnnotatedString()
    }

    fun lineHeightSpanAnnotated(annotatedString: AnnotatedString, height:Int, range:IntRange):AnnotatedString{
        val builder = AnnotatedString.Builder(annotatedString)
        val style = ParagraphStyle(lineHeight = height.sp)
        builder.addStyle(style, range.first, range.last)
        return builder.toAnnotatedString()
    }

    fun drawableMarginSpanAnnotated(annotatedString: AnnotatedString, context: Context, uri: Uri, range:IntRange):AnnotatedString{
        val builder = AnnotatedString.Builder(annotatedString)
        /*AppResources.getDrawableFromUri( context, uri)?.let {
            spannableStringBuilder[range] = DrawableMarginSpan(it)
        }*/
        return builder.toAnnotatedString()
    }

    fun iconMarginSpanAnnotated(annotatedString: AnnotatedString, context: Context, uri: Uri, range:IntRange):AnnotatedString{
        val builder = AnnotatedString.Builder(annotatedString)
        /*AppResources.getBitmapFromUri( context, uri)?.let {
         //   spannableStringBuilder[range] = IconMarginSpan(it)
        }*/
        return builder.toAnnotatedString()
    }

    fun dynamicDrawableSpanAnnotated(annotatedString: AnnotatedString, context: Context, uri: Uri,range:IntRange):AnnotatedString{
        val builder = AnnotatedString.Builder(annotatedString)
        /*AppResources.getDrawableFromUri( context, uri)?.let {
            spannableStringBuilder[range] = CustomDrawableSpan(it)
        }*/
        return builder.toAnnotatedString()
    }

    fun imageSpanAnnotated(annotatedString: AnnotatedString, context: Context, uri: Uri, range:IntRange):AnnotatedString{
        val builder = AnnotatedString.Builder(annotatedString)
        /*AppResources.getBitmapFromUri( context, uri)?.let {
            spannableStringBuilder[range] = ImageSpan(context,it)
        }*/
        return builder.toAnnotatedString()
    }

    fun leadingMarginSpanAnnotated(annotatedString: AnnotatedString, lead:Int, range:IntRange):AnnotatedString{
        val builder = AnnotatedString.Builder(annotatedString)
        val style = ParagraphStyle(
            textIndent = TextIndent( firstLine = lead.sp, restLine = 50.sp )
        )
        builder.addStyle(style, range.first, range.last)
        return builder.toAnnotatedString()
    }

    fun tabStopSpanAnnotated(annotatedString: AnnotatedString, offset: Int, range:IntRange):AnnotatedString{
        val builder = AnnotatedString.Builder(annotatedString)
        /*val style = ParagraphStyle(
            tabStops = listOf( TabStop(offset.sp))
        )
        builder.addStyle(style, range.first, range.last)*/
        return builder.toAnnotatedString()
    }

    fun lineBackgroundSpanAnnotated(annotatedString: AnnotatedString, colour: Color,range:IntRange):AnnotatedString{
        val builder = AnnotatedString.Builder(annotatedString)
        val style = SpanStyle(
            background = androidx.compose.ui.graphics.Color(colour.toArgb()),
            color = androidx.compose.ui.graphics.Color.Black
        )
        builder.addStyle(style, range.first, range.last)
        return builder.toAnnotatedString()
    }

    fun alignmentSpanAnnotated(annotatedString: AnnotatedString, alignment: String, range:IntRange):AnnotatedString{
        val builder = AnnotatedString.Builder(annotatedString)
        val style = ParagraphStyle(textAlign = when(Layout.Alignment.valueOf(alignment)){
            Layout.Alignment.ALIGN_CENTER -> TextAlign.Center
            Layout.Alignment.ALIGN_NORMAL -> TextAlign.Start
            Layout.Alignment.ALIGN_OPPOSITE -> TextAlign.End
        })
        builder.addStyle(style, range.first, range.last)
        return builder.toAnnotatedString()
    }
}