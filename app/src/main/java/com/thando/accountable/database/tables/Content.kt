package com.thando.accountable.database.tables

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Range
import androidx.compose.foundation.text.input.TextFieldState
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.thando.accountable.AppResources
import com.thando.accountable.SpannedString
import com.thando.accountable.player.TrackItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Entity(tableName= "content_table")
data class Content(
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null,

    @ColumnInfo (name = "content_script")
    var script: Long,

    @ColumnInfo (name = "content_type")
    var type: ContentType,

    @ColumnInfo (name = "content_position")
    var position: Long,

    @ColumnInfo (name = "content_content")
    var content: TextFieldState = TextFieldState(""),

    @ColumnInfo (name = "content_size")
    var size: Float = 0F,

    @ColumnInfo (name = "content_file_name")
    var filename: TextFieldState = TextFieldState(""),

    @ColumnInfo (name = "content_description")
    var description: TextFieldState = TextFieldState(""),

    @ColumnInfo (name = "content_efficiency")
    var efficiency: MutableStateFlow<Int> = MutableStateFlow(-1)
    ){

    enum class ContentType {
        TEXT, IMAGE, SCRIPT, VIDEO, DOCUMENT, AUDIO
    }
    enum class NonMediaType{ TEXT,SCRIPT }

    @Ignore
    private val contentPrefix = "Content_"

    @Ignore
    val mediaType: AppResources.ContentType? = when(type){
        ContentType.TEXT -> null
        ContentType.IMAGE -> AppResources.ContentType.IMAGE
        ContentType.SCRIPT -> null
        ContentType.VIDEO -> AppResources.ContentType.VIDEO
        ContentType.DOCUMENT -> AppResources.ContentType.DOCUMENT
        ContentType.AUDIO -> AppResources.ContentType.AUDIO
    }

    @Ignore
    val imageResource = AppResources.ImageResource(getContentMediaId())

    @Ignore
    val documentResource = AppResources.DocumentResource(getContentMediaId())

    @Ignore
    val audioResource = AppResources.AudioResource(getContentMediaId())

    @Ignore
    val videoResource = AppResources.VideoResource(getContentMediaId())

    @Ignore
    var spannedString: SpannedString = SpannedString(when(type){
        ContentType.TEXT -> content.text.toString()
        ContentType.IMAGE,
        ContentType.SCRIPT,
        ContentType.VIDEO,
        ContentType.DOCUMENT,
        ContentType.AUDIO -> description.text.toString()
    })

    @Ignore
    var replaceAsync: Deferred<String>? = null

    @Ignore
    val isPlaying = MutableStateFlow(false)

    @Ignore
    val trackItem: MutableStateFlow<TrackItem?> = MutableStateFlow(null)

    private fun getContentMediaId():String{
        if (id != null) {
            return when(type){
                ContentType.TEXT -> ""
                ContentType.SCRIPT -> ""
                ContentType.IMAGE ,
                ContentType.VIDEO,
                ContentType.DOCUMENT,
                ContentType.AUDIO -> content.text.toString()
            }
        }
        return ""
    }

    suspend fun saveFile(context: Context, inputUri:Uri?){
        withContext(Dispatchers.Main) {
            when (mediaType) {
                AppResources.ContentType.IMAGE -> content.edit {
                    replace(
                        0, length,
                        withContext(Dispatchers.IO) {
                            imageResource.saveFile(
                                context,
                                inputUri,
                                contentPrefix,
                                id
                            ) ?: ""
                        }
                    )
                }

                AppResources.ContentType.DOCUMENT -> content.edit { replace(0,length,
                    withContext(Dispatchers.IO) {documentResource.saveFile(
                        context,
                        inputUri,
                        contentPrefix,
                        id
                    )?:""}
                ) }

                AppResources.ContentType.AUDIO -> content.edit { replace(0,length,
                    withContext(Dispatchers.IO) {audioResource.saveFile(
                        context,
                        inputUri,
                        contentPrefix,
                        id
                    )?:""}
                )}

                AppResources.ContentType.VIDEO -> content.edit { replace(0,length,
                    withContext(Dispatchers.IO) {videoResource.saveFile(
                        context,
                        inputUri,
                        contentPrefix,
                        id
                    )?:""}
                )}

                null -> {}
            }
        }
    }

    suspend fun deleteFile(context: Context){
        when(mediaType){
            AppResources.ContentType.IMAGE -> imageResource.deleteFile(context)
            AppResources.ContentType.DOCUMENT -> documentResource.deleteFile(context)
            AppResources.ContentType.AUDIO -> audioResource.deleteFile(context)
            AppResources.ContentType.VIDEO -> videoResource.deleteFile(context)
            null -> {}
        }
    }

    fun getUri(context: Context): StateFlow<Uri?>? {
        return when(mediaType){
            AppResources.ContentType.IMAGE -> imageResource.getUri(context)
            AppResources.ContentType.DOCUMENT -> documentResource.getUri(context)
            AppResources.ContentType.AUDIO -> audioResource.getUri(context)
            AppResources.ContentType.VIDEO -> videoResource.getUri(context)
            null -> null
        }
    }

    fun getText(markupLanguage: MarkupLanguage?, context: Context, textSize: Float){
        spannedString.setText(
            when(type){
                ContentType.TEXT -> content.text.toString()
                ContentType.IMAGE,
                ContentType.SCRIPT,
                ContentType.VIDEO,
                ContentType.DOCUMENT,
                ContentType.AUDIO -> description.text.toString()
            }, context, textSize,markupLanguage
        )
    }

    fun replace(
        specialCharactersList: MutableList<SpecialCharacters>,
        context: Context,
        markupLanguage: MarkupLanguage?,
        isEditing: Boolean,
        lifecycleScope: CoroutineScope,
        textSize: Float
    ){
        if (replaceAsync!=null) {
            if (replaceAsync!!.isActive) replaceAsync!!.cancel()
            replaceAsync = null
        }

        // Create a CoroutineScope with Dispatchers.IO for IO-bound tasks
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // Perform an asynchronous operation
                    replaceAsync = async {
                        var newString = when(type){
                            ContentType.TEXT -> content.text.toString()
                            ContentType.IMAGE,
                            ContentType.SCRIPT,
                            ContentType.VIDEO,
                            ContentType.DOCUMENT,
                            ContentType.AUDIO -> description.text.toString()
                        }

                        // Find markup ranges
                        var markupRanges: List<MarkupLanguage.Tag>? = null
                        markupLanguage?.let { markupLanguage ->
                            markupRanges = markupLanguage.getTagRanges(newString).first
                        }

                        if (!isEditing) {
                            val occurrences = arrayListOf<Pair<SpecialCharacters, ArrayList<Int>>>()
                            specialCharactersList.forEach { specialCharacters ->
                                if (specialCharacters.canUpdateList() &&
                                    !(
                                            specialCharacters.character.value.isEmpty() ||
                                                    content.text.isEmpty() ||
                                                    specialCharacters.editingAfterChar.value.isEmpty()
                                            )
                                ) {
                                    occurrences.add(
                                        Pair(
                                            specialCharacters,
                                            findAllOccurrences(
                                                content.text.toString(),
                                                specialCharacters.character.value
                                            )
                                        )
                                    )
                                }
                            }

                            occurrences.forEach { firstPair ->
                                occurrences.forEach { secondPair ->
                                    val secondChar = secondPair.first.character.value
                                    val firstChar = firstPair.first.character.value
                                    if (secondChar != firstChar) {
                                        if (secondChar.contains(firstChar)) {
                                            secondPair.second.forEach {
                                                firstPair.second.remove(
                                                    it + secondChar.indexOf(
                                                        firstChar
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            val sortedArrayList = arrayListOf<Pair<SpecialCharacters, Int>>()
                            occurrences.forEach {
                                val charLength = it.first.character.value.length
                                it.second.forEach { int ->
                                    var doesNotOverlapSpan = true
                                    markupRanges?.let { markupRanges ->
                                        var index = 0
                                        while (doesNotOverlapSpan && index < markupRanges.size) {
                                            if (markupRanges[index].overlapsTag(
                                                    Range(
                                                        int,
                                                        int + charLength
                                                    )
                                                )
                                            ) doesNotOverlapSpan = false
                                            index++
                                        }
                                    }
                                    if (doesNotOverlapSpan) {
                                        sortedArrayList.add(
                                            Pair(
                                                it.first,
                                                int
                                            )
                                        )
                                    }
                                }
                            }
                            sortedArrayList.sortBy { it.second }

                            var addedDifference = 0
                            sortedArrayList.forEach {
                                val charLength = it.first.character.value.length
                                val replacementString = it.first.editingAfterChar.value
                                val index = it.second + addedDifference
                                newString = newString.replaceRange(
                                    index,
                                    index + charLength,
                                    replacementString
                                )
                                addedDifference += replacementString.length - charLength
                            }
                        }
                        newString
                    }
                    val result = replaceAsync!!.await()

                    // Switch to the Main thread to update UI
                    withContext(Dispatchers.Main) {
                        spannedString.setText(
                            result,
                            context,
                            textSize,
                            markupLanguage
                        )
                    }
                } catch (e: Exception) {
                    // Handle any exceptions
                    withContext(Dispatchers.Main) {
                        // Show error message on UI
                        println("Asynchronous Error: ${e.message}")
                    }
                }
            }
        }
    }

    private fun findAllOccurrences(mainString: String, subString: String): ArrayList<Int> {
        val indices = ArrayList<Int>()
        var index = mainString.indexOf(subString)
        while (index >= 0) {
            indices.add(index)
            index = mainString.indexOf(subString, index + 1)
        }
        return indices
    }

    fun appendFile(uri: Uri, contentResolver: ContentResolver){
        if (type == ContentType.TEXT) {
            val inputStream = contentResolver.openInputStream(uri)
            val inputArray = ArrayList<Char>()
            inputStream?.bufferedReader()?.forEachLine {
                if (inputArray.isNotEmpty()) inputArray.add('\n')
                inputArray.addAll(it.toList())
            }
            inputStream?.close()

            if (content.text.isNotEmpty()) content.edit { append('\n') }
            content.edit { append(inputArray.joinToString("")) }
        }
    }
}