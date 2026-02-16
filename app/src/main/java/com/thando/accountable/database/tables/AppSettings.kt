package com.thando.accountable.database.tables

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.thando.accountable.AccountableNavigationController
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update

@Entity(tableName = "app_settings_table")
data class AppSettings(
    @PrimaryKey
    var appSettingId: Long = 1L,

    @ColumnInfo(name = "main_picture") var mainPicture: MutableStateFlow<String> = MutableStateFlow("app_picture"),

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

    @ColumnInfo (name = "goal_folders_order")
    var goalFoldersOrder: MutableStateFlow<Boolean> = MutableStateFlow(true),

    @ColumnInfo (name = "goal_scripts_order")
    var goalScriptsOrder: MutableStateFlow<Boolean> = MutableStateFlow(true),

    @ColumnInfo (name = "text_size")
    var textSize: MutableStateFlow<Int> = MutableStateFlow(50)
) {

    companion object {
        @Ignore
        val FOLDER_IMAGE_ID = "Main_Picture_"
        @Ignore
        val DEFAULT_IMAGE_ID = "app_picture"
    }

    @Ignore
    val imageResource = AppResources.ImageResource(mainPicture.value)

    fun getMainPicture(): String { return mainPicture.value }

    fun saveImage(context: Context, inputUri: Uri?) {
        mainPicture.update {
            imageResource.saveFile(context, inputUri, FOLDER_IMAGE_ID, appSettingId) ?: DEFAULT_IMAGE_ID
        }
    }

    fun restoreDefaultFile(context: Context) {
        mainPicture.update {
            imageResource.setDefaultImage(context)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getUri(context: Context): Flow<ImageBitmap?> = imageResource.getUri(context).mapLatest { scriptUri ->
        scriptUri?.let { AppResources.getBitmapFromUri(context, it)?.asImageBitmap() }
    }.flowOn(MainActivity.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getImageBitmapOrAppIcon(context: Context):Flow<ImageBitmap?> = getUri(context).mapLatest {
        it?: AppResources.getAppIcon(context)
    }.flowOn(MainActivity.IO)
}