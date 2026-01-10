package com.thando.accountable.fragments.viewmodels

import android.content.Context
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.thando.accountable.AccountableNavigationController.AccountableFragment
import com.thando.accountable.AccountableRepository
import com.thando.accountable.R
import com.thando.accountable.database.tables.SpecialCharacters
import com.thando.accountable.database.tables.TeleprompterSettings
import com.thando.accountable.fragments.TeleprompterFragment
import com.thando.accountable.fragments.viewmodels.MarkupLanguageViewModel.Companion.showMarkupLanguageNameDialog
import com.thando.accountable.recyclerviewadapters.ContentItemAdapter
import com.thando.accountable.ui.cards.Colour
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TeleprompterViewModel(
    private val repository: AccountableRepository
): ViewModel() {
    // Data
    private val appSettings = repository.getAppSettings()
    val script = repository.getScript()
    private val scriptContentList = repository.getScriptContentList()
    val markupLanguage = repository.getScriptMarkupLanguage()

    val teleprompterSettings = repository.getScriptTeleprompterSetting()
    val teleprompterSettingsList = repository.getTeleprompterSettingsList()

    val specialCharactersList = repository.getSpecialCharactersList()
    private val defaultTeleprompterSetting = repository.getDefaultTeleprompterSettings()
    val selectedIndex = repository.getTeleprompterSettingsSelectedIndex()

    // Information Set To Views
    var scriptTitle = MutableStateFlow("")
    private var changeNameFunction: ()->Unit = {}
    private var deleteButtonFunction: ()->Unit = {}
    private val _showNameNotUniqueSnackBar = MutableSharedFlow<String>()
    val showNameNotUniqueSnackBar: SharedFlow<String> get() = _showNameNotUniqueSnackBar
    private val _notifySpinnerDataChanged = MutableSharedFlow<Unit>()
    val notifySpinnerDataChanged: SharedFlow<Unit> get() = _notifySpinnerDataChanged
    private val _updateContentAdapterSpecialCharacters = MutableSharedFlow<MutableList<SpecialCharacters>>()
    val updateContentAdapterSpecialCharacters: SharedFlow<MutableList<SpecialCharacters>> get() = _updateContentAdapterSpecialCharacters
    private val _addSpecialCharacterUpdate = MutableSharedFlow<Int>()
    val addSpecialCharacterUpdate: SharedFlow<Int> get() = _addSpecialCharacterUpdate


    // Click Events
    private val _navigateToScript = MutableSharedFlow<Boolean>()
    val navigateToScript: SharedFlow<Boolean> get() = _navigateToScript
    private var _skipForward = MutableSharedFlow<Boolean>()
    val skipForward: SharedFlow<Boolean> get() = _skipForward
    private var _skipBack = MutableSharedFlow<Boolean>()
    val skipBack: SharedFlow<Boolean> get() = _skipBack
    private var _cancelCountdown = MutableSharedFlow<Pair<Boolean,(()->Unit)?>>()
    val cancelCountdown = _cancelCountdown.asSharedFlow()
    private var _finishCountdown = MutableSharedFlow<Boolean>()
    val finishCountdown: SharedFlow<Boolean> get() = _finishCountdown

    // View States
    private var scrollPosition = 0
    private var scrolled = false
    private var isTouching = false
    private var _isFullscreen = MutableStateFlow(true)
    val isFullscreen: StateFlow<Boolean> get() = _isFullscreen
    private var _controlsVisibility = MutableStateFlow(View.VISIBLE)
    val controlsVisibility: StateFlow<Int> get() = _controlsVisibility
    var countdownTimer : MutableStateFlow<TeleprompterFragment.CustomCountDownTimer?> = MutableStateFlow(null)

    private var _contentSheetExpanded = MutableStateFlow(true)
    val contentSheetExpanded: StateFlow<Boolean> get() = _contentSheetExpanded
    private var _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> get() = _isPlaying

    // animation values
    private var animationDuration = 500L

    fun getContentAdapter(
        context: Context,
        viewLifecycleOwner: LifecycleOwner,
        childFragmentManager: FragmentManager,
        textSize: Float,
        markupLanguageInitializeScrollUnit:(()->Unit)
    ): ContentItemAdapter {
        return ContentItemAdapter(
            context,
            viewLifecycleOwner,
            childFragmentManager,
            null,
            markupLanguageInitializeScrollUnit,
            script,
            appSettings,
            markupLanguage,
            MutableStateFlow(false),
            viewModelScope,
            repository,
            MutableStateFlow(null),
            textSize
        )
    }

    fun loadTeleprompterSettings() {
        repository.loadTeleprompterSettingsList()
    }

    fun loadTeleprompterSetting(
        index: Int
    ){
        if (index<0 || teleprompterSettingsList.value.isEmpty()) return
        teleprompterSettingsList.value.let {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    repository.saveTeleprompterSettings()
                    repository.setRepositoryTeleprompterSetting(it[index])
                }
            }
        }
    }

    fun setTeleprompterSettingsFunctions(teleprompterSetting: TeleprompterSettings,
                                         context: Context,
                                         rootHeight:Int){
        val index = selectedIndex.value
        changeNameFunction = {
            showMarkupLanguageNameDialog(
                context, teleprompterSetting.name.value,
                context.getString(R.string.enter_teleprompter_setting_name)
            ) { name ->
                var nameUnique = true
                for ((i, mLanguage) in teleprompterSettingsList.value.withIndex()) {
                    if (i != index && mLanguage.name.value == name) {
                        nameUnique = false
                        viewModelScope.launch {
                            _showNameNotUniqueSnackBar.emit(name)
                        }
                    }
                }
                if (nameUnique) {
                    teleprompterSetting.name.value = name
                    viewModelScope.launch {
                        _notifySpinnerDataChanged.emit(Unit)
                    }
                }
            }
        }

        if (teleprompterSettingsList.value.isNotEmpty() && teleprompterSetting == teleprompterSettingsList.value.last()) {
            deleteButtonFunction = {
                repository.deleteTeleprompterSettingSpecialCharacter(teleprompterSetting) {
                    teleprompterSetting.setValues(TeleprompterSettings())
                    setSkipSizeValue(rootHeight)
                }
            }
        } else {
            deleteButtonFunction = {
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        repository.setTeleprompterSettingToScript(false) {
                            repository.deleteTeleprompterSetting(teleprompterSetting) {
                                repository.deleteTeleprompterSetting(
                                    defaultTeleprompterSetting.value,
                                    false
                                ) {
                                    loadTeleprompterSettings()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun setSelectedIndex(selection: Int){
        if (selection == selectedIndex.value){
            loadTeleprompterSetting(selection)
        }
        else repository.setTeleprompterSettingsSelectedIndex(selection)
    }

    fun emitUpdateContentAdapterSpecialCharacters(){
        viewModelScope.launch {
            script.value?.scriptTitle?.let { setScriptTitleEdited(it.text.toString(),specialCharactersList.value) }
            _updateContentAdapterSpecialCharacters.emit(specialCharactersList.value)
        }
    }

    fun setScriptTitleEdited(title:String,specialList:MutableList<SpecialCharacters>?= mutableListOf()){
        var newTitle = title
        specialList?.forEach { newTitle = newTitle.replace(it.character.value,it.editingAfterChar.value) }
        scriptTitle.value = newTitle
    }

    fun loadSpecialCharacters(){
        repository.loadTeleprompterSpecialCharacters()
    }

    fun addSpecialCharacter(){
        repository.addSpecialCharacter(_addSpecialCharacterUpdate)
    }

    fun deleteSpecialCharacter(specialCharacter: SpecialCharacters){
        repository.deleteSpecialCharacter(specialCharacter)
    }

    fun setScrollPosition(inputScrollPosition:Int){
        scrollPosition = inputScrollPosition
    }

    fun getScrollPosition(): Int{
        return scrollPosition
    }

    fun navigateToScript(){
        viewModelScope.launch {
            _navigateToScript.emit(true)
        }
    }

    fun hideControls() {
        _controlsVisibility.value = View.GONE
    }

    fun showControls() {
        _controlsVisibility.value = View.VISIBLE
    }

    fun skipBack(){
        viewModelScope.launch {
            _skipBack.emit(true)
        }
    }

    fun skipForward(){
        viewModelScope.launch {
            _skipForward.emit(true)
        }
    }

    fun playPause(){
        _isPlaying.value = _isPlaying.value.not()
    }

    fun pause(){
        _isPlaying.value = false
    }

    fun getIsPlaying() : Boolean{
        return _isPlaying.value
    }

    fun toggleContentSheet(){
        _contentSheetExpanded.value = _contentSheetExpanded.value.not()
    }

    fun triggerContentSheet(){
        _contentSheetExpanded.value = _contentSheetExpanded.value
    }

    fun getAnimationDuration():Long{ return animationDuration }

    fun scrolled(){
        if (isTouching) scrolled = true
    }

    fun getScrolledValue():Boolean{ return scrolled }

    fun getScrollCountdown():Long{ return (teleprompterSettings.value?.scrollCountDown?.value?:0).toLong()*1000}

    fun getStartCountdown():Long{ return (teleprompterSettings.value?.startCountDown?.value?:0).toLong()*1000}

    fun isTouching(){
        isTouching = true
    }

    fun isNotTouching(){
        isTouching = false
    }

    fun getScrollSpeedValue(): Int{
        return teleprompterSettings.value?.scrollSpeed?.value?:6
    }

    fun cancelCountDown(appendedUnit:(()->Unit)?=null){
        viewModelScope.launch {
            _cancelCountdown.emit(Pair(true,appendedUnit))
        }
    }

    fun finishCountDown(){
        viewModelScope.launch {
            _finishCountdown.emit(true)
        }
    }

    fun setSkipSizeValue(rootHeight:Int){
        teleprompterSettings.value?.let {
            if (it.skipSize.value < (rootHeight / 3)) {
                it.skipSize.value = rootHeight / 3
            }
            if (it.skipSize.value > rootHeight) {
                it.skipSize.value = rootHeight
            }
        }
    }

    fun getSkipSizeValue(rootHeight:Int): Int{
        setSkipSizeValue(rootHeight)
        return teleprompterSettings.value?.skipSize?.value?:(rootHeight/3)
    }

    fun toggleFullScreen(){
        if (scrolled) scrolled = false
        else _isFullscreen.value = _isFullscreen.value.not()
    }

    fun toggleControlsPosition(){
        teleprompterSettings.value?.controlsPositionBottom?.value = teleprompterSettings.value?.controlsPositionBottom?.value?.not() == true
    }

    fun chooseTextColour(context: Context){
        Colour.showColorPickerDialog(context){ selectedColour: Int ->
            teleprompterSettings.value?.textColour?.value = selectedColour
        }
    }

    fun chooseBackgroundColour(context: Context){
        Colour.showColorPickerDialog(context){ selectedColour: Int ->
            teleprompterSettings.value?.backgroundColour?.value = selectedColour
        }
    }

    fun changeTeleprompterSettingsName(){
        changeNameFunction.invoke()
    }

    fun restoreToDefaultTeleprompterSettings(){
        deleteButtonFunction.invoke()
    }

    fun closeTeleprompterFragment() {
        teleprompterSettings.value?.let { teleprompterSettings ->
            if (teleprompterSettingsList.value.isNotEmpty() && teleprompterSettings != teleprompterSettingsList.value.last()) {
                repository.deleteTeleprompterSetting(teleprompterSettingsList.value.last()){
                    repository.resetDefaultTeleprompterSetting{
                        repository.saveTeleprompterSettings{
                            repository.setTeleprompterSettingToScript(true){
                                repository.changeFragment(AccountableFragment.ScriptFragment)
                            }
                        }
                    }
                }
            } else if (teleprompterSettings.name.value != TeleprompterSettings().name.value){
                repository.saveTeleprompterSettings{
                    repository.setTeleprompterSettingToScript(true){
                        repository.resetDefaultTeleprompterSetting{
                            repository.changeFragment(AccountableFragment.ScriptFragment)
                        }
                    }
                }
            }
            else{
                repository.deleteTeleprompterSetting(teleprompterSettingsList.value.last()){
                    repository.resetDefaultTeleprompterSetting{
                        repository.setTeleprompterSettingToScript(false){
                            repository.changeFragment(AccountableFragment.ScriptFragment)
                        }
                    }
                }
            }
        }?:run {
            repository.setTeleprompterSettingToScript(false){
                repository.resetDefaultTeleprompterSetting{
                    repository.changeFragment(AccountableFragment.ScriptFragment)
                }
            }
        }
    }

    fun prepareToClose() {
        teleprompterSettings.value?.let {
            repository.saveTeleprompterSettings{
                repository.setTeleprompterSettingToScript(true)
            }
        }?:run {
            repository.setTeleprompterSettingToScript(false)
        }
    }

    companion object{
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                // Get the Application object from extras
                val application = checkNotNull(extras[APPLICATION_KEY])

                val accountableRepository = AccountableRepository.getInstance(application)

                return TeleprompterViewModel(
                    accountableRepository
                ) as T
            }
        }
    }
}