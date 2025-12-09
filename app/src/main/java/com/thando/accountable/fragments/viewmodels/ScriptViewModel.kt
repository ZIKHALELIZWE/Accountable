package com.thando.accountable.fragments.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.thando.accountable.AccountableNavigationController
import com.thando.accountable.AccountableRepository
import com.thando.accountable.AppResources
import com.thando.accountable.R
import com.thando.accountable.database.tables.Content
import com.thando.accountable.recyclerviewadapters.ContentItemAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class ScriptViewModel(
    private val repository: AccountableRepository
): ViewModel() {
    private val viewModelJob = Job()
    private val viewModelScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    // Data
    private val appSettings = repository.getAppSettings()
    val script = repository.getScript()
    val scriptContentList = repository.getScriptContentList()
    val markupLanguage = repository.getScriptMarkupLanguage()
    val isEditingScript = repository.getIsEditingScript()

    // Information Set To Views
    val menuAddTimeStampTitle = mutableStateOf("")
    var addTimeStampFunction: MutableStateFlow<((String)->Unit)?> = MutableStateFlow(null)

    // Click Events
    private val _chooseContent = MutableStateFlow<AppResources.ContentType?>(null)
    val chooseContent: StateFlow<AppResources.ContentType?> get() = _chooseContent
    private val _chooseMarkupLanguage = MutableSharedFlow<Boolean>()
    val chooseMarkupLanguage: SharedFlow<Boolean> get() = _chooseMarkupLanguage
    private val _printEntryToTextFile = MutableSharedFlow<Boolean>()
    val printEntryToTextFile : SharedFlow<Boolean> get() = _printEntryToTextFile
    private val _navigateToTeleprompter = MutableSharedFlow<Boolean>()
    val navigateToTeleprompter: SharedFlow<Boolean> get() = _navigateToTeleprompter

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun getContentAdapter(
        context: Context,
        viewLifecycleOwner: LifecycleOwner,
        childFragmentManager: FragmentManager,
        galleryLauncherMultiple: ActivityResultLauncher<String>,
        markupLanguageInitializeScrollUnit:(()->Unit)
    ): ContentItemAdapter {
        return ContentItemAdapter(
            context,
            viewLifecycleOwner,
            childFragmentManager,
            galleryLauncherMultiple,
            markupLanguageInitializeScrollUnit,
            script,
            scriptContentList,
            appSettings,
            markupLanguage,
            isEditingScript,
            viewModelScope,
            repository,
            addTimeStampFunction
        )
    }

    fun scriptViewVisibility(uri:Uri?,context: Context):Int{
        return if (uri == null) View.INVISIBLE else View.VISIBLE
    }

    fun editOrSaveScript(){
        isEditingScript.update { isEditingScript.value.not() }
        if (!isEditingScript.value) repository.saveScript()
    }

    fun chooseTopImage(chooseImage:()->Unit) {
        if (!(script.value?.scriptPicture.isNullOrEmpty())){
            repository.deleteScriptImage()
        }
        else{
            _chooseContent.value = AppResources.ContentType.IMAGE
            chooseImage.invoke()
        }
    }

    fun contentRetrieved(){
        _chooseContent.value = null
    }

    fun setTopImage(inputUri: Uri?){
        repository.saveScriptImage(inputUri)
    }

    fun chooseMarkupLanguage(){
        viewModelScope.launch {
            _chooseMarkupLanguage.emit(true)
        }
    }

    fun printToTextFile(){
        viewModelScope.launch {
            _printEntryToTextFile.emit(true)
        }
    }

    fun loadText(){
        viewModelScope.launch {
            _chooseContent.emit(AppResources.ContentType.DOCUMENT)
        }
    }

    fun addTimeStamp(context: Context){
        if (script.value!=null) {
            val cal = AppResources.CalendarResource(Calendar.getInstance())
            val date = cal.getFullDateStateFlow(context).value
            val time = cal.getTimeStateFlow(context).value
            menuAddTimeStampTitle.value = if (
                script.value!!.scriptDateTime.getFullDateStateFlow(context).value
                    ==
                    date
                ) {
                    addTimeStampFunction.value?.invoke(time)
                    context.getString(R.string.add_time_stamp)
                } else {
                    addTimeStampFunction.value?.invoke("$time $date")
                    context.getString(R.string.add_date_stamp)
                }
        }
    }

    suspend fun shareScript(context: Context): Pair<Boolean,Intent>{
        return withContext(Dispatchers.IO) {
            val intent = Intent()

            if (script.value?.scriptTitle?.value != null
                && script.value?.scriptTitle?.value.isNullOrEmpty()
            ) {
                intent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    script.value!!.scriptTitle.value
                )
            }

            val (hasContent, imageUris, sharedText) = getShareContent(context)
            if (imageUris.isNotEmpty()) {
                intent.apply {
                    action = Intent.ACTION_SEND_MULTIPLE
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
                    type = "image/jpg"
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
            } else if (sharedText.isNotEmpty()) {
                intent.apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, sharedText)
                    type = "text/plain"
                }
            }
            Pair(hasContent, intent)
        }
    }

    fun setScriptLoadedToFalse(){
        //scriptLoaded.value = false
    }

    fun openTeleprompter(){
        viewModelScope.launch {
            _navigateToTeleprompter.emit(true)
        }
    }

    fun saveScriptAndOpenTeleprompter(){
        repository.saveScript(true) {
            repository.changeFragment(AccountableNavigationController.AccountableFragment.TeleprompterFragment)
        }
    }

    fun saveScriptAndOpenMarkupLanguage() {
        repository.saveScript(true) {
            repository.changeFragment(AccountableNavigationController.AccountableFragment.MarkupLanguageFragment)
        }
    }

    fun onBackPressed(){
        if (isEditingScript.value) editOrSaveScript()
        else closeScript()
    }

    fun closeScript(){
        if (repository.isFromSearchFragment()){
            repository.changeFragment(AccountableNavigationController.AccountableFragment.SearchFragment)
        }
        else {
            repository.loadFolder()
            repository.changeFragment(AccountableNavigationController.AccountableFragment.BooksFragment)
        }
    }

    fun setScrollPosition(scrollPosition: Int, appendedUnit: () -> Unit?){
        script.value?.scrollPosition = scrollPosition
        repository.saveScript {
            appendedUnit()
        }
    }

    fun printEntry(contentItemAdapter: ContentItemAdapter){
        script.value?.scriptTitle?.value?.let { contentItemAdapter.printEntry(it) }
    }

    suspend fun getShareContent(context: Context): Triple<Boolean,ArrayList<Uri>,String>{
        return withContext(Dispatchers.IO) {
            val contentList = scriptContentList.value
            var hasContent = false
            var sharedText =
                if (script.value?.scriptTitle?.value != null && !script.value?.scriptTitle?.value.isNullOrEmpty()) {
                    hasContent = true
                    "${
                        script.value!!.scriptDateTime.getTimeStateFlow(context).value
                    }\n${
                        script.value!!.scriptDateTime.getFullDateStateFlow(context).value
                    }\n\n*${
                        script.value!!.scriptTitle.value
                    }*"
                } else ""

            val imageUris = arrayListOf<Uri>()
            contentList.forEach {
                if (it.type == Content.ContentType.TEXT) {
                    if (it.content.value.isNotEmpty()) {
                        sharedText =
                            (if (sharedText.isNotEmpty()) "$sharedText\n\n" else "") + it.content.value
                        hasContent = true
                    }
                } else if (it.type == Content.ContentType.IMAGE) {
                    val uri = it.imageResource.fileFromContentUri(context)
                    if (uri != null) {
                        val newUri = if (imageUris.isEmpty()) {
                            uri.buildUpon()
                                .appendQueryParameter("caption", sharedText)
                                .build()
                        } else {
                            val caption = imageUris.last().getQueryParameter("caption")
                            val queryText =
                                caption + (if (!caption.isNullOrEmpty() && sharedText.isNotEmpty()) "\n\n" else "") + sharedText.ifEmpty { "" }
                            val newUri = imageUris.last().buildUpon().clearQuery()
                                .appendQueryParameter("caption", queryText)
                                .build()
                            imageUris.remove(imageUris.last())
                            imageUris.add(newUri)
                            uri.buildUpon()
                                .appendQueryParameter("caption", "")
                                .build()
                        }

                        imageUris.add(newUri)
                        sharedText = ""
                    }
                }
            }

            if (imageUris.isNotEmpty()) {
                if (sharedText.isNotEmpty()) {
                    val caption = imageUris.last().getQueryParameter("caption")
                    val queryText =
                        caption + if (!caption.isNullOrEmpty()) "\n$sharedText" else sharedText
                    val newUri = imageUris.last().buildUpon().clearQuery()
                        .appendQueryParameter("caption", queryText)
                        .build()
                    imageUris.remove(imageUris.last())
                    imageUris.add(newUri)
                }
            }
            return@withContext Triple(hasContent, imageUris, sharedText)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                // Get the Application object from extras
                val application = checkNotNull(extras[APPLICATION_KEY])

                val accountableRepository = AccountableRepository.getInstance(application)

                return ScriptViewModel(
                    accountableRepository
                ) as T
            }
        }
    }
}