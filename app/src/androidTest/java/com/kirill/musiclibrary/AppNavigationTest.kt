package com.kirill.musiclibrary

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented navigation smoke tests. They make no assumptions about the catalogue contents,
 * so they pass whether the app was built from the real LIBRARY.html or example/LIBRARY.html.
 *
 * Require a device/emulator: `./gradlew :app:connectedDebugAndroidTest` (not part of pre-commit).
 */
@RunWith(AndroidJUnit4::class)
class AppNavigationTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeRendersAndListsArtists() {
        rule.onNodeWithText("Your offline catalogue").assertIsDisplayed()
        rule.onNodeWithText("Artists").performClick()
        // Any artist row renders an "N albums" label; content-independent.
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodes(hasText("albums", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun opensUsbDestination() {
        rule.onNodeWithText("On USB").performClick()
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("On USB").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun opensSettings() {
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.onNodeWithText("Dynamic colour").assertIsDisplayed()
    }
}
