package com.thando.accountable

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras

class MainActivityViewModel(
    val repository: AccountableRepository
) : ViewModel() {
    val direction = repository.getDirection()
    val appSettings = repository.getAppSettings()
    val currentFragment = repository.getCurrentFragment()
    var navController = AccountableNavigationController()
    val drawerState = mutableStateOf(DrawerState(DrawerValue.Closed))
    val toolbarVisible = mutableStateOf(false)
    private val drawerEnabled = mutableStateOf(true)

    // Handle business logic
    fun changeFragment(newFragment: AccountableNavigationController.AccountableFragment){
        repository.changeFragment(newFragment)
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