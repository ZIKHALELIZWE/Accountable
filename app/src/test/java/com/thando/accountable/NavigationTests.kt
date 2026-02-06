package com.thando.accountable

import com.thando.accountable.AccountableNavigationController.AccountableFragment
import com.thando.accountable.database.tables.Folder
import com.thando.accountable.fragments.viewmodels.BooksViewModel.Companion.INITIAL_FOLDER_ID
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import kotlin.collections.forEach
import kotlin.intArrayOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35]) // Force Robolectric to use API 35
@LooperMode(LooperMode.Mode.LEGACY)
class NavigationTests {
    @Test
    fun nav_home_fragment_navigation(){
        val testedFragment = AccountableFragment.HomeFragment
        testInvalidFragments(
            testedFragment,
            listOf(
                AccountableFragment.GoalsFragment,
                AccountableFragment.BooksFragment,
                AccountableFragment.AppSettingsFragment,
                AccountableFragment.HelpFragment
            )
        )

        var rep = AccountableRepository.NavigationArguments(true)
        rep.scriptsOrGoalsFolderId = INITIAL_FOLDER_ID
        rep.scriptsOrGoalsFolderType = Folder.FolderType.GOALS
        compareNavigationArguments(rep,testedFragment, AccountableFragment.GoalsFragment)

        rep.isDrawerFragment = true
        rep.scriptsOrGoalsFolderId = INITIAL_FOLDER_ID
        rep.scriptsOrGoalsFolderType = Folder.FolderType.SCRIPTS
        compareNavigationArguments(rep,testedFragment, AccountableFragment.BooksFragment)

        rep = AccountableRepository.NavigationArguments(true)
        rep.isDrawerFragment = true
        compareNavigationArguments(rep,testedFragment, AccountableFragment.AppSettingsFragment)
        compareNavigationArguments(rep,testedFragment, AccountableFragment.HelpFragment)
    }

    @Test
    fun nav_app_settings_fragment_navigation(){
        val testedFragment = AccountableFragment.AppSettingsFragment
        testInvalidFragments(
            testedFragment,
            listOf(
                AccountableFragment.GoalsFragment,
                AccountableFragment.BooksFragment,
                AccountableFragment.HomeFragment,
                AccountableFragment.HelpFragment
            )
        )

        var rep = AccountableRepository.NavigationArguments(true)
        rep.isDrawerFragment = true

        rep.scriptsOrGoalsFolderId = INITIAL_FOLDER_ID
        rep.scriptsOrGoalsFolderType = Folder.FolderType.SCRIPTS
        compareNavigationArguments(rep,testedFragment, AccountableFragment.BooksFragment)

        rep = AccountableRepository.NavigationArguments(true)
        rep.isDrawerFragment = true
        compareNavigationArguments(rep,testedFragment, AccountableFragment.HomeFragment)
        compareNavigationArguments(rep,testedFragment, AccountableFragment.HelpFragment)
    }

    @Test
    fun nav_help_fragment_navigation(){
        val testedFragment = AccountableFragment.HelpFragment
        testInvalidFragments(
            testedFragment,
            listOf(
                AccountableFragment.BooksFragment,
                AccountableFragment.HomeFragment,
                AccountableFragment.AppSettingsFragment
            )
        )

        var rep = AccountableRepository.NavigationArguments(true)
        rep.isDrawerFragment = true

        rep.scriptsOrGoalsFolderId = INITIAL_FOLDER_ID
        rep.scriptsOrGoalsFolderType = Folder.FolderType.SCRIPTS
        compareNavigationArguments(rep,testedFragment, AccountableFragment.BooksFragment)

        rep = AccountableRepository.NavigationArguments(true)
        rep.isDrawerFragment = true
        compareNavigationArguments(rep,testedFragment, AccountableFragment.HomeFragment)
        compareNavigationArguments(rep,testedFragment, AccountableFragment.AppSettingsFragment)
    }

    @Test
    fun nav_goals_fragment_navigation(){
        val testedFragment = AccountableFragment.GoalsFragment
        testInvalidFragments(testedFragment,listOf(
            AccountableFragment.HomeFragment,
            AccountableFragment.EditGoalFragment,
            AccountableFragment.EditFolderFragment,
            AccountableFragment.TaskFragment
        ))

        var rep = AccountableRepository.NavigationArguments(true)
        rep.isDrawerFragment = true
        compareNavigationArguments(rep,testedFragment, AccountableFragment.HomeFragment)

        rep = AccountableRepository.NavigationArguments(true)
        compareNavigationArguments(rep,testedFragment, AccountableFragment.EditGoalFragment)
        compareNavigationArguments(rep,testedFragment, AccountableFragment.EditFolderFragment)
        compareNavigationArguments(rep,testedFragment, AccountableFragment.TaskFragment)
    }

    @Test
    fun nav_books_fragment_navigation(){
        val testedFragment = AccountableFragment.BooksFragment
        testInvalidFragments(testedFragment,listOf(
            AccountableFragment.HomeFragment,
            AccountableFragment.AppSettingsFragment,
            AccountableFragment.HelpFragment,
            AccountableFragment.EditFolderFragment,
            AccountableFragment.ScriptFragment,
            AccountableFragment.SearchFragment
        ))

        var rep = AccountableRepository.NavigationArguments(true)
        rep.isDrawerFragment = true
        compareNavigationArguments(rep,testedFragment, AccountableFragment.HomeFragment)
        compareNavigationArguments(rep,testedFragment, AccountableFragment.AppSettingsFragment)
        compareNavigationArguments(rep,testedFragment, AccountableFragment.HelpFragment)

        rep = AccountableRepository.NavigationArguments(true)
        compareNavigationArguments(rep,testedFragment, AccountableFragment.EditFolderFragment)
        compareNavigationArguments(rep,testedFragment, AccountableFragment.ScriptFragment)
    }

    @Test
    fun nav_edit_folder_fragment_navigation(){
        val testedFragment = AccountableFragment.EditFolderFragment
        testInvalidFragments(testedFragment,listOf(
            AccountableFragment.BooksFragment,
            AccountableFragment.GoalsFragment
        ))

        val rep = AccountableRepository.NavigationArguments(true)
        compareNavigationArguments(rep,testedFragment, AccountableFragment.GoalsFragment)
        rep.isDrawerFragment = true
        compareNavigationArguments(rep,testedFragment, AccountableFragment.BooksFragment)
    }

    @Test
    fun nav_edit_goal_fragment_navigation(){
        val testedFragment = AccountableFragment.EditGoalFragment
        testInvalidFragments(testedFragment,listOf(
            AccountableFragment.GoalsFragment
        ))

        val rep = AccountableRepository.NavigationArguments(true)
        compareNavigationArguments(rep,testedFragment, AccountableFragment.GoalsFragment)
    }

    @Test
    fun nav_task_fragment_navigation(){
        val testedFragment = AccountableFragment.TaskFragment
        testInvalidFragments(testedFragment,listOf(
            AccountableFragment.GoalsFragment
        ))

        val rep = AccountableRepository.NavigationArguments(true)
        compareNavigationArguments(rep,testedFragment, AccountableFragment.GoalsFragment)
    }

    @Test
    fun nav_markup_language_fragment_navigation(){
        val testedFragment = AccountableFragment.MarkupLanguageFragment
        testInvalidFragments(testedFragment,listOf(
            AccountableFragment.ScriptFragment
        ))

        val rep = AccountableRepository.NavigationArguments(true)
        compareNavigationArguments(rep,testedFragment, AccountableFragment.ScriptFragment)
    }

    @Test
    fun nav_teleprompter_fragment_navigation(){
        val testedFragment = AccountableFragment.TeleprompterFragment
        testInvalidFragments(testedFragment,listOf(
            AccountableFragment.ScriptFragment
        ))

        val rep = AccountableRepository.NavigationArguments(true)
        compareNavigationArguments(rep,testedFragment, AccountableFragment.ScriptFragment)
    }

    @Test
    fun nav_script_fragment_navigation(){
        val testedFragment = AccountableFragment.ScriptFragment
        testInvalidFragments(testedFragment,listOf(
            AccountableFragment.BooksFragment,
            AccountableFragment.MarkupLanguageFragment,
            AccountableFragment.TeleprompterFragment,
            AccountableFragment.SearchFragment
        ))

        val rep = AccountableRepository.NavigationArguments(true)
        compareNavigationArguments(rep,testedFragment, AccountableFragment.MarkupLanguageFragment)
        compareNavigationArguments(rep,testedFragment, AccountableFragment.TeleprompterFragment)
        rep.isDrawerFragment = true
        compareNavigationArguments(rep,testedFragment, AccountableFragment.BooksFragment)
    }

    private fun testInvalidFragments(testedFragment: AccountableFragment, validFragments: List<AccountableFragment>){
        val invalidFragments = getInvalidFragments(validFragments)
        invalidFragments.forEach { testFalseNavigation(testedFragment,it) }
    }

    private fun getInvalidFragments(validFragments:List<AccountableFragment>):List<AccountableFragment>{
        val invalidFragments = arrayListOf<AccountableFragment>()
        AccountableFragment.entries.forEach {
            if(!validFragments.contains(it)) invalidFragments.add(it)
        }
        return invalidFragments
    }

    private fun testFalseNavigation(from: AccountableFragment, to: AccountableFragment){
        compareNavigationArguments(
            AccountableRepository.NavigationArguments(),
            from,
            to,
            true
        )
    }

    private fun compareNavigationArguments(
        expectedArgs:AccountableRepository.NavigationArguments,
        from: AccountableFragment,
        to:AccountableFragment,
        isNull:Boolean=false
    ){
        val dir = AccountableNavigationController.getFragmentDirections(from,to)
        val expectedFragment: AccountableFragment? = if (isNull) null else to
        assertEquals(expectedFragment,dir.first)
        assertEquals(expectedArgs.isValidDir,dir.second.isValidDir)
        assertEquals(expectedArgs.isDrawerFragment,dir.second.isDrawerFragment)
        assertEquals(expectedArgs.scriptsOrGoalsFolderId,dir.second.scriptsOrGoalsFolderId)
        assertEquals(expectedArgs.scriptsOrGoalsFolderType,dir.second.scriptsOrGoalsFolderType)
    }
}