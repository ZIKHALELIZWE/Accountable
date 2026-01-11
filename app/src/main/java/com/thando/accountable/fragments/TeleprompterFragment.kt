package com.thando.accountable.fragments

import android.animation.ValueAnimator
import android.app.StatusBarManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.get
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.thando.accountable.MainActivity
import com.thando.accountable.R
import com.thando.accountable.fragments.viewmodels.ScriptViewModel
import com.thando.accountable.fragments.viewmodels.TeleprompterViewModel
import com.thando.accountable.recyclerviewadapters.ContentItemAdapter
import com.thando.accountable.recyclerviewadapters.SpecialCharacterItemAdapter
import com.thando.accountable.ui.theme.AccountableTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext


class TeleprompterFragment : Fragment() {

    private val viewModel : TeleprompterViewModel by viewModels { TeleprompterViewModel.Factory }
    private val scriptViewModel : ScriptViewModel by viewModels { ScriptViewModel.Factory }
    private val handler = Handler(Looper.getMainLooper())
    private var countdownTimer : CountDownTimer? = null

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

    @Composable
    fun TeleprompterScreen(modifier: Modifier) {
        val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
        val controlsVisible by viewModel.controlsVisible.collectAsStateWithLifecycle()
        val isFullScreen by viewModel.isFullScreen.collectAsStateWithLifecycle()
        val countDownTimer by viewModel.countdownTimer.collectAsStateWithLifecycle()
        val countDownText by viewModel.countDownText.collectAsStateWithLifecycle()
        val skipForward by viewModel.skipForward.collectAsStateWithLifecycle(false)
        val skipBack by viewModel.skipBack.collectAsStateWithLifecycle(false)
        val finishCountdown by viewModel.finishCountdown.collectAsStateWithLifecycle(false)
        val cancelCountDown by viewModel.cancelCountdown.collectAsStateWithLifecycle()

        val listState = remember { viewModel.listState }
        var scrollSpeed by remember { mutableLongStateOf(10L) }
        var skipDistance by remember { androidx.compose.runtime.mutableIntStateOf(200) }
        var controlsAtTop by remember { mutableStateOf(false) }
        val onControlsAtTopChanged = { controlsAtTop = !controlsAtTop }
        var textSize by remember { mutableFloatStateOf(18f) }
        var textColor by remember { mutableStateOf(Color.Black) }
        var backgroundColor by remember { mutableStateOf(Color.White) }
        var countdownDelay by remember { androidx.compose.runtime.mutableIntStateOf(0) } // seconds
        val coroutineScope = rememberCoroutineScope()
        var remoteConnected by remember { mutableStateOf(false) }
        var isAtEndOfList by remember { mutableStateOf(false) }

        LaunchedEffect(listState) {
            isAtEndOfListState(listState){ isAtEnd ->
                isAtEndOfList = isAtEnd
                if (isAtEnd){
                    viewModel.pause()
                }
                viewModel.scrolled()
            }
        }

        LaunchedEffect(isPlaying){
            if (isPlaying){
                if (isAtEndOfList){
                    viewModel.pause()
                }
                else{
                    startAutoScrollRunnable(false, coroutineScope)
                }
            }
            else{
                stopAutoScroll()
            }
        }

        LaunchedEffect(this,skipForward){
            skip(skipForward,true, coroutineScope)
        }

        LaunchedEffect(this, skipBack){
            skip(skipBack,false, coroutineScope)
        }

        LaunchedEffect(cancelCountDown){
            if (cancelCountDown){
                countdownTimer?.cancel()
                viewModel.hideCountDownButton()
                countdownTimer = null
                viewModel.countDownCancelled()
            }
        }

        LaunchedEffect(finishCountdown){
            if (finishCountdown){
                countdownTimer?.onFinish()
                viewModel.countDownFinished()
            }
        }

        // Register skip back handler
        LaunchedEffect(Unit) {
            TeleprompterController.registerSkipBack {
                viewModel.skipBack()
            }
        }

        // Hide/show system bars
        LaunchedEffect(isFullScreen) {
            if (isFullScreen) hideBars() else showBars()
        }

        Column(modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
        ) {
            val hideControlsButton = @Composable {
                Button(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    onClick = { viewModel.toggleControlsVisible() }
                ) {
                    Text(if (controlsVisible) "Hide Controls" else "Show Controls")
                }
            }
            val controls = @Composable {
                ControlsSection(
                    isPlaying = isPlaying,
                    remoteConnected = remoteConnected,
                    togglePlayPause = { viewModel.playPause() },
                    onSkipBack = {
                        viewModel.skipBack()
                    },
                    onSkipForward = {
                        viewModel.skipForward()
                    },
                    scrollSpeed = scrollSpeed,
                    onSpeedChange = { scrollSpeed = it },
                    skipDistance = skipDistance,
                    onSkipChange = { skipDistance = it },
                    textSize = textSize,
                    onTextSizeChange = { textSize = it },
                    countDownDelay = countdownDelay,
                    onCountDownDelayChanged = { countdownDelay = it },
                    textColor = textColor,
                    onTextColorChange = {
                        textColor = when (textColor) {
                            Color.Black -> Color.Red
                            Color.Red -> Color.Blue
                            Color.Blue -> Color.Green
                            else -> Color.Black
                        }
                    },
                    backgroundColor = backgroundColor,
                    onBackgroundColorChange = {
                        backgroundColor = when (backgroundColor) {
                            Color.White -> Color.LightGray
                            Color.LightGray -> Color.Yellow
                            Color.Yellow -> Color.Black
                            else -> Color.White
                        }
                    },
                    controlsAtTop = controlsAtTop,
                    onControlsAtTopChanged = onControlsAtTopChanged
                )
            }
            // Controls at top (only if not full screen)
            if (controlsAtTop && controlsVisible && !isFullScreen) controls()
            if (controlsAtTop && !isFullScreen) hideControlsButton()

            Box(Modifier
                .weight(1f)
            ) {
                if(countDownTimer!=null) {
                    Button(
                        onClick = { viewModel.finishCountDown() }
                    ) {
                        Text(countDownText)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { change ->
                                        if (change.changedToDown()) {
                                            if (isPlaying) stopAutoScroll(false)
                                            viewModel.isTouching()
                                        }
                                        else if (change.changedToUp()) {
                                            if (isPlaying){
                                                if(viewModel.getScrolledValue()) startAutoScrollRunnable(true, coroutineScope)
                                                else if (countdownTimer==null) startAutoScroll(coroutineScope)
                                            }
                                            viewModel.toggleFullScreen()
                                            viewModel.isNotTouching()
                                        }
                                        else if (change.pressed) {
                                            if (!viewModel.isTouchingValue())
                                                viewModel.toggleFullScreen()
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    items(10) {
                        Text(
                            text = """
 Welcome to the teleprompter demo.
 Tap the text area to toggle full screen mode.
 In full screen, controls and status bar are hidden.
 Tap again to exit full screen.
 """.trimIndent(),
                            fontSize = textSize.sp,
                            color = textColor,
                            style = MaterialTheme.typography.bodyLarge
                        )
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
    fun ControlsSection(
        isPlaying: Boolean,
        togglePlayPause: () -> Unit,
        onSkipBack: () -> Unit,
        onSkipForward: () -> Unit,
        scrollSpeed: Long,
        onSpeedChange: (Long) -> Unit,
        skipDistance: Int,
        onSkipChange: (Int) -> Unit,
        textSize: Float,
        onTextSizeChange: (Float) -> Unit,
        textColor: Color,
        onTextColorChange: () -> Unit,
        backgroundColor: Color,
        onBackgroundColorChange: () -> Unit,
        countDownDelay: Int,
        onCountDownDelayChanged: (Int) -> Unit,
        remoteConnected: Boolean,
        controlsAtTop: Boolean,
        onControlsAtTopChanged: () -> Unit
    ) {
        val height = (LocalResources.current.displayMetrics.heightPixels*2)/5
        LazyColumn(modifier = Modifier.padding(16.dp)
                .height((height/LocalResources.current.displayMetrics.density).dp),
            state = rememberLazyListState(), reverseLayout = controlsAtTop
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = onSkipBack) { Text("⏪ Back") }
                    Button(onClick = togglePlayPause) { if (isPlaying) Text("Pause") else Text("Play") }
                    Button(onClick = onSkipForward) { Text("⏩ Forward") }
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Text("Scroll Speed (ms delay): $scrollSpeed")
            }
            item {
                Slider(
                    value = scrollSpeed.toFloat(),
                    onValueChange = { onSpeedChange(it.toLong()) },
                    valueRange = 1f..60f
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Text("Skip Distance (px): $skipDistance")
            }
            item {
                Slider(
                    value = skipDistance.toFloat(),
                    onValueChange = { onSkipChange(it.toInt()) },
                    valueRange = 50f..1000f
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Text("Text Size (sp): ${textSize.toInt()}")
            }
            item {
                Slider(
                    value = textSize,
                    onValueChange = { onTextSizeChange(it) },
                    valueRange = 12f..40f
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Text("Countdown Delay (seconds): $countDownDelay")
            }
            item {
                Slider(
                    value = countDownDelay.toFloat(),
                    onValueChange = { onCountDownDelayChanged(it.toInt()) },
                    valueRange = 0f..10f
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Button(onClick = onTextColorChange, modifier = Modifier.fillMaxWidth()) {
                    Text("Change Text Color (Current: ${colorName(textColor)})")
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                Button(onClick = onBackgroundColorChange, modifier = Modifier.fillMaxWidth()) {
                    Text("Change Background Color (Current: ${colorName(backgroundColor)})")
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                Button(onClick = onControlsAtTopChanged, modifier = Modifier.fillMaxWidth()) {
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (remoteConnected) "Remote: Connected (Skip Back)"
                        else "Remote: Not Connected"
                    )
                }
            }
        }
    }

    // Helper to show color names
    fun colorName(color: Color): String = when (color) {
        Color.Black -> "Black"
        Color.Red -> "Red"
        Color.Blue -> "Blue"
        Color.Green -> "Green"
        Color.White -> "White"
        Color.LightGray -> "Light Gray"
        Color.Yellow -> "Yellow"
        else -> "Custom"
    }

    @Composable
    fun TeleprompterFragmentView(
        modifier: Modifier = Modifier,
        scriptViewModel: ScriptViewModel
    ){
        scriptViewModel.setIsScriptFragment(false)
        TeleprompterScreen(modifier)
        /*ScriptFragmentView(
            modifier = modifier,
            viewModel = scriptViewModel
        )*/
        /*

        collectFlow(this,viewModel.script){ script ->
            script?.scriptTitle?.let { title ->
                viewModel.setScriptTitleEdited(title.text.toString())
            }
        }

        collectFlow(this,viewModel.isFullscreen) { isFullscreen ->
            if (isFullscreen){
                showBars()
            }
            else{
                hideBars()
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

    inner class CustomCountDownTimer(
        millisInFuture:Long,
        countDownInterval:Long
    ):CountDownTimer(millisInFuture,countDownInterval){
        val millisUntilItIsFinished:MutableLiveData<Long?> = MutableLiveData()
        override fun onTick(millisUntilFinished: Long) {
            millisUntilItIsFinished.value = millisUntilFinished
        }

        override fun onFinish() {
            startAutoScroll(lifecycleScope)
            viewModel.countdownTimer.value = null
        }
    }

    suspend fun isAtEndOfListState(listState: LazyListState, appendedUnit: (Boolean)->Unit){
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItemsCount = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val lastItemOffset = layoutInfo.visibleItemsInfo.lastOrNull()?.offset
            Pair(lastVisibleItemIndex == totalItemsCount - 1, lastItemOffset)
        }.collect { isAtEnd ->
            appendedUnit((isAtEnd.first
                    && isAtEnd.second != null
                    && listState.firstVisibleItemScrollOffset >= isAtEnd.second!!))
        }
    }

    private fun startAutoScrollRunnable(isScroll:Boolean, coroutineScope: CoroutineScope){
        viewModel.cancelCountDown()
        countdownTimer = object : CountDownTimer(
            if (isScroll) viewModel.getScrollCountdown()
            else viewModel.getStartCountdown(),
            1000
        ) {
            override fun onTick(millisUntilFinished: Long) {
                MainActivity.log("Tick: ${String.format(((millisUntilFinished / 1000)+1).toString())}")
                viewModel.setCountDownText(String.format(((millisUntilFinished / 1000)+1).toString()))
            }

            override fun onFinish() {
                MainActivity.log("Finished")
                startAutoScroll(coroutineScope)
                viewModel.hideCountDownButton()
                countdownTimer = null
            }
        }
        viewModel.showCountDownButton()
        MainActivity.log("Started count down timer")
        countdownTimer!!.start()
    }

    private suspend fun skip(shouldSkip:Boolean, skipForward:Boolean, coroutineScope: CoroutineScope){
        if (shouldSkip){
            val height = requireActivity().window.decorView.height
            var skipSize = viewModel.getSkipSizeValue(height)
            if (!skipForward) skipSize*=-1
            if (viewModel.getIsPlaying()) stopAutoScroll()

            val offset = viewModel.listState.firstVisibleItemScrollOffset
            viewModel.listState.animateScrollToItem(
                viewModel.listState.firstVisibleItemIndex,
                (offset + skipSize).coerceAtLeast(0)
            )
            if (viewModel.getIsPlaying()) startAutoScrollRunnable(true, coroutineScope)
        }
    }

    private fun startAutoScroll(coroutineScope: CoroutineScope) {
        val runnable = object : Runnable {
            override fun run() {
                val currentOffset = viewModel.listState.firstVisibleItemScrollOffset
                coroutineScope.launch {
                    viewModel.listState.scrollToItem(
                        viewModel.listState.firstVisibleItemIndex,
                        currentOffset + viewModel.getScrollSpeedValue()
                    )
                }
                if (viewModel.isPlaying.value) handler.postDelayed(this, 0)
                else stopAutoScroll()
            }
        }
        handler.post(runnable)
    }

    private fun stopAutoScroll(cancelCountDown:Boolean = true) {
        if (cancelCountDown) viewModel.cancelCountDown()
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