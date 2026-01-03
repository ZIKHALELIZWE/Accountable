package com.thando.accountable.database.tables

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
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
import androidx.compose.ui.text.AnnotatedString
import androidx.core.text.set
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivity
import com.thando.accountable.R
import com.thando.accountable.recyclerviewadapters.MarkupLanguageCardAdapter.Companion.EXAMPLE_PARAGRAPH
import com.thando.accountable.recyclerviewadapters.MarkupLanguageCardAdapter.Companion.EXAMPLE_TEXT
import com.thando.accountable.ui.cards.MarkupLanguageCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3
import kotlin.reflect.KFunction4

// we will use '=()' for Spans with values
@Entity(tableName = "markup_language_table")
data class MarkupLanguage(
    @PrimaryKey
    var name: MutableStateFlow<String> = MutableStateFlow(MainActivity.ResourceProvider.getString(R.string.new_markup_language)),

    @ColumnInfo(name = "opening")
    var opening: MutableStateFlow<String> = MutableStateFlow(""),

    @ColumnInfo(name = "closing")
    var closing: MutableStateFlow<String> = MutableStateFlow(""),

    @ColumnInfo(name = "relative_size")
    var relativeSize: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "absolute_size")
    var absoluteSize: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "scale_x")
    var scaleX: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "bold")
    var bold: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "italic")
    var italic: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "underline")
    var underline: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "strikethrough")
    var strikethrough: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "superscript")
    var superscript: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "subscript")
    var subscript: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "colour_text")
    var colourText: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "highlight")
    var highlight: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "url")
    var url: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "clickable")
    var clickable: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "dynamic_drawable")
    var dynamicDrawable: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "image")
    var image: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "alignment")
    var alignment: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "quote")
    var quote: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "bullet_point")
    var bulletPoint: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "bullet_point_colour")
    var bulletPointColour: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "line_height")
    var lineHeight: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "drawable_margin")
    var drawableMargin: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "icon_margin")
    var iconMargin: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "leading_margin")
    var leadingMargin: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "tab_stop")
    var tabStop: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow("")),

    @ColumnInfo(name = "line_background")
    var lineBackground: Pair<MutableStateFlow<String>,MutableStateFlow<String>> = Pair(MutableStateFlow(""),MutableStateFlow(""))
)
{
    companion object{
        const val VALUE_OPENING = "=("
        const val VALUE_CLOSING = ")"
        const val CLOSING_INDICATOR = "/"

        fun getAlignmentMenuOnClick(
            context: Context,
            view: View,
            alignNormal: () -> Unit,
            alignCenter: () -> Unit,
            alignOpposite: () -> Unit
        ):()->Unit {
            return {
                val popup = PopupMenu(context, view)
                popup.menuInflater.inflate(R.menu.text_alignment_menu, popup.menu)
                popup.setOnMenuItemClickListener { item: MenuItem ->
                    when (item.itemId) {
                        R.id.alignment_menu_normal -> {
                            alignNormal()
                            true
                        }

                        R.id.alignment_menu_center -> {
                            alignCenter()
                            true
                        }

                        R.id.alignment_menu_opposite -> {
                            alignOpposite()
                            true
                        }

                        else -> false
                    }
                }
                popup.show()
            }
        }

        fun getFloat(inputFloat: MutableStateFlow<String>): Float? {
            return inputFloat.value.toFloatOrNull()
        }

        fun getInt(inputInt: MutableStateFlow<String>): Int? {
            return inputInt.value.toIntOrNull()
        }

        fun getColour(inputColour: MutableStateFlow<String>): Color? {
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
        FUNCTION_URL, FUNCTION_CLICKABLE, FUNCTION_STRING
    }

    @Ignore
    private val spans = mapOf(
        "Bold" to Tag("Bold", false, TagType.FUNCTION, bold, opening, closing, function = this::boldSpan),
        "Italic" to Tag( "Italic",false, TagType.FUNCTION, italic, opening, closing, function = this::italicSpan),
        "Underline" to Tag( "Underline",false, TagType.FUNCTION, underline, opening, closing, function = this::underlineSpan),
        "Strikethrough" to Tag( "Strikethrough", false, TagType.FUNCTION, strikethrough, opening, closing, function = this::strikethroughSpan),
        "Superscript" to Tag( "Superscript",false, TagType.FUNCTION, superscript, opening, closing, function = this::superscriptSpan),
        "Subscript" to Tag( "Subscript",false, TagType.FUNCTION, subscript, opening, closing, function = this::subscriptSpan),
        "Bullet Point" to Tag( "Bullet Point",true, TagType.FUNCTION, bulletPoint, opening, closing, function = this::bulletPointSpan),
        "Absolute Size" to Tag( "Absolute Size",false, TagType.FUNCTION_INT, absoluteSize, opening, closing, functionInt = this::absoluteSizeSpan),
        "Line Height" to Tag( "Line Height", true, TagType.FUNCTION_INT, lineHeight, opening, closing, functionInt = this::lineHeightSpan),
        "Leading Margin" to Tag( "Leading Margin", true, TagType.FUNCTION_INT, leadingMargin, opening, closing, functionInt = this::leadingMarginSpan),
        "Tab Stop" to Tag( "Tab Stop", true, TagType.FUNCTION_INT, tabStop, opening, closing, functionInt = this::tabStopSpan),
        "Relative Size" to Tag( "Relative Size", false, TagType.FUNCTION_FLOAT, relativeSize, opening, closing, functionFloat = this::relativeSizeSpan),
        "Scale X" to Tag( "Scale X", false, TagType.FUNCTION_FLOAT, scaleX, opening, closing, functionFloat = this::scaleXSpan),
        "Colour Text" to Tag( "Colour Text", false, TagType.FUNCTION_COLOUR, colourText, opening, closing, functionColour = this::colourTextSpan),
        "Highlight" to Tag( "Highlight", false, TagType.FUNCTION_COLOUR, highlight, opening, closing, functionColour = this::highlightSpan),
        "Quote" to Tag( "Quote", true, TagType.FUNCTION_COLOUR, quote, opening, closing, functionColour = this::quoteSpan),
        "Bullet Point Colour" to Tag( "Bullet Point Colour", true, TagType.FUNCTION_COLOUR, bulletPointColour, opening, closing, functionColour = this::bulletPointColourSpan),
        "Line Background" to Tag( "Line Background", true, TagType.FUNCTION_COLOUR, lineBackground, opening, closing, functionColour = this::lineBackgroundSpan),
        "Dynamic Drawable" to Tag( "Dynamic Drawable", false, TagType.FUNCTION_IMAGE_URI, dynamicDrawable, opening, closing, functionImageUri = this::dynamicDrawableSpan),
        "Image" to Tag( "Image", false, TagType.FUNCTION_IMAGE_URI, image, opening, closing, functionImageUri = this::imageSpan),
        "Drawable Margin" to Tag( "Drawable Margin", true, TagType.FUNCTION_IMAGE_URI, drawableMargin, opening, closing, functionImageUri = this::drawableMarginSpan),
        "Icon Margin" to Tag( "Icon Margin", true, TagType.FUNCTION_IMAGE_URI, iconMargin, opening, closing, functionImageUri = this::iconMarginSpan),
        "Url" to Tag( "Url", false, TagType.FUNCTION_URL, url, opening, closing, functionUrl = this::urlSpan),
        "Clickable" to Tag( "Clickable", false, TagType.FUNCTION_CLICKABLE, clickable, opening, closing, functionClickable = this::clickableSpan),
        "Alignment" to Tag( "Alignment", true, TagType.FUNCTION_STRING, alignment, opening, closing, functionString = this::alignmentSpan)
    )

    data class Tag(
        val spanName: String,
        val isParagraph: Boolean,
        val spanType: TagType,
        var spanCharValue: Pair<MutableStateFlow<String>,MutableStateFlow<String>>,
        var openingChar:MutableStateFlow<String>,
        var closingChar:MutableStateFlow<String>,
        var function: KFunction2<SpannableStringBuilder, IntRange, SpannableStringBuilder>? = null,
        var functionInt: KFunction3<SpannableStringBuilder, Int, IntRange, SpannableStringBuilder>? = null,
        var functionFloat: KFunction3<SpannableStringBuilder, Float, IntRange, SpannableStringBuilder>? = null,
        var functionColour: KFunction3<SpannableStringBuilder, Color, IntRange, SpannableStringBuilder>? = null,
        var functionImageUri: KFunction4<SpannableStringBuilder, Context, Uri, IntRange, SpannableStringBuilder>? = null,
        var functionUrl: KFunction3<SpannableStringBuilder, String, IntRange, SpannableStringBuilder>? = null,
        var functionClickable: KFunction3<SpannableStringBuilder, () -> Unit, IntRange, SpannableStringBuilder>? = null,
        var functionString: KFunction3<SpannableStringBuilder, String, IntRange, SpannableStringBuilder>? = null
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
                    && functionString==null) throw IllegalArgumentException("No function passed")
                else if (functionInt!=null
                    && functionFloat!=null
                    && functionColour!=null
                    && functionImageUri!=null
                    && functionUrl!=null
                    && functionClickable!=null
                    && functionString!=null) throw IllegalArgumentException("Too many functions passed")
            }
            if (spanCharValue.first.value.isEmpty().not()) {
                openingTag = openingChar.value + spanCharValue.first.value
                if (spanType != TagType.FUNCTION) {
                    openingTag += VALUE_OPENING
                    valueClosing = VALUE_CLOSING + closingChar.value
                } else openingTag += closingChar.value
                closingTag = openingChar.value + CLOSING_INDICATOR + spanCharValue.first.value + closingChar.value
            }
            hasValue = function==null
            if (spanType == TagType.FUNCTION_IMAGE_URI && spanCharValue.second.value.isEmpty()) spanCharValue.second.value = "app_picture"
            else if (spanType == TagType.FUNCTION_COLOUR && spanCharValue.second.value.isEmpty()) spanCharValue.second.value = Color.YELLOW.toString()
            else if (spanType == TagType.FUNCTION_STRING && spanCharValue.second.value.isEmpty()) spanCharValue.second.value = "ALIGN_NORMAL"
        }

        fun spanIsValid():Boolean{
            return spanCharValue.first.value.isEmpty().not() &&
                    openingChar.value.isEmpty().not() &&
                    closingChar.value.isEmpty().not()
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
            context: Context? = null,
            clickable: (() -> Unit)? = null
        ){
            val storedContentRange = contentRange
            contentRange = range
            applyTag(spannableStringBuilder, context, clickable)
            contentRange = storedContentRange
        }

        fun applyTag(
            spannableStringBuilder: MutableStateFlow<SpannableStringBuilder>,
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
                    context,
                    clickable
                )
            }
        }

        fun applyTagAnnotated(
            annotatedString: MutableStateFlow<AnnotatedString>,
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
                            context,
                            clickable
                        )
                    }
                    contentRange = storedContentRange
                    annotatedString.value = stringBuilder
                }
            } else {
                annotatedString.value = executeFunction(
                    annotatedString.value,
                    context,
                    clickable
                )
            }
        }

        private fun executeFunction(
            spannableStringBuilder: SpannableStringBuilder,
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
                            AppResources.ImageResource(spanCharValue.second.value).getAbsoluteUri(
                                context
                            ),
                            contentRange!!
                        )
                    }
                    TagType.FUNCTION_URL -> return functionUrl!!(
                        spannableStringBuilder,
                        spanCharValue.second.value,
                        contentRange!!
                    )

                    TagType.FUNCTION_CLICKABLE -> if (clickable != null) return functionClickable!!(
                            spannableStringBuilder,
                            clickable,
                            contentRange!!
                        )

                    TagType.FUNCTION_STRING -> return functionString!!(
                        spannableStringBuilder,
                        spanCharValue.second.value,
                        contentRange!!
                    )
                }
            }
            return spannableStringBuilder
        }

        private fun executeFunction(
            annotatedString: AnnotatedString,
            context: Context? = null,
            clickable: (() -> Unit)? = null
        ): AnnotatedString {
            /*if (contentRange!=null && contentRange!!.last<annotatedString.length) {
                when (spanType) {
                    TagType.FUNCTION -> return function!!(
                        annotatedString,
                        contentRange!!
                    )

                    TagType.FUNCTION_INT -> return functionInt!!(
                        annotatedString,
                        getInt(spanCharValue.second)!!,
                        contentRange!!
                    )

                    TagType.FUNCTION_FLOAT -> return functionFloat!!(
                        annotatedString,
                        getFloat(spanCharValue.second)!!,
                        contentRange!!
                    )

                    TagType.FUNCTION_COLOUR -> return functionColour!!(
                        annotatedString,
                        getColour(spanCharValue.second)!!,
                        contentRange!!
                    )

                    TagType.FUNCTION_IMAGE_URI -> if (context != null){
                        return functionImageUri!!(
                            annotatedString,
                            context,
                            AppResources.ImageResource(spanCharValue.second.value).getAbsoluteUri(
                                context
                            ),
                            contentRange!!
                        )
                    }
                    TagType.FUNCTION_URL -> return functionUrl!!(
                        annotatedString,
                        spanCharValue.second.value,
                        contentRange!!
                    )

                    TagType.FUNCTION_CLICKABLE -> if (clickable != null) return functionClickable!!(
                        annotatedString,
                        clickable,
                        contentRange!!
                    )

                    TagType.FUNCTION_STRING -> return functionString!!(
                        annotatedString,
                        spanCharValue.second.value,
                        contentRange!!
                    )
                }
            }*/
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

    fun relativeSizeSpan(spannableStringBuilder: SpannableStringBuilder, proportion:Float, range:IntRange):SpannableStringBuilder{
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            spannableStringBuilder[range] = QuoteSpan(inputColour.toArgb(),20,20)
        }
        else spannableStringBuilder[range] = QuoteSpan(inputColour.toArgb())
        return spannableStringBuilder
    }

    fun bulletPointSpan(spannableStringBuilder: SpannableStringBuilder, range:IntRange):SpannableStringBuilder{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            spannableStringBuilder[range] = BulletSpan(20,Color.BLACK,20)
        }
        return spannableStringBuilder
    }

    fun bulletPointColourSpan(spannableStringBuilder: SpannableStringBuilder, inputColour:Color,range:IntRange):SpannableStringBuilder{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            spannableStringBuilder[range] = BulletSpan(20,inputColour.toArgb(),20)
        }
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
            paint: android.graphics.Paint
        ) {
            val drawable = getDrawable()
            canvas.save()
            val transY = bottom - drawable.bounds.bottom
            canvas.translate(x, transY.toFloat())
            drawable.draw(canvas)
            canvas.restore()
        }
    }
}