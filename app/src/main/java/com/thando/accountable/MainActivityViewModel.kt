package com.thando.accountable

import android.net.Uri
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.thando.accountable.database.tables.Content
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class MainActivityViewModel(
    val repository: AccountableRepository
) : ViewModel() {
    val direction = repository.getDirection()
    val appSettings = repository.getAppSettings()
    val currentFragment = repository.getCurrentFragment()
    val drawerState = mutableStateOf(DrawerState(DrawerValue.Closed))
    val drawerEnabled = mutableStateOf(true)
    var galleryLauncherReturnProcess: ((Uri?)->Unit)? = null
    var galleryLauncherMultipleReturnProcess: ((List<@JvmSuppressWildcards Uri>)->Unit)? = null
    private val _galleryLauncherEvent = MutableSharedFlow<String>()
    val galleryLauncherEvent = _galleryLauncherEvent.asSharedFlow()
    private val _galleryLauncherMultipleEvent = MutableSharedFlow<String>()
    val galleryLauncherMultipleEvent = _galleryLauncherMultipleEvent.asSharedFlow()

    fun clearGalleryLaunchers(){
        setGalleryLauncherReturn()
        setGalleryLauncherMultipleReturn()
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

    fun processGalleryLauncherResult(result:Uri?){
        galleryLauncherReturnProcess?.let { it(result) }
    }

    fun processGalleryLauncherMultipleReturn(result: List<@JvmSuppressWildcards Uri>){
        galleryLauncherMultipleReturnProcess?.let { it(result) }
    }

    fun closeUpdateSettings() {
        repository.updateAppSettings()
    }

    fun enableDrawer(){
        drawerEnabled.value = true
    }

    suspend fun disableDrawer(){
        drawerEnabled.value = true
        toggleDrawer(false)
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
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                // Get the Application object from extras
                val application = checkNotNull(extras[APPLICATION_KEY])

                val accountableRepository = AccountableRepository.getInstance(application)

                return MainActivityViewModel(
                    accountableRepository
                ) as T
            }
        }
    }
}