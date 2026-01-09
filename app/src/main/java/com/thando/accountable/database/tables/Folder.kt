package com.thando.accountable.database.tables

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.thando.accountable.AppResources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Entity(tableName = "folder_table")
open class Folder(
    @PrimaryKey(autoGenerate = true)
    var folderId: Long? = null,

    @ColumnInfo (name = "folder_parent")
    var folderParent: Long,

    @ColumnInfo (name = "folder_type")
    var folderType: FolderType,

    @ColumnInfo(name = "folder_name")
    var folderName: TextFieldState = TextFieldState(""),

    @ColumnInfo (name = "folder_picture")
    private var folderPicture: String? = null,

    @ColumnInfo (name = "folder_position")
    var folderPosition: Long = 0L,

    @ColumnInfo (name = "folder_show_scripts")
    var folderShowScripts: MutableStateFlow<Boolean> = MutableStateFlow(true),

    @ColumnInfo (name = "folder_scroll_position")
    val folderScrollPosition: LazyListState = LazyListState(),

    @ColumnInfo (name = "folder_folders_order")
    var folderFoldersOrder: MutableStateFlow<Boolean> = MutableStateFlow(true),

    @ColumnInfo (name = "folder_scripts_order")
    var folderScriptsOrder: MutableStateFlow<Boolean> = MutableStateFlow(true)
) {
    enum class FolderType{
        SCRIPTS, GOALS
    }

    enum class FolderListType{
        FOLDERS, SCRIPTS, GOALS
    }

    companion object {
        private const val FOLDER_IMAGE_PREFIX = "Folder_"
    }

    @Ignore
    private var _progressBarVisibility: MutableStateFlow<Int> = if (folderPicture == null){
        MutableStateFlow(View.GONE)
    } else {
        MutableStateFlow(View.VISIBLE)
    }

    @get:Ignore
    val progressBarVisibility: StateFlow<Int> get() = _progressBarVisibility

    @Ignore
    val imageResource = AppResources.ImageResource(folderPicture?:"")

    @Ignore
    val numFolders = MutableStateFlow(0)

    @Ignore
    val numScripts = MutableStateFlow(0)

    @Ignore
    val numGoals = MutableStateFlow(0)

    fun getFolderPicture(): String? { return folderPicture }

    suspend fun saveImage(context: Context, inputUri: Uri?){
        folderPicture = imageResource.saveFile(context, inputUri, FOLDER_IMAGE_PREFIX, folderId)
    }

    suspend fun deleteFile(context: Context) {
        if (imageResource.deleteFile(context)) {
            folderPicture = null
        }
    }

    fun getUri(context: Context): StateFlow<Uri?> {
        _progressBarVisibility.value = View.GONE
        return imageResource.getUri(context)
    }
}