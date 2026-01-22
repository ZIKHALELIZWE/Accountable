package com.thando.accountable

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class MainActivityViewModel(
    val repository: AccountableRepository,
    val isIntentActivity: Boolean = false
) : ViewModel() {
    val direction = repository.getDirection()
    val appSettings = repository.getAppSettings()
    val currentFragment = repository.getCurrentFragment()
    val drawerState = mutableStateOf(DrawerState(DrawerValue.Closed))
    val drawerEnabled = mutableStateOf(true)
    var galleryLauncherReturnProcess: ((Uri?)->Unit)? = null
    var galleryLauncherMultipleReturnProcess: ((List<@JvmSuppressWildcards Uri>)->Unit)? = null
    var restoreBackupReturnProcess: ((
        Intent?,
        ActivityResultLauncher<String>,
        AtomicReference<(() -> Unit)?>
    ) -> Unit)? = null
    var makeBackupReturnProcess: ((
        Intent?,
        ActivityResultLauncher<String>,
        AtomicReference<(() -> Unit)?>
    ) -> Unit)? = null
    private val _galleryLauncherEvent = MutableSharedFlow<String>()
    val galleryLauncherEvent = _galleryLauncherEvent.asSharedFlow()
    private val _galleryLauncherMultipleEvent = MutableSharedFlow<String>()
    val galleryLauncherMultipleEvent = _galleryLauncherMultipleEvent.asSharedFlow()
    private val _restoreBackupEvent = MutableSharedFlow<Intent>()
    val restoreBackupEvent = _restoreBackupEvent.asSharedFlow()
    private val _makeBackupEvent = MutableSharedFlow<Intent>()
    val makeBackupEvent = _makeBackupEvent.asSharedFlow()
    val accountableNavigationController = AccountableNavigationController(
        this,
        isIntentActivity
    )


    fun clearGalleryLaunchers(){
        setGalleryLauncherReturn()
        setGalleryLauncherMultipleReturn()
        setRestoreBackupReturn()
        setMakeBackupReturn()
    }

    fun launchGalleryLauncher(type: AppResources.ContentType){
        val accessor = AppResources.ContentTypeAccessor[type]
        accessor?.let {
            viewModelScope.launch { _galleryLauncherEvent.emit(it) }
        }
    }

    fun launchGalleryLauncherMultiple(type: AppResources.ContentType){
        val accessor = AppResources.ContentTypeAccessor[type]
        accessor?.let {
            viewModelScope.launch { _galleryLauncherMultipleEvent.emit(it) }
        }
    }

    fun launchRestoreBackup(intent: Intent){
        viewModelScope.launch { _restoreBackupEvent.emit(intent) }
    }

    fun launchMakeBackup(intent: Intent){
        viewModelScope.launch { _makeBackupEvent.emit(intent) }
    }

    // Handle business logic
    fun changeFragment(newFragment: AccountableNavigationController.AccountableFragment){
        repository.changeFragment(newFragment)
    }

    fun setGalleryLauncherReturn(process: ((Uri?) -> Unit)? = null) {
        galleryLauncherReturnProcess = process
    }

    fun setGalleryLauncherMultipleReturn(process: ((List<@JvmSuppressWildcards Uri>) -> Unit)? = null) {
        galleryLauncherMultipleReturnProcess = process
    }

    fun setRestoreBackupReturn(process: (
        (
        Intent?,
        ActivityResultLauncher<String>,
        AtomicReference<(() -> Unit)?>
    ) -> Unit)? = null) {
        restoreBackupReturnProcess = process
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun setMakeBackupReturn(process: (
        (
            Intent?,
            ActivityResultLauncher<String>,
            AtomicReference<(() -> Unit)?>
        ) -> Unit)? = null) {
        makeBackupReturnProcess = process
    }

    fun processGalleryLauncherResult(result:Uri?){
        galleryLauncherReturnProcess?.let { it(result) }
    }

    fun processGalleryLauncherMultipleReturn(result: List<@JvmSuppressWildcards Uri>){
        galleryLauncherMultipleReturnProcess?.let { it(result) }
    }

    fun processMakeBackupResult(
        result:Intent?,
        pushNotificationPermissionLauncher: ActivityResultLauncher<String>,
        pushNotificationUnit: AtomicReference<(()->Unit)?>
    ){
        makeBackupReturnProcess?.let { it(
            result,
            pushNotificationPermissionLauncher,
            pushNotificationUnit
        ) }
    }

    fun processRestoreBackupResult(
        result:Intent?,
        pushNotificationPermissionLauncher: ActivityResultLauncher<String>,
        pushNotificationUnit: AtomicReference<(()->Unit)?>
    ){
        restoreBackupReturnProcess?.let { it(
            result,
            pushNotificationPermissionLauncher,
            pushNotificationUnit
        ) }
    }

    fun closeUpdateSettings() {
        repository.updateAppSettings()
    }

    fun enableDrawer(){
        drawerEnabled.value = true
    }

    fun disableDrawer(){
        drawerEnabled.value = false
    }

    suspend fun toggleDrawer(open: Boolean? = null):Boolean{
        if (drawerEnabled.value) {
            val drawerOpened = drawerState.value.isOpen
            open?.let { open ->
                if (open) drawerState.value.open()
                else{
                    drawerState.value.close()
                    return drawerOpened
                }
            } ?: run {
                drawerState.value.apply { if (isClosed) open() else close() }
            }
        }
        return false
    }

    companion object {
        fun Factory(isIntentActivity: Boolean): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                // Get the Application object from extras
                val application = checkNotNull(extras[APPLICATION_KEY])

                val accountableRepository = AccountableRepository.getInstance(application)

                return MainActivityViewModel(
                    accountableRepository,
                    isIntentActivity
                ) as T
            }
        }
    }
}