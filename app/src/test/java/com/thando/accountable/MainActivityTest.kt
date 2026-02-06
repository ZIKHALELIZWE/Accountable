package com.thando.accountable

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.material3.DrawerValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.TestMonotonicFrameClock
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thando.accountable.AccountableNavigationController.AccountableFragment
import com.thando.accountable.database.tables.Folder
import com.thando.accountable.fragments.viewmodels.AppSettingsViewModel
import com.thando.accountable.fragments.viewmodels.BooksViewModel
import com.thando.accountable.fragments.viewmodels.HelpViewModel
import com.thando.accountable.fragments.viewmodels.HomeViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLog
import java.io.PrintStream
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Force Robolectric to use API 34
@LooperMode(LooperMode.Mode.LEGACY)
class MainActivityTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        ShadowLog.stream = FilteredPrintStream(System.out)
    }

    @After
    fun cleanup() {

    }

    class FilteredPrintStream(private val delegate: PrintStream) : PrintStream(delegate) {
        override fun println(x: String?) {
            if (x != null) {
                if (x.contains("FATAL EXCEPTION")) {
                    delegate.println(x)
                }
            }
        }
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
    fun TestScope.runSuspend(function: suspend TestScope.()->Unit) {
        val scheduler = TestCoroutineScheduler()
        // Create our test frame clock
        val frameClock = TestMonotonicFrameClock( MainScope().plus(scheduler))
        //CoroutineScope(Dispatchers.Main + frameClock).launch {
        MainScope().plus(frameClock).launch {
            function()
        }
    }

    @Test
    fun coreClassesNotNull() = runTest {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create().start().resume().get()
        assertNotNull(activity)
        assertNotNull(activity.viewModel)
        assertNotNull(activity.viewModel.repository)
    }

    @Test
    fun currentFragmentIsHomeFragment() = runTest {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()  // Creates the activity
            .start()   // Starts the activity
            .resume()  // Resumes the activity to make it interactive
            .get()     // Gets the activity instance

        assertNotNull(activity.viewModel.currentFragment.value)
        assertEquals(
            AccountableFragment.HomeFragment,
            activity.viewModel.currentFragment.value
        )
        assertEquals(
            AccountableFragment.HomeFragment,
            activity.viewModel.currentFragment.value
        )
    }

    @Test
    fun directionIsNull() = runTest {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()  // Creates the activity
            .start()   // Starts the activity
            .resume()  // Resumes the activity to make it interactive
            .get()     // Gets the activity instance
        assertNull(activity.viewModel.direction.value)
    }

    @Test
    fun `Main Activity Initialized`() = runTest {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup().get()

        assertNotNull(activity.viewModel.appSettings.value)
        assertNotNull(activity.viewModel.direction)
        assertNotNull(activity.viewModel.accountableNavigationController)
        composeTestRule.onNodeWithTag("NavigationDrawerItemHomeFragment").assertExists()
        if (AccountableNavigationController.isDrawerFragment(
                activity.viewModel.currentFragment.value!!
            )
        ) composeTestRule.onNodeWithTag("NavigationDrawerItemHomeFragment").assertIsSelected()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `Switch Fragments`() = runTest {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup().get()

        assertNull(activity.viewModel.direction.value)
        assertNotNull(activity.viewModel.currentFragment.value)
        assertEquals(
            AccountableFragment.HomeFragment,
            activity.viewModel.currentFragment.value
        )

        val destinations = listOf(
            AccountableFragment.BooksFragment,
            AccountableFragment.AppSettingsFragment,
            AccountableFragment.HelpFragment,
            AccountableFragment.AppSettingsFragment,
            AccountableFragment.BooksFragment,
            AccountableFragment.HomeFragment
        )
        destinations.forEach {
            switchToNavigationDrawer(activity, it)
        }
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
    private fun switchToNavigationDrawer(
        activity: MainActivity,
        fragment: AccountableFragment
    ) {
        assertTrue(activity.viewModel.drawerEnabled.value)
        assertFalse(activity.viewModel.drawerState.value == DrawerValue.Open)
        composeTestRule.onNodeWithTag("MainActivityModalNavigationDrawerContent").assertIsNotDisplayed()

        activity.viewModel.toggleDrawer()

        assertTrue(activity.viewModel.drawerState.value == DrawerValue.Open)
        composeTestRule.onNodeWithTag("MainActivityModalNavigationDrawerContent").assertIsDisplayed()

        // Switch to fragment
        when (fragment) {
            AccountableFragment.HomeFragment -> {
                composeTestRule.onNodeWithTag("NavigationDrawerItemHomeFragment")
                    .assertExists()
                    .assertHasClickAction()
                activity.viewModel.changeFragment(AccountableFragment.HomeFragment)
                composeTestRule.waitForIdle()
                assertEquals(
                    HomeViewModel::class.java.name,
                    activity.viewModel.accountableNavigationController.fragmentViewModel.value?.javaClass?.name
                )
            }
            AccountableFragment.BooksFragment -> {
                composeTestRule.onNodeWithTag("NavigationDrawerItemBooksFragment").assertExists()
                activity.viewModel.changeFragment(AccountableFragment.BooksFragment)
                composeTestRule.waitForIdle()
                assertEquals(
                    BooksViewModel::class.java.name,
                    activity.viewModel.accountableNavigationController.fragmentViewModel.value?.javaClass?.name
                )
            }
            AccountableFragment.AppSettingsFragment -> {
                composeTestRule.onNodeWithTag("NavigationDrawerItemAppSettingsFragment").assertExists()
                activity.viewModel.changeFragment(AccountableFragment.AppSettingsFragment)
                composeTestRule.waitForIdle()
                assertEquals(
                    AppSettingsViewModel::class.java.name,
                    activity.viewModel.accountableNavigationController.fragmentViewModel.value?.javaClass?.name
                )
            }
            AccountableFragment.HelpFragment -> {
                composeTestRule.onNodeWithTag("NavigationDrawerItemHelpFragment").assertExists()
                activity.viewModel.changeFragment(AccountableFragment.HelpFragment)
                composeTestRule.waitForIdle()
                assertEquals(
                    HelpViewModel::class.java.name,
                    activity.viewModel.accountableNavigationController.fragmentViewModel.value?.javaClass?.name
                )
            }
            else -> {}
        }
        currentFragmentIs(activity, fragment)
    }

    @OptIn(ExperimentalTestApi::class)
    private fun currentFragmentIs(
        activity: MainActivity,
        fragment: AccountableFragment
    ) {
        assertEquals(
            fragment,
            activity.viewModel.currentFragment.value
        )
        assertNotNull(
            activity.viewModel.accountableNavigationController.navController?.currentDestination?.route
        )
        assertEquals(
            fragment.name,
            activity.viewModel.accountableNavigationController.navController?.currentDestination?.route
        )
    }

    @Test
    fun `Folders And Scripts Books Test`() = runTest {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .setup().get()

        switchToNavigationDrawer(activity, AccountableFragment.BooksFragment)

        val fragmentViewModel = activity.viewModel.accountableNavigationController.fragmentViewModel
        assertNotNull(fragmentViewModel)

        assertEquals(fragmentViewModel.value?.javaClass?.name, BooksViewModel::class.java.name)
        val booksViewModel = fragmentViewModel.value as BooksViewModel

        assertNull(booksViewModel.folder.value)
        assertNotNull(booksViewModel.folderType.value)
        assertEquals(Folder.FolderType.SCRIPTS, booksViewModel.folderType.value)
        assertNull(booksViewModel.intentString)
        assertNotNull(booksViewModel.appSettings.value)

        assertNotNull(booksViewModel.foldersList.value)
        assertEquals(false,booksViewModel.showScripts.value?.value)
        booksViewModel.switchFolderScript()
        composeTestRule.waitForIdle()
        assertNotNull(booksViewModel.scriptsList.value)
        assertEquals(true, booksViewModel.showScripts.value?.value)
        booksViewModel.switchFolderScript()
        composeTestRule.waitForIdle()
        assertNull(booksViewModel.goalsList.value)
        /*areEqual(-1L,
            repository.getScriptsOrGoalsFolderId().value,
            "Repository Folder ID")
        areEqual(Folder.FolderType.SCRIPTS,
            repository.getScriptsOrGoalsFolderType().value,
            "Repository Folder Type")

        notNull(foldersAndScriptsFragment.binding.viewModel,"Binding View Model Set")
        notNull(foldersAndScriptsFragment.binding.folderAdapter,"Binding Folder Adapter Set")
        notNull(foldersAndScriptsFragment.binding.scriptAdapter,"Binding Script Adapter Set")
        notNull(foldersAndScriptsFragment.binding.goalAdapter,"Binding Goal Adapter Set")
        areEqual(
            foldersAndScriptsFragment.binding.collapsingToolbar.title.toString(),
            activity.getString(R.string.books),
            "Collapsing Toolbar Title Set To Books"
        )
        notNull(
            foldersAndScriptsFragment.binding.topImageView.drawable,
            "Top Image Drawable Is Not Null"
        )
        var currentSelection = foldersAndScriptsFragment.viewModel.showScripts.value?.value
        var currentImage = foldersAndScriptsFragment.binding.switchFolderScriptButton.drawable
        var currentAdapter = foldersAndScriptsFragment.binding.foldersAndScriptsList.adapter

        // Switch to script
        foldersAndScriptsFragment.binding.switchFolderScriptButton.performClick()
        foldersAndScriptsFragment.binding.executePendingBindings()
        notEqual(currentSelection,foldersAndScriptsFragment.viewModel.showScripts.value?.value,
            "Switch Button Changed Selection")
        notEqual(currentImage, foldersAndScriptsFragment.binding.switchFolderScriptButton.drawable,
            "Switch Button Changed Image")
        notEqual(currentAdapter, foldersAndScriptsFragment.binding.foldersAndScriptsList.adapter,
            "Switch Button Changed Adapter")

        currentSelection = foldersAndScriptsFragment.viewModel.showScripts.value?.value
        currentImage = foldersAndScriptsFragment.binding.switchFolderScriptButton.drawable
        currentAdapter = foldersAndScriptsFragment.binding.foldersAndScriptsList.adapter

        // Switch back to folder
        foldersAndScriptsFragment.binding.switchFolderScriptButton.performClick()
        foldersAndScriptsFragment.binding.executePendingBindings()
        notEqual(currentSelection,foldersAndScriptsFragment.viewModel.showScripts.value?.value,
            "Switch Button Changed Selection")
        notEqual(currentImage, foldersAndScriptsFragment.binding.switchFolderScriptButton.drawable,
            "Switch Button Changed Image")
        notEqual(currentAdapter, foldersAndScriptsFragment.binding.foldersAndScriptsList.adapter,
            "Switch Button Changed Adapter")

        // Switch to Edit Folder Fragment
        foldersAndScriptsFragment.binding.addFolderScriptButton.performClick()
        val editFolderFragment = activity.navController.currentFragmentClass as EditFolderFragment

        notNull(editFolderFragment,"Edit Folder Fragment Retrieval")

        notNull(editFolderFragment.binding,"Folders And Scripts Fragment Binding")
        areEqual(-1L,repository.getScriptsOrGoalsFolderId().value,
            "Correct Id Sent To Edit Folder")
        areEqual(Folder.FolderType.SCRIPTS,repository.getScriptsOrGoalsFolderType().value,
            "Correct Type Sent To Edit Folder")
        isNull(editFolderFragment.viewModel.editFolder.value,
            "Edit Folder Opened From Root")
        areEqual(Folder.FolderType.SCRIPTS,
            editFolderFragment.viewModel.folderType.value,
            "Edit Folder ViewModel Folder Type")
        activity.onBackPressedDispatcher.onBackPressed()

        foldersAndScriptsFragment = activity.navController.currentFragmentClass as BooksFragment

        notNull(foldersAndScriptsFragment,"Folders And Scripts Fragment Retrieval")
        notNull(foldersAndScriptsFragment.binding,"Folders And Scripts Fragment Binding")
        areEqual(-1L,
            foldersAndScriptsFragment.viewModel.currentFolderId.value,
            "FoldersASF ViewModel Current Folder ID")
        areEqual(Folder.FolderType.SCRIPTS,
            foldersAndScriptsFragment.viewModel.folderType.value,
            "FoldersASF ViewModel Folder Type")
        areEqual(-1L,
            repository.getScriptsOrGoalsFolderId().value,
            "Repository Folder ID")
        areEqual(Folder.FolderType.SCRIPTS,
            repository.getScriptsOrGoalsFolderType().value,
            "Repository Folder Type")

        notNull(foldersAndScriptsFragment.binding.viewModel,"Binding View Model Set")
        notNull(foldersAndScriptsFragment.binding.folderAdapter,"Binding Folder Adapter Set")
        notNull(foldersAndScriptsFragment.binding.scriptAdapter,"Binding Script Adapter Set")
        notNull(foldersAndScriptsFragment.binding.goalAdapter,"Binding Goal Adapter Set")*/
    }
/*
    @Test
    fun testOrder(){
        activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()  // Creates the activity
            .start()   // Starts the activity
            .resume()  // Resumes the activity to make it interactive
            .get()
        viewModel = activity.viewModel

        var passed = 0
        var failed = 0
        MActivityTest.entries.forEach {
            val (pReturn,fReturn) = getTest(it).run()
            passed+=pReturn
            failed+=fReturn
        }
        val green = "\u001B[32m"
        val blue = "\u001B[34m"
        val reset = "\u001B[0m"
        println("All Tests: $green$passed$reset of $blue${passed+failed}$reset Passed")
    }
*/
    private enum class MActivityTest {
        CoreClassesNotNull,
        CurrentFragmentIsHomeFragment,
        DirectionIsNull,
        MainActivityInitialized,
        SwitchFragments,
        FoldersAndScriptsBooksTest,
        AddFolderTest,
        FolderDialogTest,
        FoldersAndScriptsFolderNavigation
    }

    private class MTest(
        private val description:String,
        private val log:Boolean,
        private val function: (
            notNull: KFunction2<Any?,String?,Unit>,
            isNull: KFunction2<Any?,String?,Unit>,
            areEqual: KFunction3<Any?,Any?,String?,Unit>,
            notEqual: KFunction3<Any?,Any?,String?,Unit>) -> Unit
    )
    {
        private var passedTests = 0
        private var failedTests = 0
        private val purple = "\u001B[35m"
        private val red = "\u001B[31m"
        private val green = "\u001B[32m"
        private val blue = "\u001B[34m"
        private val reset = "\u001B[0m"

        fun run():Pair<Int,Int>{
            passedTests = 0
            failedTests = 0
            if (log) println("${purple}$description")
            function(::notNull,::isNull,::areEqual,::notEqual)
            println("$description: $green$passedTests$blue of ${passedTests+failedTests} Passed")
            if (failedTests!=0) println("${red}TESTS FAILED: $reset$failedTests")
            return Pair(passedTests,failedTests)
        }

        private fun notNull(value:Any?,description: String?){
            if (value == null) {
                ++failedTests
                if (log) Log.e("notNull",description?.let {"$description Is Null"}?:"Value Is Null")
            }
            else{
                ++passedTests
                if (log) Log.w("notNull", description?.let {
                    "$description Is Not Null:\n" +
                            "$value"
                } ?: ("Value Is Not Null:\n" +
                        "$value")
                )
            }
        }

        private fun isNull(value:Any?,description: String?){
            if (value != null) {
                ++failedTests
                if (log) Log.e("isNull", description?.let {
                    "$description Is Not Null:\n" +
                            "$value"
                } ?: ("Value Is Not Null:\n" +
                        "$value")
                )
            }
            else{
                ++passedTests
                if (log) Log.w("isNull",description?.let {"$description Is Null"}?:"Value Is Null")
            }
        }

        private fun areEqual(expected:Any?,actual:Any?,description: String?){
            if (expected==actual){
                ++passedTests
                if (log) Log.w(
                    "areEqual",
                    description?.let {
                        "$description:\n\t$expected"
                    }
                        ?:"$expected")
            }
            else{
                ++failedTests
                if (log) Log.e(
                    "areEqual",
                    description?.let {
                        "$description:\n\tactual:\t$expected\n\texpected:\t${actual}"
                    }
                        ?:"actual:\t$expected\nexpected:\t${actual}")
            }
        }

        private fun notEqual(expected:Any?,actual:Any?,description: String?){
            if (expected!=actual){
                ++passedTests
                if (log) Log.w(
                    "notEqual",
                    description?.let {
                        "$description:\n\texpected:\t$expected\n\tactual:\t${actual}"
                    }
                        ?:"expected:\t$expected\nactual:\t${actual}")
            }
            else{
                ++failedTests
                if (log) Log.e(
                    "notEqual",
                    description?.let {
                        "$description:\n\t$expected"
                    }
                        ?:"$expected")
            }
        }
    }
/*
    private fun getTest(test: MActivityTest):MTest = when(test) {
        MActivityTest.CoreClassesNotNull -> MTest(
            "Core Classes Not Null",false
        ) { notNull, _, _, _ ->
            notNull(activity,"activity")
            notNull(activity.viewModel,"viewModel")
            notNull(activity.viewModel.repository,"repository")
        }

        MActivityTest.CurrentFragmentIsHomeFragment -> MTest(
            "Current Fragment Is Home Fragment", false
        ) { notNull, _, areEqual, _ ->
            runSuspend {
                notNull(activity.viewModel.currentFragment.value, "viewModel.currentFragment.value")
                areEqual(
                    AccountableFragment.HomeFragment,
                    activity.viewModel.currentFragment.value,
                    "Current Fragment"
                )
            }
        }

        MActivityTest.DirectionIsNull -> MTest(
            "Direction Is Null", false
        ){ _, isNull, _, _ ->
            runSuspend {
                isNull(activity.viewModel.direction.first(), "ViewModel Direction Is Null")
            }
        }
        MActivityTest.MainActivityInitialized -> MTest(
            "Main Activity Initialized", false
        ){ notNull, _, areEqual, _ ->
            notNull(viewModel.appSettings.value,"viewModel.appSettings.value")
            notNull(viewModel.direction,"viewModel.direction")
            notNull(activity.navController,"activity.navController")
            notNull(binding.navView.checkedItem,"binding.navView.checkedItem")
            if (AccountableNavigationController.isDrawerFragment(
                    viewModel.currentFragment.value!!
                )) areEqual(
                AccountableNavigationController.getFragmentId(viewModel.currentFragment.value!!),
                binding.navView.checkedItem!!.itemId,
                    "Checked Drawer Item"
            )
        }
        MActivityTest.SwitchFragments -> MTest(
            "Switch Fragments", false
        ){ notNull, isNull, areEqual, _ ->
            isNull(viewModel.direction.value,"ViewModel Direction Is Null")
            notNull(viewModel.currentFragment.value,"viewModel.currentFragment.value")
            areEqual(
                AccountableFragment.HomeFragment,
                viewModel.currentFragment.value,
                "Current Fragment"
            )
            val destinations = listOf(
                AccountableFragment.GoalsFragment,
                AccountableFragment.BooksFragment,
                AccountableFragment.AppSettingsFragment,
                AccountableFragment.HelpFragment,
                AccountableFragment.AppSettingsFragment,
                AccountableFragment.BooksFragment,
                AccountableFragment.GoalFragment,
                AccountableFragment.HomeFragment)
            destinations.forEach { switchToNavigationDrawer(it,notNull,areEqual) }
        }
        MActivityTest.FoldersAndScriptsBooksTest -> MTest(
            "Folders And Scripts Books Test", false
        ){ notNull, isNull, areEqual, notEqual ->
            switchToNavigationDrawer(AccountableFragment.BooksFragment,
                notNull,
                areEqual)

            var foldersAndScriptsFragment = activity.navController.currentFragmentClass as BooksFragment

            notNull(foldersAndScriptsFragment,"Folders And Scripts Fragment Retrieval")
            notNull(foldersAndScriptsFragment.binding,"Folders And Scripts Fragment Binding")
            areEqual(-1L,
                foldersAndScriptsFragment.viewModel.currentFolderId.value,
                "FoldersASF ViewModel Current Folder ID")
            areEqual(Folder.FolderType.SCRIPTS,
                foldersAndScriptsFragment.viewModel.folderType.value,
                "FoldersASF ViewModel Folder Type")
            areEqual(-1L,
                repository.getScriptsOrGoalsFolderId().value,
                "Repository Folder ID")
            areEqual(Folder.FolderType.SCRIPTS,
                repository.getScriptsOrGoalsFolderType().value,
                "Repository Folder Type")

            notNull(foldersAndScriptsFragment.binding.viewModel,"Binding View Model Set")
            notNull(foldersAndScriptsFragment.binding.folderAdapter,"Binding Folder Adapter Set")
            notNull(foldersAndScriptsFragment.binding.scriptAdapter,"Binding Script Adapter Set")
            notNull(foldersAndScriptsFragment.binding.goalAdapter,"Binding Goal Adapter Set")
            areEqual(
                foldersAndScriptsFragment.binding.collapsingToolbar.title.toString(),
                activity.getString(R.string.books),
                "Collapsing Toolbar Title Set To Books"
            )
            notNull(
                foldersAndScriptsFragment.binding.topImageView.drawable,
                "Top Image Drawable Is Not Null"
            )
            var currentSelection = foldersAndScriptsFragment.viewModel.showScripts.value?.value
            var currentImage = foldersAndScriptsFragment.binding.switchFolderScriptButton.drawable
            var currentAdapter = foldersAndScriptsFragment.binding.foldersAndScriptsList.adapter

            // Switch to script
            foldersAndScriptsFragment.binding.switchFolderScriptButton.performClick()
            foldersAndScriptsFragment.binding.executePendingBindings()
            notEqual(currentSelection,foldersAndScriptsFragment.viewModel.showScripts.value?.value,
                "Switch Button Changed Selection")
            notEqual(currentImage, foldersAndScriptsFragment.binding.switchFolderScriptButton.drawable,
                "Switch Button Changed Image")
            notEqual(currentAdapter, foldersAndScriptsFragment.binding.foldersAndScriptsList.adapter,
                "Switch Button Changed Adapter")

            currentSelection = foldersAndScriptsFragment.viewModel.showScripts.value?.value
            currentImage = foldersAndScriptsFragment.binding.switchFolderScriptButton.drawable
            currentAdapter = foldersAndScriptsFragment.binding.foldersAndScriptsList.adapter

            // Switch back to folder
            foldersAndScriptsFragment.binding.switchFolderScriptButton.performClick()
            foldersAndScriptsFragment.binding.executePendingBindings()
            notEqual(currentSelection,foldersAndScriptsFragment.viewModel.showScripts.value?.value,
                "Switch Button Changed Selection")
            notEqual(currentImage, foldersAndScriptsFragment.binding.switchFolderScriptButton.drawable,
                "Switch Button Changed Image")
            notEqual(currentAdapter, foldersAndScriptsFragment.binding.foldersAndScriptsList.adapter,
                "Switch Button Changed Adapter")

            // Switch to Edit Folder Fragment
            foldersAndScriptsFragment.binding.addFolderScriptButton.performClick()
            val editFolderFragment = activity.navController.currentFragmentClass as EditFolderFragment

            notNull(editFolderFragment,"Edit Folder Fragment Retrieval")

            notNull(editFolderFragment.binding,"Folders And Scripts Fragment Binding")
            areEqual(-1L,repository.getScriptsOrGoalsFolderId().value,
                "Correct Id Sent To Edit Folder")
            areEqual(Folder.FolderType.SCRIPTS,repository.getScriptsOrGoalsFolderType().value,
                "Correct Type Sent To Edit Folder")
            isNull(editFolderFragment.viewModel.editFolder.value,
                "Edit Folder Opened From Root")
            areEqual(Folder.FolderType.SCRIPTS,
                editFolderFragment.viewModel.folderType.value,
                "Edit Folder ViewModel Folder Type")
            activity.onBackPressedDispatcher.onBackPressed()

            foldersAndScriptsFragment = activity.navController.currentFragmentClass as BooksFragment

            notNull(foldersAndScriptsFragment,"Folders And Scripts Fragment Retrieval")
            notNull(foldersAndScriptsFragment.binding,"Folders And Scripts Fragment Binding")
            areEqual(-1L,
                foldersAndScriptsFragment.viewModel.currentFolderId.value,
                "FoldersASF ViewModel Current Folder ID")
            areEqual(Folder.FolderType.SCRIPTS,
                foldersAndScriptsFragment.viewModel.folderType.value,
                "FoldersASF ViewModel Folder Type")
            areEqual(-1L,
                repository.getScriptsOrGoalsFolderId().value,
                "Repository Folder ID")
            areEqual(Folder.FolderType.SCRIPTS,
                repository.getScriptsOrGoalsFolderType().value,
                "Repository Folder Type")

            notNull(foldersAndScriptsFragment.binding.viewModel,"Binding View Model Set")
            notNull(foldersAndScriptsFragment.binding.folderAdapter,"Binding Folder Adapter Set")
            notNull(foldersAndScriptsFragment.binding.scriptAdapter,"Binding Script Adapter Set")
            notNull(foldersAndScriptsFragment.binding.goalAdapter,"Binding Goal Adapter Set")
        }
        MActivityTest.AddFolderTest -> MTest(
            "Add Folder Test", false
        ){ notNull, isNull, areEqual, _ ->
            var foldersAndScriptsFragment = activity.navController.currentFragmentClass as BooksFragment
            // Switch to Edit Folder Fragment
            foldersAndScriptsFragment.binding.addFolderScriptButton.performClick()
            var editFolderFragment = activity.navController.currentFragmentClass as EditFolderFragment
            var editFolderViewModel = editFolderFragment.viewModel
            var editFolderBinding = editFolderFragment.binding
            notNull(editFolderFragment,"Edit Folder Fragment Retrieval")
            editFolderBinding.executePendingBindings()
            areEqual(-1L,repository.getScriptsOrGoalsFolderId().value,
                "Correct Id Sent To Edit Folder")
            areEqual(Folder.FolderType.SCRIPTS,repository.getScriptsOrGoalsFolderType().value,
                "Correct Type Sent To Edit Folder")
            areEqual(Folder.FolderType.SCRIPTS,
                editFolderViewModel.folderType.value,
                "Edit Folder ViewModel Folder Type")

            isNull(editFolderViewModel.editFolder.value,
                "Edit Folder Opened From Root")
            isNull(editFolderViewModel.folder.value,
                "Parent Folder")
            notNull(editFolderViewModel.appSettings.value,
                "App Settings")
            notNull(editFolderViewModel.folderType.value,
                "Folder Type")

            notNull(editFolderViewModel.newEditFolder.value,
                "New Edit Folder That Populates The Views")
            notNull(editFolderViewModel.updateButtonText.value,
                "Update Button Text Should Be Initialized")
            areEqual(editFolderViewModel.updateButtonText.value?.asString(activity.applicationContext),
                activity.getString(R.string.add_folder),
                "Update Button Should Be Add Folder")

            notNull(editFolderBinding,"Edit Folder Fragment Binding")
            notNull(editFolderBinding.viewModel,"ViewModel Set To Binding")
            isNull(editFolderBinding.folderImage.drawable,"No Image Set")
            areEqual(editFolderBinding.folderImage.visibility, View.GONE,
                "Image is invisible")

            areEqual(editFolderBinding.folderName.isEnabled,
                true,
                "Edit Text Enabled")
            areEqual(editFolderBinding.folderName.text.toString(),"",
                "No Title Is Set")
            editFolderBinding.folderName.setText("Random Text")
            editFolderBinding.executePendingBindings()
            areEqual(editFolderBinding.folderName.text.toString(),"Random Text",
                "Text Can Be Written")
            areEqual(editFolderViewModel.newEditFolder.value?.folderName?.value,"Random Text",
                "Changes Reflect In View Model")

            areEqual(editFolderBinding.chooseImageButton.isEnabled,true,
                "Choose Image Button Must Be Enabled")
            areEqual(editFolderBinding.removeImageButton.isEnabled,true,
                "Remove Image Button Must Be Enabled")
            areEqual(editFolderBinding.removeImageButton.visibility,View.GONE,
                "Remove Image Button Must Be Invisible")
            areEqual(editFolderBinding.updateFolderButton.isEnabled,true,
                "Update Folder Button Must Be Enabled")
            areEqual(editFolderBinding.updateFolderButton.text.toString(),
                activity.getString(R.string.add_folder),
                "Update Folder Button Text Must Be Add Folder")

            editFolderBinding.folderName.setText("")
            editFolderBinding.executePendingBindings()
            areEqual(editFolderBinding.folderName.text.toString(),"",
                "Text Can Be Written")
            areEqual(editFolderViewModel.newEditFolder.value?.folderName?.value,"",
                "Changes Reflect In View Model")
            areEqual(editFolderBinding.updateFolderButton.isEnabled,false,
                "Update Folder Button Must Be Disabled When There Is No Title")

            // Images
            editFolderViewModel.setImage(getUriFromDrawable(R.drawable.ic_home_black_24dp))
            notNull(editFolderViewModel.newEditFolder.value?.getUri(activity.applicationContext)?.value,
                "Image Uri Is Set")
            editFolderBinding.executePendingBindings()

            notNull(editFolderBinding.folderImage.drawable,"Drawable")
            areEqual(editFolderBinding.folderImage.visibility, View.VISIBLE,
                "Image Visibility")
            areEqual(editFolderBinding.removeImageButton.visibility,View.VISIBLE,
                "Remove Image Button")

            editFolderBinding.removeImageButton.performClick()
            editFolderBinding.executePendingBindings()

            isNull(editFolderBinding.folderImage.drawable,"Image Delete Drawable")
            areEqual(editFolderBinding.folderImage.visibility, View.GONE,
                "Image Visibility")
            areEqual(editFolderBinding.removeImageButton.visibility,View.GONE,
                "Remove Image Button")

            //On back pressed with Image and text
            editFolderBinding.folderName.setText("Random Text")
            editFolderViewModel.setImage(getUriFromDrawable(R.drawable.ic_home_black_24dp))
            editFolderBinding.executePendingBindings()
            var newEditFolderId = editFolderViewModel.newEditFolder.value?.folderId
            var newEditFolder:Folder?=null
            editFolderViewModel.viewModelScope.launch {
                newEditFolder = repository.dao.getFolderNow(newEditFolderId)
            }
            notNull(newEditFolder,"New Edit Folder Is Saved")
            areEqual(newEditFolder!!.folderName.value,"Random Text","Folder Name")
            var newEditFolderImage = newEditFolder!!.imageResource
            notNull(newEditFolderImage.getUriFromStorage(activity.applicationContext),
                "Image In Storage")

            // Test that choose image replaces already chosen image
            val imageUri = newEditFolderImage.getUriFromStorage(activity.applicationContext)
            val parentFile = File(imageUri?.toFile()?.parent!!)
            val parentFileSize = parentFile.listFiles()?.size
            areEqual(parentFileSize,1,
                "Only New Folder Image Is Stored")
            editFolderViewModel.setImage(getUriFromDrawable(R.drawable.ic_stars_black_24dp))
            editFolderBinding.executePendingBindings()
            areEqual(parentFile.listFiles()?.size,parentFileSize,
                "Previous Image Deleted")

            activity.onBackPressedDispatcher.onBackPressed()
            foldersAndScriptsFragment = activity.navController.currentFragmentClass as BooksFragment
            notNull(foldersAndScriptsFragment,"Back To Books")
            foldersAndScriptsFragment.viewModel.viewModelScope.launch {
                newEditFolder = repository.dao.getFolderNow(newEditFolderId)
            }

            isNull(newEditFolder,"New Edit Folder")
            isNull(newEditFolderImage.getUriFromStorage(activity.applicationContext),
                "Image In Storage")

            // Switch to Edit Folder Fragment
            foldersAndScriptsFragment.binding.addFolderScriptButton.performClick()
            editFolderFragment = activity.navController.currentFragmentClass as EditFolderFragment
            editFolderViewModel = editFolderFragment.viewModel
            editFolderBinding = editFolderFragment.binding
            notNull(editFolderFragment,"Saving Folder")
            editFolderBinding.executePendingBindings()

            editFolderBinding.folderName.setText("Random Text")
            editFolderViewModel.setImage(getUriFromDrawable(R.drawable.ic_home_black_24dp))
            editFolderBinding.executePendingBindings()

            newEditFolderId = editFolderViewModel.newEditFolder.value?.folderId
            newEditFolderImage = editFolderViewModel.newEditFolder.value!!.imageResource

            editFolderBinding.updateFolderButton.performClick()

            foldersAndScriptsFragment = activity.navController.currentFragmentClass as BooksFragment
            notNull(foldersAndScriptsFragment,"Back To Books")

            foldersAndScriptsFragment.viewModel.viewModelScope.launch {
                newEditFolder = repository.dao.getFolderNow(newEditFolderId)
            }
            notNull(newEditFolder,"New Edit Folder")
            notNull(newEditFolderImage.getUriFromStorage(activity.applicationContext),
                "Image In Storage")
        }
        MActivityTest.FolderDialogTest -> MTest(
            "Folder Dialog Test", false
        ){ notNull, isNull, areEqual, notEqual ->
            var foldersAndScriptsFragment = activity.navController.currentFragmentClass as BooksFragment
            val foldersAndScriptsViewModel = foldersAndScriptsFragment.viewModel
            var foldersAndScriptsBinding = foldersAndScriptsFragment.binding

            notNull(foldersAndScriptsFragment,"Folders And Scripts Fragment Retrieval")
            foldersAndScriptsBinding.executePendingBindings()

            notNull(foldersAndScriptsViewModel.showScripts.value?.value,
                "Show Scripts")
            areEqual(foldersAndScriptsViewModel.showScripts.value?.value,false,
                "Showing Folders")
            areEqual(foldersAndScriptsViewModel.currentFolderId.value,-1L,
                "Current Folder App Settings")
            areEqual(foldersAndScriptsViewModel.foldersList.value?.size,1,"Folders List")
            areEqual(foldersAndScriptsBinding.foldersAndScriptsList.adapter,
                foldersAndScriptsBinding.folderAdapter,
                "Folder Adapter Set")
            foldersAndScriptsBinding.executePendingBindings()
            var folderItemAdapter = foldersAndScriptsBinding.foldersAndScriptsList.adapter as FolderItemAdapter
            notNull(folderItemAdapter,"Folder Item Adapter")

            areEqual(folderItemAdapter.itemCount,1,"Item Count")

            var viewHolder = folderItemAdapter.onCreateViewHolder(foldersAndScriptsBinding.foldersAndScriptsList, 0)
            folderItemAdapter.onBindViewHolder(viewHolder, 0)

            notNull(viewHolder.binding,"Adapter Binds")
            viewHolder.binding.executePendingBindings()

            notNull(viewHolder.binding.task,"Binding Task")
            areEqual(viewHolder.binding.root.hasOnClickListeners(),true,"Binding OnClickListener")
            areEqual(viewHolder.binding.root.hasOnLongClickListeners(),true,"Binding OnLongClickListener")

            val folders = repository.dao.getFoldersNow(viewHolder.binding.task?.folderId, viewHolder.binding.task?.folderType)
            areEqual(viewHolder.binding.numFoldersText.text.toString(),
                (folders?.size ?: 0).toString(),
                "Num Folders Text")
            foldersAndScriptsViewModel.viewModelScope.launch {
                val scripts = repository.dao.getScriptsNow(viewHolder.binding.task?.folderId)
                areEqual(viewHolder.binding.numScriptsText.text.toString(),
                    (scripts?.size ?: 0).toString(),
                    "Num Scripts Text")
            }

            notNull(viewHolder.binding.folderImageView.drawable,"Folder Image")

            areEqual(viewHolder.binding.folderName.text.toString(),
                "Random Text",
                "Folder Name")
            viewHolder.binding.root.performLongClick()
            viewHolder.binding.executePendingBindings()
            notNull(viewHolder.longClickDialog,"Long Click Dialog")
            notNull(viewHolder.longClickDialog?.binding,"Dialog Binding")
            viewHolder.longClickDialog?.binding?.executePendingBindings()

            notNull(viewHolder.longClickDialog?.onEditAction,"On Edit Action")
            notNull(viewHolder.longClickDialog?.onDeleteAction,"On Delete Action")
            notNull(viewHolder.longClickDialog?.folderAdapter,"Folder Adapter")
            notNull(viewHolder.longClickDialog?.folder,"Folder")
            notNull(viewHolder.longClickDialog?.dismissListener,"Dismiss Listener")

            areEqual(viewHolder.longClickDialog?.binding?.deleteButton?.hasOnClickListeners(),true,
                "Delete Button On Click")
            areEqual(viewHolder.longClickDialog?.binding?.editButton?.hasOnClickListeners(),true,
                "Edit Button On Click")
            areEqual(viewHolder.longClickDialog?.binding?.deleteButton?.visibility,View.VISIBLE,
                "Delete Button Visibility")
            areEqual(viewHolder.longClickDialog?.binding?.editButton?.visibility,View.VISIBLE,
                "Edit Button Visibility")
            areEqual(viewHolder.longClickDialog?.binding?.displayViewFrameLayout?.size,1,
                "Folder View Holder Displayed")
            areEqual(viewHolder.longClickDialog?.binding?.displayViewFrameLayout?.get(0)?.isEnabled,false,
                "Folder View Holder Is Disabled")
            viewHolder.longClickDialog?.dismiss()
            isNull(viewHolder.longClickDialog,"Long Click Dialog Dismissed")
            notNull(viewHolder.binding.task?.imageResource?.getUriFromStorage(activity.applicationContext),
                "Folder Image Stored")

            // Testing Edit Folder
            var folder = viewHolder.binding.task
            viewHolder.binding.root.performLongClick()
            viewHolder.binding.executePendingBindings()
            viewHolder.longClickDialog?.binding?.editButton?.performClick()

            var editFolderFragment = activity.navController.currentFragmentClass as EditFolderFragment
            var editFolderViewModel = editFolderFragment.viewModel
            var editFolderBinding = editFolderFragment.binding
            notNull(editFolderFragment,"Edit Folder Fragment Retrieval")
            editFolderBinding.executePendingBindings()
            areEqual(-1L,repository.getScriptsOrGoalsFolderId().value,
                "Correct Id Sent To Edit Folder")
            areEqual(Folder.FolderType.SCRIPTS,repository.getScriptsOrGoalsFolderType().value,
                "Correct Type Sent To Edit Folder")
            areEqual(Folder.FolderType.SCRIPTS,
                editFolderViewModel.folderType.value,
                "Edit Folder ViewModel Folder Type")

            notNull(editFolderViewModel.editFolder.value,
                "Edit Folder Opened From Edit")
            notNull(editFolderViewModel.editFolder.value?.imageResource?.getUriFromStorage(activity.applicationContext),
                "Folder Image Stored")
            isNull(editFolderViewModel.folder.value,
                "Parent Folder")
            notNull(editFolderViewModel.appSettings.value,
                "App Settings")
            notNull(editFolderViewModel.folderType.value,
                "Folder Type")

            notNull(editFolderViewModel.newEditFolder.value,
                "New Edit Folder That Populates The Views")
            notNull(editFolderViewModel.updateButtonText.value,
                "Update Button Text Should Be Initialized")
            areEqual(editFolderViewModel.updateButtonText.value?.asString(activity.applicationContext),
                activity.getString(R.string.update_folder),
                "Update Button Should Be Update Folder")

            notNull(editFolderBinding,"Edit Folder Fragment Binding")
            notNull(editFolderBinding.viewModel,"ViewModel Set To Binding")
            notNull(editFolderBinding.folderImage.drawable,"Image Loaded")
            areEqual(editFolderBinding.folderImage.visibility, View.VISIBLE,
                "Image Is Visible")
            areEqual(editFolderBinding.removeImageButton.visibility, View.VISIBLE,
                "Remove Image")

            areEqual(editFolderBinding.folderName.isEnabled,
                true,
                "Edit Text Enabled")
            areEqual(editFolderBinding.folderName.text.toString(),"Random Text",
                "Title Is Set")

            notEqual(editFolderViewModel.editFolder.value?.folderId,editFolderViewModel.newEditFolder.value?.folderId,
                "Ids of original and duplicate folder")

            // Since newEditFolder duplicates the folder, there should be 2 images in files
            val imageUri = editFolderViewModel.newEditFolder.value?.imageResource?.getUriFromStorage(activity.applicationContext)
            var parentFileSize = getImageResourceFolderSize(editFolderViewModel.newEditFolder.value?.imageResource)
            areEqual(parentFileSize,2,"Original And Duplicate Image")
            notEqual(imageUri,editFolderViewModel.editFolder.value?.imageResource?.getUriFromStorage(activity.applicationContext),
                "Original And Duplicate Image")
            notEqual(folder?.imageResource?.getUriFromStorage(activity.applicationContext),
                imageUri,
                "Original From Database And Duplicate Image")
            areEqual(folder?.imageResource?.getUriFromStorage(activity.applicationContext),
                editFolderViewModel.editFolder.value?.imageResource?.getUriFromStorage(activity.applicationContext),
                "Original From Database And Original")

            editFolderViewModel.setImage(getUriFromDrawable(R.drawable.folder_drawable))
            editFolderBinding.executePendingBindings()
            areEqual(folder?.imageResource?.getUriFromStorage(activity.applicationContext),
                editFolderViewModel.editFolder.value?.imageResource?.getUriFromStorage(activity.applicationContext),
                "Original From Database And Original")
            parentFileSize = getImageResourceFolderSize(folder?.imageResource)
            areEqual(parentFileSize,2,"Original And Duplicate Image")
            notEqual(folder?.imageResource?.getUriFromStorage(activity.applicationContext),
                imageUri, "Original From Database And Duplicate Image")

            var folderTitle = folder?.folderName?.value
            activity.onBackPressedDispatcher.onBackPressed()

            foldersAndScriptsFragment = activity.navController.currentFragmentClass as BooksFragment
            foldersAndScriptsBinding = foldersAndScriptsFragment.binding

            notNull(foldersAndScriptsFragment,"Folders And Scripts Fragment Retrieval")
            foldersAndScriptsBinding.executePendingBindings()

            parentFileSize = getImageResourceFolderSize(folder?.imageResource)
            areEqual(parentFileSize,1,"Original Image")
            isNull(repository.getEditFolder().value,"Edit Folder")
            isNull(repository.getNewEditFolder().value,"New Edit Folder")
            areEqual(repository.getFoldersList().value?.size,1,"Folder List")

            // Go To Edit
            folderItemAdapter = foldersAndScriptsBinding.foldersAndScriptsList.adapter as FolderItemAdapter
            notNull(folderItemAdapter,"Folder Item Adapter")

            areEqual(folderItemAdapter.itemCount,1,"Item Count")

            viewHolder = folderItemAdapter.onCreateViewHolder(foldersAndScriptsBinding.foldersAndScriptsList, 0)
            folderItemAdapter.onBindViewHolder(viewHolder, 0)
            notNull(viewHolder.binding,"Adapter Binds")

            viewHolder.binding.executePendingBindings()
            viewHolder.binding.root.performLongClick()
            viewHolder.binding.executePendingBindings()
            viewHolder.longClickDialog?.binding?.editButton?.performClick()

            editFolderFragment = activity.navController.currentFragmentClass as EditFolderFragment
            editFolderViewModel = editFolderFragment.viewModel
            editFolderBinding = editFolderFragment.binding
            notNull(editFolderFragment,"Edit Folder Fragment Retrieval")
            editFolderBinding.executePendingBindings()

            areEqual(editFolderViewModel.editFolder.value?.folderName?.value,
                folderTitle,
                "Edit Folder Database Folder Name \"Random Text\"")

            editFolderBinding.folderName.setText("New Text")
            folderTitle = "New Text"
            editFolderViewModel.setImage(getUriFromDrawable(R.drawable.folder_drawable))

            editFolderBinding.updateFolderButton.performClick()

            foldersAndScriptsFragment = activity.navController.currentFragmentClass as BooksFragment
            foldersAndScriptsBinding = foldersAndScriptsFragment.binding

            notNull(foldersAndScriptsFragment,"Folders And Scripts Fragment Retrieval")
            foldersAndScriptsBinding.executePendingBindings()
            areEqual(repository.getFoldersList().value?.size,1,"Folder List")

            folder = repository.getFoldersList().value!![0]
            areEqual(folder.folderName.value,folderTitle,"Updated Folder Title")
        }
        MActivityTest.FoldersAndScriptsFolderNavigation -> MTest(
            "Folders And Scripts Folder Navigation", true
        ){ notNull, isNull, areEqual, notEqual ->
            var foldersAndScriptsFragment = activity.navController.currentFragmentClass as BooksFragment
            var foldersAndScriptsViewModel = foldersAndScriptsFragment.viewModel
            var foldersAndScriptsBinding = foldersAndScriptsFragment.binding

            notNull(foldersAndScriptsFragment,"Folders And Scripts Fragment Retrieval")
            foldersAndScriptsBinding.executePendingBindings()
            areEqual(repository.getFoldersList().value?.size,1,"Folder List")

            // click into the folder
            var (folderItemAdapter,viewHolder,folders) = clickFolder(0,foldersAndScriptsBinding,foldersAndScriptsViewModel,notNull,areEqual)
            areEqual(folders?.size,0,"Click Folder Size")
            areEqual(folderItemAdapter.itemCount,0,"Item Adapter Size")
            areEqual(foldersAndScriptsBinding.collapsingToolbar.title.toString(),
                viewHolder.binding.task?.folderName?.value,
                "Title Folder Name")
            notNull(foldersAndScriptsBinding.topImageView.drawable,"Top Image")
            areEqual(foldersAndScriptsBinding.topImageView.drawable is BitmapDrawable,viewHolder.binding.folderImageView.drawable is BitmapDrawable,
                "Selected Folder Drawable")
            areEqual(repository.getScriptsOrGoalsFolderId().value,viewHolder.binding.task?.folderId,
                "Repository Scripts Folder Id")
            notNull(viewHolder.binding.task?.folderShowScripts?.value,"folder Show Scripts")
            notEqual(foldersAndScriptsBinding.foldersAndScriptsList.adapter,foldersAndScriptsFragment.folderAdapter,
                "Folder Adapter")
            if (viewHolder.binding.task?.folderShowScripts?.value == true) foldersAndScriptsBinding.switchFolderScriptButton.performClick()
            foldersAndScriptsBinding.executePendingBindings()
            areEqual(foldersAndScriptsBinding.foldersAndScriptsList.adapter,foldersAndScriptsFragment.folderAdapter,
                "Folder Adapter")

            folderItemAdapter = foldersAndScriptsBinding.foldersAndScriptsList.adapter as FolderItemAdapter
            notNull(folderItemAdapter,"Folder Item Adapter")

            for (i in 0 until 10){
                val (out1,out2) = createBookFolder(foldersAndScriptsFragment,
                    "Folder 1 Folder",
                    R.drawable.ic_back_button,
                    notNull,isNull,areEqual)
                foldersAndScriptsFragment = out1
                foldersAndScriptsBinding = out2
            }

            areEqual(repository.getFoldersList().value?.size,10,"Folders In List")
            areEqual(foldersAndScriptsViewModel.foldersList.value?.size,10,"ViewModel List")
            areEqual(foldersAndScriptsBinding.foldersAndScriptsList.adapter?.itemCount,10,
                "Adapter List")

            val (out1,out2,out3) = clickFolder(2,foldersAndScriptsBinding, foldersAndScriptsViewModel, notNull, areEqual)
            folderItemAdapter = out1
            viewHolder = out2
            folders = out3
        }
    }

    private fun clickFolder(
        position: Int,
        foldersAndScriptsBinding:FragmentFoldersAndScriptsBinding,
        foldersAndScriptsViewModel:FoldersAndScriptsViewModel,
        notNull: KFunction2<Any?, String?, Unit>,
        areEqual: KFunction3<Any?, Any?, String?, Unit>
    ):Triple<FolderItemAdapter, FolderItemAdapter.FolderItemViewHolder, List<Folder>?>{
        val folderItemAdapter = foldersAndScriptsBinding.foldersAndScriptsList.adapter as FolderItemAdapter
        notNull(folderItemAdapter,"Folder Item Adapter")

        val viewHolder = folderItemAdapter.onCreateViewHolder(foldersAndScriptsBinding.foldersAndScriptsList, 0)
        folderItemAdapter.onBindViewHolder(viewHolder, position)

        notNull(viewHolder.binding,"Adapter Binds")
        viewHolder.binding.executePendingBindings()

        notNull(viewHolder.binding.task,"Binding Task")
        areEqual(viewHolder.binding.root.hasOnClickListeners(),true,"Binding OnClickListener")
        areEqual(viewHolder.binding.root.hasOnLongClickListeners(),true,"Binding OnLongClickListener")

        val folders = repository.dao.getFoldersNow(viewHolder.binding.task?.folderId, viewHolder.binding.task?.folderType)
        areEqual(viewHolder.binding.numFoldersText.text.toString(),
            (folders?.size ?: 0).toString(),
            "Num Folders Text")
        foldersAndScriptsViewModel.viewModelScope.launch {
            val scripts = repository.dao.getScriptsNow(viewHolder.binding.task?.folderId)
            areEqual(viewHolder.binding.numScriptsText.text.toString(),
                (scripts?.size ?: 0).toString(),
                "Num Scripts Text")
        }

        notNull(viewHolder.binding.folderImageView.drawable,"Folder Image")
        viewHolder.binding.root.performClick()
        foldersAndScriptsBinding.executePendingBindings()
        return Triple(folderItemAdapter,viewHolder,folders)
    }

    private fun createBookFolder(foldersAndScriptsFragmentInput: BooksFragment,
                                 folderName:String,
                                 drawableId: Int,
                                 notNull: KFunction2<Any?, String?, Unit>,
                                 isNull: KFunction2<Any?, String?, Unit>,
                                 areEqual: KFunction3<Any?, Any?, String?, Unit>
    ):Pair<BooksFragment,FragmentFoldersAndScriptsBinding>{
        var foldersAndScriptsFragment = foldersAndScriptsFragmentInput
        foldersAndScriptsFragment.binding.executePendingBindings()
        if (repository.getCurrentFragment().value != AccountableFragment.BooksFragment) return Pair(foldersAndScriptsFragment,foldersAndScriptsFragment.binding)
        if (foldersAndScriptsFragment.viewModel.showScripts.value==null) return Pair(foldersAndScriptsFragment,foldersAndScriptsFragment.binding)
        if (foldersAndScriptsFragment.viewModel.showScripts.value!!.value == true) return Pair(foldersAndScriptsFragment,foldersAndScriptsFragment.binding)

        // Switch to Edit Folder Fragment
        foldersAndScriptsFragment.binding.addFolderScriptButton.performClick()
        val editFolderFragment = activity.navController.currentFragmentClass as EditFolderFragment
        val editFolderViewModel = editFolderFragment.viewModel
        val editFolderBinding = editFolderFragment.binding
        notNull(editFolderFragment,"Saving Folder")
        editFolderBinding.executePendingBindings()

        editFolderBinding.folderName.setText("Random Text")
        editFolderViewModel.setImage(getUriFromDrawable(R.drawable.ic_home_black_24dp))
        editFolderBinding.executePendingBindings()

        val newEditFolderId = editFolderViewModel.newEditFolder.value?.folderId
        val newEditFolderImage = editFolderViewModel.newEditFolder.value!!.imageResource

        editFolderBinding.updateFolderButton.performClick()

        foldersAndScriptsFragment = activity.navController.currentFragmentClass as BooksFragment
        notNull(foldersAndScriptsFragment,"Back To Books")

        foldersAndScriptsFragment.viewModel.viewModelScope.launch {
            val newEditFolder = repository.dao.getFolderNow(newEditFolderId)
            notNull(newEditFolder,"New Edit Folder")
            notNull(newEditFolderImage.getUriFromStorage(activity.applicationContext),
                "Image In Storage")
        }

        val foldersAndScriptsBinding = foldersAndScriptsFragment.binding

        notNull(foldersAndScriptsFragment,"Folders And Scripts Fragment Retrieval")
        foldersAndScriptsBinding.executePendingBindings()
        return Pair(foldersAndScriptsFragment,foldersAndScriptsBinding)
    }

    private fun getImageResourceFolderSize(imageResource: AppResources.ImageResource?):Int?{
        val imageUri = imageResource?.getUriFromStorage(activity.applicationContext)
        val parentFile = imageUri?.let { File(it.toFile().parent!!) }
        return parentFile?.listFiles()?.size
    }

    private fun switchToNavigationDrawer(
        fragment: AccountableFragment,
        notNull:KFunction2<Any?,String?,Unit>,
        areEqual: KFunction3<Any?, Any?, String?, Unit>
    ){
        binding.navView.menu.performIdentifierAction(
            AccountableNavigationController.getFragmentId(fragment),
            GravityCompat.START
        )
        currentFragmentIs(fragment,notNull,areEqual)
    }

    private fun currentFragmentIs(
        fragment: AccountableFragment,
        notNull:KFunction2<Any?,String?,Unit>,
        areEqual: KFunction3<Any?, Any?, String?, Unit>
    ){
        notNull(viewModel.currentFragment.value,"viewModel.currentFragment.value")
        areEqual(
            fragment,
            viewModel.currentFragment.value,
            "Current Fragment"
        )
        notNull(activity.navController.currentFragmentClass,"Fragment Class Not Null")
        areEqual(
            AccountableNavigationController.getFragmentClassString(fragment),
            activity.navController.currentFragmentClass.javaClass.toString(),
            "Comparing Current Fragment"
        )
    }
*/
    object Log {
        private const val RED = "\u001B[31m"
        private const val GREEN = "\u001B[32m"
        private const val BLUE = "\u001B[34m"
        private const val RESET = "\u001B[0m"

        fun i(tag: String, msg: String): Int {
            println("${BLUE}$tag:${RESET} $msg")
            return 0
        }

        fun w(tag: String, msg: String): Int {
            println("${GREEN}$tag:${RESET} $msg")
            return 0
        }

        fun e(tag: String, msg: String): Int {
            println("${RED}$tag:${RESET} $msg")
            return 0
        }
    }
/*
    private fun getUriFromDrawable(drawableId: Int): Uri {
        // Get the drawable resource
        val drawable = ResourcesCompat.getDrawable(activity.applicationContext.resources,drawableId, null)
            ?: return Uri.EMPTY
        // Create a file in the cache directory
        val file = File(activity.applicationContext.cacheDir, "${activity.applicationContext.resources.getResourceEntryName(drawableId)}.png")
        file.createNewFile()

        // Convert the drawable to a bitmap and save it to the file
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)


        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
        outputStream.close()

        // Get the URI using FileProvider
        return Uri.fromFile(file)
    }
    */
}