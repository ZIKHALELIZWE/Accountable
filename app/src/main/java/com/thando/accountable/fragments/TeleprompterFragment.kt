package com.thando.accountable.fragments

import android.animation.ValueAnimator
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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.contains
import androidx.core.view.get
import androidx.core.view.size
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.thando.accountable.MainActivity
import com.thando.accountable.MainActivity.Companion.collectFlow
import com.thando.accountable.R
import com.thando.accountable.database.tables.TeleprompterSettings
import com.thando.accountable.databinding.FragmentTeleprompterBinding
import com.thando.accountable.fragments.viewmodels.TeleprompterViewModel
import com.thando.accountable.recyclerviewadapters.ContentItemAdapter
import com.thando.accountable.recyclerviewadapters.SpecialCharacterItemAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class TeleprompterFragment : Fragment() {

    private var _binding: FragmentTeleprompterBinding? = null
    private val binding get() = _binding!!
    private val viewModel : TeleprompterViewModel by viewModels { TeleprompterViewModel.Factory }
    private lateinit var bindingController: WindowInsetsControllerCompat
    private lateinit var contentAdapter: ContentItemAdapter
    private lateinit var specialCharactersAdapter: SpecialCharacterItemAdapter
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val textSize = 26
        // Inflate the layout for this fragment
        _binding = FragmentTeleprompterBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        contentAdapter = viewModel.getContentAdapter(
            requireContext(),
            viewLifecycleOwner,
            childFragmentManager,
            textSize.toFloat()
        ) {
            setScrollPosition(viewModel.getScrollPosition())
            viewModel.loadTeleprompterSettings()
        }
        binding.teleprompterRecyclerView.adapter = contentAdapter

        specialCharactersAdapter = SpecialCharacterItemAdapter(viewLifecycleOwner, viewModel)

        binding.specialCharactersRecyclerView.adapter = specialCharactersAdapter

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val textSize = 26
        bindingController = WindowCompat.getInsetsController(requireActivity().window, binding.root)
        // Configure the behavior of the hidden system bars.
        bindingController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val touchListener = View.OnTouchListener { v, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN){
                if (viewModel.getIsPlaying()) stopAutoScroll(false)
                viewModel.isTouching()
            }
            if (motionEvent.action == MotionEvent.ACTION_UP){
                if (viewModel.getIsPlaying()){
                    if(viewModel.getScrolledValue()) startAutoScrollRunnable(true)
                    else if (viewModel.countdownTimer.value==null) startAutoScroll()
                }
                viewModel.toggleFullScreen()
                viewModel.isNotTouching()
                v.performClick()
            }
            false
        }
        (binding.teleprompterCoordinatorLayout as View).setOnTouchListener(touchListener)
        (binding.teleprompterRecyclerView as View).setOnTouchListener(touchListener)
        (binding.titleTextView as View).setOnTouchListener(touchListener)

        binding.appBarLayout.addOnOffsetChangedListener { _, verticalOffset ->
            scrollChangedListener(verticalOffset)
        }
        binding.teleprompterRecyclerView.setOnScrollChangeListener { _, _, _, _, oldScrollY ->
            scrollChangedListener(oldScrollY)
        }

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

        collectFlow(this,viewModel.skipForward){ skipForward ->
            skip(skipForward,true)
        }

        collectFlow(this,viewModel.skipBack){ skipBack ->
            skip(skipBack,false)
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

        collectFlow(this,viewModel.navigateToScript) { navigate ->
            if (navigate) {
                context.let {
                    viewModel.viewModelScope.launch {
                        withContext(Dispatchers.IO) {
                            viewModel.closeTeleprompterFragment()
                        }
                    }
                }
            }
        }
    }

    private fun scrollChangedListener(oldScrollY:Int){
        // Handle scroll event
        if (oldScrollY < 0) {
            // Scrolling down
            if (isAtEndOfRecyclerView(binding.teleprompterRecyclerView)) viewModel.pause()
        } else {
            // Scrolling up
        }
        viewModel.scrolled()
    }

    private fun isAtEndOfRecyclerView(recyclerView: RecyclerView): Boolean{
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val visibleItemCount = layoutManager.childCount
        val totalItemCount = layoutManager.itemCount
        val pastVisibleItems = layoutManager.findFirstVisibleItemPosition()

        if ((visibleItemCount + pastVisibleItems) >= totalItemCount && recyclerView.size>0) {
            val view = recyclerView[recyclerView.size-1]
            val bottomDetector: Int = view.bottom - (recyclerView.height + recyclerView.scrollY)
            if (bottomDetector == 0) {
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        val mainActivity = (requireActivity() as MainActivity)

        mainActivity.onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true){
                override fun handleOnBackPressed() {
                    viewModel.navigateToScript()
                }
            }
        )
    }

    inner class CustomCountDownTimer(millisInFuture:Long,
                               countDownInterval:Long
    ):CountDownTimer(millisInFuture,countDownInterval){
        val millisUntilItIsFinished:MutableLiveData<Long?> = MutableLiveData()
        override fun onTick(millisUntilFinished: Long) {
            millisUntilItIsFinished.value = millisUntilFinished
        }

        override fun onFinish() {
            startAutoScroll()
            viewModel.countdownTimer.value = null
        }
    }

    private fun startAutoScrollRunnable(isScroll:Boolean){
        viewModel.cancelCountDown {
            viewModel.countdownTimer.value = CustomCountDownTimer(
                if (isScroll) viewModel.getScrollCountdown()
                else viewModel.getStartCountdown(),
                1000
            )
            viewModel.countdownTimer.value!!.start()
        }
    }

    private fun getControlsHeight():Int{
        val peekHeight = binding.buttonsContainer.height + binding.contentSheetButton.height
        return if (viewModel.contentSheetExpanded.value) binding.root.height / 2
        else peekHeight
    }

    private fun animateHeightChange(view: View, finalHeight:Int, duration: Long = 300) {
        val animator = ValueAnimator.ofInt( view.measuredHeight, finalHeight)
        animator.addUpdateListener { setViewHeight( view, it.animatedValue as Int) }
        animator.duration = duration
        animator.start()
    }

    private fun setViewHeight(view: View, height: Int){
        val layoutParams: ViewGroup.LayoutParams = view.layoutParams
        layoutParams.height = height
        view.setLayoutParams(layoutParams)
    }

    private fun skip(shouldSkip:Boolean, skipForward:Boolean){
        if (shouldSkip){
            var skipSize = viewModel.getSkipSizeValue(binding.rootConstraintLayout.height)
            if (!skipForward) skipSize*=-1
            if (viewModel.getIsPlaying()) stopAutoScroll()
            binding.teleprompterRecyclerView.smoothScrollBy(
                0,
                skipSize,
                android.view.animation.AccelerateDecelerateInterpolator(),
                0
            )
            if (viewModel.getIsPlaying()) startAutoScrollRunnable(true)
        }
    }

    private fun startAutoScroll() {
        val runnable = object : Runnable {
            override fun run() {
                binding.teleprompterRecyclerView.nestedScrollBy(0,viewModel.getScrollSpeedValue())
                /*binding.teleprompterRecyclerView.smoothScrollBy(
                    0,
                    viewModel.getScrollSpeedValue(),
                    android.view.animation.AccelerateDecelerateInterpolator(),
                    0
                )*/
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
        bindingController.show(WindowInsetsCompat.Type.systemBars())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            requireActivity().window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        }
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        viewModel.showControls()
    }

    private fun hideBars(){
        // Hide both the status bar and the navigation bar.
        bindingController.hide(WindowInsetsCompat.Type.systemBars())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            requireActivity().window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        viewModel.hideControls()
    }

    override fun onPause() {
        viewModel.setScrollPosition(getScrollPosition())
        showBars()
        viewModel.prepareToClose()
        super.onPause()
    }

    private fun setScrollPosition(position: Int) {
        val layoutManager = binding.teleprompterRecyclerView.layoutManager as LinearLayoutManager
        binding.teleprompterRecyclerView.post {
            layoutManager.scrollToPosition(position)
        }
    }

    private fun getScrollPosition(): Int{
        val layoutManager = binding.teleprompterRecyclerView.layoutManager as LinearLayoutManager
        return layoutManager.findLastCompletelyVisibleItemPosition()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}