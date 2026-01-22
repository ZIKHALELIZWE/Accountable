package com.thando.accountable.fragments

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thando.accountable.AccountableRepository.Companion.accountablePlayer
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivity
import com.thando.accountable.MainActivityViewModel
import com.thando.accountable.R
import com.thando.accountable.database.tables.AppSettings
import com.thando.accountable.database.tables.Content
import com.thando.accountable.database.tables.Content.ContentType
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.database.tables.Script
import com.thando.accountable.database.tables.TeleprompterSettings
import com.thando.accountable.fragments.viewmodels.ScriptViewModel
import com.thando.accountable.ui.cards.GetContentCard
import com.thando.accountable.ui.screens.MenuItemData
import com.thando.accountable.ui.screens.basicDropdownMenu
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.random.Random

@Composable
fun ScriptView(
    viewModel: ScriptViewModel,
    mainActivityViewModel: MainActivityViewModel
) {
    DisposableEffect(Unit) {
        onDispose {
            viewModel.prepareToClose {}
        }
    }

    mainActivityViewModel.setGalleryLauncherMultipleReturn { list ->
        viewModel.multipleContentsStateFlow.update { list }
    }

    mainActivityViewModel.setGalleryLauncherReturn { galleryUri ->
        try{
            if (galleryUri!=null){
                when (viewModel.chooseContent) {
                    AppResources.ContentType.IMAGE -> {
                        viewModel.contentRetrieved()
                        viewModel.setTopImage(galleryUri)
                    }
                    AppResources.ContentType.DOCUMENT -> {
                        viewModel.contentRetrieved()
                        viewModel.appendFile(galleryUri)
                    }
                    else -> {
                        viewModel.contentRetrieved()
                    }
                }
            }
        }catch(e:Exception){
            e.printStackTrace()
        }
        viewModel.contentRetrieved()
    }

    val scope = rememberCoroutineScope()
    BackHandler {
       scope.launch {
           viewModel.onBackPressed()
       }
    }

    viewModel.setIsScriptFragment(true)
    AccountableTheme {
        ScriptFragmentView(
            modifier = Modifier.fillMaxSize(),
            viewModel,
            mainActivityViewModel,
            teleprompterSettings = null
        )
    }
}

enum class ContentPosition{ ABOVE, AT_CURSOR_POINT, BELOW }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptFragmentView(
    modifier: Modifier = Modifier.fillMaxSize(),
    viewModel: ScriptViewModel,
    mainActivityViewModel: MainActivityViewModel,
    teleprompterSettings: TeleprompterSettings? = null
) {
    val script by viewModel.script.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val markupLanguage by viewModel.markupLanguage.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AccountableTheme {
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val imageHeight = (
                (LocalResources.current.displayMetrics.heightPixels/3)
                        /LocalResources.current.displayMetrics.density
                ).dp

        viewModel.toolBarCollapsedFunction = { scrollBy, ifTrueRun ->
            scope.launch {
                if ((scrollBy>0 && scrollBehavior.state.collapsedFraction != 1f)
                    || (scrollBy<0 && scrollBehavior.state.collapsedFraction != 0f)) {
                    var leftOverScroll = 0f
                    val collapsedImageHeight = imageHeight.value * (1 - scrollBehavior.state.collapsedFraction)
                    if (scrollBy>0 && scrollBehavior.state.collapsedFraction != 1f){
                        leftOverScroll = collapsedImageHeight - scrollBy
                    }
                    if (leftOverScroll<0) {
                        scrollBehavior.nestedScrollConnection.onPreScroll(
                            available = Offset(0f, -collapsedImageHeight),
                            source = NestedScrollSource.UserInput
                        )
                        ifTrueRun(-leftOverScroll)
                    }
                    else {
                        scrollBehavior.nestedScrollConnection.onPreScroll(
                            available = Offset(0f, -scrollBy),
                            source = NestedScrollSource.UserInput
                        )
                    }
                } else ifTrueRun(scrollBy)
            }
        }


        script?.let { script ->
            val scriptUri by script.getUri(context).collectAsStateWithLifecycle(null)

            var image by remember { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(scriptUri) {
                image = scriptUri?.let { imageUri ->
                    AppResources.getBitmapFromUri(
                        context,
                        imageUri
                    )
                }
                    ?.asImageBitmap() ?: ImageBitmap(1, 1)
            }

            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            var bottomSheet by remember { mutableStateOf<Pair<Int?,Content>?>(null) }
            var textIndex by remember { mutableStateOf<Triple<Int?,Content, (String,Int)->Unit>?>(null) }
            var menuAddTimeStampTitle by remember { viewModel.menuAddTimeStampTitle }
            val cal = AppResources.CalendarResource(Calendar.getInstance())
            val date =
                cal.getFullDateStateFlow(LocalContext.current).collectAsState().value
            menuAddTimeStampTitle =
                if (script.scriptDateTime.getFullDateStateFlow(LocalContext.current)
                        .collectAsState().value
                    ==
                    date
                ) {
                    stringResource(R.string.add_time_stamp)
                } else {
                    stringResource(R.string.add_date_stamp)
                }

            val isEditingScript by viewModel.isEditingScript.collectAsStateWithLifecycle()
            val menuListOriginal = listOf(
                MenuItemData(
                    text = if (isEditingScript) stringResource(R.string.preview_script) else stringResource(
                        R.string.edit_script
                    ),
                    onClick = { viewModel.editOrSaveScript() }
                ),
                MenuItemData(
                    text = scriptUri?.let { stringResource(R.string.remove_image) }
                        ?: stringResource(R.string.choose_image),
                    onClick = {
                        viewModel.chooseTopImage { contentType ->
                            mainActivityViewModel.launchGalleryLauncher(contentType)
                        }
                    }
                ),
                MenuItemData(
                    text = menuAddTimeStampTitle,
                    onClick = {
                        textIndex?.let { textIndex ->
                            textIndex.first?.let { index ->
                                viewModel.addTimeStamp(
                                    context, index,
                                    textIndex.second, textIndex.third
                                )
                            }
                        }
                    }
                ),
                MenuItemData(
                    text = if (script.scriptMarkupLanguage == null)
                        stringResource(R.string.choose_markup_language)
                    else stringResource(
                        R.string.change_markup_language,
                        script.scriptMarkupLanguage!!
                    ),
                    onClick = { viewModel.saveScriptAndOpenMarkupLanguage() }
                ),
                MenuItemData(
                    text = stringResource(R.string.load_document),
                    onClick = {
                        viewModel.loadText { contentType ->
                            mainActivityViewModel.launchGalleryLauncher(contentType)
                        }
                    }
                ),
                MenuItemData(
                    text = stringResource(R.string.print_to_text_file),
                    onClick = { viewModel.printEntry() }
                )
            )
            val menuListNoTimeStamp =
                menuListOriginal.toMutableList().apply { removeAt(2) }.toList()
            var menuList by remember { mutableStateOf(menuListNoTimeStamp) }
            menuList = if (textIndex == null) menuListNoTimeStamp else menuListOriginal

            Scaffold(
                modifier = modifier
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
                    Box(Modifier.fillMaxWidth()) {
                        image?.let { image ->
                            Image(
                                bitmap = image,
                                contentDescription = stringResource(R.string.script_display_image),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        }

                        LargeTopAppBar(
                            modifier = if (!viewModel.getIsScriptFragment())
                                    Modifier.height(imageHeight * (1 - scrollBehavior.state.collapsedFraction))
                                else Modifier,
                            expandedHeight = imageHeight,
                            navigationIcon = {
                                IconButton(
                                    modifier = Modifier,
                                    onClick = { if (viewModel.getIsScriptFragment()) scope.launch { viewModel.onBackPressed() }
                                                else (context as MainActivity).onBackPressedDispatcher.onBackPressed()
                                    })
                                {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.scripts_navigate_back_button),
                                        tint = Color.Black
                                    )
                                }
                            },
                            scrollBehavior = scrollBehavior,
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = if (scriptUri!=null) Color.Transparent else
                                    MaterialTheme.colorScheme.primary,
                                titleContentColor = Color.White,
                                scrolledContainerColor = if (scriptUri!=null) Color.Transparent else
                                    MaterialTheme.colorScheme.primary
                            ),
                            title = {},
                            actions = { if (viewModel.getIsScriptFragment()) {
                                if (textIndex!=null) {
                                    IconButton(
                                        modifier = Modifier,
                                        onClick = {
                                            bottomSheet =
                                                textIndex?.let { Pair(it.first, it.second) }
                                        })
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.AddCircle,
                                            contentDescription = stringResource(R.string.add_content),
                                            tint = Color.Black
                                        )
                                    }
                                }
                                IconButton(
                                    modifier = Modifier,
                                    onClick = {
                                        scope.launch {
                                            val result = viewModel.shareScript(context)
                                            val hasContent = result.first
                                            val intent = result.second
                                            withContext(Dispatchers.Main) {
                                                if (hasContent) context.startActivity(
                                                    Intent.createChooser(intent, null)
                                                )
                                                else Toast.makeText(
                                                    context,
                                                    "Nothing To Share",
                                                    Toast.LENGTH_SHORT
                                                )
                                                    .show()
                                            }
                                        }
                                    })
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = stringResource(R.string.share),
                                        tint = Color.Black
                                    )
                                }
                                IconButton(
                                    modifier = Modifier,
                                    onClick = {
                                        viewModel.saveScriptAndOpenTeleprompter()
                                    })
                                {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = stringResource(R.string.open_teleprompter),
                                        tint = Color.Black
                                    )
                                }
                                basicDropdownMenu(options = menuList).invoke(Modifier)
                            }}
                        )
                    }
                }
            ) { innerPadding ->
                ScriptFragmentCatalog(
                    viewModel = viewModel,
                    script = script,
                    markupLanguage = markupLanguage,
                    appSettings = appSettings,
                    teleprompterSettings = teleprompterSettings,
                    modifier = Modifier.padding(innerPadding),
                    isEditingScript = isEditingScript,
                    onBottomSheetChange = { bottomSheet = it },
                    onTextIndexChange = { textIndex = it }
                )
                if (bottomSheet != null) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            bottomSheet = null
                        },
                        sheetState = sheetState
                    ) {
                        val scriptContentList = remember { viewModel.scriptContentList }
                        var addType by remember { mutableStateOf<ContentType?>(null) }
                        var delete by remember { mutableStateOf(false) }

                        val aboveBelow by remember {
                            mutableStateOf(bottomSheet?.let {
                                getAboveBelowContentType(scriptContentList, it.second)
                            } ?: Pair(null, null))
                        }

                        addType?.let { type ->
                            if (delete) {
                                bottomSheet?.let { bottomSheetNotNull ->
                                    val content = bottomSheetNotNull.second
                                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                        GetContentCard(
                                            content,
                                            false,
                                            {},
                                            appSettings,
                                            markupLanguage,
                                            Modifier,
                                            teleprompterSettings
                                        )
                                        Button(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            shape = RectangleShape,
                                            colors = ButtonColors(
                                                Color.Red,
                                                Color.Black,
                                                Color.LightGray,
                                                Color.DarkGray
                                            ),
                                            onClick = {
                                                accountablePlayer.close(content)
                                                viewModel.deleteContent(content)
                                                bottomSheet = null
                                            }
                                        ) {
                                            Text(
                                                stringResource(
                                                    R.string.delete_content,
                                                    when (content.type) {
                                                        ContentType.TEXT -> stringResource(R.string.text)
                                                        ContentType.IMAGE -> stringResource(R.string.image)
                                                        ContentType.SCRIPT -> stringResource(R.string.script)
                                                        ContentType.VIDEO -> stringResource(R.string.video)
                                                        ContentType.DOCUMENT -> stringResource(R.string.document)
                                                        ContentType.AUDIO -> stringResource(R.string.audio)
                                                    }
                                                )
                                            )
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                ) {
                                    if (!(addType == ContentType.TEXT && aboveBelow.first == ContentType.TEXT)) {
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
                                                    if (accessor != null || nonMediaType != null) getMultipleContent(
                                                        accessor,
                                                        type,
                                                        ContentPosition.ABOVE,
                                                        bottomSheet.second,
                                                        bottomSheet.first,
                                                        scope,
                                                        viewModel,
                                                        mainActivityViewModel
                                                    )
                                                }
                                                bottomSheet = null
                                            }
                                        ) {
                                            Text(stringResource(R.string.add_above))
                                        }
                                    }
                                    if (bottomSheet?.second?.type == ContentType.TEXT) {
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
                                                    if (accessor != null || nonMediaType != null) getMultipleContent(
                                                        accessor,
                                                        type,
                                                        ContentPosition.AT_CURSOR_POINT,
                                                        bottomSheet.second,
                                                        bottomSheet.first,
                                                        scope,
                                                        viewModel,
                                                        mainActivityViewModel
                                                    )
                                                }
                                                bottomSheet = null
                                            }
                                        ) {
                                            Text(stringResource(R.string.add_at_cursor_point))
                                        }
                                    }
                                    if (!(addType == ContentType.TEXT && aboveBelow.second == ContentType.TEXT)) {
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
                                                    if (accessor != null || nonMediaType != null) getMultipleContent(
                                                        accessor,
                                                        type,
                                                        ContentPosition.BELOW,
                                                        bottomSheet.second,
                                                        bottomSheet.first,
                                                        scope,
                                                        viewModel,
                                                        mainActivityViewModel
                                                    )
                                                }
                                                bottomSheet = null
                                            }
                                        ) {
                                            Text(stringResource(R.string.add_below))
                                        }
                                    }
                                }
                            }
                        }
                            ?: run {
                                Column(
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                ) {
                                    if (textIndex == null && !(aboveBelow.first == ContentType.TEXT && aboveBelow.second == ContentType.TEXT)) {
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
                                        colors = ButtonColors(
                                            Color.Red,
                                            Color.Black,
                                            Color.LightGray,
                                            Color.DarkGray
                                        ),
                                        onClick = {
                                            delete = true
                                            addType = ContentType.TEXT
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
        }
    }
}

private fun getAboveBelowContentType(scriptContentList: List<Content>, content: Content):Pair<ContentType?, ContentType?>{
    var above: ContentType? = null
    var below: ContentType? = null
    if (scriptContentList.isNotEmpty()){
        val position = scriptContentList.indexOf(content)
        if (position!=0){
            above = scriptContentList[position-1].type
        }
        if (position!=(scriptContentList.size-1)){
            below = scriptContentList[position+1].type
        }
    }
    return Pair(above,below)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptFragmentCatalog(
    viewModel: ScriptViewModel,
    script: Script,
    markupLanguage: MarkupLanguage?,
    appSettings: AppSettings?,
    teleprompterSettings: TeleprompterSettings?,
    modifier: Modifier = Modifier,
    isEditingScript: Boolean,
    onBottomSheetChange: (Pair<Int?, Content>?) -> Unit,
    onTextIndexChange: (Triple<Int, Content, (String, Int) -> Unit>?) -> Unit
) {
    val listState = remember { viewModel.listState }

    val scriptTitle = remember { script.scriptTitle }
    val scriptTime by script.scriptDateTime.getTimeStateFlow(LocalContext.current).collectAsStateWithLifecycle()
    val scriptDayNum by script.scriptDateTime.getDayNumStateFlow(LocalContext.current).collectAsStateWithLifecycle()
    val scriptDayWord by script.scriptDateTime.getDayWordStateFlow(LocalContext.current).collectAsStateWithLifecycle()
    val scriptMonthYear by script.scriptDateTime.getMonthYearStateFlow(LocalContext.current).collectAsStateWithLifecycle()
    val teleprompterBackgroundColour by teleprompterSettings?.backgroundColour?.collectAsStateWithLifecycle()?:remember { mutableStateOf(null) }
    val teleprompterTextColor by teleprompterSettings?.textColour?.collectAsStateWithLifecycle()?:remember { mutableStateOf(null) }
    val teleprompterTextSize by (teleprompterSettings?.textSize?:MutableStateFlow(null)).collectAsStateWithLifecycle()


    val scriptContentList = remember { viewModel.scriptContentList }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().imePadding()
    ) {
        item(key = "The Top") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                elevation = CardDefaults.cardElevation(),
                colors = CardColors(
                    teleprompterBackgroundColour?.let { Color(it) }?:Color.White,
                    teleprompterTextColor?.let { Color(it) }?:Color.Black,
                    Color.LightGray,
                    Color.DarkGray
                ),
                shape = RectangleShape
            ) {
                val backgroundModifier = teleprompterBackgroundColour?.let { Modifier.background(Color(it)) }?:Modifier
                val teleprompterTextColor = teleprompterTextColor?.let { Color(it) }?:Color.Black

                Column(
                    modifier = backgroundModifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Row(
                        modifier = backgroundModifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = scriptTime,
                            fontSize = 40.sp,
                            modifier = backgroundModifier.weight(1f),
                            textAlign = TextAlign.Start,
                            color = teleprompterTextColor
                        )
                        Row(
                            modifier = backgroundModifier,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = backgroundModifier,
                                text = scriptDayNum,
                                fontSize = 40.sp,
                                color = teleprompterTextColor
                            )
                            Column {
                                Text(
                                    modifier = backgroundModifier,
                                    text = scriptDayWord,
                                    color = teleprompterTextColor.let { if (it!=Color.Black) it else Color.Cyan }
                                )
                                Text(modifier = backgroundModifier, text = scriptMonthYear,
                                    color = teleprompterTextColor)
                            }
                        }
                    }
                    if (isEditingScript) {
                        TextField(
                            state = scriptTitle,
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
                            text = scriptTitle.text.toString(),
                            modifier = backgroundModifier
                                .fillMaxWidth()
                                .padding(vertical = 15.dp, horizontal = 5.dp),
                            textAlign = TextAlign.Center,
                            fontSize = teleprompterTextSize?.sp?:24.sp,
                            fontWeight = FontWeight.Bold,
                            color = teleprompterTextColor
                        )
                    }
                }
            }
        }
        items(items = scriptContentList, key = {listContent-> listContent.id?:Random.nextLong()}) { content ->
            val mod = Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onBottomSheetChange(null to content) }
                )
            }
            // Add Content Cards
            GetContentCard(
                content,
                isEditingScript,
                {value -> onTextIndexChange(value)},
                appSettings,
                markupLanguage,
                mod,
                teleprompterSettings,
            )
        }
    }
}

private fun addContentView(chosenContentType: ContentType):Pair<AppResources.ContentType?,Content.NonMediaType?>{
    val accessor = when(chosenContentType){
        ContentType.TEXT -> null
        ContentType.IMAGE -> AppResources.ContentType.IMAGE
        ContentType.SCRIPT -> null
        ContentType.VIDEO -> AppResources.ContentType.VIDEO
        ContentType.DOCUMENT -> AppResources.ContentType.DOCUMENT
        ContentType.AUDIO -> AppResources.ContentType.AUDIO
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
    mediaType: AppResources.ContentType?,
    contentType: ContentType,
    position: ContentPosition,
    content: Content,
    cursorPosition: Int?,
    lifecycleScope: CoroutineScope,
    viewModel: ScriptViewModel,
    mainActivityViewModel: MainActivityViewModel
){
    if (mediaType!=null){
        viewModel.multipleContentsStateFlow.let { multipleContentsStateFlow ->
            viewModel.multipleContentsJob.set(
                lifecycleScope.launch {
                    multipleContentsStateFlow.collectLatest { list ->
                        if (list != null) {
                            viewModel.addContent(list, contentType, position, content, cursorPosition)
                            multipleContentsStateFlow.value = null
                            viewModel.multipleContentsJob.get()?.cancel()
                        }
                    }
                })
            mainActivityViewModel.launchGalleryLauncherMultiple(mediaType)
        }
    }
    else {
        viewModel.addContent( null, contentType, position, content, cursorPosition)
        viewModel.multipleContentsJob.get()?.cancel()
        viewModel.multipleContentsStateFlow.value = null
    }
}
