package dev.anon.novatv.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appNameIsVisible() {
        rule.onNodeWithText("NovaTV").assertIsDisplayed()
    }

    @Test
    fun playerNavigationWorks() {
        rule.onNodeWithText("Oynatıcıyı aç").assertIsDisplayed()
    }
}
