package com.thando.accountable.database.tables

import android.content.Context
import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.thando.accountable.AccountableNavigationController
import com.thando.accountable.AppResources
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Entity(tableName = "app_settings_table")
data class AppSettings(
    @PrimaryKey
    var appSettingId: Long = 1L,

    @ColumnInfo(name = "main_picture")
    private var mainPicture: String = "app_picture",

    @ColumnInfo (name = "scroll_position")
    var scrollPosition: LazyListState = LazyListState(),

    @ColumnInfo (name = "show_scripts")
    var showScripts: MutableStateFlow<Boolean> = MutableStateFlow(false),

    @ColumnInfo (name = "show_goals")
    var showGoals: MutableStateFlow<Boolean> = MutableStateFlow(false),

    @ColumnInfo (name = "initial_fragment")
    var initialFragment: AccountableNavigationController.AccountableFragment = AccountableNavigationController.AccountableFragment.HomeFragment,

    @ColumnInfo (name = "folders_order")
    var foldersOrder: MutableStateFlow<Boolean> = MutableStateFlow(true),

    @ColumnInfo (name = "scripts_order")
    var scriptsOrder: MutableStateFlow<Boolean> = MutableStateFlow(true),

    @ColumnInfo (name = "text_size")
    var textSize: Int = 50
) {

    companion object {
        @Ignore
        val FOLDER_IMAGE_ID = "Main_Picture_"
        @Ignore
        val DEFAULT_IMAGE_ID = "app_picture"
    }

    @Ignore
    val imageResource = AppResources.ImageResource(mainPicture)

    fun getMainPicture(): String { return mainPicture }

    suspend fun saveImage(context: Context, inputUri: Uri?) {
        mainPicture = imageResource.saveFile(context, inputUri, FOLDER_IMAGE_ID, appSettingId) ?: DEFAULT_IMAGE_ID
    }

    suspend fun restoreDefaultFile(context: Context) {
        mainPicture = imageResource.setDefaultImage(context)
    }

    fun getUri(context: Context): StateFlow<Uri?>  = imageResource.getUri(context)
}