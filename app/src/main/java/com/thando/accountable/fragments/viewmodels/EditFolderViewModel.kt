package com.thando.accountable.fragments.viewmodels

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.thando.accountable.AccountableNavigationController
import com.thando.accountable.AccountableRepository
import com.thando.accountable.AppResources
import com.thando.accountable.R
import com.thando.accountable.database.tables.Folder
import kotlinx.coroutines.flow.MutableStateFlow

class EditFolderViewModel(
    private val repository: AccountableRepository
): ViewModel() {

    // Data
    var folder = repository.getFolder()
    var editFolder = repository.getEditFolder()
    var appSettings = repository.getAppSettings()
    var newEditFolder = repository.getNewEditFolder()
    val folderType = repository.getScriptsOrGoalsFolderType()

    // Information Set To Views
    val updateButtonText: MutableStateFlow<Int?> = MutableStateFlow(null)

    suspend fun initializeEditFolder(inputFolder:Folder?){
        updateButtonText.value = if (inputFolder == null) R.string.add_folder
        else R.string.update_folder
        repository.setNewEditFolder(inputFolder)
    }

    suspend fun setImage(uri:Uri?){
        repository.setNewEditFolderImage(uri)
    }

    private fun goBackToFoldersAndScripts(){
        repository.changeFragment(
            if (folderType.value == Folder.FolderType.SCRIPTS) AccountableNavigationController.AccountableFragment.BooksFragment
            else AccountableNavigationController.AccountableFragment.GoalsFragment
        )
    }

    fun setUpdateFolderButtonEnabled(folderName: String?):Boolean {
        return !folderName.isNullOrEmpty()
    }

    fun newEditFolderViewEnabled(value:Folder?):Boolean{
        return value!=null
    }

    suspend fun removeImage() {
        repository.deleteNewEditFolderImage()
    }

    suspend fun closeFolder(){
        repository.clearNewEditFolder(false)
        goBackToFoldersAndScripts()
    }

    suspend fun saveAndCloseFolder() {
        repository.saveNewEditFolderToNewFolder()
        goBackToFoldersAndScripts()
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

                return EditFolderViewModel(
                    accountableRepository
                ) as T
            }
        }
    }
}