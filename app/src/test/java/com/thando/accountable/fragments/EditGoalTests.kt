package com.thando.accountable.fragments

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thando.accountable.AccountableNavigationController.AccountableFragment
import com.thando.accountable.MainActivity
import com.thando.accountable.MainActivityTest.FilteredPrintStream
import com.thando.accountable.fragments.viewmodels.BooksViewModel
import com.thando.accountable.fragments.viewmodels.EditGoalViewModel
import com.thando.accountable.fragments.viewmodels.HomeViewModel
import com.thando.accountable.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLog

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Force Robolectric to use API 34
@LooperMode(LooperMode.Mode.LEGACY)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class EditGoalTests {
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

    private fun getActivity(): MainActivity {
        return Robolectric.buildActivity(MainActivity::class.java)
            .setup().get()
    }

    @Test
    fun `1 coreClassesNotNull`() = runTest {
        val activity = getActivity()
        composeTestRule.waitForIdle()
        assertNotNull(activity)
        assertNotNull(activity.viewModel)
        assertNotNull(activity.viewModel.repository)
    }

    @Test
    fun `2 currentFragmentIsHomeFragment`() = runTest {
        val activity = getActivity()
        composeTestRule.waitForIdle()
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

    fun checkFragmentIs(
        activity: MainActivity,
        expectedFragment: AccountableFragment,
        expectedViewModel: String
    ) {
        composeTestRule.waitForIdle()
        assertEquals(
            expectedFragment,
            activity.viewModel.currentFragment.value
        )
        assertEquals(
            expectedViewModel,
            activity.viewModel.accountableNavigationController.fragmentViewModel.value?.javaClass?.name
        )
    }

    @Test
    fun `3 Can Go To Goals`() = runTest {
        val activity = getActivity()
        composeTestRule.waitForIdle()
        assertEquals(
            HomeViewModel::class.java.name,
            activity.viewModel.accountableNavigationController.fragmentViewModel.value?.javaClass?.name
        )
        assertEquals(
            AccountableFragment.HomeFragment,
            activity.viewModel.currentFragment.value
        )
        with(composeTestRule.onNodeWithTag("HomeFragmentLoadGoalsButton")){
            assertExists()
            assertHasClickAction()
            assertIsDisplayed()
            performClick()
        }
        advanceUntilIdle()
        checkFragmentIs(
            activity, AccountableFragment.GoalsFragment,
            BooksViewModel::class.java.name
        )
    }

    @Test
    fun `4 Is In AppSettings Base, Set To Goals And Is Empty`() = runTest {
        val activity = getActivity()
        composeTestRule.waitForIdle()
        assertEquals(
            AccountableFragment.GoalsFragment,
            activity.viewModel.currentFragment.value
        )
        assertEquals(
            BooksViewModel::class.java.name,
            activity.viewModel.accountableNavigationController.fragmentViewModel.value?.javaClass?.name
        )
        val booksViewModel: BooksViewModel = activity.viewModel.accountableNavigationController.fragmentViewModel.value!! as BooksViewModel

        assertNull(booksViewModel.folder.value)
        assertNotNull(booksViewModel.appSettings.value)
        assertNotNull(booksViewModel.showScripts.value?.value)
        assertFalse(booksViewModel.showScripts.value!!.value)
        with(composeTestRule.onNodeWithTag("BooksSwitchFolderScriptButton")) {
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            performClick()
        }
        advanceUntilIdle()
        composeTestRule.waitForIdle()
        assertTrue(booksViewModel.showScripts.value!!.value)
        assertNotNull(booksViewModel.goalsList.value)
        assertNull(booksViewModel.foldersList.value)
        assertNull(booksViewModel.scriptsList.value)

        assertEquals(0,booksViewModel.goalsList.value!!.first().size)
    }

    @Test
    fun `5 Open Edit Goal And Check Views`() = runTest {
        val activity = getActivity()
        composeTestRule.waitForIdle()

        with(composeTestRule.onNodeWithTag("BooksFloatingActionButton")){
            assertExists()
            assertIsDisplayed()
            assertHasClickAction()
            performClick()
        }
        advanceUntilIdle()
        checkFragmentIs(
            activity,
            AccountableFragment.EditGoalFragment,
            EditGoalViewModel::class.java.name
        )

        val editGoalViewModel: EditGoalViewModel =
            activity.viewModel.accountableNavigationController.fragmentViewModel.value!! as EditGoalViewModel
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        val titleTextCompose = composeTestRule.onNodeWithTag("EditGoalTitle")
        titleTextCompose.apply {
            assertExists()
            assertIsDisplayed()
            assertTextEquals(activity.getString(R.string.new_goal))
        }
    }

    @Test
    fun `6 Input Goal`() = runTest {
        val activity = getActivity()
        composeTestRule.waitForIdle()

        val editGoalViewModel: EditGoalViewModel =
            activity.viewModel.accountableNavigationController.fragmentViewModel.value!! as EditGoalViewModel
        assertNotNull(editGoalViewModel)
        assertNull(editGoalViewModel.editGoal.value)

        composeTestRule.onNodeWithTag("EditGoalGoal").apply {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(activity.getString(R.string.goal))
            assertTextContains("")
            performTextInput("My New Goal")
            assertTextContains("My New Goal")
        }

        composeTestRule.onNodeWithTag("EditGoalLocation").apply {
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertTextContains(activity.getString(R.string.location))
            assertTextContains("")
            performTextInput("My New Location")
            assertTextContains("My New Location")
        }
    }
}