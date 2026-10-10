package com.kirill.musiclibrary

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented navigation smoke tests. Require a device/emulator
 * (`./gradlew :app:connectedDebugAndroidTest`) and are not part of pre-commit.
 */
@RunWith(AndroidJUnit4::class)
class AppNavigationTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeRendersAndNavigatesToArtists() {
        rule.onNodeWithText("Music Library").assertIsDisplayed()
        rule.onNodeWithText("Artists").performClick()
        rule.onNodeWithText("Agata Kristi").assertIsDisplayed()
    }

    @Test
    fun usbEmptyStateExplainsTheStickIsNotPopulated() {
        rule.onNodeWithText("On USB").performClick()
        rule.onNodeWithText("Nothing on the USB stick yet").assertIsDisplayed()
    }
}
