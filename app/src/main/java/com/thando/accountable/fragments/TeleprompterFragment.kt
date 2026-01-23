package com.thando.accountable.fragments

import android.app.Activity
import android.content.Context
import android.os.CountDownTimer
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.snackbar.Snackbar
import com.thando.accountable.MainActivityViewModel
import com.thando.accountable.R
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.database.tables.SpecialCharacters
import com.thando.accountable.database.tables.TeleprompterSettings
import com.thando.accountable.fragments.viewmodels.ScriptViewModel
import com.thando.accountable.fragments.viewmodels.TeleprompterViewModel
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun TeleprompterView(
    viewModel: TeleprompterViewModel,
    mainActivityViewModel: MainActivityViewModel
) {
    val scriptViewModel = viewModel<ScriptViewModel>(factory = ScriptViewModel.Factory)
    val activity = LocalActivity.current
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose {
            activity?.let { showBars(it) }
            coroutineScope.launch {
                viewModel.prepareToClose()
            }
        }
    }

    val navigateToScript by viewModel.navigateToScript.collectAsStateWithLifecycle(false)
    LaunchedEffect(navigateToScript) {
        if (navigateToScript) {
            viewModel.closeTeleprompterFragment()
        }
    }

    BackHandler {
        viewModel.navigateToScript()
    }

    AccountableTheme {
        viewModel.colourPickerDialog.ColourPicker()
        TeleprompterFragmentView(
            modifier = Modifier,
            viewModel = viewModel,
            mainActivityViewModel = mainActivityViewModel,
            scriptViewModel = scriptViewModel
        )
    }
}

@Composable
fun TeleprompterFragmentView(
    modifier: Modifier = Modifier,
    viewModel: TeleprompterViewModel,
    mainActivityViewModel: MainActivityViewModel,
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

    val selectedIndex by viewModel.selectedIndex.collectAsStateWithLifecycle()
    val deleteButtonText by viewModel.deleteButtonText.collectAsStateWithLifecycle()
    val controlsAtTop by teleprompterSettings?.controlsPositionBottom?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(false) }
    viewModel.listState = remember { scriptViewModel.listState }
    var countDownTimer by remember { viewModel.countDownTimer }
    val markupLanguage by viewModel.markupLanguage.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = LocalActivity.current
    val resources = LocalResources.current
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

    LaunchedEffect(teleprompterSettings?.specialCharactersList){
        viewModel.updateStates()
        viewModel.emitUpdateContentAdapterSpecialCharacters()
    }

    LaunchedEffect(selectedIndex){
        viewModel.loadTeleprompterSetting(selectedIndex)
    }

    LaunchedEffect(teleprompterSettings, teleprompterSettingsList){
        snapshotFlow { teleprompterSettingsList.toList() }.collect { teleprompterSettingsList ->
            if (teleprompterSettings != null && teleprompterSettingsList.isNotEmpty()) {
                val height = resources.displayMetrics.heightPixels
                viewModel.setTeleprompterSettingsFunctions(
                    teleprompterSettings!!,
                    context,
                    height,
                    coroutineScope
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

    LaunchedEffect(viewModel.listState) {
        // Do not combine with lower "LaunchedEffect(listState)". It does not work
        viewModel.listState?.let { listState ->
            snapshotFlow { listState.isScrollInProgress }
                .collect { isScrolling ->
                    if (isScrolling) {
                        viewModel.scrolled()
                    }
                }
        }
    }

    LaunchedEffect(viewModel.listState) {
        viewModel.listState?.let{ listState ->
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
                startAutoScrollRunnable(
                    viewModel,
                    scriptViewModel,
                    false,
                    coroutineScope,
                    countDownTimer
                ) { countDownTimer = it }
            }
        }
        else{
            stopAutoScroll(
                viewModel,
                true,
                countDownTimer
            ) { countDownTimer = it }
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
            skip(
                viewModel,
                scriptViewModel,
                resources.displayMetrics.heightPixels,
                false,
                coroutineScope,
                countDownTimer
            ) { countDownTimer = it }
        }
    }

    // Hide/show system bars
    LaunchedEffect(isFullScreen) {
        if (isFullScreen)
            activity?.let { hideBars(it) }
        else
            activity?.let { showBars(it) }
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
                viewModel = viewModel,
                scriptViewModel = scriptViewModel,
                modifier = Modifier.background(Color.White),
                isPlaying = isPlaying,
                markupLanguage = markupLanguage,
                teleprompterSettings = teleprompterSettings,
                teleprompterSettingsList = teleprompterSettingsList,
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
                                            viewModel,
                                            false,
                                            countDownTimer
                                        ) { countDownTimer = it }
                                        viewModel.isTouching()
                                    } else if (change.changedToUp()) {
                                        if (isPlaying) {
                                            if (viewModel.getScrolledValue()) startAutoScrollRunnable(
                                                viewModel,
                                                scriptViewModel,
                                                true,
                                                coroutineScope,
                                                countDownTimer
                                            ) { countDownTimer = it }
                                            else if (countDownTimer == null) startAutoScroll(
                                                viewModel,
                                                scriptViewModel,
                                                coroutineScope,
                                                null
                                            ) { countDownTimer = it }
                                        }
                                        viewModel.isNotTouching()
                                    }
                                }
                            }
                        }
                    },
                viewModel = scriptViewModel,
                mainActivityViewModel = mainActivityViewModel,
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
}

@Composable
fun SpecialCharacterCard(
    viewModel: TeleprompterViewModel,
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
    viewModel: TeleprompterViewModel,
    scriptViewModel: ScriptViewModel,
    isPlaying: Boolean,
    markupLanguage: MarkupLanguage?,
    spinnerEnabled: Boolean,
    teleprompterSettings: TeleprompterSettings?,
    teleprompterSettingsList: SnapshotStateList<TeleprompterSettings>,
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
    val resources = LocalResources.current
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
                Button(onClick = {skip(
                    viewModel,
                    scriptViewModel,
                    resources.displayMetrics.heightPixels,
                    false,
                    scope,
                    countDownTimer,
                    onCountDownTimerChanged
                )},
                    shape = RectangleShape) { Text("Backward") }
                Button(onClick = togglePlayPause,
                    shape = RectangleShape) { if (isPlaying) Text("Pause") else Text("Play") }
                Button(onClick = {skip(
                    viewModel,
                    scriptViewModel,
                    resources.displayMetrics.heightPixels,
                    true,
                    scope,
                    countDownTimer,
                    onCountDownTimerChanged
                )},
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
                    value = if (selectedOptionName == TeleprompterSettings().name.collectAsState().value)
                        stringResource(
                            R.string.change_name_to_save,
                            selectedOptionName
                        )
                    else selectedOptionName,
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
                        focusedTextColor = if (selectedOptionName == TeleprompterSettings().name.collectAsState().value)
                            Color.Red
                        else MaterialTheme.colorScheme.onPrimary,
                        unfocusedTextColor = if (selectedOptionName == TeleprompterSettings().name.collectAsState().value)
                            Color.Red
                        else MaterialTheme.colorScheme.onPrimary,
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
                                        text = if (selectionOptionName == TeleprompterSettings().name.collectAsState().value)
                                            stringResource(
                                                R.string.change_name_to_save,
                                                selectionOptionName
                                            )
                                        else selectionOptionName,
                                        color = if (selectionOptionName == TeleprompterSettings().name.collectAsState().value)
                                                Color.Red
                                            else MaterialTheme.colorScheme.onPrimary
                                    )
                                },
                                onClick = {
                                    scope.launch {
                                        viewModel.setSelectedIndex(
                                            teleprompterSettingsList.indexOf(selectionOption)
                                        )
                                    }
                                    onSpinnerExpandedChanged(false)
                                }
                            )
                        }
                    }
                }
            }
        }

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
            teleprompterSettings?.scrollSpeed?.let { mutableScrollSpeed ->
                val scrollSpeed by mutableScrollSpeed.collectAsStateWithLifecycle()
                Text(stringResource(R.string.scroll_speed, scrollSpeed))
            }
        }
        item {
            teleprompterSettings?.scrollSpeed?.let { mutableScrollSpeed ->
                val scrollSpeed by mutableScrollSpeed.collectAsStateWithLifecycle()
                Slider(
                    value = scrollSpeed.toFloat(),
                    onValueChange = { scope.launch { mutableScrollSpeed.emit(it.toInt()) } },
                    valueRange = 1f..60f
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            teleprompterSettings?.skipSize?.let { mutableSkipSize ->
                val skipSize by mutableSkipSize.collectAsStateWithLifecycle()
                Text(stringResource(R.string.skip_size, skipSize))
            }
        }
        item {
            teleprompterSettings?.skipSize?.let { mutableSkipSize ->
                val skipSize by mutableSkipSize.collectAsStateWithLifecycle()
                Slider(
                    value = skipSize.toFloat(),
                    onValueChange = { scope.launch { mutableSkipSize.emit( it.toInt()) }},
                    valueRange = 100f..3000f
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            teleprompterSettings?.textSize?.let { mutableTextSize ->
                val textSize by mutableTextSize.collectAsStateWithLifecycle()
                Text(stringResource(R.string.text_size, textSize))
            }
        }
        item {
            teleprompterSettings?.textSize?.let { mutableTextSize ->
                val textSize by mutableTextSize.collectAsStateWithLifecycle()
                Slider(
                    value = textSize.toFloat(),
                    onValueChange = { scope.launch { mutableTextSize.emit( it.toInt()) } },
                    valueRange = 12f..70f
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            teleprompterSettings?.startCountDown?.let { mutableStartCountDown ->
                val startCountDown by mutableStartCountDown.collectAsStateWithLifecycle()
                Text(stringResource(R.string.start_count_down, startCountDown))
            }
        }
        item {
            teleprompterSettings?.startCountDown?.let { mutableStartCountDown ->
                val startCountDown by mutableStartCountDown.collectAsStateWithLifecycle()
                Slider(
                    value = startCountDown.toFloat(),
                    onValueChange = { scope.launch { mutableStartCountDown.emit(it.toInt()) } },
                    valueRange = 0f..10f
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            teleprompterSettings?.scrollCountDown?.let { mutableScrollCountDown ->
                val scrollCountDown by mutableScrollCountDown.collectAsStateWithLifecycle()
                Text(stringResource(R.string.scroll_count_down, scrollCountDown))
            }
        }
        item {
            teleprompterSettings?.scrollCountDown?.let { mutableScrollCountDown ->
                val scrollCountDown by mutableScrollCountDown.collectAsStateWithLifecycle()
                val range: ClosedFloatingPointRange<Float> = 0f..10f
                Slider(
                    value = scrollCountDown.toFloat(),
                    onValueChange = { scope.launch { mutableScrollCountDown.emit(it.toInt()) } },
                    valueRange = range
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            teleprompterSettings?.textColour?.let { mutableTextColour ->
                val textColor by mutableTextColour.collectAsStateWithLifecycle()
                Button(
                    onClick = { viewModel.chooseTextColour(
                        Color(textColor)
                    ) },
                    shape = RectangleShape, modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Change Text Color (Current: $textColor)")
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            teleprompterSettings?.backgroundColour?.let { mutableBackgroundColour ->
            val backgroundColour by mutableBackgroundColour.collectAsStateWithLifecycle()
            Button(
                onClick = { viewModel.chooseBackgroundColour(
                    Color(backgroundColour)
                ) },
                shape = RectangleShape, modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Background Color (Current: $backgroundColour)")
            }
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
        items(
            teleprompterSettings?.specialCharactersList?:mutableStateListOf()
        ) { specialCharacter ->
            val textSize by (teleprompterSettings?.textSize?:MutableStateFlow(24))
                .collectAsStateWithLifecycle()
            SpecialCharacterCard(
                viewModel,
                specialCharacter,
                context,
                scope,
                markupLanguage,
                textSize.toFloat()
            )
        }
    }
}

fun cancelCountDown(
    countDownTimer: CountDownTimer?,
    onCountDownTimerChanged: (CountDownTimer?)->Unit
){
    countDownTimer?.cancel()
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
    viewModel: TeleprompterViewModel,
    scriptViewModel: ScriptViewModel,
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
            startAutoScroll(
                viewModel,
                scriptViewModel,
                coroutineScope,
                countDownTimer,
                onCountDownTimerChanged)
            onCountDownTimerChanged(null)
        }
    }
    onCountDownTimerChanged(newCountDownTimer)
    newCountDownTimer.start()
}

private fun skip(
    viewModel: TeleprompterViewModel,
    scriptViewModel: ScriptViewModel,
    displayHeight: Int,
    skipForward:Boolean, coroutineScope: CoroutineScope,
    countDownTimer: CountDownTimer?,
    onCountDownTimerChanged: (CountDownTimer?)->Unit
){
    coroutineScope.launch {
        var skipSize = viewModel.getSkipSizeValue(displayHeight)
        if (!skipForward) skipSize *= -1
        if (viewModel.getIsPlaying()) stopAutoScroll(
            viewModel,
            true,
            countDownTimer,
            onCountDownTimerChanged
        )

        if (!skipForward){ // skipBack
            viewModel.listState?.scrollIndicatorState?.let { scrollIndicatorState ->
                var difference = scrollIndicatorState.scrollOffset + skipSize
                if (difference>0) difference = 0
                viewModel.listState?.scrollBy((skipSize - difference).toFloat())
                if (difference<0) scriptViewModel.toolBarCollapsed(difference.toFloat()){}
            }
        }
        else {
            scriptViewModel.toolBarCollapsed(skipSize.toFloat()){ leftOverScroll ->
                viewModel.listState?.scrollBy(leftOverScroll)
            }
        }
        /*scriptViewModel.toolBarCollapsed(skipSize.toFloat()){
            listState?.scrollBy(skipSize.toFloat())
        }*/
        if (viewModel.getIsPlaying()) startAutoScrollRunnable(
            viewModel,
            scriptViewModel,
            true,
            coroutineScope,
            countDownTimer,
            onCountDownTimerChanged
        )
    }
}

private fun startAutoScroll(
    viewModel: TeleprompterViewModel,
    scriptViewModel: ScriptViewModel,
    coroutineScope: CoroutineScope,
    countDownTimer: CountDownTimer?,
    onCountDownTimerChanged: (CountDownTimer?) -> Unit
) {
    val runnable = object : Runnable {
        override fun run() {
            viewModel.listState?.let { listState ->
                coroutineScope.launch {
                    scriptViewModel.toolBarCollapsed(viewModel.getScrollSpeedValue().toFloat()){
                        listState.scrollBy(viewModel.getScrollSpeedValue().toFloat())
                    }
                }
            }
            if (viewModel.isPlaying.value) viewModel.handler.postDelayed(this, 0)
            else stopAutoScroll(
                viewModel,
                true,
                countDownTimer,
                onCountDownTimerChanged
            )
        }
    }
    viewModel.handler.post(runnable)
}

private fun stopAutoScroll(
    viewModel: TeleprompterViewModel,
    cancelCountDown:Boolean = true,
    countDownTimer: CountDownTimer?,
    onCountDownTimerChanged: (CountDownTimer?) -> Unit
) {
    if (cancelCountDown) cancelCountDown(
        countDownTimer,
        onCountDownTimerChanged
    )
    viewModel.handler.removeCallbacksAndMessages(null)
}

private fun showBars(activity: Activity){
    // Show both the status bar and the navigation bar.
    val window = activity.window
    WindowCompat.getInsetsController(window, window.decorView)
        .show(WindowInsetsCompat.Type.systemBars())
    activity.window.attributes.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
    activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    activity.window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
}

private fun hideBars(activity: Activity){
    // Hide both the status bar and the navigation bar.
    val window = activity.window
    WindowCompat.getInsetsController(window, window.decorView)
        .hide(WindowInsetsCompat.Type.systemBars())
    activity.window.attributes.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    activity.window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
}