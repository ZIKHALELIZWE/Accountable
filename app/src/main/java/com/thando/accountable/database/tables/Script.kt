package com.thando.accountable.database.tables

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.thando.accountable.AppResources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Entity(tableName= "script_table")
data class Script (
    @PrimaryKey (autoGenerate = true)
    var scriptId: Long? = null,

    @ColumnInfo (name = "script_parent_type")
    var scriptParentType: ScriptParentType,

    @ColumnInfo (name = "script_parent")
    var scriptParent: Long,

    @ColumnInfo (name = "script_date_time")
    var scriptDateTime: AppResources.CalendarResource,

    @ColumnInfo (name = "script_title")
    var scriptTitle: MutableState<String> = mutableStateOf(""),

    @ColumnInfo (name = "script_picture")
    var scriptPicture: String? = null,

    @ColumnInfo (name = "script_position")
    var scriptPosition: Long,

    @ColumnInfo (name = "script_scroll_position")
    var scrollPosition: LazyListState = LazyListState(),

    @ColumnInfo (name = "script_import_script")
    var scriptImportScript: String? = null,

    @ColumnInfo (name = "script_markup_language")
    var scriptMarkupLanguage: String? = null,

    @ColumnInfo (name = "script_settings")
    var scriptSettings: String? = null,

    @ColumnInfo (name = "script_teleprompter_settings")
    var scriptTeleprompterSettings: Long? = null,

    @ColumnInfo (name = "script_size")
    var size: MutableStateFlow<Float> = MutableStateFlow(0F),

    @ColumnInfo (name = "script_num_images")
    var numImages: MutableStateFlow<Int> = MutableStateFlow(0),

    @ColumnInfo (name = "script_num_videos")
    var numVideos: MutableStateFlow<Int> = MutableStateFlow(0),

    @ColumnInfo (name = "script_num_audios")
    var numAudios: MutableStateFlow<Int> = MutableStateFlow(0),

    @ColumnInfo (name = "script_num_documents")
    var numDocuments: MutableStateFlow<Int> = MutableStateFlow(0),

    @ColumnInfo (name = "script_num_scripts")
    var numScripts: MutableStateFlow<Int> = MutableStateFlow(0)
) {
    enum class ScriptParentType {
        SCRIPT, FOLDER, TASK
    }

    companion object {
        @Ignore
        private const val SCRIPT_IMAGE_PREFIX = "Script_"
    }

    @Ignore
    val imageResource = AppResources.ImageResource(scriptPicture?:"")

    suspend fun saveImage(context: Context, inputUri: Uri?){
        scriptPicture = imageResource.saveFile(context, inputUri, SCRIPT_IMAGE_PREFIX, scriptId)
    }

    suspend fun deleteFile(context: Context) {
        imageResource.deleteFile(context)
        scriptPicture = null
    }

    fun getUri(context: Context): StateFlow<Uri?> = imageResource.getUri(context)
}