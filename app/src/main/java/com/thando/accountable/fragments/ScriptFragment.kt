package com.thando.accountable.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.thando.accountable.AccountableRepository.Companion.accountablePlayer
import com.thando.accountable.AppResources
import com.thando.accountable.MainActivity
import com.thando.accountable.MainActivity.Companion.collectFlow
import com.thando.accountable.R
import com.thando.accountable.database.tables.AppSettings
import com.thando.accountable.database.tables.Content
import com.thando.accountable.database.tables.Content.ContentType
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.database.tables.Script
import com.thando.accountable.database.tables.TeleprompterSettings
import com.thando.accountable.fragments.ScriptFragment.ContentPosition
import com.thando.accountable.fragments.viewmodels.ScriptViewModel
import com.thando.accountable.ui.cards.GetContentCard
import com.thando.accountable.ui.cards.TextFieldAccountable
import com.thando.accountable.ui.management.states.toolbar.FixedScrollFlagState
import com.thando.accountable.ui.management.states.toolbar.ToolbarState
import com.thando.accountable.ui.screens.CollapsedPadding
import com.thando.accountable.ui.screens.ContentPadding
import com.thando.accountable.ui.screens.Elevation
import com.thando.accountable.ui.screens.ExpandedPadding
import com.thando.accountable.ui.screens.MaxToolbarHeight
import com.thando.accountable.ui.screens.MenuItemData
import com.thando.accountable.ui.screens.MinToolbarHeight
import com.thando.accountable.ui.screens.basicDropdownMenu
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

class ScriptFragment : Fragment() {
    val viewModel : ScriptViewModel by viewModels { ScriptViewModel.Factory }
    private val galleryLauncherMultiple = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ){
        list -> multipleContentsStateFlow.value = list
    }
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { galleryUri ->
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

    private val multipleContentsStateFlow: MutableStateFlow<List<@JvmSuppressWildcards Uri>?> = MutableStateFlow(null)
    private val multipleContentsJob = AtomicReference<Job?>(null)

    enum class ContentPosition{ ABOVE, AT_CURSOR_POINT, BELOW }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val mainActivity = (requireActivity() as MainActivity)
        mainActivity.viewModel.toolbarVisible.value = false

        return ComposeView(requireContext()).apply {
            WindowCompat.setDecorFitsSystemWindows(mainActivity.window, false)
            setContent {
                mainActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner,
                    object : OnBackPressedCallback(true) {
                        override fun handleOnBackPressed() {
                            viewModel.onBackPressed()
                        }
                    }
                )
                AccountableTheme {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        viewModel.setIsScriptFragment(true)
                        ScriptFragmentView(
                            modifier = Modifier.padding(innerPadding),
                            viewModel,
                            galleryLauncher,
                            galleryLauncherMultiple,
                            multipleContentsJob,
                            multipleContentsStateFlow
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        activity?.lifecycleScope?.launch {
            withContext(Dispatchers.IO) {
                viewModel.prepareToClose {}
            }
        }
        super.onPause()
    }
}

@Composable
fun ScriptFragmentView(
    modifier: Modifier = Modifier,
    viewModel: ScriptViewModel,
    galleryLauncher: ActivityResultLauncher<String>? = null,
    galleryLauncherMultiple: ActivityResultLauncher<String>? = null,
    multipleContentsJob: AtomicReference<Job?>? = null ,
    multipleContentsStateFlow: MutableStateFlow<List<@JvmSuppressWildcards Uri>?>? = null
) {
    val script by viewModel.script.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val markupLanguage by viewModel.markupLanguage.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    script?.let { script ->
        ScriptFragmentCatalog(
            viewModel = viewModel,
            multipleContentsJob = multipleContentsJob,
            multipleContentsStateFlow = multipleContentsStateFlow,
            galleryLauncher = galleryLauncher,
            galleryLauncherMultiple = galleryLauncherMultiple,
            script = script,
            markupLanguage = markupLanguage,
            appSettings = appSettings,
            teleprompterSettings = null,
            modifier = modifier,
            collapseType = ToolbarState.CollapseType.EnterAlwaysCollapsed,
            navigationIcon = { modifier ->
                IconButton(
                    modifier = modifier,
                    onClick = { viewModel.onBackPressed() })
                {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.scripts_navigate_back_button)
                    )
                }
            },
            shareIcon = { modifier ->
                IconButton(
                    modifier = modifier,
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
                        contentDescription = stringResource(R.string.share)
                    )
                }
            },
            teleprompterIcon = { modifier ->
                IconButton(
                    modifier = modifier,
                    onClick = {
                        viewModel.saveScriptAndOpenTeleprompter()
                    })
                {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.open_teleprompter)
                    )
                }
            }
        )
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
    collapseType: ToolbarState.CollapseType = ToolbarState.CollapseType.EnterAlwaysCollapsed,
    navigationIcon:@Composable (Modifier)-> Unit,
    shareIcon:@Composable (Modifier)-> Unit,
    teleprompterIcon:@Composable (Modifier)-> Unit,
    multipleContentsJob: AtomicReference<Job?> ? = null,
    multipleContentsStateFlow: MutableStateFlow<List<@JvmSuppressWildcards Uri>?>? = null,
    galleryLauncher: ActivityResultLauncher<String>? = null,
    galleryLauncherMultiple: ActivityResultLauncher<String>? = null,
) {
    val imageUri: Uri? = script.getUri(LocalContext.current).collectAsStateWithLifecycle(null).value
    val toolbarHeightRange = with(LocalDensity.current) {
        MinToolbarHeight.roundToPx()..MaxToolbarHeight.roundToPx()
    }
    val toolbarState = ToolbarState.rememberToolbarState(
        if (viewModel.getIsScriptFragment())
            collapseType
        else ToolbarState.CollapseType.Scroll
        , toolbarHeightRange)
    val listState = remember { viewModel.listState }

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
    var textIndex by remember { mutableStateOf<Triple<Int?,Content, (String,Int)->Unit>?>(null) }
    val addContentButton:@Composable (Modifier)-> Unit = { modifier ->
        IconButton(
            modifier = modifier,
            onClick = {
                bottomSheet = textIndex?.let { Pair(it.first,it.second) }
            })
        {
            Icon(
                imageVector = Icons.Filled.AddCircle,
                contentDescription = stringResource(R.string.add_content)
            )
        }
    }

    var menuAddTimeStampTitle by remember { viewModel.menuAddTimeStampTitle }
    val cal = AppResources.CalendarResource(Calendar.getInstance())
    val date = cal.getFullDateStateFlow(LocalContext.current).collectAsState().value
    menuAddTimeStampTitle =
        if (script.scriptDateTime.getFullDateStateFlow(LocalContext.current).collectAsState().value
            ==
            date
        ) {
            stringResource(R.string.add_time_stamp)
        } else {
            stringResource(R.string.add_date_stamp)
        }

    val scriptUri by script.getUri(LocalContext.current).collectAsStateWithLifecycle()
    val isEditingScript by viewModel.isEditingScript.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val menuListOriginal = listOf(
        MenuItemData(
            text = if (isEditingScript) stringResource(R.string.preview_script) else stringResource(R.string.edit_script),
            onClick = { viewModel.editOrSaveScript() }
        ),
        MenuItemData(
            text = scriptUri?.let { stringResource(R.string.remove_image)}?:stringResource(R.string.choose_image),
            onClick = { viewModel.chooseTopImage { accessorString ->
                galleryLauncher?.launch(accessorString)
            } }
        ),
        MenuItemData(
            text = menuAddTimeStampTitle,
            onClick = {
                textIndex?.let { textIndex ->
                    textIndex.first?.let { index ->
                        viewModel.addTimeStamp(context, index,
                            textIndex.second, textIndex.third)
                    }
                }
            }
        ),
        MenuItemData(
            text = if (script.scriptMarkupLanguage==null)
                stringResource(R.string.choose_markup_language)
            else stringResource(R.string.change_markup_language,script.scriptMarkupLanguage!!),
            onClick = { viewModel.saveScriptAndOpenMarkupLanguage() }
        ),
        MenuItemData(
            text = stringResource(R.string.load_document),
            onClick = { viewModel.loadText { accessorString ->
                galleryLauncher?.launch(accessorString)
            } }
        ),
        MenuItemData(
            text = stringResource(R.string.print_to_text_file),
            onClick = { viewModel.printEntry() }
        )
    )
    val menuListNoTimeStamp = menuListOriginal.toMutableList().apply { removeAt(2) }.toList()
    var menuList by remember { mutableStateOf(menuListNoTimeStamp) }
    menuList = if (textIndex==null) menuListNoTimeStamp else menuListOriginal

    Box(modifier = modifier.nestedScroll(nestedScrollConnection)) {
        LazyScriptContentCatalog(
            viewModel = viewModel,
            script = script,
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
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .height(with(LocalDensity.current) { toolbarState.height.toDp() })
                .graphicsLayer { translationY = toolbarState.offset },
            progress = toolbarState.progress,
            progressMax = toolbarState.scrollOffset,
            imageUri,
            navigationIcon,
            if (textIndex!=null) addContentButton else null,
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
                                            galleryLauncherMultiple,
                                            multipleContentsJob,
                                            multipleContentsStateFlow
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
                                            galleryLauncherMultiple,
                                            multipleContentsJob,
                                            multipleContentsStateFlow
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
                                            galleryLauncherMultiple,
                                            multipleContentsJob,
                                            multipleContentsStateFlow
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
    lifecycleScope: CoroutineScope,
    viewModel: ScriptViewModel,
    galleryLauncherMultiple: ActivityResultLauncher<String>? = null,
    multipleContentsJob: AtomicReference<Job?> ? = null,
    multipleContentsStateFlow: MutableStateFlow<List<@JvmSuppressWildcards Uri>?>?=null
){
    if (mediaType!=null){
        multipleContentsStateFlow?.let { multipleContentsStateFlow ->
            multipleContentsJob?.set(
                collectFlow(
                    lifecycleScope,
                    multipleContentsStateFlow
                ) { list ->
                    if (list != null) {
                        viewModel.addContent(list, contentType, position, content, cursorPosition)
                        multipleContentsStateFlow.value = null
                        multipleContentsJob.get()?.cancel()
                    }
                })
            galleryLauncherMultiple?.launch(mediaType)
        }
    }
    else {
        viewModel.addContent( null, contentType, position, content, cursorPosition)
        multipleContentsJob?.get()?.cancel()
        multipleContentsStateFlow?.value = null
    }
}

@Composable
fun LazyScriptContentCatalog(
    viewModel: ScriptViewModel,
    script: Script,
    markupLanguage: MarkupLanguage?,
    teleprompterSettings: TeleprompterSettings?,
    appSettings: AppSettings?,
    textIndex:(Triple<Int,Content, (String,Int)->Unit>?)->Unit,
    onLongClickListener:(Pair<Int?,Content>)->Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val isEditingScript by viewModel.isEditingScript.collectAsStateWithLifecycle()

    val scriptTitle = remember { script.scriptTitle }
    val scriptTime by script.scriptDateTime.getTimeStateFlow(LocalContext.current).collectAsStateWithLifecycle()
    val scriptDayNum by script.scriptDateTime.getDayNumStateFlow(LocalContext.current).collectAsStateWithLifecycle()
    val scriptDayWord by script.scriptDateTime.getDayWordStateFlow(LocalContext.current).collectAsStateWithLifecycle()
    val scriptMonthYear by script.scriptDateTime.getMonthYearStateFlow(LocalContext.current).collectAsStateWithLifecycle()

    val scriptContentList = remember { viewModel.scriptContentList }
    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier.imePadding()
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
                    Color.DarkGray
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            scriptTime,
                            fontSize = 40.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                        Row(
                            modifier = Modifier,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                scriptDayNum,
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
                        TextFieldAccountable(
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
        items(items = scriptContentList, key = {listContent-> listContent.id?:Random.nextLong()}) { content ->
            val mod = Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClickListener(null to content) }
                )
            }
            // Add Content Cards
            GetContentCard(
                content,
                isEditingScript,
                textIndex,
                appSettings,
                markupLanguage,
                mod,
                teleprompterSettings,
            )
        }
    }
}

@Composable
fun ScriptsCollapsingToolbar(
    viewModel: ScriptViewModel,
    modifier: Modifier = Modifier,
    progress: Float,
    progressMax: Float,
    imageUri: Uri?,
    navigationIcon:@Composable (Modifier)-> Unit,
    addContentButton:(@Composable (Modifier)-> Unit)?,
    shareIcon:@Composable (Modifier)-> Unit,
    teleprompterIcon:@Composable (Modifier)-> Unit,
    basicDropdownMenu:@Composable (Modifier)-> Unit,
) {
    val logoPadding = with(LocalDensity.current) {
        lerp(CollapsedPadding.toPx(), ExpandedPadding.toPx(), progress).toDp()
    }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = Elevation
    ) {
        Box (modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()) {
            //#region Background Image
            imageUri?.let {
                Image(
                    bitmap = AppResources.getBitmapFromUri(
                        LocalContext.current,
                        imageUri
                    )
                        ?.asImageBitmap() ?: ImageBitmap(1, 1),
                    contentDescription = stringResource(R.string.script_display_image),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = progress
                        }
                )
            }
            //#endregion
            if (viewModel.getIsScriptFragment()) {
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = ContentPadding)
                        .fillMaxSize()
                ) {
                    addContentButton?.let {
                        ScriptsCollapsingToolbarLayout(progress = progress) {
                            val mod = Modifier
                                .padding(logoPadding)
                                .wrapContentWidth()
                            navigationIcon(mod)
                            Text(modifier = mod.graphicsLayer {
                                alpha = progressMax - progress
                            }, text = stringResource(R.string.script))
                            shareIcon(mod)
                            teleprompterIcon(mod)
                            basicDropdownMenu(mod)
                            addContentButton(mod)
                        }
                    } ?: run {
                        ScriptsCollapsingToolbarLayout(progress = progress) {
                            val mod = Modifier
                                .padding(logoPadding)
                                .wrapContentWidth()
                            navigationIcon(mod)
                            Text(modifier = mod.graphicsLayer {
                                alpha = progressMax - progress
                            }, text = stringResource(R.string.script))
                            shareIcon(mod)
                            teleprompterIcon(mod)
                            basicDropdownMenu(mod)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScriptsCollapsingToolbarLayout(
    progress: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        check(measurables.size == 5 || measurables.size == 6)

        val items = measurables.map {
            it.measure(constraints)
        }
        layout(
            width = constraints.maxWidth,
            height = constraints.maxHeight
        ) {
            val navigationIcon = items[0]
            val titleText = items[1]
            val shareIcon = items[2]
            val teleprompterIcon = items[3]
            val basicDropdownMenu = items[4]
            val addContentIcon = if (measurables.size == 6) items[5] else null
            navigationIcon.placeRelative(
                x = 0,
                y = lerp(
                    start = (constraints.maxHeight - navigationIcon.height) / 2,
                    stop = 0,
                    fraction = progress
                )
            )
            titleText.placeRelative(
                x = navigationIcon.width + titleText.width/2 + ((constraints.maxWidth - basicDropdownMenu.width - teleprompterIcon.width - shareIcon.width - (addContentIcon?.width
                    ?: 0) - navigationIcon.width)/2)/2,
                y = lerp(
                    start = (constraints.maxHeight - titleText.height) / 2,
                    stop = 0,
                    fraction = progress
                )
            )
            addContentIcon?.placeRelative(
                x = constraints.maxWidth - basicDropdownMenu.width - teleprompterIcon.width - shareIcon.width - addContentIcon.width,
                y = lerp(
                    start = (constraints.maxHeight - addContentIcon.height) / 2,
                    stop = 0,
                    fraction = progress
                )
            )
            shareIcon.placeRelative(
                x = constraints.maxWidth - basicDropdownMenu.width - teleprompterIcon.width - shareIcon.width,
                y = lerp(
                    start = (constraints.maxHeight - shareIcon.height) / 2,
                    stop = 0,
                    fraction = progress
                )
            )
            teleprompterIcon.placeRelative(
                x = constraints.maxWidth - basicDropdownMenu.width - teleprompterIcon.width,
                y = lerp(
                    start = (constraints.maxHeight - teleprompterIcon.height) / 2,
                    stop = 0,
                    fraction = progress
                )
            )
            basicDropdownMenu.placeRelative(
                x = constraints.maxWidth - basicDropdownMenu.width,
                y = lerp(
                    start = (constraints.maxHeight - basicDropdownMenu.height) / 2,
                    stop = 0,
                    fraction = progress
                )
            )
        }
    }
}
