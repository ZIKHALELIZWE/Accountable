package com.thando.accountable.fragments

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import com.thando.accountable.MainActivity
import com.thando.accountable.R
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.database.tables.SpecialCharacters
import com.thando.accountable.database.tables.TeleprompterSettings
import com.thando.accountable.fragments.viewmodels.ScriptViewModel
import com.thando.accountable.fragments.viewmodels.TeleprompterViewModel
import com.thando.accountable.recyclerviewadapters.SpecialCharacterItemAdapter
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class TeleprompterFragment : Fragment() {

    private val viewModel : TeleprompterViewModel by viewModels { TeleprompterViewModel.Factory }
    private val scriptViewModel : ScriptViewModel by viewModels { ScriptViewModel.Factory }
    private var listState: LazyListState? = null
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var specialCharactersAdapter: SpecialCharacterItemAdapter

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val navigateToScript by viewModel.navigateToScript.collectAsStateWithLifecycle(false)
                LaunchedEffect(navigateToScript) {
                    if (navigateToScript){
                        viewModel.viewModelScope.launch {
                            withContext(Dispatchers.IO) {
                                viewModel.closeTeleprompterFragment()
                            }
                        }
                    }
                }

                val mainActivity = (requireActivity() as MainActivity)
                mainActivity.onBackPressedDispatcher.addCallback(
                    viewLifecycleOwner,
                    object : OnBackPressedCallback(true){
                        override fun handleOnBackPressed() {
                            viewModel.navigateToScript()
                        }
                    }
                )
                AccountableTheme {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        TeleprompterFragmentView(
                            modifier = Modifier.padding(innerPadding),
                            scriptViewModel = scriptViewModel
                        )
                    }
                }
            }
        }
    }

    // Controller object to bridge Activity and Compose
    object TeleprompterController {
        private var skipBackHandler: (() -> Unit)? = null
        fun registerSkipBack(handler: () -> Unit) {
            skipBackHandler = handler
        }
        fun skipBack() {
            skipBackHandler?.invoke()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ControlsSection(
        isPlaying: Boolean,
        markupLanguage: MarkupLanguage?,
        spinnerEnabled: Boolean,
        teleprompterSettings: TeleprompterSettings?,
        teleprompterSettingsList: SnapshotStateList<TeleprompterSettings>,
        specialCharactersList: SnapshotStateList<SpecialCharacters>?,
        deleteButtonText: String,
        spinnerExpanded: Boolean,
        onSpinnerExpandedChanged: (Boolean) -> Unit,
        setSpinnerView: (View) -> Unit,
        togglePlayPause: () -> Unit,
        remoteConnected: Boolean,
        controlsAtTop: Boolean,
        countDownTimer: CountDownTimer?,
        onCountDownTimerChanged: (CountDownTimer?) -> Unit,
        modifier: Modifier = Modifier
    ) {
        val context = LocalContext.current
        val height = (LocalResources.current.displayMetrics.heightPixels*2)/5
        val scope = rememberCoroutineScope()
        LazyColumn(modifier = modifier
            .padding(16.dp)
            .height((height / LocalResources.current.displayMetrics.density).dp),
            state = rememberLazyListState(), reverseLayout = controlsAtTop
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = {skip(false, scope,
                        countDownTimer,
                        onCountDownTimerChanged)},
                        shape = RectangleShape) { Text("Backward") }
                    Button(onClick = togglePlayPause,
                        shape = RectangleShape) { if (isPlaying) Text("Pause") else Text("Play") }
                    Button(onClick = {skip(true, scope,
                        countDownTimer,
                        onCountDownTimerChanged)},
                        shape = RectangleShape) { Text("Forward") }
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                val selectedOptionName by teleprompterSettings?.name?.collectAsStateWithLifecycle()
                    ?:remember { mutableStateOf("") }
                ExposedDropdownMenuBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary),
                    expanded = spinnerExpanded,
                    onExpandedChange = { if (spinnerEnabled) onSpinnerExpandedChanged(!spinnerExpanded) },
                ) {
                    setSpinnerView(LocalView.current)
                    val fillMaxWidth = Modifier.fillMaxWidth()
                    TextField(
                        value = selectedOptionName,
                        onValueChange = {},
                        readOnly = true,
                        enabled = spinnerEnabled,
                        label = {
                            Text(
                                "Choose an option",
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = if (spinnerExpanded)
                                    Icons.Filled.ArrowDropUp
                                else Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                tint = if (spinnerEnabled) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                            )
                        },
                        modifier = fillMaxWidth.menuAnchor(
                            ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                            spinnerEnabled
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                            focusedIndicatorColor = MaterialTheme.colorScheme.surface,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                            cursorColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    if (spinnerEnabled) {
                        ExposedDropdownMenu(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary),
                            expanded = spinnerExpanded,
                            onDismissRequest = { onSpinnerExpandedChanged(false) }
                        ) {
                            teleprompterSettingsList.forEach { selectionOption ->
                                val selectionOptionName by selectionOption.name.collectAsStateWithLifecycle()
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            selectionOptionName,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSelectedIndex(
                                            teleprompterSettingsList.indexOf(selectionOption)
                                        )
                                        onSpinnerExpandedChanged(false)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            teleprompterSettings?.let { teleprompterSettings ->
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                        Button(
                            onClick = { viewModel.changeTeleprompterSettingsName() },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(1.dp),
                            shape = RectangleShape,
                            colors = ButtonColors(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.onPrimary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                stringResource(R.string.change_name),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Button(
                            onClick = { viewModel.restoreToDefaultTeleprompterSettings() },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(1.dp),
                            shape = RectangleShape,
                            colors = ButtonColors(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.onPrimary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                deleteButtonText,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    val scrollSpeed by remember {
                        teleprompterSettings.scrollSpeed
                    }
                    Text(stringResource(R.string.scroll_speed, scrollSpeed))
                }
                item {
                    var scrollSpeed by remember {
                        teleprompterSettings.scrollSpeed
                    }
                    Slider(
                        value = scrollSpeed.toFloat(),
                        onValueChange = { scrollSpeed = it.toInt() },
                        valueRange = 1f..60f
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    val skipSize by remember {
                        teleprompterSettings.skipSize
                    }
                    Text(stringResource(R.string.skip_size, skipSize))
                }
                item {
                    var skipSize by remember {
                        teleprompterSettings.skipSize
                    }
                    Slider(
                        value = skipSize.toFloat(),
                        onValueChange = { skipSize = it.toInt() },
                        valueRange = 100f..3000f
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    val textSize by remember {
                        teleprompterSettings.textSize
                    }
                    Text(stringResource(R.string.text_size, textSize))
                }
                item {
                    var textSize by remember {
                        teleprompterSettings.textSize
                    }
                    Slider(
                        value = textSize.toFloat(),
                        onValueChange = { textSize = it.toInt() },
                        valueRange = 12f..70f
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    val startCountDown by remember {
                        teleprompterSettings.startCountDown
                    }
                    Text(stringResource(R.string.start_count_down, startCountDown))
                }
                item {
                    var startCountDown by remember {
                        teleprompterSettings.startCountDown
                    }
                    Slider(
                        value = startCountDown.toFloat(),
                        onValueChange = { startCountDown = it.toInt() },
                        valueRange = 0f..10f
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    var scrollCountDown by remember {
                        teleprompterSettings.scrollCountDown
                    }
                    Text(stringResource(R.string.scroll_count_down, scrollCountDown))
                }
                item {
                    var scrollCountDown by remember {
                        teleprompterSettings.scrollCountDown
                    }
                    val range: ClosedFloatingPointRange<Float> = 0f..10f
                    Slider(
                        value = scrollCountDown.toFloat(),
                        onValueChange = { scrollCountDown = it.toInt() },
                        valueRange = range
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    val textColor by teleprompterSettings.textColour.collectAsStateWithLifecycle()
                    Button(
                        onClick = { viewModel.chooseTextColour(context) },
                        shape = RectangleShape, modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Change Text Color (Current: $textColor)")
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    val backgroundColour by teleprompterSettings.backgroundColour.collectAsStateWithLifecycle()
                    Button(
                        onClick = { viewModel.chooseBackgroundColour(context) },
                        shape = RectangleShape, modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Change Background Color (Current: $backgroundColour)")
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    Button(
                        onClick = { viewModel.toggleControlsPosition() },
                        shape = RectangleShape, modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (controlsAtTop) "Move Controls to Bottom" else "Move Controls to Top")
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    // Remote connection button
                    Button(
                        enabled = remoteConnected,
                        onClick = {
                            // Change what the button does
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RectangleShape
                    ) {
                        Text(
                            if (remoteConnected) "Remote: Connected (Skip Back)"
                            else "Remote: Not Connected"
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item {
                    Button(
                        onClick = { viewModel.addSpecialCharacter() },
                        shape = RectangleShape, modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.add_special_character))
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (specialCharactersList!=null) {
                    items(specialCharactersList) { specialCharacter ->
                        var textSize by remember { teleprompterSettings.textSize }
                        SpecialCharacterCard(
                            specialCharacter,
                            context,
                            scope,
                            markupLanguage,
                            textSize.toFloat()
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun TeleprompterFragmentView(
        modifier: Modifier = Modifier,
        scriptViewModel: ScriptViewModel
    ){
        scriptViewModel.setIsScriptFragment(false)
        val script by viewModel.script.collectAsStateWithLifecycle()
        val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
        val controlsVisible by viewModel.controlsVisible.collectAsStateWithLifecycle()
        val isFullScreen by viewModel.isFullScreen.collectAsStateWithLifecycle()
        val countDownText by viewModel.countDownText.collectAsStateWithLifecycle()
        val finishCountdown by viewModel.finishCountdown.collectAsStateWithLifecycle(false)
        val teleprompterSettings by viewModel.teleprompterSettings.collectAsStateWithLifecycle()
        val teleprompterSettingsList = remember { viewModel.teleprompterSettingsList }
        val specialCharactersList = teleprompterSettings?.let { teleprompterSettings ->
            remember { teleprompterSettings.specialCharactersList }
        }
        val selectedIndex by viewModel.selectedIndex.collectAsStateWithLifecycle()
        val deleteButtonText by viewModel.deleteButtonText.collectAsStateWithLifecycle()
        val controlsAtTop by teleprompterSettings?.controlsPositionBottom?.collectAsStateWithLifecycle()
            ?: remember { mutableStateOf(false) }
        listState = remember { scriptViewModel.listState }
        var countDownTimer by remember { viewModel.countDownTimer }
        val markupLanguage by viewModel.markupLanguage.collectAsStateWithLifecycle()

        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        var remoteConnected by remember { mutableStateOf(false) }
        var isAtEndOfList by remember { mutableStateOf(false) }
        var spinnerExpanded by remember { mutableStateOf(false) }
        var spinnerEnabled by remember { mutableStateOf(false) }
        var spinnerView by remember{ mutableStateOf<View?>(null) }
        var nameNotUnique by remember { mutableStateOf("") }
        val nameNotUniqueErrorMessage = stringResource(R.string.name_is_not_unique, nameNotUnique)

        LaunchedEffect(script) {
            if (script!=null) {
                viewModel.loadTeleprompterSettings()
            }
        }

        LaunchedEffect(teleprompterSettingsList) {
            snapshotFlow { teleprompterSettingsList.toList() }.collect { teleprompterSettingsList ->
                if (teleprompterSettingsList.isEmpty()) {
                    spinnerEnabled = false
                    return@collect
                } else spinnerEnabled = true

                if (viewModel.script.value?.scriptMarkupLanguage == null) {
                    viewModel.setSelectedIndex(teleprompterSettingsList.size - 1)
                } else {
                    for ((index, language) in teleprompterSettingsList.withIndex()) {
                        if (language.id == viewModel.script.value?.scriptTeleprompterSettings) {
                            viewModel.setSelectedIndex(index)
                            break
                        }
                    }
                }
            }
        }

        LaunchedEffect(specialCharactersList){
            viewModel.updateStates()
            viewModel.emitUpdateContentAdapterSpecialCharacters()
        }

        LaunchedEffect(selectedIndex){
            viewModel.loadTeleprompterSetting(selectedIndex)
        }

        LaunchedEffect(teleprompterSettings, teleprompterSettingsList){
            snapshotFlow { teleprompterSettingsList.toList() }.collect { teleprompterSettingsList ->
                if (teleprompterSettings != null && teleprompterSettingsList.isNotEmpty()) {
                    val height = requireActivity().window.decorView.height
                    viewModel.setTeleprompterSettingsFunctions(
                        teleprompterSettings!!,
                        context,
                        height
                    )
                }
            }
        }

        LaunchedEffect(viewModel.showNameNotUniqueSnackBar){
            if (spinnerView==null) return@LaunchedEffect
            viewModel.showNameNotUniqueSnackBar.collect { name ->
                nameNotUnique = name
                Snackbar.make(
                    spinnerView!!,
                    nameNotUniqueErrorMessage,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        LaunchedEffect(listState) {
            // Do not combine with lower "LaunchedEffect(listState)". It does not work
            listState?.let { listState ->
                snapshotFlow { listState.isScrollInProgress }
                    .collect { isScrolling ->
                        if (isScrolling) {
                            viewModel.scrolled()
                        }
                    }
            }
        }

        LaunchedEffect(listState) {
            listState?.let{ listState ->
                isAtEndOfListState(listState){ isAtEnd ->
                    isAtEndOfList = isAtEnd
                    if (isAtEnd){
                        viewModel.pause()
                    }
                }
            }
        }

        LaunchedEffect(isPlaying){
            if (isPlaying){
                if (isAtEndOfList){
                    viewModel.pause()
                }
                else{
                    startAutoScrollRunnable(false, coroutineScope,
                        countDownTimer, {countDownTimer = it})
                }
            }
            else{
                stopAutoScroll(true,countDownTimer, {countDownTimer = it})
            }
        }

        LaunchedEffect(finishCountdown){
            if (finishCountdown){
                countDownTimer?.onFinish()
                viewModel.countDownFinished()
            }
        }

        // Register skip back handler
        LaunchedEffect(Unit) {
            TeleprompterController.registerSkipBack {
                skip(false, coroutineScope, countDownTimer, {countDownTimer = it})
            }
        }

        // Hide/show system bars
        LaunchedEffect(isFullScreen) {
            if (isFullScreen) hideBars() else showBars()
        }

        Column(modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly)
        {
            val hideControlsButton = @Composable {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(Color.White),
                    onClick = { viewModel.toggleControlsVisible() },
                    shape = RectangleShape
                ) {
                    Text(if (controlsVisible) "Hide Controls" else "Show Controls")
                }
            }
            val controls = @Composable {
                ControlsSection(
                    modifier = Modifier.background(Color.White),
                    isPlaying = isPlaying,
                    markupLanguage = markupLanguage,
                    teleprompterSettings = teleprompterSettings,
                    teleprompterSettingsList = teleprompterSettingsList,
                    specialCharactersList = specialCharactersList,
                    deleteButtonText = deleteButtonText,
                    spinnerEnabled = spinnerEnabled,
                    spinnerExpanded = spinnerExpanded,
                    remoteConnected = remoteConnected,
                    togglePlayPause = { viewModel.playPause() },
                    controlsAtTop = controlsAtTop,
                    onSpinnerExpandedChanged = { spinnerExpanded = it },
                    setSpinnerView = { spinnerView = it },
                    countDownTimer = countDownTimer,
                    onCountDownTimerChanged = {countDownTimer = it}
                )
            }
            // Controls at top (only if not full screen)
            if (controlsAtTop && controlsVisible && !isFullScreen) controls()
            if (controlsAtTop && !isFullScreen) hideControlsButton()

            Box(Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)) {
                ScriptFragmentView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { viewModel.toggleFullScreen() })
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { change ->
                                        if (change.changedToDown()) {
                                            if (isPlaying) stopAutoScroll(
                                                false,
                                                countDownTimer, { countDownTimer = it })
                                            viewModel.isTouching()
                                        } else if (change.changedToUp()) {
                                            if (isPlaying) {
                                                if (viewModel.getScrolledValue()) startAutoScrollRunnable(
                                                    true,
                                                    coroutineScope,
                                                    countDownTimer,
                                                    { countDownTimer = it }
                                                )
                                                else if (countDownTimer == null) startAutoScroll(
                                                    coroutineScope,
                                                    countDownTimer,
                                                    { countDownTimer = it }
                                                )
                                            }
                                            viewModel.isNotTouching()
                                        }
                                    }
                                }
                            }
                        },
                    viewModel = scriptViewModel,
                    teleprompterSettings = teleprompterSettings
                )
                if (countDownTimer != null) {
                    Button(
                        modifier = Modifier
                            .padding(16.dp),
                        shape = RectangleShape,
                        colors = ButtonColors(
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.onSecondary,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f)
                        ),
                        onClick = { viewModel.finishCountDown() }
                    ) {
                        Text(countDownText)
                    }
                }
            }

            // Toggle buttons (only if not full screen)
            if (!controlsAtTop && !isFullScreen) hideControlsButton()
            // Controls at bottom (only if not full screen)
            if (!controlsAtTop && controlsVisible && !isFullScreen) controls()
        }
        /*

        collectFlow(this,viewModel.script){ script ->
            script?.scriptTitle?.let { title ->
                viewModel.setScriptTitleEdited(title.text.toString())
            }
        }

        collectFlow(this,viewModel.teleprompterSettingsList){ settingsList ->
            if (settingsList.isEmpty()) return@collectFlow
            binding.rootConstraintLayoutHeight = binding.rootConstraintLayout.height

            val adapter = ArrayAdapter(requireContext(),android.R.layout.simple_list_item_1,settingsList)
            binding.teleprompterSettingsSpinner.adapter = adapter

            var initializing = 0
            binding.teleprompterSettingsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?, position: Int, id: Long
                ) {
                    if (initializing == 0){
                        initializing++
                        if (viewModel.script.value?.scriptTeleprompterSettings == null){
                            binding.teleprompterSettingsSpinner.setSelection(settingsList.size-1)
                            viewModel.setSelectedIndex(
                                settingsList.size-1
                            )
                            if (settingsList.size-1==0) initializing++
                        }
                        else{
                            for ((index,setting) in settingsList.withIndex()){
                                if (setting.id == viewModel.script.value?.scriptTeleprompterSettings){
                                    binding.teleprompterSettingsSpinner.setSelection(index)
                                    viewModel.setSelectedIndex(
                                        index
                                    )
                                    if(index == 0) initializing++
                                    break
                                }
                            }
                        }
                    }
                    else{
                        if(initializing == 1){
                            initializing++
                        }
                        else if (initializing>1){
                            viewModel.setSelectedIndex(
                                position
                            )
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }
            }
        }

        var controlsPositionBottomJob:Job? = null
        var backgroundColourJob:Job? = null
        var settingsNameJob:Job? = null
        collectFlow(this,viewModel.teleprompterSettings){ teleprompterSettings ->
            if (teleprompterSettings!=null && viewModel.teleprompterSettingsList.value.isNotEmpty()){
                viewModel.setTeleprompterSettingsFunctions(teleprompterSettings, requireContext(),
                    binding.rootConstraintLayout.height)
                contentAdapter.setTeleprompterSettings(viewModel.teleprompterSettings)
                viewModel.setSkipSizeValue(binding.rootConstraintLayout.height)

                binding.restoreToDefaultSettingsButton.isEnabled = true
                binding.restoreToDefaultSettingsButton.text =
                    if (teleprompterSettings == viewModel.teleprompterSettingsList.value.last())
                        getString(R.string.restore_default_settings)
                    else
                        getString(R.string.delete)

                binding.changeSettingsNameButton.isEnabled = true
                settingsNameJob?.cancel()
                settingsNameJob = collectFlow(this,teleprompterSettings.name){
                    binding.changeSettingsNameButton.text =
                        if (it == TeleprompterSettings().name.value) getString(R.string.change_name_to_save)
                        else getString(R.string.change_name)
                }

                backgroundColourJob?.cancel()
                backgroundColourJob = collectFlow(this,teleprompterSettings.backgroundColour){
                    binding.root.setBackgroundColor(it)
                }

                controlsPositionBottomJob?.cancel()
                controlsPositionBottomJob = collectFlow(this,teleprompterSettings.controlsPositionBottom){ positionBottom ->
                    if (positionBottom){
                        binding.controlsPositionButton.text = getString(R.string.move_controls_to_top)
                        binding.contentSheet.layoutParams = ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.MATCH_PARENT,
                            ConstraintLayout.LayoutParams.WRAP_CONTENT
                        ).apply{
                            startToStart = binding.rootConstraintLayout.id
                            endToEnd = binding.rootConstraintLayout.id
                            bottomToBottom = binding.rootConstraintLayout.id
                        }
                        binding.teleprompterCoordinatorLayout.layoutParams = ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.MATCH_PARENT,
                            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                        ).apply {
                            topToTop = binding.rootConstraintLayout.id
                            startToStart = binding.rootConstraintLayout.id
                            endToEnd = binding.rootConstraintLayout.id
                            bottomToTop = binding.contentSheet.id
                        }
                        viewModel.triggerContentSheet()
                    }
                    else{
                        binding.controlsPositionButton.text = getString(R.string.move_controls_to_bottom)
                        binding.contentSheet.layoutParams = ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.MATCH_PARENT,
                            ConstraintLayout.LayoutParams.WRAP_CONTENT
                        ).apply{
                            startToStart = binding.rootConstraintLayout.id
                            endToEnd = binding.rootConstraintLayout.id
                            topToTop = binding.rootConstraintLayout.id
                        }
                        binding.teleprompterCoordinatorLayout.layoutParams = ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.MATCH_PARENT,
                            ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
                        ).apply {
                            bottomToBottom = binding.rootConstraintLayout.id
                            startToStart = binding.rootConstraintLayout.id
                            endToEnd = binding.rootConstraintLayout.id
                            topToBottom = binding.contentSheet.id
                        }
                        viewModel.triggerContentSheet()
                    }
                }

                viewModel.loadSpecialCharacters()
            }
            else{
                binding.changeSettingsNameButton.isEnabled = false
                binding.restoreToDefaultSettingsButton.isEnabled = false
                binding.restoreToDefaultSettingsButton.text = getString(R.string.restore_default_settings)
            }
        }

        collectFlow(this,viewModel.showNameNotUniqueSnackBar){ name ->
            Snackbar.make(
                binding.teleprompterSettingsSpinner,
                requireContext().getString(R.string.name_is_not_unique, name),
                Snackbar.LENGTH_SHORT
            ).show()
        }

        collectFlow(this,viewModel.notifySpinnerDataChanged){
            (binding.teleprompterSettingsSpinner.adapter as ArrayAdapter<*>).notifyDataSetChanged()
        }

        collectFlow(this,viewModel.selectedIndex){ index ->
            viewModel.loadTeleprompterSetting(index)
        }

        collectFlow(this,viewModel.updateContentAdapterSpecialCharacters){
            specialCharactersList ->  contentAdapter.updateSpecialCharacters(specialCharactersList, textSize.toFloat())
        }

        collectFlow(this, viewModel.addSpecialCharacterUpdate){
            position -> specialCharactersAdapter.notifyAddSpecialCharacter(position)
        }

        collectFlow(this,viewModel.isPlaying){ playing ->
            if (playing){
                if (isAtEndOfRecyclerView(binding.teleprompterRecyclerView)){
                    viewModel.pause()
                }
                else{
                    startAutoScrollRunnable(false)
                }
            }
            else{
                stopAutoScroll()
            }
        }

        collectFlow(this,viewModel.contentSheetExpanded){ isExpanded ->
            if (isExpanded){
                if (binding.contentSheet.contains(binding.buttonsContainer)){

                    binding.contentSheet.removeView(binding.buttonsContainer)
                    binding.nestedScrollViewConstraintLayout.addView(binding.buttonsContainer)

                    binding.buttonsContainer.layoutParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topToTop = binding.rootConstraintLayout.id
                        startToStart = binding.rootConstraintLayout.id
                        endToEnd = binding.rootConstraintLayout.id
                    }
                    binding.teleprompterSettingsSpinner.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        topToBottom = binding.buttonsContainer.id
                    }
                    binding.nestedScrollView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        topToBottom = binding.contentSheetButton.id
                    }
                }
            }
            else{
                binding.nestedScrollView.smoothScrollTo(0,0)

                if (binding.nestedScrollViewConstraintLayout.contains(binding.buttonsContainer)){

                    binding.nestedScrollViewConstraintLayout.removeView(binding.buttonsContainer)
                    binding.contentSheet.addView(binding.buttonsContainer)

                    binding.buttonsContainer.layoutParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topToBottom = binding.contentSheetButton.id
                        startToStart = binding.rootConstraintLayout.id
                        endToEnd = binding.rootConstraintLayout.id
                    }
                    binding.teleprompterSettingsSpinner.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        topToBottom = binding.nestedScrollViewConstraintLayout.id
                    }
                    binding.nestedScrollView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                        topToBottom = binding.buttonsContainer.id
                    }
                }
            }
            animateHeightChange(
                binding.contentSheet,
                getControlsHeight(),
                viewModel.getAnimationDuration()
            )
        }

        collectFlow(this,viewModel.cancelCountdown){ cancelUnitPair ->
            if (cancelUnitPair.first){
                viewModel.countdownTimer.value?.cancel()
                viewModel.countdownTimer.value = null
                cancelUnitPair.second?.invoke()
            }
        }

        collectFlow(this,viewModel.finishCountdown){ finish ->
            if (finish){
                viewModel.countdownTimer.value?.onFinish()
            }
        }

        contentAdapter = viewModel.getContentAdapter(
            requireContext(),
            viewLifecycleOwner,
            childFragmentManager,
            textSize.toFloat()
        ) {
            setScrollPosition(viewModel.getScrollPosition())
            viewModel.loadTeleprompterSettings()
        }
        specialCharactersAdapter = SpecialCharacterItemAdapter(viewLifecycleOwner, viewModel)*/
    }

    @Composable
    fun SpecialCharacterCard(
        specialCharacter: SpecialCharacters,
        context: Context,
        scope: CoroutineScope,
        markupLanguage: MarkupLanguage?,
        textSize: Float,
        modifier: Modifier = Modifier
    ){
        val errorMessageVisible by specialCharacter.errorMessageButtonVisibility.collectAsStateWithLifecycle()
        val errorMessage by specialCharacter.duplicateErrorMessage.collectAsStateWithLifecycle()
        val backgroundColour by specialCharacter.backgroundColour.collectAsStateWithLifecycle()
        val character = remember { specialCharacter.character }
        val editingAfterChar = remember { specialCharacter.editingAfterChar }

        LaunchedEffect(character.text){
            viewModel.updateStates()
            if (specialCharacter.canUpdateList()) {
                viewModel.updateSpecialCharacters(
                    context,
                    scope,
                    markupLanguage,
                    textSize
                )
            }
        }

        LaunchedEffect(editingAfterChar.text){
            if (specialCharacter.canUpdateList())
                viewModel.updateSpecialCharacters(
                    context,
                    scope,
                    markupLanguage,
                    textSize
                )
        }

        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(5.dp)
                .background(Color(backgroundColour)),
            shape = RoundedCornerShape(10.dp),
            elevation = CardDefaults.cardElevation(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color(backgroundColour))
        ) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 3.dp)) {
                    Text(stringResource(R.string.character))
                    TextField(
                        state = character,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        placeholder = { Text(stringResource(R.string.special_character)) }
                    )
                }
                if (errorMessageVisible) Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp),
                    text = errorMessage
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp),
                    text = stringResource(R.string.replace_with)
                )
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp),
                    state = editingAfterChar,
                    placeholder = {Text(stringResource(R.string.replacement))}
                )
                Button(onClick = {viewModel.deleteSpecialCharacter(
                    specialCharacter,
                    context,
                    scope,
                    markupLanguage,
                    textSize
                )},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 3.dp),
                    shape = RectangleShape,) {
                    Text(stringResource(R.string.delete))
                }
            }
        }
    }

    fun cancelCountDown(
        countDownTimer: CountDownTimer?,
        onCountDownTimerChanged: (CountDownTimer?)->Unit
    ){
        countDownTimer?.cancel()
        viewModel.hideCountDownButton()
        onCountDownTimerChanged(null)
    }

    suspend fun isAtEndOfListState(listState: LazyListState, appendedUnit: (Boolean)->Unit){
        snapshotFlow {
            listState.scrollIndicatorState?.let { state ->
                state.scrollOffset >= (state.contentSize-state.viewportSize)
            }
        }.collect { isAtEnd ->
            appendedUnit(isAtEnd?:true)
        }
    }

    private fun startAutoScrollRunnable(
        isScroll:Boolean,
        coroutineScope: CoroutineScope,
        countDownTimer: CountDownTimer?,
        onCountDownTimerChanged: (CountDownTimer?)->Unit
    ){
        cancelCountDown(countDownTimer,onCountDownTimerChanged)
        val newCountDownTimer = object : CountDownTimer(
            if (isScroll) viewModel.getScrollCountdown()
            else viewModel.getStartCountdown(),
            1000
        ) {
            override fun onTick(millisUntilFinished: Long) {
                viewModel.setCountDownText(String.format(((millisUntilFinished / 1000)+1).toString()))
            }

            override fun onFinish() {
                startAutoScroll(coroutineScope,
                    countDownTimer,
                    onCountDownTimerChanged)
                viewModel.hideCountDownButton()
                onCountDownTimerChanged(null)
            }
        }
        onCountDownTimerChanged(newCountDownTimer)
        viewModel.showCountDownButton()
        newCountDownTimer.start()
    }

    private fun skip(skipForward:Boolean, coroutineScope: CoroutineScope,
                     countDownTimer: CountDownTimer?,
                     onCountDownTimerChanged: (CountDownTimer?)->Unit){
        coroutineScope.launch {
            val height = requireActivity().window.decorView.height
            var skipSize = viewModel.getSkipSizeValue(height)
            if (!skipForward) skipSize *= -1
            if (viewModel.getIsPlaying()) stopAutoScroll(
                true,
                countDownTimer,
                onCountDownTimerChanged
            )

            if (!skipForward){ // skipBack
                listState?.scrollIndicatorState?.let { scrollIndicatorState ->
                    var difference = scrollIndicatorState.scrollOffset + skipSize
                    if (difference>0) difference = 0
                    listState?.scrollBy((skipSize - difference).toFloat())
                    if (difference<0) scriptViewModel.toolBarCollapsed(difference.toFloat()){}
                }
            }
            else {
                scriptViewModel.toolBarCollapsed(skipSize.toFloat()){ leftOverScroll ->
                    listState?.scrollBy(leftOverScroll)
                }
            }
            /*scriptViewModel.toolBarCollapsed(skipSize.toFloat()){
                listState?.scrollBy(skipSize.toFloat())
            }*/
            if (viewModel.getIsPlaying()) startAutoScrollRunnable(true, coroutineScope,
                countDownTimer,
                onCountDownTimerChanged
            )
        }
    }

    private fun startAutoScroll(coroutineScope: CoroutineScope,
                                countDownTimer: CountDownTimer?,
                                onCountDownTimerChanged: (CountDownTimer?) -> Unit) {
        val runnable = object : Runnable {
            override fun run() {
                listState?.let { listState ->
                    coroutineScope.launch {
                        scriptViewModel.toolBarCollapsed(viewModel.getScrollSpeedValue().toFloat()){
                            listState.scrollBy(viewModel.getScrollSpeedValue().toFloat())
                        }
                    }
                }
                if (viewModel.isPlaying.value) handler.postDelayed(this, 0)
                else stopAutoScroll(true,countDownTimer,onCountDownTimerChanged)
            }
        }
        handler.post(runnable)
    }

    private fun stopAutoScroll(cancelCountDown:Boolean = true,
                               countDownTimer: CountDownTimer?,
                               onCountDownTimerChanged: (CountDownTimer?) -> Unit) {
        if (cancelCountDown) cancelCountDown(countDownTimer,onCountDownTimerChanged)
        handler.removeCallbacksAndMessages(null)
    }

    private fun showBars(){
        // Show both the status bar and the navigation bar.
        val window = requireActivity().window
        WindowCompat.getInsetsController(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
        requireActivity().window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }

    private fun hideBars(){
        // Hide both the status bar and the navigation bar.
        val window = requireActivity().window
        WindowCompat.getInsetsController(window, window.decorView)
            .hide(WindowInsetsCompat.Type.systemBars())
        requireActivity().window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }

    override fun onPause() {
        showBars()
        viewModel.prepareToClose()
        super.onPause()
    }
}