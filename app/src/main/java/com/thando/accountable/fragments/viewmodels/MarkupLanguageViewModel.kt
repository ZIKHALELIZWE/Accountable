package com.thando.accountable.fragments.viewmodels

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.thando.accountable.recyclerviewadapters.MarkupLanguageCardAdapter
import com.thando.accountable.recyclerviewadapters.MarkupLanguageCardAdapter.Companion.EXAMPLE_SPAN
import com.thando.accountable.recyclerviewadapters.MarkupLanguageCardAdapter.Companion.EXAMPLE_TEXT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
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
    val openingClosingExampleSpannedString = SpannedString("")

    private var changeNameFunction: ()->Unit = {}
    private val _deleteButtonText = MutableStateFlow(MainActivity.ResourceProvider.getString(R.string.restore_default_settings))
    val deleteButtonText: StateFlow<String> get() = _deleteButtonText
    private var deleteButtonFunction: ()->Unit = {}
    private val _showNameNotUniqueSnackBar = MutableSharedFlow<String>()
    val showNameNotUniqueSnackBar: SharedFlow<String> get() = _showNameNotUniqueSnackBar
    private val _notifySpinnerDataChanged = MutableSharedFlow<Unit>()
    val notifySpinnerDataChanged: SharedFlow<Unit> get() = _notifySpinnerDataChanged

    // Click Events
    private val _navigateToScript = MutableSharedFlow<Boolean>()
    val navigateToScript: SharedFlow<Boolean> get() = _navigateToScript

    // View States
    private var scrollPosition = 0
    val selectedIndex = repository.getMarkupLanguageSelectedIndex()
    var isShow = MutableStateFlow(true)
    var scrollRange = MutableStateFlow(-1)
    var addition = MutableStateFlow(0)

    fun getVisibility(opening:String?,closing:String?): Int{
        return if (opening.isNullOrEmpty() || closing.isNullOrEmpty())
            View.GONE
        else View.VISIBLE
    }

    fun getToolbarVisibility(addition: Int):Int{
        return if (addition == 0) {
            View.VISIBLE
        } else{
            View.GONE
        }
    }

    fun getToolbarTitle(addition: Int, context: Context):String{
        return if (addition == 0) {
            context.getString(R.string.markup_language)
        } else{
            " "
        }
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
                for ((i, mLanguage) in markupLanguagesList.value.withIndex()) {
                    if (i != index && mLanguage.name.value == name) {
                        nameUnique = false
                        viewModelScope.launch {
                            _showNameNotUniqueSnackBar.emit(name)
                        }
                    }
                }
                if (nameUnique) {
                    markupLanguage.name.value = name
                    viewModelScope.launch {
                        _notifySpinnerDataChanged.emit(Unit)
                    }
                }
            }
        }

        if (markupLanguage == markupLanguagesList.value.last()) {
            _deleteButtonText.value = context.getString(R.string.no_setting)
            deleteButtonFunction = {
                // Close
                viewModelScope.launch {
                    _navigateToScript.emit(false)
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

    fun loadMarkupLanguage(index: Int,similarList: List<String>){
        if (index<0 || markupLanguagesList.value.isEmpty()) return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.saveMarkupLanguage(similarList){
                    repository.setRepositoryMarkupLanguage(markupLanguagesList.value[index])
                    repository.setMarkupLanguageToScript(true)
                }
            }
        }
    }

    fun setSelectedIndex(selection:Int,similarList:List<String>? = null){
        if (similarList!=null && selection == selectedIndex.value){
            loadMarkupLanguage(selection, similarList)
        }
        else repository.setMarkupLanguageSelectedIndex(selection)
    }

    fun setScrollPosition(inputScrollPosition:Int){
        scrollPosition = inputScrollPosition
    }

    fun getScrollPosition(): Int{
        return scrollPosition
    }

    fun navigateToScript(save: Boolean){
        viewModelScope.launch {
            _navigateToScript.emit(save)
        }
    }

    fun changeLanguageName(){
        changeNameFunction.invoke()
    }

    fun clearLanguage(){
        deleteButtonFunction.invoke()
    }

    fun closeMarkupLanguageFragment(save:Boolean,similarList: List<String>, context: Context){
        if (save){
            repository.spansNotSimilarAndNameUnique(similarList) { isValid, innerSimilarList, nameUniqueErrorMessage ->
                if (isValid) {
                    viewModelScope.launch {
                        if (markupLanguage.value != defaultMarkupLanguage.value){
                            repository.deleteDefaultMarkupLanguage()
                        }
                        repository.saveMarkupLanguage(innerSimilarList){
                            repository.setMarkupLanguageToScript(true){
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
                        closeMarkupLanguageFragment(false,similarList,context)
                    }
                }
            }
        }
        else{
            repository.setMarkupLanguageToScript(false){
                repository.deleteDefaultMarkupLanguage {
                    repository.resetDefaultMarkupLanguage {
                        repository.changeFragment(AccountableFragment.ScriptFragment)
                    }
                }
            }
        }
    }

    fun setOpeningClosingExample(context: Context, cardAdapter: MarkupLanguageCardAdapter){
        markupLanguage.value?.let { markupLanguage ->
            val openingIsNotEmpty = markupLanguage.opening.value.isNotEmpty()
            val closingIsNotEmpty = markupLanguage.closing.value.isNotEmpty()
            val stringBuilder = StringBuilder("")
            val spanRange = IntRange(8,15)
            getProcessedExampleString(stringBuilder, markupLanguage.opening.value,
                markupLanguage.closing.value,openingIsNotEmpty,
                closingIsNotEmpty,false,spanRange)
            stringBuilder.append('\n')
            getProcessedExampleString(stringBuilder, markupLanguage.opening.value,
                markupLanguage.closing.value,openingIsNotEmpty,
                closingIsNotEmpty,true,spanRange)
            openingClosingExampleSpannedString.setText(stringBuilder.toString(), context)

            if (openingIsNotEmpty && closingIsNotEmpty) {
                val spanAddition = EXAMPLE_TEXT.length + 1 + markupLanguage.opening.value.length * 2 + EXAMPLE_SPAN.length * 2 + markupLanguage.closing.value.length * 2 + MarkupLanguage.CLOSING_INDICATOR.length
                openingClosingExampleSpannedString.spannableStringBuilder.value = markupLanguage.boldSpan(
                    openingClosingExampleSpannedString.spannableStringBuilder.value,
                    IntRange(
                        spanAddition + spanRange.first,
                        spanAddition + spanRange.last
                    )
                )
                openingClosingExampleSpannedString.spannableStringBuilder.value = markupLanguage.italicSpan(
                    openingClosingExampleSpannedString.spannableStringBuilder.value,
                    IntRange(
                        spanAddition + spanRange.first,
                        spanAddition + spanRange.last
                    )
                )
                cardAdapter.spanCharChanged()
            }
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
        stringBuilder.append(EXAMPLE_TEXT.substring(0,spanRange.first))
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