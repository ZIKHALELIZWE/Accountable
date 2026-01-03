package com.thando.accountable.ui.screens

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivity.Companion.collectFlow
import com.thando.accountable.R
import com.thando.accountable.database.tables.AppSettings
import com.thando.accountable.database.tables.Content
import com.thando.accountable.database.tables.Content.ContentType
import com.thando.accountable.database.tables.Folder
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.database.tables.Script
import com.thando.accountable.database.tables.TeleprompterSettings
import com.thando.accountable.fragments.FoldersAndScriptsFragment
import com.thando.accountable.fragments.viewmodels.FoldersAndScriptsViewModel
import com.thando.accountable.ui.cards.AudioCard
import com.thando.accountable.ui.cards.DocumentCard
import com.thando.accountable.ui.cards.ImageCard
import com.thando.accountable.ui.cards.ScriptCard
import com.thando.accountable.ui.cards.TextCard
import com.thando.accountable.ui.cards.VideoCard
import com.thando.accountable.ui.management.states.toolbar.FixedScrollFlagState
import com.thando.accountable.ui.management.states.toolbar.ToolbarState
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

val MinToolbarHeight = 64.dp
val MaxToolbarHeight = 300.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Catalog(
    listsCollection: FoldersAndScriptsFragment.FoldersAndScriptsLists,
    columns: Int,
    modifier: Modifier = Modifier,
    collapseType: ToolbarState.CollapseType = ToolbarState.CollapseType.Scroll,
    imageUri: Uri?,
    navigationIcon:@Composable (Modifier)-> Unit,
    titleText:@Composable (Modifier)-> Unit,
    searchIcon:@Composable (Modifier)-> Unit,
    orderIcon:@Composable (Modifier)-> Unit,
    folderScriptSwitchIcon:@Composable (Modifier)-> Unit
) {
    val toolbarHeightRange = with(LocalDensity.current) {
        MinToolbarHeight.roundToPx()..MaxToolbarHeight.roundToPx()
    }
    val toolbarState = ToolbarState.rememberToolbarState( collapseType, toolbarHeightRange)
    val listState = rememberLazyListState()

    val scope = rememberCoroutineScope()
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                toolbarState.scrollTopLimitReached = listState.firstVisibleItemIndex == 0
                        && listState.firstVisibleItemScrollOffset == 0
                toolbarState.scrollOffset -= available.y
                return Offset(0f, toolbarState.consumed)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (available.y > 0){
                    scope.launch {
                        animateDecay(
                            initialValue = toolbarState.height + toolbarState.offset,
                            initialVelocity = available.y,
                            animationSpec = FloatExponentialDecaySpec()
                        ){ value, _ ->
                            toolbarState.scrollTopLimitReached = listState.firstVisibleItemIndex == 0
                                    && listState.firstVisibleItemScrollOffset == 0
                            toolbarState.scrollOffset -= value - (toolbarState.height + toolbarState.offset)
                            if (toolbarState.scrollOffset == 0f) scope.coroutineContext.cancelChildren()
                        }
                    }
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bottomSheet by listsCollection.onClickListeners.bottomSheetListeners.collectAsStateWithLifecycle()

    Box(modifier = modifier.nestedScroll(nestedScrollConnection)) {
        when(listsCollection.listShown){
            FoldersAndScriptsFragment.FoldersAndScriptsLists.FASListType.FOLDERS -> {
                LazyFolderCatalog(
                    foldersList = listsCollection.foldersList,
                    onClickListeners = listsCollection.onClickListeners,
                    columns = columns,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onPress = { scope.coroutineContext.cancelChildren() })
                        }
                        .graphicsLayer { translationY = toolbarState.height + toolbarState.offset },
                    listState = listState,
                    contentPadding = PaddingValues(bottom = if (toolbarState is FixedScrollFlagState) MinToolbarHeight else 0.dp)
                )
            }
            FoldersAndScriptsFragment.FoldersAndScriptsLists.FASListType.SCRIPTS -> {
                LazyScriptCatalog(
                    scriptsList = listsCollection.scriptsList,
                    onClickListeners = listsCollection.onClickListeners,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onPress = { scope.coroutineContext.cancelChildren() })
                        }
                        .graphicsLayer { translationY = toolbarState.height + toolbarState.offset },
                    listState = listState,
                    contentPadding = PaddingValues(bottom = if (toolbarState is FixedScrollFlagState) MinToolbarHeight else 0.dp)
                )
            }
            FoldersAndScriptsFragment.FoldersAndScriptsLists.FASListType.GOALS -> {

            }
        }
        FoldersAndScriptsCollapsingToolbar(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(LocalDensity.current) { toolbarState.height.toDp() })
                .graphicsLayer { translationY = toolbarState.offset },
            progress = toolbarState.progress,
            imageUri,
            navigationIcon,
            titleText,
            searchIcon,
            orderIcon,
            folderScriptSwitchIcon
        )
    }
    bottomSheet?.let { bottomSheet ->
        ModalBottomSheet(
            onDismissRequest = {
                listsCollection.onClickListeners.bottomSheetListeners.update { null }
            },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                bottomSheet.displayView.invoke()
                Row {
                    bottomSheet.onEditClickListener?.let {
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            shape = RectangleShape,
                            colors = ButtonColors(
                                Color.Blue,
                                Color.Black,
                                Color.LightGray,
                                Color.DarkGray
                            ),
                            onClick = {
                                bottomSheet.onEditClickListener.invoke()
                                listsCollection.onClickListeners.bottomSheetListeners.update { null }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.bottom_sheet_edit_button)
                            )
                        }
                    }
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RectangleShape,
                        colors = ButtonColors(Color.Red,Color.Black,Color.LightGray,Color.DarkGray),
                        onClick = {
                            bottomSheet.onDeleteClickListener.invoke()
                            listsCollection.onClickListeners.bottomSheetListeners.update { null }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.bottom_sheet_delete_button)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LazyFolderCatalog(
    foldersList: MutableList<Folder>,
    onClickListeners: FoldersAndScriptsViewModel.OnClickListeners,
    columns: Int,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val chunkedItems = remember(
        foldersList
    ) { foldersList.chunked(columns) }

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier
    ) {
        chunkedItems.forEach { chunk ->
            item {
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                ) {
                    chunk.forEach { folder ->
                        FolderCard(
                            folder = folder,
                            modifier = Modifier
                                .padding(2.dp)
                                .weight(1f),
                            onClickListeners
                        )
                    }
                    val emptyCells = columns - chunk.size
                    if (emptyCells > 0) {
                        Spacer(modifier = Modifier.weight(emptyCells.toFloat()))
                    }
                }
            }
        }
    }
}

@Composable
fun LazyScriptCatalog(
    scriptsList: MutableList<Script>,
    onClickListeners: FoldersAndScriptsViewModel.OnClickListeners,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier
    ) {
        items(scriptsList, key = {it.scriptId?:Random.nextLong()}) { script ->
            ScriptCard(
                script = script,
                modifier = Modifier
                    .padding(2.dp)
                    .fillMaxWidth(),
                onClickListeners
            )
        }
    }
}

enum class ContentPosition{ ABOVE, AT_CURSOR_POINT, BELOW }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptFragmentCatalog(
    script: Script,
    isEditingScript: Boolean,
    scriptContentList: MutableList<Content>,
    markupLanguage: MarkupLanguage?,
    appSettings: AppSettings?,
    teleprompterSettings: TeleprompterSettings?,
    galleryLauncherMultiple: ActivityResultLauncher<String>,
    processResults: (List<Uri>?, ContentType, ContentPosition, Content, Int?) -> Unit,
    multipleContentsStateFlow: MutableStateFlow<List<@JvmSuppressWildcards Uri>?>,
    multipleContentsJob: AtomicReference<Job?>,
    fragmentLifecycleScope: LifecycleCoroutineScope,
    modifier: Modifier = Modifier,
    collapseType: ToolbarState.CollapseType = ToolbarState.CollapseType.EnterAlwaysCollapsed,
    navigationIcon:@Composable (Modifier)-> Unit,
    shareIcon:@Composable (Modifier)-> Unit,
    teleprompterIcon:@Composable (Modifier)-> Unit,
    menuList:List<MenuItemData>
) {
    val imageUri: Uri? = script.getUri(LocalContext.current).collectAsStateWithLifecycle(null).value
    val toolbarHeightRange = with(LocalDensity.current) {
        MinToolbarHeight.roundToPx()..MaxToolbarHeight.roundToPx()
    }
    val toolbarState = ToolbarState.rememberToolbarState( collapseType, toolbarHeightRange)
    val listState = rememberLazyListState()

    val scope = rememberCoroutineScope()
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                toolbarState.scrollTopLimitReached = listState.firstVisibleItemIndex == 0
                        && listState.firstVisibleItemScrollOffset == 0
                toolbarState.scrollOffset -= available.y
                return Offset(0f, toolbarState.consumed)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (available.y > 0){
                    scope.launch {
                        animateDecay(
                            initialValue = toolbarState.height + toolbarState.offset,
                            initialVelocity = available.y,
                            animationSpec = FloatExponentialDecaySpec()
                        ){ value, _ ->
                            toolbarState.scrollTopLimitReached = listState.firstVisibleItemIndex == 0
                                    && listState.firstVisibleItemScrollOffset == 0
                            toolbarState.scrollOffset -= value - (toolbarState.height + toolbarState.offset)
                            if (toolbarState.scrollOffset == 0f) scope.coroutineContext.cancelChildren()
                        }
                    }
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var bottomSheet by remember { mutableStateOf<Pair<Int?,Content>?>(null) }
    var textIndex by remember { mutableStateOf<Pair<Int?,Content>?>(null) }
    val addContentButton:@Composable (Modifier)-> Unit = { modifier ->
        IconButton(
            modifier = modifier,
            onClick = {
                bottomSheet = textIndex
            })
        {
            Icon(
                imageVector = Icons.Filled.AddCircle,
                contentDescription = stringResource(R.string.add_content)
            )
        }
    }

    Box(modifier = modifier.nestedScroll(nestedScrollConnection)) {
        LazyScriptContentCatalog(
            script = script,
            isEditingScript = isEditingScript,
            scriptContentList = scriptContentList,
            markupLanguage = markupLanguage,
            teleprompterSettings = teleprompterSettings,
            appSettings = appSettings,
            textIndex = {value -> textIndex = value},
            onLongClickListener = { contentIndex ->
                bottomSheet = contentIndex
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onPress = { scope.coroutineContext.cancelChildren() })
                }
                .graphicsLayer { translationY = toolbarState.height + toolbarState.offset },
            listState = listState,
            contentPadding = PaddingValues(bottom = if (toolbarState is FixedScrollFlagState) MinToolbarHeight else 0.dp)
        )
        ScriptsCollapsingToolbar(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(LocalDensity.current) { toolbarState.height.toDp() })
                .graphicsLayer { translationY = toolbarState.offset },
            progress = toolbarState.progress,
            progressMax = toolbarState.scrollOffset,
            imageUri,
            navigationIcon,
            if (textIndex==null) null else addContentButton,
            shareIcon,
            teleprompterIcon,
            basicDropdownMenu(options = menuList)
        )
    }
    if (bottomSheet != null) {
        ModalBottomSheet(
            onDismissRequest = {
                bottomSheet = null
            },
            sheetState = sheetState
        ) {
            var addType by remember { mutableStateOf<ContentType?>(null) }

            addType?.let { type ->
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RectangleShape,
                        colors = ButtonColors(
                            Color.Transparent,
                            Color.Black,
                            Color.LightGray,
                            Color.DarkGray
                        ),
                        onClick = {
                            val (accessor, nonMediaType) = addContentView(type)
                            bottomSheet?.let { bottomSheet ->
                                if (accessor!=null || nonMediaType!=null) getMultipleContent(
                                    accessor,
                                    type,
                                    ContentPosition.ABOVE,
                                    bottomSheet.second,
                                    bottomSheet.first,
                                    galleryLauncherMultiple,
                                    processResults,
                                    multipleContentsStateFlow,
                                    multipleContentsJob,
                                    fragmentLifecycleScope
                                )
                            }
                            bottomSheet = null
                        }
                    ) {
                        Text(stringResource(R.string.add_above))
                    }
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RectangleShape,
                        colors = ButtonColors(
                            Color.Transparent,
                            Color.Black,
                            Color.LightGray,
                            Color.DarkGray
                        ),
                        onClick = {
                            val (accessor, nonMediaType) = addContentView(type)
                            bottomSheet?.let { bottomSheet ->
                                if (accessor!=null || nonMediaType!=null) getMultipleContent(
                                    accessor,
                                    type,
                                    ContentPosition.AT_CURSOR_POINT,
                                    bottomSheet.second,
                                    bottomSheet.first,
                                    galleryLauncherMultiple,
                                    processResults,
                                    multipleContentsStateFlow,
                                    multipleContentsJob,
                                    fragmentLifecycleScope
                                )
                            }
                            bottomSheet = null
                        }
                    ) {
                        Text(stringResource(R.string.add_at_cursor_point))
                    }
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RectangleShape,
                        colors = ButtonColors(
                            Color.Transparent,
                            Color.Black,
                            Color.LightGray,
                            Color.DarkGray
                        ),
                        onClick = {
                            val (accessor, nonMediaType) = addContentView(type)
                            bottomSheet?.let { bottomSheet ->
                                if (accessor!=null || nonMediaType!=null) getMultipleContent(
                                    accessor,
                                    type,
                                    ContentPosition.BELOW,
                                    bottomSheet.second,
                                    bottomSheet.first,
                                    galleryLauncherMultiple,
                                    processResults,
                                    multipleContentsStateFlow,
                                    multipleContentsJob,
                                    fragmentLifecycleScope
                                )
                            }
                            bottomSheet = null
                        }
                    ) {
                        Text(stringResource(R.string.add_below))
                    }
                }
            }
            ?: run{
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    if (textIndex == null) {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth(),
                            shape = RectangleShape,
                            colors = ButtonColors(
                                Color.Transparent,
                                Color.Black,
                                Color.LightGray,
                                Color.DarkGray
                            ),
                            onClick = {
                                addType = ContentType.TEXT
                            }
                        ) {
                            Text(stringResource(R.string.add_text))
                        }
                    }
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RectangleShape,
                        colors = ButtonColors(
                            Color.Transparent,
                            Color.Black,
                            Color.LightGray,
                            Color.DarkGray
                        ),
                        onClick = {
                            addType = ContentType.IMAGE
                        }
                    ) {
                        Text(stringResource(R.string.add_pictures))
                    }
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RectangleShape,
                        colors = ButtonColors(
                            Color.Transparent,
                            Color.Black,
                            Color.LightGray,
                            Color.DarkGray
                        ),
                        onClick = {
                            addType = ContentType.AUDIO
                        }
                    ) {
                        Text(stringResource(R.string.add_audios))
                    }
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RectangleShape,
                        colors = ButtonColors(
                            Color.Transparent,
                            Color.Black,
                            Color.LightGray,
                            Color.DarkGray
                        ),
                        onClick = {
                            addType = ContentType.VIDEO
                        }
                    ) {
                        Text(stringResource(R.string.add_videos))
                    }
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RectangleShape,
                        colors = ButtonColors(
                            Color.Transparent,
                            Color.Black,
                            Color.LightGray,
                            Color.DarkGray
                        ),
                        onClick = {
                            addType = ContentType.DOCUMENT
                        }
                    ) {
                        Text(stringResource(R.string.add_documents))
                    }
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RectangleShape,
                        colors = ButtonColors(
                            Color.Transparent,
                            Color.Black,
                            Color.LightGray,
                            Color.DarkGray
                        ),
                        onClick = {
                            addType = ContentType.SCRIPT
                        }
                    ) {
                        Text(stringResource(R.string.add_script))
                    }
                    Button(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RectangleShape,
                        colors = ButtonColors(Color.Red,Color.Black,Color.LightGray,Color.DarkGray),
                        onClick = {
                            // todo
                            bottomSheet = null
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.bottom_sheet_delete_button)
                        )
                    }
                }
            }
        }
    }
}

private fun addContentView(chosenContentType: ContentType):Pair<String?,Content.NonMediaType?>{
    val accessor = when(chosenContentType){
        ContentType.TEXT -> null
        ContentType.IMAGE -> AppResources.ContentTypeAccessor[AppResources.ContentType.IMAGE]
        ContentType.SCRIPT -> null
        ContentType.VIDEO -> AppResources.ContentTypeAccessor[AppResources.ContentType.VIDEO]
        ContentType.DOCUMENT -> AppResources.ContentTypeAccessor[AppResources.ContentType.DOCUMENT]
        ContentType.AUDIO -> AppResources.ContentTypeAccessor[AppResources.ContentType.AUDIO]
    }
    val nonMediaType = when(chosenContentType){
        ContentType.TEXT -> Content.NonMediaType.TEXT
        ContentType.IMAGE -> null
        ContentType.SCRIPT -> Content.NonMediaType.SCRIPT
        ContentType.VIDEO -> null
        ContentType.DOCUMENT -> null
        ContentType.AUDIO -> null
    }
    return accessor to nonMediaType
}

private fun getMultipleContent(
    mediaType:String?,
    contentType: ContentType,
    position: ContentPosition,
    content: Content,
    cursorPosition: Int?,
    galleryLauncherMultiple: ActivityResultLauncher<String>,
    processResults: (List<Uri>?, ContentType, ContentPosition,Content,Int?) -> Unit,
    multipleContentsStateFlow: MutableStateFlow<List<@JvmSuppressWildcards Uri>?>,
    multipleContentsJob: AtomicReference<Job?>,
    fragmentLifecycleScope: LifecycleCoroutineScope
){
    if (mediaType!=null){
        multipleContentsJob.set(collectFlow(fragmentLifecycleScope,multipleContentsStateFlow){ list ->
            if (list!=null){
                processResults( list, contentType, position, content, cursorPosition)
                multipleContentsStateFlow.value = null
                multipleContentsJob.get()?.cancel()
            }
        })
        galleryLauncherMultiple.launch(mediaType)
    }
    else {
        processResults( null, contentType, position, content, cursorPosition)
        multipleContentsJob.get()?.cancel()
        multipleContentsStateFlow.value = null
    }
}

@Composable
fun LazyScriptContentCatalog(
    script: Script,
    isEditingScript: Boolean,
    scriptContentList: MutableList<Content>,
    markupLanguage: MarkupLanguage?,
    teleprompterSettings: TeleprompterSettings?,
    appSettings: AppSettings?,
    textIndex: (Pair<Int?,Content>?)->Unit,
    onLongClickListener:(Pair<Int?,Content>)->Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    var scriptTitle by remember { script.scriptTitle }
    val scriptTime by script.scriptDateTime.getTimeStateFlow(LocalContext.current).collectAsStateWithLifecycle()
    val scriptDayNum by script.scriptDateTime.getDayNumStateFlow(LocalContext.current).collectAsStateWithLifecycle()
    val scriptDayWord by script.scriptDateTime.getDayWordStateFlow(LocalContext.current).collectAsStateWithLifecycle()
    val scriptMonthYear by script.scriptDateTime.getMonthYearStateFlow(LocalContext.current).collectAsStateWithLifecycle()


    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier
    ) {
        item(key = "The Top") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                elevation = CardDefaults.cardElevation(),
                colors = CardColors(
                    Color.White,
                    Color.Black,
                    Color.LightGray,
                    Color.DarkGray)
            ) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(scriptTime,
                            fontSize = 40.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start)
                        Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
                            Text(scriptDayNum,
                                fontSize = 40.sp
                            )
                            Column {
                                Text(
                                    text = scriptDayWord,
                                    color = Color.Cyan
                                )
                                Text(scriptMonthYear)
                            }
                        }
                    }
                    if (isEditingScript) {
                        TextField(
                            value = scriptTitle,
                            onValueChange = { newTitle -> scriptTitle = newTitle },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 15.dp, horizontal = 5.dp),
                            textStyle = TextStyle(
                                textAlign = TextAlign.Center,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            placeholder = {
                                Text(
                                    stringResource(R.string.enter_script_title),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent, // Removes gray background
                                unfocusedContainerColor = Color.Transparent, // Removes gray background
                                unfocusedIndicatorColor = Color.Transparent, // Removes underline when unfocused
                                disabledIndicatorColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(
                            text = scriptTitle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 15.dp, horizontal = 5.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        itemsIndexed(items = scriptContentList, key = { _, listContent-> listContent.id?:Random.nextLong()}) { index, content ->
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            ) {
                val mod = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = {
                        onLongClickListener(null to content)
                    }
                )
                // Add Content Cards
                when (content.type) {
                    ContentType.TEXT -> TextCard(content,
                        isEditingScript,
                        textIndex,
                        appSettings,
                        markupLanguage,
                        teleprompterSettings)
                    ContentType.IMAGE -> ImageCard(
                        content,
                        isEditingScript,
                        mod,
                        appSettings,
                        markupLanguage,
                        teleprompterSettings
                    )
                    ContentType.VIDEO -> VideoCard(content, isEditingScript)
                    ContentType.DOCUMENT -> DocumentCard(content, isEditingScript)
                    ContentType.AUDIO -> AudioCard(content, isEditingScript)
                    ContentType.SCRIPT -> ScriptCard(content, isEditingScript)
                }
            }
        }
    }
}

data class MenuItemData(
    var text:String = "No Name",
    val onClick:()->Unit
)

fun basicDropdownMenu(options:List<MenuItemData>):@Composable ((Modifier) -> Unit) = @Composable {
    modifier ->
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = !expanded }) {
             Icon(
                 Icons.Default.MoreVert,
                 contentDescription = stringResource(R.string.more_options)
             )
        }
        DropdownMenu(
             expanded = expanded,
             onDismissRequest = { expanded = false }
        ) {
             options.forEach { option ->
                 DropdownMenuItem(
                     text = { Text(option.text) },
                     onClick = {
                         option.onClick.invoke()
                         expanded = false
                     }
                 )
             }
        }
    }
}