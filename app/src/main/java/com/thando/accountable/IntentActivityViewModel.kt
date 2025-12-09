package com.thando.accountable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras

class IntentActivityViewModel(
    val repository: AccountableRepository
) : ViewModel() {
    val direction = repository.getDirection()
    var navController = AccountableNavigationController()

    class Factory(private val intentString: String? = null): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T {
            // Get the Application object from extras
            val application = checkNotNull(extras[APPLICATION_KEY])

            val accountableRepository = AccountableRepository.getInstance(application)

            accountableRepository.intentString = intentString

            return IntentActivityViewModel(
                accountableRepository
            ) as T
        }
    }
}