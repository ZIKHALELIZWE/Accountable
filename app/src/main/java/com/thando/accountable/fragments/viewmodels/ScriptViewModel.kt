package com.thando.accountable.fragments.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.thando.accountable.AccountableNavigationController
import com.thando.accountable.AccountableRepository
import com.thando.accountable.AppResources
import com.thando.accountable.R
import com.thando.accountable.database.tables.Content
import com.thando.accountable.database.tables.Content.ContentType
import com.thando.accountable.fragments.ContentPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.Calendar
import java.util.concurrent.atomic.AtomicReference

class ScriptViewModel(
    private val repository: AccountableRepository
): ViewModel() {
    // Data
    val appSettings = repository.getAppSettings()
    val script = repository.getScript()
    val scriptContentList = repository.getScriptContentList()
    val markupLanguage = repository.getScriptMarkupLanguage()
    val isEditingScript = repository.getIsEditingScript()

    // Information Set To Views
    val menuAddTimeStampTitle = mutableStateOf("")

    // Click Events
    var chooseContent: AppResources.ContentType? = null

    // Script or Teleprompter
    private var isScriptFragment = true
    val listState = LazyListState()
    var toolBarCollapsedFunction: suspend (Float, suspend (Float)->Unit)->Unit =  { _, ifTrueRun ->
        ifTrueRun(0f)
    }

    val multipleContentsStateFlow: MutableStateFlow<List<@JvmSuppressWildcards Uri>?> = MutableStateFlow(null)
    val multipleContentsJob = AtomicReference<Job?>(null)

    fun setIsScriptFragment(isScriptFragment: Boolean) {
        this.isScriptFragment = isScriptFragment
    }

    fun getIsScriptFragment(): Boolean {
        return isScriptFragment
    }

    suspend fun toolBarCollapsed(scrollBy: Float, ifTrueRun: suspend (Float)->Unit){
        toolBarCollapsedFunction(scrollBy){ leftOverScroll ->
            ifTrueRun(leftOverScroll)
        }
    }

    fun chooseTopImage(chooseImage:(AppResources.ContentType)->Unit) {
        if (!(script.value?.scriptPicture.isNullOrEmpty())){
            repository.deleteScriptImage()
        }
        else{
            chooseContent = AppResources.ContentType.IMAGE
            AppResources.ContentTypeAccessor[chooseContent]?.let {
                chooseImage(AppResources.ContentType.IMAGE)
            }?:{
                contentRetrieved()
            }
        }
    }

    fun contentRetrieved(){
        chooseContent = null
    }

    fun setTopImage(inputUri: Uri?){
        repository.saveScriptImage(inputUri)
    }

    fun loadText(loadTextUnit: (AppResources.ContentType)->Unit){
        chooseContent = AppResources.ContentType.DOCUMENT
        AppResources.ContentTypeAccessor[chooseContent]?.let {
            loadTextUnit(AppResources.ContentType.DOCUMENT)
        }?:{
            contentRetrieved()
        }
    }

    fun appendFile(uri: Uri){
        repository.appendFileToScript(uri)
    }

    fun addContent(multipleContentList:List<Uri>?, contentType: ContentType, contentPosition: ContentPosition, item: Content, cursorPosition:Int?){
        repository.addContent(
            multipleContentList,
            contentType,
            contentPosition,
            item,
            cursorPosition
        )
    }

    fun deleteContent(content:Content){
        repository.deleteContent(content)
    }

    fun addTimeStamp(context: Context, index:Int, content: Content, updateTextFieldValue:(String,Int)->Unit){
        if (script.value!=null) {
            val dateTimeNow = LocalDateTime.now()
            val date = AppResources.getFullDate(context,dateTimeNow)
            val time = AppResources.getTime(dateTimeNow)
            val (title,stampString) = if (
                AppResources.getFullDate(context,script.value!!.scriptDateTime.value)
                ==
                date
            ) {
                Pair(context.getString(R.string.add_time_stamp),
                time)
            } else {
                Pair(context.getString(R.string.add_date_stamp),
                "$time $date")
            }
            menuAddTimeStampTitle.value = title
            val stringBuilder = StringBuilder(if(content.type != ContentType.TEXT) content.description.text.toString() else content.content.text.toString())
            stringBuilder.insert(index, stampString)
            updateTextFieldValue(stringBuilder.toString(),index+stampString.length)
        }
    }

    suspend fun shareScript(context: Context): Pair<Boolean,Intent>{
        return withContext(Dispatchers.IO) {
            val intent = Intent()

            if (script.value?.scriptTitle != null
                && script.value?.scriptTitle?.text.isNullOrEmpty()
            ) {
                intent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    script.value!!.scriptTitle.text.toString()
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

    fun saveScriptAndOpenTeleprompter(){
        if (isEditingScript.value) editOrSaveScript()
        repository.saveScript(true) {
            repository.changeFragment(AccountableNavigationController.AccountableFragment.TeleprompterFragment)
        }
    }

    fun saveScriptAndOpenMarkupLanguage() {
        repository.saveScript(true) {
            repository.changeFragment(AccountableNavigationController.AccountableFragment.MarkupLanguageFragment)
        }
    }

    fun editOrSaveScript(){
        isEditingScript.update { isEditingScript.value.not() }
        if (!isEditingScript.value) repository.saveScript()
    }

    suspend fun onBackPressed(){
        if (isEditingScript.value) editOrSaveScript()
        else closeScript()
    }

    suspend fun closeScript(){
        script.value?.scrollPosition?.requestScrollToItem(0,0)
        repository.saveScript()
        if (repository.isFromSearchFragment()){
            repository.changeFragment(AccountableNavigationController.AccountableFragment.SearchFragment)
        }
        else {
            repository.loadFolder()
            repository.changeFragment(AccountableNavigationController.AccountableFragment.BooksFragment)
        }
    }

    fun prepareToClose( appendedUnit: () -> Unit?){
        repository.saveScript()
        appendedUnit()
    }

    fun printEntry(){
        repository.printScriptEntry()
    }

    suspend fun getShareContent(context: Context): Triple<Boolean,ArrayList<Uri>,String>{
        return withContext(Dispatchers.IO) {
            var hasContent = false
            var sharedText =
                if (script.value?.scriptTitle != null && !script.value?.scriptTitle?.text.isNullOrEmpty()) {
                    hasContent = true
                    "${
                        AppResources.getTime(script.value!!.scriptDateTime.value)
                    }\n${
                        AppResources.getFullDate(context,script.value!!.scriptDateTime.value)
                    }\n\n*${
                        script.value!!.scriptTitle.text
                    }*"
                } else ""

            val imageUris = arrayListOf<Uri>()
            scriptContentList.forEach {
                if (it.type == ContentType.TEXT) {
                    if (it.content.text.isNotEmpty()) {
                        sharedText =
                            (if (sharedText.isNotEmpty()) "$sharedText\n\n" else "") + it.content.text.toString()
                        hasContent = true
                    }
                } else if (it.type == ContentType.IMAGE) {
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