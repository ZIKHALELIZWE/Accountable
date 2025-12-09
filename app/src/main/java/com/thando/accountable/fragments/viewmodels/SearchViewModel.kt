package com.thando.accountable.fragments.viewmodels

import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.thando.accountable.AccountableNavigationController
import com.thando.accountable.AccountableRepository
import com.thando.accountable.MainActivity
import com.thando.accountable.R
import com.thando.accountable.database.tables.Content
import com.thando.accountable.database.tables.Script
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.math.abs


class SearchViewModel(
    private val repository: AccountableRepository
): ViewModel() {

    val intentString = repository.intentString
    private val searchScrollPosition = repository.getSearchScrollPosition()
    val searchString = repository.getSearchString()
    val matchCaseCheck = repository.getMatchCaseCheck()
    val wordCheck = repository.getWordCheck()
    val searchJob = repository.getSearchJob()
    val occurrences = repository.getSearchOccurrences()
    val numScripts = repository.getSearchNumScripts()
    val scriptsList = repository.getSearchScriptsList()

    private val _openScript = MutableSharedFlow<Long>()
    val openScript = _openScript.asSharedFlow()
    private val _notifyListCleared = MutableSharedFlow<Unit>()
    val notifyListCleared = _notifyListCleared.asSharedFlow()

    data class ScriptSearch(
        val folderId : Long,
        val folderTitle: String,
        val script : Script,
        val onScriptClick:(scriptId:Long)->Unit
    ){
        private val characters = 200
        val searchOccurrences = MutableStateFlow(0)
        private val ranges = arrayListOf<Triple<String,MutableList<IntRange>,Content?>>()
        val leftButtonVisibility = MutableStateFlow(View.GONE)
        val rightButtonVisibility = MutableStateFlow(View.GONE)
        val snippets = arrayListOf<SpannableString>()
        val snippetIndex = MutableStateFlow(0)

        fun openScript(){
            script.scriptId?.let { onScriptClick(it) }
        }

        fun addRanges(list:ArrayList<Triple<String,MutableList<IntRange>,Content?>>){
            ranges.addAll(list)
            list.forEach {
                searchOccurrences.value += it.second.size
                it.second.forEach { range ->
                    val sides = (characters - 6 - range.last + range.first) / 2
                    var startIndex: Int = range.first - sides
                    var endIndex: Int = range.last + sides

                    if (startIndex<0) endIndex += abs(startIndex)
                    if (endIndex>=it.first.length) startIndex -= endIndex-it.first.length+1
                    if (startIndex<0) startIndex = 0
                    if (endIndex>=it.first.length) endIndex = it.first.length

                    var rangeFirst = range.first
                    var rangeLast = range.last + 1
                    var snippet = it.first.substring(startIndex,endIndex)
                    if (startIndex!=0){
                        snippet = "...$snippet"
                        rangeFirst = range.first-startIndex+3
                        rangeLast = rangeLast-startIndex+3
                    }
                    if (endIndex!=it.first.length) snippet = "$snippet..."

                    val snippetHighlight = SpannableString(snippet)
                    snippetHighlight.setSpan(BackgroundColorSpan(MainActivity.ResourceProvider.resources.getColor(
                        R.color.purple_200,null)), rangeFirst, rangeLast, 0)
                    snippets.add(snippetHighlight)
                }
            }
            setButtonVisibility()
        }

        private fun setButtonVisibility(){
            if (snippetIndex.value>0) leftButtonVisibility.value = View.VISIBLE
            else leftButtonVisibility.value = View.GONE
            if (snippetIndex.value<snippets.size-1) rightButtonVisibility.value = View.VISIBLE
            else rightButtonVisibility.value = View.GONE
        }

        fun addRanges(item:Triple<String,MutableList<IntRange>,Content?>){
            addRanges(arrayListOf(item))
        }

        fun moveRight(){
            if (snippetIndex.value<snippets.size-1) snippetIndex.value += 1
            setButtonVisibility()
        }

        fun moveLeft(){
            if (snippetIndex.value>0) snippetIndex.value -= 1
            setButtonVisibility()
        }
    }

    fun search(searchComplete:(()->Unit)?=null){
        repository.searchFragmentSearch(
            {scriptId:Long->onScriptClick(scriptId)},
            _notifyListCleared,
            searchComplete)
    }

    private fun onScriptClick(scriptId: Long) {
        viewModelScope.launch {
            _openScript.emit(scriptId)
        }
    }

    fun loadAndOpenScript(scriptId: Long, activity: FragmentActivity?) {
        if (repository.intentString==null) {
            repository.setIsFromSearchFolderToTrue {
                repository.loadAndOpenScript(scriptId)
            }
        }
        else{
            repository.appendIntentStringToScript(scriptId,activity)
        }
    }

    fun getScriptContentPreview(scriptId: Long): AccountableRepository.ContentPreview{
        return repository.ContentPreview(scriptId)
    }

    fun navigateToFoldersAndScripts(){
        repository.resetSearchData {
            repository.changeFragment(AccountableNavigationController.AccountableFragment.BooksFragment)
        }
    }

    fun setScrollPosition(inputScrollPosition:Int){
        repository.setSearchScrollPosition(inputScrollPosition)
    }

    fun getScrollPosition(): Int{
        return searchScrollPosition.value
    }

    companion object{
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[APPLICATION_KEY])

                val accountableRepository = AccountableRepository.getInstance(application)

                return SearchViewModel(
                    accountableRepository
                ) as T
            }
        }
    }
}