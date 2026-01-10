package com.thando.accountable.fragments.viewmodels

import android.content.Context
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.thando.accountable.AccountableNavigationController.AccountableFragment
import com.thando.accountable.AccountableRepository
import com.thando.accountable.MainActivity
import com.thando.accountable.R
import com.thando.accountable.SpannedString
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.recyclerviewadapters.MarkupLanguageCardAdapter.Companion.EXAMPLE_SPAN
import com.thando.accountable.recyclerviewadapters.MarkupLanguageCardAdapter.Companion.EXAMPLE_TEXT
import com.thando.accountable.ui.cards.MarkupLanguageCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MarkupLanguageViewModel(
    private val repository: AccountableRepository
): ViewModel() {
    // Data
    val script = repository.getScript()
    private val defaultMarkupLanguage = repository.getDefaultMarkupLanguage()

    val markupLanguage = repository.getScriptMarkupLanguage()
    val markupLanguagesList = repository.getMarkupLanguagesList()

    // Information Set To Views
    val cardsList: SnapshotStateList<MarkupLanguageCard> = mutableStateListOf()
    val openingClosingExampleSpannedString = SpannedString("")

    private var changeNameFunction: ()->Unit = {}
    private val _deleteButtonText = MutableStateFlow(MainActivity.ResourceProvider.getString(R.string.restore_default_settings))
    val deleteButtonText: StateFlow<String> get() = _deleteButtonText
    private var deleteButtonFunction: ()->Unit = {}
    private val _showNameNotUniqueSnackBar = MutableSharedFlow<String>()
    val showNameNotUniqueSnackBar: SharedFlow<String> get() = _showNameNotUniqueSnackBar

    // View States
    val selectedIndex = repository.getMarkupLanguageSelectedIndex()
    val menuOpen = MutableStateFlow(true)
    var lazyListState = LazyListState()

    fun toggleMenuOpen(){
        menuOpen.update { menuOpen.value.not() }
    }

    fun spansNotSimilar(markupLanguage: MarkupLanguage?): List<String> {
        val similarList = ArrayList<String>()
        for ((index,card) in cardsList.withIndex()){
            if (card.tag.spanCharValue.first.text.isEmpty()) continue
            if (card.tag.spanCharValue.first.text.toString() == markupLanguage?.opening?.text.toString()) if (!similarList.contains("Opening")){
                similarList.add("Opening")
                if (!similarList.contains(card.tag.spanName)) similarList.add(card.tag.spanName)
            }
            if (card.tag.spanCharValue.first.text.toString() == markupLanguage?.closing?.text.toString()) if (!similarList.contains("Closing")){
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
        return original.spanName!=check.spanName && original.spanCharValue.first.text.isNotEmpty() && check.spanCharValue.first.text.isNotEmpty() && original.spanCharValue.first.text.toString() == check.spanCharValue.first.text.toString()
    }

    fun setMarkupLanguageFunctions(
        markupLanguage: MarkupLanguage,
        context: Context
    ){
        val index = selectedIndex.value
        changeNameFunction = {
            showMarkupLanguageNameDialog(
                context, markupLanguage.name.value,
                context.getString(R.string.enter_markup_language_name)
            ) { name ->
                var nameUnique = true
                for ((i, mLanguage) in markupLanguagesList.withIndex()) {
                    if (i != index && mLanguage.name.value == name) {
                        nameUnique = false
                        viewModelScope.launch {
                            _showNameNotUniqueSnackBar.emit(name)
                        }
                    }
                }
                if (nameUnique) {
                    markupLanguage.name.value = name
                }
            }
        }

        if (markupLanguage == markupLanguagesList.last()) {
            _deleteButtonText.value = context.getString(R.string.no_setting)
            deleteButtonFunction = {
                // Close
                viewModelScope.launch {
                    closeMarkupLanguageFragment(false,context)
                }
            }
        } else {
            _deleteButtonText.value = context.getString(R.string.delete)
            deleteButtonFunction = {
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        repository.setMarkupLanguageToScript(false){
                            repository.deleteMarkupLanguage(markupLanguage){
                                loadMarkupLanguage()
                            }
                        }
                    }
                }
            }
        }
    }

    fun loadMarkupLanguage() {
        repository.getMarkupLanguages()
    }

    fun loadMarkupLanguage(index: Int, markupLanguage: MarkupLanguage?){
        val similarList = spansNotSimilar(markupLanguage)
        if (index<0 || markupLanguagesList.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.saveMarkupLanguage(similarList){
                    repository.setRepositoryMarkupLanguage(markupLanguagesList[index])
                    repository.setMarkupLanguageToScript(true)
                }
            }
        }
    }

    fun setSelectedIndex(selection:Int,markupLanguage: MarkupLanguage?, checkSimilarList: Boolean){
        if (checkSimilarList && selection == selectedIndex.value){
            loadMarkupLanguage(selection, markupLanguage)
        }
        else repository.setMarkupLanguageSelectedIndex(selection)
    }

    fun setMarkupLanguage(markupLanguage: MarkupLanguage){
        val newList = markupLanguage.getCards()
        cardsList.clear()
        cardsList.addAll(newList)
        updateStates()
    }

    fun updateStates(){
        val charList = arrayListOf<Pair<String,String>>()
        cardsList.forEach { specialCharacter -> specialCharacter.tag.spanCharValue.first.text.toString().let { charList.add(Pair(specialCharacter.tag.spanName,it)) } }
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

    fun changeLanguageName(){
        changeNameFunction.invoke()
    }

    fun clearLanguage(){
        deleteButtonFunction.invoke()
    }

    suspend fun closeMarkupLanguageFragment(
        save:Boolean,
        context: Context
    ){
        if (save) {
            markupLanguage.collect { markupLanguage ->
                val similarList = spansNotSimilar(markupLanguage)
                repository.spansNotSimilarAndNameUnique(similarList) { isValid, innerSimilarList, nameUniqueErrorMessage ->
                    if (isValid) {
                        viewModelScope.launch {
                            if (markupLanguage != defaultMarkupLanguage.value) {
                                repository.deleteDefaultMarkupLanguage()
                            }
                            repository.saveMarkupLanguage(innerSimilarList) {
                                repository.setMarkupLanguageToScript(true) {
                                    repository.resetDefaultMarkupLanguage {
                                        repository.changeFragment(AccountableFragment.ScriptFragment)
                                    }
                                }
                            }
                        }
                    } else {
                        showErrorExitDialog(context, similarList, nameUniqueErrorMessage, {
                            // Dismiss

                        }) {
                            // Close
                            viewModelScope.launch {
                                closeMarkupLanguageFragment(false, context)
                            }
                        }
                    }
                }
            }
        } else {
            repository.setMarkupLanguageToScript(false) {
                repository.deleteDefaultMarkupLanguage {
                    repository.resetDefaultMarkupLanguage {
                        repository.changeFragment(AccountableFragment.ScriptFragment)
                    }
                }
            }
        }
    }

    fun setOpeningClosingExample(context: Context, textSize:Float){
        markupLanguage.value?.let { markupLanguage ->
            val openingIsNotEmpty = markupLanguage.opening.text.isNotEmpty()
            val closingIsNotEmpty = markupLanguage.closing.text.isNotEmpty()
            val stringBuilder = StringBuilder("")
            val spanRange = IntRange(8,15)
            getProcessedExampleString(stringBuilder, markupLanguage.opening.text.toString(),
                markupLanguage.closing.text.toString(),openingIsNotEmpty,
                closingIsNotEmpty,false,spanRange)
            stringBuilder.append('\n')
            getProcessedExampleString(stringBuilder, markupLanguage.opening.text.toString(),
                markupLanguage.closing.text.toString(),openingIsNotEmpty,
                closingIsNotEmpty,true,spanRange)
            openingClosingExampleSpannedString.setText(stringBuilder.toString(), context, textSize)

            if (openingIsNotEmpty && closingIsNotEmpty) {
                val spanAddition = EXAMPLE_TEXT.length + 1 + markupLanguage.opening.text.length * 2 + EXAMPLE_SPAN.length * 2 + markupLanguage.closing.text.length * 2 + MarkupLanguage.CLOSING_INDICATOR.length
                openingClosingExampleSpannedString.spannableStringBuilder.update {
                    markupLanguage.boldSpan(
                        openingClosingExampleSpannedString.spannableStringBuilder.value,
                        IntRange(
                            spanAddition + spanRange.first,
                            spanAddition + spanRange.last
                        )
                    )
                }
                openingClosingExampleSpannedString.spannableStringBuilder.update {
                    markupLanguage.italicSpan(
                        openingClosingExampleSpannedString.spannableStringBuilder.value,
                        IntRange(
                            spanAddition + spanRange.first,
                            spanAddition + spanRange.last
                        )
                    )
                }
                openingClosingExampleSpannedString.spannableAnnotatedString.update {
                    markupLanguage.boldSpanAnnotated(
                        openingClosingExampleSpannedString.spannableAnnotatedString.value,
                        IntRange(
                            spanAddition + spanRange.first,
                            spanAddition + spanRange.last
                        )
                    )
                }
                openingClosingExampleSpannedString.spannableAnnotatedString.update {
                    markupLanguage.italicSpanAnnotated(
                        openingClosingExampleSpannedString.spannableAnnotatedString.value,
                        IntRange(
                            spanAddition + spanRange.first,
                            spanAddition + spanRange.last
                        )
                    )
                }
                spanCharChanged()
            }
        }
    }

    fun spanCharChanged(){
        cardsList.forEach {
            it.tag.spanCharValue.first.setTextAndPlaceCursorAtEnd(it.tag.spanCharValue.first.text.toString())
        }
    }

    private fun getProcessedExampleString(
        stringBuilder: StringBuilder,
        opening:String?,
        closing:String?,
        openingIsNotEmpty:Boolean,
        closingIsNotEmpty:Boolean,
        isSpan:Boolean,
        spanRange:IntRange
    ){
        stringBuilder.append(EXAMPLE_TEXT.take(spanRange.first))
        if (if (isSpan) openingIsNotEmpty && !closingIsNotEmpty else openingIsNotEmpty) {
            stringBuilder.append(opening).append(EXAMPLE_SPAN)
        }
        if (!isSpan) if (openingIsNotEmpty && closingIsNotEmpty) stringBuilder.append(closing)
        stringBuilder.append(EXAMPLE_TEXT.substring(spanRange.first,spanRange.last))
        if (!isSpan) if (openingIsNotEmpty && closingIsNotEmpty) {
            stringBuilder.append(opening).append(MarkupLanguage.CLOSING_INDICATOR)
        }
        if (if (isSpan) closingIsNotEmpty && !openingIsNotEmpty else closingIsNotEmpty) {
            stringBuilder.append(EXAMPLE_SPAN).append(closing)
        }
        stringBuilder.append(EXAMPLE_TEXT.substring(spanRange.last, EXAMPLE_TEXT.length))
    }

    companion object{
        fun showMarkupLanguageNameDialog(context: Context, previousName:String, title:String, processName:(name:String)->Unit) {
            val editText = EditText(context)
            editText.setText( previousName)
            val enterNameDialog: AlertDialog = AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
                .setTitle(title)
                .setView(editText)
                .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Enter") { dialog, _ ->
                    val value = editText.text.toString().trim()
                    if (value.isEmpty()){
                        Toast.makeText(context,context.getString(R.string.enter_markup_language_name),Toast.LENGTH_SHORT).show()
                    }
                    else{
                        dialog.dismiss()
                        processName(value)
                    }
                }
                .create()
            enterNameDialog.show()
        }

        fun showErrorExitDialog(context: Context, similarList:List<String>, nameUniqueErrorMessage:String, dismissEvent:()->Unit, closeFragment:()->Unit) {
            val textView = TextView(context)
            val errorMessage = StringBuilder("")
            if (similarList.isNotEmpty()){
                errorMessage.append("There are conflicting Spans:\n")
                similarList.forEach { errorMessage.append("\n$it") }
            }
            if (nameUniqueErrorMessage.isNotEmpty()){
                errorMessage.append(nameUniqueErrorMessage)
            }
            errorMessage.append("\n\nClosing will not save the Markup Language.\nDo you want to close?\n")
            textView.text = errorMessage.toString()
            val enterNameDialog: AlertDialog = AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
                .setTitle(context.getString(R.string.warning))
                .setView(textView)
                .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Yes") { dialog, _ ->
                    dialog.dismiss()
                    closeFragment()
                }
                .setOnDismissListener {
                    dismissEvent()
                }
                .create()
            enterNameDialog.show()
        }

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                // Get the Application object from extras
                val application = checkNotNull(extras[APPLICATION_KEY])

                val accountableRepository = AccountableRepository.getInstance(application)

                return MarkupLanguageViewModel(
                    accountableRepository
                ) as T
            }
        }
    }
}