package com.thando.accountable.fragments

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thando.accountable.IntentActivity
import com.thando.accountable.MainActivity
import com.thando.accountable.R
import com.thando.accountable.fragments.viewmodels.SearchViewModel
import com.thando.accountable.ui.cards.TextFieldAccountable
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private val viewModel: SearchViewModel by viewModels { SearchViewModel.Factory }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            WindowCompat.setDecorFitsSystemWindows(
                if (viewModel.intentString==null) (requireActivity() as MainActivity).window
                else (requireActivity() as IntentActivity).window,
                false
            )
            setContent {
                if (viewModel.intentString==null) {
                    val mainActivity = (requireActivity() as MainActivity)
                    mainActivity.viewModel.toolbarVisible.value = false
                    mainActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner,
                        object : OnBackPressedCallback(true) {
                            override fun handleOnBackPressed() {
                                viewModel.navigateToFoldersAndScripts()
                            }
                        }
                    )
                }
                else{
                    val intentActivity = (requireActivity() as IntentActivity)
                    intentActivity.dialogFragment.dialog?.setOnKeyListener { _, keyCode, event ->
                        if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                            viewModel.navigateToFoldersAndScripts()
                            true
                        }
                        else false
                    }
                }

                var menuOpen by remember { viewModel.searchMenuOpen }
                val scope = rememberCoroutineScope()
                var matchCaseCheck by remember { viewModel.matchCaseCheck }
                var wordCheck by remember { viewModel.wordCheck }
                AccountableTheme {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { Text(stringResource(R.string.search)) },
                                navigationIcon = { IconButton(onClick = {
                                    scope.launch{ viewModel.navigateToFoldersAndScripts() }
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.search_navigate_back_button)
                                    )
                                } },
                                actions = {
                                    IconButton(onClick = {viewModel.toggleMenuOpen()}) {
                                        Icon(
                                            if (menuOpen) Icons.Default.KeyboardArrowUp
                                            else Icons.Default.KeyboardArrowDown,
                                            contentDescription = stringResource(R.string.open_search_input_view)
                                        )
                                    }
                                    val selectableColors = SelectableChipColors(
                                        containerColor = Color.Transparent,
                                        disabledContainerColor = Color.LightGray,
                                        disabledLeadingIconColor = Color.Gray,
                                        disabledSelectedContainerColor = Color(
                                            context.resources.getColor(
                                                R.color.purple_700, null
                                            )
                                        ),
                                        selectedContainerColor = Color(
                                            context.resources.getColor(
                                                R.color.purple_200, null
                                            )
                                        ),
                                        selectedLeadingIconColor = Color.Black,
                                        labelColor = Color.Black,
                                        leadingIconColor = Color.Black,
                                        trailingIconColor = Color.Black,
                                        disabledLabelColor = Color.Gray,
                                        disabledTrailingIconColor = Color.Gray,
                                        selectedLabelColor = Color.Black,
                                        selectedTrailingIconColor = Color.Black
                                    )
                                    FilterChip(
                                        selected = matchCaseCheck,
                                        onClick = { viewModel.toggleMatchCaseCheck() },
                                        label = { Text(stringResource(R.string.case_string)) },
                                        leadingIcon = null,
                                        colors = selectableColors
                                    )
                                    FilterChip(
                                        selected = wordCheck,
                                        onClick = { viewModel.toggleWordCheck() },
                                        label = { Text(stringResource(R.string.word)) },
                                        leadingIcon = null,
                                        colors = selectableColors
                                    )
                                }
                            )
                        }
                    ) { innerPadding ->
                        SearchFragmentView(modifier = Modifier.padding(innerPadding), menuOpen)
                    }
                }
            }
        }
    }

    @Composable
    fun SearchFragmentView(modifier: Modifier = Modifier, menuOpen: Boolean){
        val searchString = remember { viewModel.searchString }
        val matchCaseCheck by remember { viewModel.matchCaseCheck }
        val wordCheck by remember { viewModel.wordCheck }
        val searchJob by viewModel.searchJob.collectAsStateWithLifecycle()
        val numScripts by viewModel.numScripts.collectAsStateWithLifecycle()
        val occurrences by viewModel.occurrences.collectAsStateWithLifecycle()
        val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
        val textSize by MutableStateFlow(appSettings?.textSize?:24).collectAsStateWithLifecycle()

        var initialized by remember { viewModel.initialized }
        val initScrollValues by remember { mutableStateOf(Pair(
            viewModel.searchScrollPosition.firstVisibleItemIndex,
            viewModel.searchScrollPosition.firstVisibleItemScrollOffset
        )) }

        val bringIntoViewRequester = remember { BringIntoViewRequester() }
        val scope = rememberCoroutineScope()

        LaunchedEffect(searchString.selection, matchCaseCheck, wordCheck) {
            viewModel.search {
                if (initialized){
                    if (viewModel.searchScrollPosition.firstVisibleItemIndex!=0 ||
                        viewModel.searchScrollPosition.firstVisibleItemScrollOffset!=0){
                        viewModel.setScrollPosition(0,0)
                    }
                }
                else{
                    viewModel.searchScrollPosition.requestScrollToItem(
                        initScrollValues.first,
                        initScrollValues.second
                    )
                    viewModel.initialized()
                }
            }
        }

        Column(modifier = modifier.fillMaxWidth()) {
            if (menuOpen) {
                TextFieldAccountable(
                    state = searchString,
                    modifier = Modifier.fillMaxWidth().bringIntoViewRequester(bringIntoViewRequester)
                        .onFocusEvent { focusState ->
                            if (focusState.isFocused) {
                                scope.launch {
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                    placeholder = { Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = textSize.sp,
                        text = stringResource(R.string.search)
                    ) },
                    textStyle = TextStyle(
                        textAlign = TextAlign.Center,
                        color = Color.Black,
                        fontSize = textSize.sp
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, // Removes gray background
                        unfocusedContainerColor = Color.Transparent, // Removes gray background
                        focusedIndicatorColor = Color(
                            LocalResources.current.getColor(
                                R.color.purple_200,null
                            )
                        ),
                        unfocusedIndicatorColor = Color(
                            LocalResources.current.getColor(
                                R.color.purple_200,null
                            )
                        ),
                        disabledIndicatorColor = Color.Gray
                    )
                )
            }
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                var stringHeightPx by remember { mutableIntStateOf(0) }
                if (searchString.text.isNotEmpty()){
                    Text(
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                            .padding(horizontal = 3.dp)
                            .onGloballyPositioned { coordinates ->
                            // Get height in pixels
                            stringHeightPx = coordinates.size.height
                        },
                        text = stringResource(R.string.scripts_and_occurrences,numScripts, occurrences))
                }
                searchJob?.let { _ ->
                    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 1000,
                                easing = LinearEasing
                            ),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rotationAnim"
                    )
                    val size = (stringHeightPx / LocalResources.current.displayMetrics.density).dp
                    // Draw a custom circular progress indicator
                    val contextResources = LocalResources.current
                    Canvas(
                        modifier = Modifier
                            .size(size)
                            .rotate(rotation)
                            .padding(3.dp)
                    ) {
                        drawArc(
                            color = Color(
                                contextResources.getColor(
                                    R.color.purple_200,null
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 270f, // Arc length
                            useCenter = false,
                            style = Stroke(width = (size/10).toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
            SearchContent()
        }
    }

    @Composable
    fun SearchContent(){
        val scriptsList = remember { viewModel.scriptsList }
        LazyColumn(
            state = viewModel.searchScrollPosition,
            contentPadding = PaddingValues(2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(items = scriptsList, key = {"${it.folderId}:${it.script.scriptId}"}) { result ->
                ScriptContentCard(result)
            }
        }
    }

    @Composable
    fun ScriptContentCard(item: SearchViewModel.ScriptSearch){
        val leftButtonVisibility by item.leftButtonVisibility.collectAsStateWithLifecycle()
        val rightButtonVisibility by item.rightButtonVisibility.collectAsStateWithLifecycle()
        val snippetIndex by item.snippetIndex.collectAsStateWithLifecycle()

        Column(modifier = Modifier.fillMaxSize()) {
            ScriptCard(script = item.script,
                modifier = Modifier
                    .padding(2.dp)
                    .fillMaxWidth(),
                getScriptContentPreview = { viewModel.getScriptContentPreview(it) },
                onLongClick = {},
                onClick = {
                    item.script.scriptId?.let {
                        viewModel.loadAndOpenScript( it,activity)
                    }
                }
            )
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                if (leftButtonVisibility) {
                    IconButton(
                        onClick = { item.moveLeft() },
                        modifier = Modifier.fillMaxHeight(),
                        colors = IconButtonColors(
                            containerColor = Color(
                                LocalResources.current.getColor(
                                    R.color.purple_200,null
                                )
                            ),
                            contentColor = Color.Black,
                            disabledContentColor = Color.Gray,
                            disabledContainerColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowLeft,
                            contentDescription = stringResource(R.string.move_left)
                        )
                    }
                }
                Text(
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                    text = item.snippets[snippetIndex]
                )
                if (rightButtonVisibility) {
                    IconButton(
                        onClick = { item.moveRight() },
                        modifier = Modifier.fillMaxHeight(),
                        colors = IconButtonColors(
                            containerColor = Color(
                                LocalResources.current.getColor(
                                    R.color.purple_200,null
                                )
                            ),
                            contentColor = Color.Black,
                            disabledContentColor = Color.Gray,
                            disabledContainerColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                            contentDescription = stringResource(R.string.move_right)
                        )
                    }
                }
            }
            HorizontalDivider(
                color = Color(LocalResources.current.getColor(R.color.purple_200,null)),
                thickness = 2.dp
            )
        }
    }
}