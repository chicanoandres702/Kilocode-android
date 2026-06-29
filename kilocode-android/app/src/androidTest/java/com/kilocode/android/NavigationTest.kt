package com.kilocode.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun app_launches_and_shows_repo_screen() {
        composeTestRule.onNodeWithText("Repositories").assertIsDisplayed()
    }

    @Test
    fun repoScreen_showsTabs() {
        composeTestRule.onNodeWithText("Your Repos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Search GitHub").assertIsDisplayed()
    }

    @Test
    fun repoScreen_showsCreateSection() {
        composeTestRule.onNodeWithText("Create a new repository").assertIsDisplayed()
    }

    @Test
    fun navigate_to_searchTab() {
        composeTestRule.onNodeWithText("Search GitHub").performClick()
        composeTestRule.onNodeWithText("Search GitHub repositories").assertIsDisplayed()
    }

    @Test
    fun repoScreen_showsNoReposMessage() {
        composeTestRule.onNodeWithText("No repositories yet").assertIsDisplayed()
    }

    @Test
    fun repoScreen_showsCloneButton() {
        composeTestRule.onNodeWithText("Clone").assertIsDisplayed()
    }

    @Test
    fun repoScreen_showsCreateButton() {
        composeTestRule.onNodeWithText("Create").assertIsDisplayed()
    }
}
