package com.thando.accountable.fragments.viewmodels

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.thando.accountable.AccountableRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

class AppSettingsViewModel(private val repository: AccountableRepository): ViewModel() {

    val appSettings = repository.getAppSettings()

    private val _navigateToChooseImage = MutableSharedFlow<Boolean>()
    val navigateToChooseImage: SharedFlow<Boolean> get() = _navigateToChooseImage

    fun setCustomImage(inputImage: Uri?) {
        repository.saveCustomAppSettingsImage(inputImage)
    }

    fun chooseDefaultImage() {
        repository.restoreDefaultCustomAppSettingsImage()
    }

    fun restoreFromBackup(data: Intent?,
                          pushNotificationPermissionLauncher: ActivityResultLauncher<String>,
                          pushNotificationUnit: AtomicReference<(() -> Unit)?>
    ){
        repository.restoreAccountableDataFromBackup(data, pushNotificationPermissionLauncher,pushNotificationUnit)
    }

    fun makeBackup(data:Intent?,
                   pushNotificationPermissionLauncher: ActivityResultLauncher<String>,
                   pushNotificationUnit: AtomicReference<(() -> Unit)?>){
        repository.makeAccountableBackup(data, pushNotificationPermissionLauncher,pushNotificationUnit)
    }

    fun chooseImage(){
        viewModelScope.launch {
            _navigateToChooseImage.emit(true)
        }
    }

    fun setTextSize(textSize:Int){
        repository.setAppSettingsTextSize(textSize)
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

                return AppSettingsViewModel(
                    accountableRepository
                ) as T
            }
        }
    }
}