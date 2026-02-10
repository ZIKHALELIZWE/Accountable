package com.thando.accountable.input_forms

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.performScrollTo
import com.thando.accountable.AccountableComposeRobolectricTest
import kotlinx.coroutines.test.runTest
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TimeBlockTest(
    val addTimeBlockButtonTag: String,
    parentParameters:Triple<
            InstantTaskExecutorRule,
            ComposeContentTestRule,
            TestMainActivity
    >?=null
): AccountableComposeRobolectricTest(
    parentParameters
) {
    @Test
    fun `01 Add Time Button Exists`() = runTest {
        withTag(addTimeBlockButtonTag){
            assertExists()
            performScrollTo()
            assertIsDisplayed()
            assertHasClickAction()
        }
    }
}