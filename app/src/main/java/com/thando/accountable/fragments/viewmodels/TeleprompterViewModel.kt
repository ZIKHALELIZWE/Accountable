package com.thando.accountable.fragments.viewmodels

import android.content.Context
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.thando.accountable.AccountableNavigationController.AccountableFragment
import com.thando.accountable.AccountableRepository
import com.thando.accountable.MainActivity
import com.thando.accountable.R
import com.thando.accountable.database.tables.MarkupLanguage
import com.thando.accountable.database.tables.SpecialCharacters
import com.thando.accountable.database.tables.TeleprompterSettings
import com.thando.accountable.fragments.viewmodels.MarkupLanguageViewModel.Companion.showMarkupLanguageNameDialog
import com.thando.accountable.ui.cards.ColourPickerDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TeleprompterViewModel(
    private val repository: AccountableRepository
): ViewModel() {
    val script = repository.getScript()
    val markupLanguage = repository.getScriptMarkupLanguage()

    val teleprompterSettings = repository.getScriptTeleprompterSetting()
    val teleprompterSettingsList = repository.getTeleprompterSettingsList()

    private val defaultTeleprompterSetting = repository.getDefaultTeleprompterSettings()
    val selectedIndex = repository.getTeleprompterSettingsSelectedIndex()

    // Information Set To Views
    var scriptTitle = MutableStateFlow("")
    private var changeNameFunction: ()->Unit = {}
    private var deleteButtonFunction: ()->Unit = {}
    private val _showNameNotUniqueSnackBar = MutableSharedFlow<String>()
    val showNameNotUniqueSnackBar: SharedFlow<String> get() = _showNameNotUniqueSnackBar

    // Click Events
    private val _navigateToScript = MutableSharedFlow<Boolean>()
    val navigateToScript: SharedFlow<Boolean> get() = _navigateToScript
    private var _finishCountdown = MutableSharedFlow<Boolean>()
    val finishCountdown: SharedFlow<Boolean> get() = _finishCountdown

    // View States
    private var scrolled = false
    private var isTouching = false
    private var _isFullscreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> get() = _isFullscreen
    private var _controlsVisible = MutableStateFlow(true)
    val controlsVisible: StateFlow<Boolean> get() = _controlsVisible
    var countDownText = MutableStateFlow("")
    private val _deleteButtonText = MutableStateFlow(MainActivity.ResourceProvider.getString(R.string.restore_default_settings))
    val deleteButtonText: StateFlow<String> get() = _deleteButtonText
    val countDownTimer = mutableStateOf<CountDownTimer?>(null)

    private var _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> get() = _isPlaying

    var listState: LazyListState? = null
    val handler = Handler(Looper.getMainLooper())

    val colourPickerDialog = ColourPickerDialog()

    fun setCountDownText(countDownTextAfterTick: String){
        countDownText.update { countDownTextAfterTick }
    }

    suspend fun loadTeleprompterSettings() {
        repository.loadTeleprompterSettingsList()
    }

    suspend fun loadTeleprompterSetting(
        index: Int
    ){
        if (index<0 || teleprompterSettingsList.isEmpty()) return
        repository.saveTeleprompterSettings{
            repository.setRepositoryTeleprompterSetting(teleprompterSettingsList[index])
        }
    }

    fun setTeleprompterSettingsFunctions(teleprompterSetting: TeleprompterSettings,
                                         context: Context,
                                         rootHeight:Int,
                                         coroutineScope: CoroutineScope){
        val index = selectedIndex.value
        changeNameFunction = {
            showMarkupLanguageNameDialog(
                context, teleprompterSetting.name.value,
                context.getString(R.string.enter_teleprompter_setting_name)
            ) { name ->
                var nameUnique = true
                for ((i, mLanguage) in teleprompterSettingsList.withIndex()) {
                    if (i != index && mLanguage.name.value == name) {
                        nameUnique = false
                        viewModelScope.launch {
                            _showNameNotUniqueSnackBar.emit(name)
                        }
                    }
                }
                if (nameUnique) {
                    teleprompterSetting.name.update { name }
                }
            }
        }

        if (teleprompterSettingsList.isNotEmpty() && teleprompterSetting == teleprompterSettingsList.last()) {
            _deleteButtonText.value = context.getString(R.string.restore_default_settings)
            deleteButtonFunction = {
                repository.deleteTeleprompterSettingSpecialCharacter(teleprompterSetting) {
                    teleprompterSetting.setValues(TeleprompterSettings())
                    setSkipSizeValue(rootHeight)
                }
            }
        } else {
            _deleteButtonText.value = context.getString(R.string.delete)
            deleteButtonFunction = {
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        repository.setTeleprompterSettingToScript(false) {
                            repository.deleteTeleprompterSetting(teleprompterSetting) {
                                repository.deleteTeleprompterSetting(
                                    defaultTeleprompterSetting.value,
                                    false
                                ) {
                                    coroutineScope.launch { loadTeleprompterSettings() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun updateStates(){
        val charList = arrayListOf<String>()
        snapshotFlow { teleprompterSettings }.collect { teleprompterSettings ->
            teleprompterSettings.value?.specialCharactersList?.forEach { specialCharacter -> charList.add(specialCharacter.character.text.toString()) }
            charList.forEachIndexed { index, searchString ->
                if (searchString.isEmpty()) teleprompterSettings.value?.specialCharactersList[index]?.setDuplicateIndexes(
                    listOf(),
                    index
                )
                else {
                    teleprompterSettings.value?.specialCharactersList[index]?.setDuplicateIndexes(
                        charList.mapIndexed { i, string -> i to string }
                            .filter { it.second == searchString }
                            .map { it.first },
                        index
                    )
                }
            }
        }
    }

    suspend fun setSelectedIndex(selection: Int){
        if (selection == selectedIndex.value){
            loadTeleprompterSetting(selection)
        }
        else repository.setTeleprompterSettingsSelectedIndex(selection)
    }

    fun emitUpdateContentAdapterSpecialCharacters(){
        viewModelScope.launch {
            snapshotFlow { teleprompterSettings.value }.collect { teleprompterSettings ->
                teleprompterSettings?.let {
                    script.value?.scriptTitle?.let {
                        setScriptTitleEdited(
                            it.text.toString(),
                            teleprompterSettings.specialCharactersList
                        )
                    }
                }
            }
        }
    }

    fun setScriptTitleEdited(title:String,specialList:MutableList<SpecialCharacters>?= mutableListOf()){
        var newTitle = title
        specialList?.forEach { newTitle = newTitle.replace(it.character.text.toString(),it.editingAfterChar.text.toString()) }
        scriptTitle.value = newTitle
    }

    fun addSpecialCharacter(){
        repository.addSpecialCharacter()
    }

    fun deleteSpecialCharacter(
        specialCharacter: SpecialCharacters,
        context:Context,
        coroutineScope: CoroutineScope,
        markupLanguage: MarkupLanguage?,
        textSize: Float
    ){
        viewModelScope.launch {
            repository.deleteSpecialCharacter(specialCharacter)
            updateStates()
            updateSpecialCharacters(
                context,
                coroutineScope,
                markupLanguage,
                textSize
            )
        }
    }

    fun updateSpecialCharacters(
        context:Context,
        coroutineScope: CoroutineScope,
        markupLanguage: MarkupLanguage?,
        textSize: Float
    ){
        viewModelScope.launch {
            snapshotFlow { teleprompterSettings.value }.collect { teleprompterSettings ->
                teleprompterSettings?.let {
                    repository.getScriptContentList().forEach { content ->
                        content.replace(
                            specialCharactersList = teleprompterSettings.specialCharactersList,
                            context = context,
                            markupLanguage = markupLanguage,
                            isEditing = false,
                            lifecycleScope = coroutineScope,
                            textSize = textSize
                        )
                    }
                }
            }
        }
    }

    fun navigateToScript(){
        viewModelScope.launch {
            _navigateToScript.emit(true)
        }
    }

    fun toggleControlsVisible(){
        if (_controlsVisible.value) hideControls()
        else showControls()
    }

    fun hideControls() {
        _controlsVisible.value = false
    }

    fun showControls() {
        _controlsVisible.value = true
    }

    fun playPause(){
        _isPlaying.value = _isPlaying.value.not()
    }

    fun pause(){
        _isPlaying.value = false
    }

    suspend fun countDownFinished(){
        _finishCountdown.emit(false)
    }

    fun getIsPlaying() : Boolean{
        return _isPlaying.value
    }

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
        scrolled = false
    }

    fun getScrollSpeedValue(): Int{
        return teleprompterSettings.value?.scrollSpeed?.value?:6
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
        return teleprompterSettings.value?.skipSize?.value?:(rootHeight/3)
    }

    fun toggleFullScreen(){
        if (scrolled) scrolled = false
        else _isFullscreen.value = _isFullscreen.value.not()
    }

    fun toggleControlsPosition(){
        teleprompterSettings.value?.controlsPositionBottom?.value = teleprompterSettings.value?.controlsPositionBottom?.value?.not() == true
    }

    fun chooseTextColour(originalColour: Color? = null){
        colourPickerDialog.pickColour(originalColour) { selectedColour: Int ->
            teleprompterSettings.value?.textColour?.value = selectedColour
        }
    }

    fun chooseBackgroundColour(originalColour: Color? = null){
        colourPickerDialog.pickColour(originalColour) { selectedColour: Int ->
            teleprompterSettings.value?.backgroundColour?.value = selectedColour
        }
    }

    fun changeTeleprompterSettingsName(){
        changeNameFunction.invoke()
    }

    fun restoreToDefaultTeleprompterSettings(){
        deleteButtonFunction.invoke()
    }

    suspend fun closeTeleprompterFragment(backToScript:Boolean = true) {
        teleprompterSettings.value?.let { teleprompterSettings ->
            if (teleprompterSettingsList.isNotEmpty()
                && teleprompterSettings != teleprompterSettingsList.last()
                && teleprompterSettings.name.value.trim() != TeleprompterSettings().name.value.trim()
            ) {
                repository.deleteTeleprompterSetting(teleprompterSettingsList.last()){
                    repository.resetDefaultTeleprompterSetting{
                        repository.saveTeleprompterSettings{
                            repository.setTeleprompterSettingToScript(true){
                                if (backToScript) {
                                    script.value?.scriptId?.let { repository.loadAndOpenScript(it) }
                                        ?: repository.changeFragment(AccountableFragment.ScriptFragment)
                                }
                            }
                        }
                    }
                }
            } else if (teleprompterSettings.name.value.trim() != TeleprompterSettings().name.value.trim()){
                repository.saveTeleprompterSettings{
                    repository.setTeleprompterSettingToScript(true){
                        repository.resetDefaultTeleprompterSetting{
                            if (backToScript) {
                                script.value?.scriptId?.let { repository.loadAndOpenScript(it) }
                                    ?: repository.changeFragment(AccountableFragment.ScriptFragment)
                            }
                        }
                    }
                }
            }
            else{
                repository.deleteTeleprompterSetting(teleprompterSettings){
                    repository.deleteTeleprompterSetting(teleprompterSettingsList.last()) {
                        repository.resetDefaultTeleprompterSetting {
                            repository.setTeleprompterSettingToScript(false) {
                                if (backToScript) {
                                    script.value?.scriptId?.let { repository.loadAndOpenScript(it) }
                                        ?: repository.changeFragment(AccountableFragment.ScriptFragment)
                                }
                            }
                        }
                    }
                }
            }
        }?:run {
            repository.setTeleprompterSettingToScript(false){
                repository.resetDefaultTeleprompterSetting{
                    if (backToScript) {
                        script.value?.scriptId?.let { repository.loadAndOpenScript(it) }
                            ?: repository.changeFragment(AccountableFragment.ScriptFragment)
                    }
                }
            }
        }
    }

    suspend fun prepareToClose() {
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