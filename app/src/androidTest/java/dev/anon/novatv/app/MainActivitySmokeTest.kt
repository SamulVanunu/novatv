package dev.anon.novatv.app

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.anon.novatv.app.common.NovaApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appStartsOnLiveOrLastChannel() {
        rule.waitUntil(timeoutMillis = 8_000) {
            exists("group-rail") || exists("player-root")
        }
    }

    @Test
    fun sourcesScreenOpensFromLive() {
        returnToLive()

        rule.onNodeWithTag("open-playlists").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(timeoutMillis = 5_000) { exists("add-source") }
    }

    private fun returnToLive() {
        rule.waitUntil(timeoutMillis = 8_000) { exists("group-rail") || exists("player-root") }
        if (exists("player-root")) {
            rule.onNodeWithTag("player-root").performKeyInput { pressKey(Key.Back) }
            rule.waitUntil(timeoutMillis = 5_000) { exists("group-rail") }
        }
    }

    private fun exists(tag: String): Boolean =
        rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
}
