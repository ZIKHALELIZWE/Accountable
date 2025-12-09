package com.thando.accountable.recyclerviewadapters

import android.content.Context
import android.text.InputType
import android.text.Layout
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thando.accountable.MainActivity
import com.thando.accountable.R
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.databinding.MarkupLanguageCardBinding
import com.thando.accountable.recyclerviewadapters.diffutils.MarkupLanguageCardDiffItemCallback
import com.thando.accountable.ui.cards.Colour
import com.thando.accountable.ui.cards.MarkupLanguageCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MarkupLanguageCardAdapter(
    private val context: Context
): ListAdapter<MarkupLanguageCard, MarkupLanguageCardAdapter.MarkupLanguageCardViewHolder>(MarkupLanguageCardDiffItemCallback()) {

    companion object {
        const val EXAMPLE_SPAN = "bi"
        const val EXAMPLE_TEXT = "This is example text."
        const val EXAMPLE_PARAGRAPH = "This\t is the first paragraph.\n\nThis\t is the second paragraph.\n\nThis\t is the third paragraph."
    }

    private var cardsList: MutableList<MarkupLanguageCard> = mutableListOf()
    private var markupLanguage: MarkupLanguage? = null

    init {
        submitList(cardsList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkupLanguageCardViewHolder {
        return inflateCard(parent)
    }

    override fun onBindViewHolder(holder: MarkupLanguageCardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: MarkupLanguageCardViewHolder) {
        holder.unbind()
        super.onViewRecycled(holder)
    }

    fun spansNotSimilar(): List<String> {
        val similarList = ArrayList<String>()
        for ((index,card) in cardsList.withIndex()){
            if (card.tag.spanCharValue.first.value.isEmpty()) continue
            if (card.tag.spanCharValue.first.value == markupLanguage?.opening?.value) if (!similarList.contains("Opening")){
                similarList.add("Opening")
                if (!similarList.contains(card.tag.spanName)) similarList.add(card.tag.spanName)
            }
            if (card.tag.spanCharValue.first.value == markupLanguage?.closing?.value) if (!similarList.contains("Closing")){
                similarList.add("Closing")
                if (!similarList.contains(card.tag.spanName)) similarList.add(card.tag.spanName)
            }
            if (index == cardsList.size-1) continue
            cardsList.forEachIndexed { innerIndex, innerCard ->
                if (innerIndex>index && spanCharSimilar(card,innerCard)) {
                    if (!similarList.contains(card.tag.spanName)) similarList.add(card.tag.spanName)
                    if (!similarList.contains(innerCard.tag.spanName)) similarList.add(innerCard.tag.spanName)
                }
            }
        }
        return similarList
    }

    private fun spanCharSimilar(originalCard:MarkupLanguageCard, checkCard:MarkupLanguageCard):Boolean{
        val original = originalCard.tag
        val check = checkCard.tag
        return original.spanName!=check.spanName && original.spanCharValue.first.value.isNotEmpty() && check.spanCharValue.first.value.isNotEmpty() && original.spanCharValue.first.value == check.spanCharValue.first.value
    }

    fun spanCharChanged(){
        cardsList.forEach {
            it.tag.spanCharValue.first.value = it.tag.spanCharValue.first.value
        }
    }

    suspend fun getExampleAndSpanIndex(
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
            markupLanguage?.opening?.value?.isNotEmpty() == true &&
            markupLanguage?.closing?.value?.isNotEmpty() == true &&
            card.getSpanIdentifierSet() &&
            variablesSet
        )
        {
            markupLanguage?.opening?.value?.let { opening->
                markupLanguage?.closing?.value?.let { closing ->
                    if (!card.tag.isParagraph) {
                        var spanAddition =
                            EXAMPLE_TEXT.length + 1 + opening.length * 2 + card.tag.spanCharValue.first.value.length * 2 + closing.length * 2 + 1
                        builder.append(EXAMPLE_TEXT.substring(0, spanStart))
                            .append(opening)
                            .append(card.tag.spanCharValue.first.value)

                        if (card.tag.spanCharValue.second.value.isNotEmpty()) {
                            builder.append(MarkupLanguage.VALUE_OPENING).append(card.tag.spanCharValue.second.value).append(MarkupLanguage.VALUE_CLOSING)
                            spanAddition += 3 + card.tag.spanCharValue.second.value.length
                        }

                        builder.append(closing)
                            .append(EXAMPLE_TEXT.substring(spanStart, spanEnd))
                            .append(opening)
                            .append(MarkupLanguage.CLOSING_INDICATOR)
                            .append(card.tag.spanCharValue.first.value)
                            .append(closing)
                            .append(EXAMPLE_TEXT.substring(spanEnd, EXAMPLE_TEXT.length))
                            .append('\n')
                            .append(EXAMPLE_TEXT)

                        range = IntRange(
                            spanAddition + spanStart,
                            spanAddition + spanEnd
                        )
                    }
                    else{
                        builder.append(StringBuilder(opening))
                            .append(card.tag.spanCharValue.first.value)

                        if (card.tag.spanCharValue.second.value.isNotEmpty()) {
                            builder.append(MarkupLanguage.VALUE_OPENING).append(card.tag.spanCharValue.second.value).append(MarkupLanguage.VALUE_CLOSING)
                        }

                        builder.append(closing)
                            .append('\n')
                            .append(EXAMPLE_PARAGRAPH)
                            .append('\n')
                            .append(opening)
                            .append(MarkupLanguage.CLOSING_INDICATOR)
                            .append(card.tag.spanCharValue.first.value)
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

    fun setMarkupLanguage(markupLanguage: MarkupLanguage){
        this.markupLanguage = markupLanguage
        val newList = markupLanguage.getCards()
        val previousSize = cardsList.size
        val newSize = newList.size
        cardsList.clear()
        cardsList.addAll(newList)
        if (newSize<previousSize){
            notifyItemRangeChanged(0,newSize)
            notifyItemRangeRemoved(newSize,previousSize-newSize)
        }
        else if ( newSize == previousSize) notifyItemRangeChanged(0,newSize)
        else{
            notifyItemRangeChanged(0,previousSize)
            notifyItemRangeInserted(previousSize,newSize-previousSize)
        }
        updateStates()
    }

    private fun updateStates(){
        val charList = arrayListOf<Pair<String,String>>()
        cardsList.forEach { specialCharacter -> specialCharacter.tag.spanCharValue.first.value.let { charList.add(Pair(specialCharacter.tag.spanName,it)) } }
        charList.forEachIndexed { index, pair ->
            if (pair.second.isEmpty())  cardsList[index].setDuplicateIndexes(listOf())
            else{
                cardsList[index].setDuplicateIndexes(
                    charList.mapIndexed { i, string -> i to string }
                        .filter { it.second.second == pair.second }
                        .map { it.second.first }
                )
            }
        }
    }

    private fun inflateCard(parent: ViewGroup): MarkupLanguageCardViewHolder{
        val binding = MarkupLanguageCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MarkupLanguageCardViewHolder(binding, parent.findViewTreeLifecycleOwner()!!)
    }

    inner class MarkupLanguageCardViewHolder(
        private val binding: MarkupLanguageCardBinding,
        private val lifecycleOwner: LifecycleOwner
    ): RecyclerView.ViewHolder(binding.root) {

        private val jobs: ArrayList<Job> = arrayListOf()
        private var clickableSpan: (()->Unit)? = null
        private var isProcessing = false

        private fun processText(item: MarkupLanguageCard, span:String, value:String) {
            if (isProcessing) return
            else isProcessing = true
            lifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    if (span.isNotEmpty()) {
                        if (value == "0") {
                            item.tag.spanCharValue.second.value = ""
                        } else {
                            getExampleAndSpanIndex(
                                item,
                                item.variablesSet(context, clickableSpan)
                            ) { text, range ->
                                item.spannedString.setText(text, context)
                                if (range != null) {
                                    item.tag.applyTag(
                                        range,
                                        item.spannedString.spannableStringBuilder,
                                        context,
                                        clickable = clickableSpan
                                    )
                                }
                            }
                            item.colourButtonEnabled.value = true
                        }
                    } else {
                        item.spannedString.setText("", context)
                        item.colourButtonEnabled.value = false
                    }
                    updateStates()
                    if (item.getSpanType() == MarkupLanguage.TagType.FUNCTION_URL || item.getSpanType() == MarkupLanguage.TagType.FUNCTION_CLICKABLE) {
                        binding.markupLanguageCardCharExampleTextview.movementMethod =
                            LinkMovementMethod.getInstance()
                    }
                    isProcessing = false
                }
            }
        }

        fun bind(
            item: MarkupLanguageCard
        ) {
            binding.task = item
            binding.lifecycleOwner = lifecycleOwner
            binding.cardViewHolder = this

            val views = listOf(binding.markupLanguageCardCharTextview.id,
                binding.markupLanguageCardCharEditView.id,
                binding.markupLanguageCardCharValueEditView.id,
                binding.markupLanguageCardCharColourButton.id) // Add more view IDs as needed
            binding.flow.referencedIds = views.toIntArray()

            if (item.hasValue()) {
                if (item.getSpanType() == MarkupLanguage.TagType.FUNCTION_FLOAT) binding.markupLanguageCardCharValueEditView.inputType =
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                else if (item.getSpanType() == MarkupLanguage.TagType.FUNCTION_INT) binding.markupLanguageCardCharValueEditView.inputType =
                    InputType.TYPE_CLASS_NUMBER
                else if (item.getSpanType() == MarkupLanguage.TagType.FUNCTION_COLOUR) {
                    binding.markupLanguageCardCharColourButton.setOnClickListener {
                        Colour.showColorPickerDialog(context) { selectedColour: Int ->
                            item.tag.spanCharValue.second.value = selectedColour.toString()
                        }
                    }
                } else if (item.getSpanType() == MarkupLanguage.TagType.FUNCTION_URL) {
                    binding.markupLanguageCardCharValueEditView.inputType =
                        InputType.TYPE_CLASS_TEXT
                } else if (item.getSpanType() == MarkupLanguage.TagType.FUNCTION_CLICKABLE) {
                    clickableSpan = {
                        Toast.makeText(context, "Clickable Clicked!!", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else if (item.getSpanType() == MarkupLanguage.TagType.FUNCTION_STRING) {
                    binding.markupLanguageCardCharColourButton.text =
                        context.getString(R.string.select_alignment)
                    binding.markupLanguageCardCharColourButton.setOnClickListener {
                        MarkupLanguage.getAlignmentMenuOnClick(context, it,
                            {
                                //Normal
                                item.tag.spanCharValue.second.value =
                                    Layout.Alignment.ALIGN_NORMAL.name
                            },
                            {
                                //Center
                                item.tag.spanCharValue.second.value =
                                    Layout.Alignment.ALIGN_CENTER.name
                            },
                            {
                                //Opposite
                                item.tag.spanCharValue.second.value =
                                    Layout.Alignment.ALIGN_OPPOSITE.name
                            }
                        )()
                    }
                }
            }

            jobs.add(MainActivity.collectFlow(lifecycleOwner,item.tag.spanCharValue.first){
                processText(item,it,item.tag.spanCharValue.second.value)
            })

            jobs.add(MainActivity.collectFlow(lifecycleOwner,item.tag.spanCharValue.second){
                processText(item,item.tag.spanCharValue.first.value,it)
            })

            item.tag.setClickable(clickableSpan)
        }

        fun unbind(){
            jobs.forEach { it.cancel() }
            jobs.clear()
            clickableSpan = null
        }
    }


}