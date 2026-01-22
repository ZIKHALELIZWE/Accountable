package com.thando.accountable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.thando.accountable.database.tables.Folder
import com.thando.accountable.fragments.AppSettingsView
import com.thando.accountable.fragments.BooksView
import com.thando.accountable.fragments.EditFolderView
import com.thando.accountable.fragments.EditGoalView
import com.thando.accountable.fragments.HelpView
import com.thando.accountable.fragments.HomeView
import com.thando.accountable.fragments.MarkupLanguageView
import com.thando.accountable.fragments.ScriptView
import com.thando.accountable.fragments.SearchView
import com.thando.accountable.fragments.TaskView
import com.thando.accountable.fragments.TeleprompterView
import com.thando.accountable.fragments.viewmodels.AppSettingsViewModel
import com.thando.accountable.fragments.viewmodels.BooksViewModel
import com.thando.accountable.fragments.viewmodels.BooksViewModel.Companion.INITIAL_FOLDER_ID
import com.thando.accountable.fragments.viewmodels.EditFolderViewModel
import com.thando.accountable.fragments.viewmodels.EditGoalViewModel
import com.thando.accountable.fragments.viewmodels.HelpViewModel
import com.thando.accountable.fragments.viewmodels.HomeViewModel
import com.thando.accountable.fragments.viewmodels.MarkupLanguageViewModel
import com.thando.accountable.fragments.viewmodels.ScriptViewModel
import com.thando.accountable.fragments.viewmodels.SearchViewModel
import com.thando.accountable.fragments.viewmodels.TaskViewModel
import com.thando.accountable.fragments.viewmodels.TeleprompterViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AccountableNavigationController(
    private val mainActivityViewModel: MainActivityViewModel,
    private val isIntentActivity: Boolean = false
) {

    enum class AccountableFragment {
        HomeFragment,
        GoalsFragment,
        BooksFragment,
        AppSettingsFragment,
        HelpFragment,
        EditFolderFragment,
        EditGoalFragment,
        TaskFragment,
        MarkupLanguageFragment,
        ScriptFragment,
        TeleprompterFragment,
        SearchFragment
    }

    companion object {
        private const val HOME_FRAGMENT_ID = 1
        private const val GOAL_FRAGMENT_ID = 2
        private const val BOOKS_FRAGMENT_ID = 3
        private const val APP_SETTINGS_FRAGMENT_ID = 4
        private const val HELP_FRAGMENT_ID = 5
        private const val EDIT_FOLDER_FRAGMENT_ID = 6
        private const val EDIT_GOAL_FRAGMENT_ID = 7
        private const val TASK_FRAGMENT_ID = 8
        private const val MARKUP_LANGUAGE_FRAGMENT_ID = 9
        private const val SCRIPT_FRAGMENT_ID = 10
        private const val TELEPROMPTER_FRAGMENT_ID = 11
        private const val SEARCH_FRAGMENT_ID = 12

        fun getFragmentId(fragment: AccountableFragment) = when(fragment) {
            AccountableFragment.HomeFragment -> HOME_FRAGMENT_ID
            AccountableFragment.GoalsFragment -> GOAL_FRAGMENT_ID
            AccountableFragment.BooksFragment -> BOOKS_FRAGMENT_ID
            AccountableFragment.AppSettingsFragment -> APP_SETTINGS_FRAGMENT_ID
            AccountableFragment.HelpFragment -> HELP_FRAGMENT_ID
            AccountableFragment.EditFolderFragment -> EDIT_FOLDER_FRAGMENT_ID
            AccountableFragment.EditGoalFragment -> EDIT_GOAL_FRAGMENT_ID
            AccountableFragment.TaskFragment -> TASK_FRAGMENT_ID
            AccountableFragment.MarkupLanguageFragment -> MARKUP_LANGUAGE_FRAGMENT_ID
            AccountableFragment.ScriptFragment -> SCRIPT_FRAGMENT_ID
            AccountableFragment.TeleprompterFragment -> TELEPROMPTER_FRAGMENT_ID
            AccountableFragment.SearchFragment -> SEARCH_FRAGMENT_ID
        }

        fun isDrawerFragment(fragment: AccountableFragment) = when(fragment){
            AccountableFragment.HomeFragment -> true
            AccountableFragment.BooksFragment -> true
            AccountableFragment.AppSettingsFragment -> true
            AccountableFragment.HelpFragment -> true
            else -> false
        }

        fun getFragmentFromId(fragment: Int) = when(fragment) {
            HOME_FRAGMENT_ID -> AccountableFragment.HomeFragment
            GOAL_FRAGMENT_ID -> AccountableFragment.GoalsFragment
            BOOKS_FRAGMENT_ID -> AccountableFragment.BooksFragment
            APP_SETTINGS_FRAGMENT_ID -> AccountableFragment.AppSettingsFragment
            HELP_FRAGMENT_ID -> AccountableFragment.HelpFragment
            EDIT_FOLDER_FRAGMENT_ID -> AccountableFragment.EditFolderFragment
            EDIT_GOAL_FRAGMENT_ID -> AccountableFragment.EditGoalFragment
            TASK_FRAGMENT_ID -> AccountableFragment.TaskFragment
            MARKUP_LANGUAGE_FRAGMENT_ID -> AccountableFragment.MarkupLanguageFragment
            SCRIPT_FRAGMENT_ID -> AccountableFragment.ScriptFragment
            TELEPROMPTER_FRAGMENT_ID -> AccountableFragment.TeleprompterFragment
            SEARCH_FRAGMENT_ID -> AccountableFragment.SearchFragment
            else -> AccountableFragment.HomeFragment
        }

        fun getFragmentDirections(currentFragment: AccountableFragment, newFragment: AccountableFragment):Pair<AccountableFragment?,AccountableRepository.NavigationArguments>{
            val navArgs = isNavigationValid(newFragment, currentFragment)
            return Pair(if (navArgs.isValidDir) newFragment else null,navArgs)
        }

        private fun isNavigationValid(newFragment: AccountableFragment, startDestination: AccountableFragment):AccountableRepository.NavigationArguments{
            val navArgs = AccountableRepository.NavigationArguments()
            if (newFragment == startDestination) return navArgs
            if (isDrawerFragment(newFragment) && isDrawerFragment(startDestination)){
                navArgs.isDrawerFragment = true
                navArgs.isValidDir = true
                if (newFragment == AccountableFragment.BooksFragment){
                    navArgs.scriptsOrGoalsFolderId = INITIAL_FOLDER_ID
                    navArgs.scriptsOrGoalsFolderType = Folder.FolderType.SCRIPTS
                }
                return navArgs
            }

            when(startDestination){
                AccountableFragment.HomeFragment -> {
                    if (newFragment == AccountableFragment.GoalsFragment){
                        navArgs.scriptsOrGoalsFolderId = INITIAL_FOLDER_ID
                        navArgs.scriptsOrGoalsFolderType = Folder.FolderType.GOALS
                        navArgs.isValidDir = true
                        navArgs.isDrawerFragment = false
                    }
                }
                AccountableFragment.AppSettingsFragment -> {}
                AccountableFragment.HelpFragment -> {}
                AccountableFragment.GoalsFragment -> {
                    navArgs.isValidDir = listOf(
                        AccountableFragment.HomeFragment,
                        AccountableFragment.EditFolderFragment,
                        AccountableFragment.EditGoalFragment,
                        AccountableFragment.TaskFragment
                    ).contains(newFragment)
                    if (newFragment == AccountableFragment.HomeFragment){
                        navArgs.isDrawerFragment = true
                    }
                }
                AccountableFragment.BooksFragment -> {
                    navArgs.isValidDir = listOf(
                        AccountableFragment.EditFolderFragment,
                        AccountableFragment.ScriptFragment,
                        AccountableFragment.SearchFragment
                    ).contains(newFragment)
                }
                AccountableFragment.EditFolderFragment -> {
                    if (newFragment == AccountableFragment.BooksFragment ||
                        newFragment == AccountableFragment.GoalsFragment ){
                        navArgs.isValidDir = true
                        navArgs.isDrawerFragment = true
                    }
                }
                AccountableFragment.EditGoalFragment -> {
                    if (newFragment == AccountableFragment.GoalsFragment){
                        navArgs.isValidDir = true
                        navArgs.isDrawerFragment = true
                    }
                }
                AccountableFragment.TaskFragment -> {
                    if (newFragment == AccountableFragment.GoalsFragment){
                        navArgs.isValidDir = true
                        navArgs.isDrawerFragment = true
                    }
                }
                AccountableFragment.MarkupLanguageFragment -> {
                    if (newFragment == AccountableFragment.ScriptFragment){
                        navArgs.isValidDir = true
                    }
                }
                AccountableFragment.ScriptFragment -> {
                    navArgs.isValidDir = listOf(
                        AccountableFragment.MarkupLanguageFragment,
                        AccountableFragment.TeleprompterFragment,
                        AccountableFragment.BooksFragment,
                        AccountableFragment.SearchFragment
                    ).contains(newFragment)
                    if (newFragment == AccountableFragment.BooksFragment){
                        navArgs.isDrawerFragment = true
                    }
                }
                AccountableFragment.TeleprompterFragment -> {
                    if (newFragment == AccountableFragment.ScriptFragment){
                        navArgs.isValidDir = true
                    }
                }
                AccountableFragment.SearchFragment -> {
                    if (newFragment == AccountableFragment.BooksFragment ||
                        newFragment == AccountableFragment.ScriptFragment){
                        navArgs.isValidDir = true
                    }
                }
            }
            return navArgs
        }
    }

    @Composable
    fun GetAccountableActivity(
        modifier: Modifier = Modifier
    ){
        val navController = rememberNavController()
        val scope = rememberCoroutineScope()
        var startDestination by remember { mutableStateOf<AccountableFragment?>(null) }

        LaunchedEffect(Unit) {
            mainActivityViewModel.direction.collect { direction ->
                if (direction != null){
                    scope.launch { mainActivityViewModel.toggleDrawer(false) }
                    mainActivityViewModel.clearGalleryLaunchers()
                    (0 until navController.currentBackStack.value.size).forEach { _ ->
                        navController.popBackStack()
                    }
                    if (
                        isDrawerFragment(direction)
                    ) mainActivityViewModel.enableDrawer()
                    else mainActivityViewModel.disableDrawer()
                    navController.navigate(
                        direction.name
                    ){ launchSingleTop = true }
                }
            }
        }

        LaunchedEffect(Unit) {
            mainActivityViewModel.currentFragment
                .filterNotNull()
                .firstOrNull()?.let { fragment ->
                    var currentFragment = fragment
                    if (isIntentActivity) {
                        currentFragment =
                            if (
                                fragment == AccountableFragment.BooksFragment ||
                                fragment == AccountableFragment.SearchFragment
                            ) fragment else AccountableFragment.BooksFragment
                    }

                    scope.launch { mainActivityViewModel.toggleDrawer(false) }
                    mainActivityViewModel.clearGalleryLaunchers()
                    (0 until navController.currentBackStack.value.size).forEach { _ ->
                        navController.popBackStack()
                    }
                    if (
                        isDrawerFragment(currentFragment)
                    ) mainActivityViewModel.enableDrawer()
                    else mainActivityViewModel.disableDrawer()
                    startDestination = currentFragment
            }
        }

        startDestination?.let { startDestination ->
            NavHost(
                modifier = modifier,
                navController = navController,
                startDestination = startDestination.name
            ) {
                composable(AccountableFragment.HomeFragment.name) {
                    val viewModel = viewModel<HomeViewModel>(factory = HomeViewModel.Factory)
                    HomeView(viewModel, mainActivityViewModel)
                }
                composable(AccountableFragment.GoalsFragment.name) {
                    val viewModel = viewModel<BooksViewModel>(
                        factory = BooksViewModel.Factory
                    )
                    BooksView(viewModel, mainActivityViewModel)
                }
                composable(AccountableFragment.BooksFragment.name) {
                    val viewModel = viewModel<BooksViewModel>(
                        factory = BooksViewModel.Factory
                    )
                    BooksView(viewModel, mainActivityViewModel)
                }
                composable(AccountableFragment.AppSettingsFragment.name) {
                    val viewModel =
                        viewModel<AppSettingsViewModel>(factory = AppSettingsViewModel.Factory)
                    AppSettingsView(viewModel, mainActivityViewModel)
                }
                composable(AccountableFragment.HelpFragment.name) {
                    val viewModel = viewModel<HelpViewModel>(factory = HelpViewModel.Factory)
                    HelpView(viewModel, mainActivityViewModel)
                }
                composable(AccountableFragment.EditFolderFragment.name) {
                    val viewModel =
                        viewModel<EditFolderViewModel>(factory = EditFolderViewModel.Factory)
                    EditFolderView(viewModel, mainActivityViewModel)
                }
                composable(AccountableFragment.EditGoalFragment.name) {
                    val viewModel =
                        viewModel<EditGoalViewModel>(factory = EditGoalViewModel.Factory)
                    EditGoalView(viewModel, mainActivityViewModel)
                }
                composable(AccountableFragment.TaskFragment.name) {
                    val viewModel = viewModel<TaskViewModel>(factory = TaskViewModel.Factory)
                    TaskView(viewModel, mainActivityViewModel)
                }
                composable(AccountableFragment.MarkupLanguageFragment.name) {
                    val viewModel =
                        viewModel<MarkupLanguageViewModel>(factory = MarkupLanguageViewModel.Factory)
                    MarkupLanguageView(viewModel)
                }
                composable(AccountableFragment.ScriptFragment.name) {
                    val viewModel =
                        viewModel<ScriptViewModel>(factory = ScriptViewModel.Factory)
                    ScriptView(viewModel, mainActivityViewModel)
                }
                composable(AccountableFragment.TeleprompterFragment.name) {
                    val viewModel =
                        viewModel<TeleprompterViewModel>(factory = TeleprompterViewModel.Factory)
                    TeleprompterView(viewModel, mainActivityViewModel)
                }
                composable(AccountableFragment.SearchFragment.name) {
                    val viewModel =
                        viewModel<SearchViewModel>(factory = SearchViewModel.Factory)
                    SearchView(viewModel, mainActivityViewModel).searchView()
                }
            }
        }
    }
}