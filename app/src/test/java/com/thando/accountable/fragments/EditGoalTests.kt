package com.thando.accountable.fragments

import androidx.compose.ui.test.junit4.createComposeRule
import com.thando.accountable.MainActivity
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Force Robolectric to use API 34
@LooperMode(LooperMode.Mode.LEGACY)
class EditGoalTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyTest() {
        val mainActivity = Robolectric.buildActivity(MainActivity::class.java)
            .setup()
            .get()

        composeTestRule.setContent {
            mainActivity
        }

        assertNotNull(mainActivity)
        assertNotNull(mainActivity.viewModel)
        //composeTestRule.onNodeWithText("Testing Compose").assertIsDisplayed()
    }
}