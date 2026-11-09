package dev.anon.novatv.app

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL

/**
 * Mock panel ile uçtan uca akış. Sunucu çalışmıyorsa atlanır:
 *
 *   node tools/mock-server/server.mjs &
 *   adb reverse tcp:8089 tcp:8089
 *   ./gradlew :app:connectedDebugAndroidTest
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class EndToEndTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun xtreamSourceSyncsPlaysAndRemembersLastChannel() {
        val baseUrl = reachableBaseUrl()
        assumeTrue("mock sunucuya emülatörden erişilemiyor", baseUrl != null)
        returnToLive()
        ensureXtreamSource(baseUrl!!)
        refreshSources()
        openLive()
        openFirstChannel()

        rule.waitUntil(timeoutMillis = 20_000) { exists("playback-error") || exists("player-root") }
        rule.waitForIdle()

        // OSD'yi aç ve kanal adını doğrula (gerçek D-pad tuşu).
        rule.onNodeWithTag("player-root").performKeyInput { pressKey(Key.Info) }
        rule.waitUntil(timeoutMillis = 5_000) { exists("osd-channel-name") }
        rule.onNodeWithText("TRT 1").assertExists()

        val application = rule.activity.application as NovaApplication
        var last: String? = null
        val deadline = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < deadline) {
            last = runBlocking { application.graph.channels.lastChannel() }?.name
            if (last != null) break
            Thread.sleep(250)
        }
        assertEquals("son kanal kaydedilmeli", "TRT 1", last)
    }

    private fun returnToLive() {
        rule.waitUntil(timeoutMillis = 10_000) { exists("group-rail") || exists("player-root") }
        if (exists("player-root")) {
            rule.onNodeWithTag("player-root").performKeyInput { pressKey(Key.Back) }
            rule.waitUntil(timeoutMillis = 5_000) { exists("group-rail") }
        }
    }

    private fun ensureXtreamSource(baseUrl: String) {
        rule.onNodeWithTag("open-playlists").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(timeoutMillis = 5_000) { exists("add-source") }

        if (exists("playlist-name")) {
            // Daha önceki koşudan kalan kaynak var; yenilemek yeterli.
            return
        }

        rule.onNodeWithTag("add-source").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(timeoutMillis = 5_000) { exists("field-name") }

        rule.onNodeWithTag("field-name").performTextInput("Mock Xtream")
        rule.onNodeWithTag("field-url").performTextInput(baseUrl)
        rule.onNodeWithTag("field-username").performTextInput("demo")
        rule.onNodeWithTag("field-password").performTextInput("demo")

        rule.onNodeWithTag("test-connection").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(timeoutMillis = 10_000) { exists("connection-status") }

        rule.onNodeWithTag("save-source").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(timeoutMillis = 10_000) { exists("playlist-name") }
    }

    private fun refreshSources() {
        val buttons = rule.onAllNodesWithText("Yenile").fetchSemanticsNodes()
        if (buttons.isNotEmpty()) {
            rule.onAllNodesWithText("Yenile")[0].performSemanticsAction(SemanticsActions.OnClick)
        }
    }

    private fun openLive() {
        rule.onNodeWithTag("back-to-live").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(timeoutMillis = 10_000) { exists("group-rail") }
    }

    private fun openFirstChannel() {
        rule.waitUntil(timeoutMillis = 20_000) { exists("channel-0") }
        rule.onNodeWithTag("channel-0").performSemanticsAction(SemanticsActions.OnClick)
        rule.waitUntil(timeoutMillis = 10_000) { exists("player-root") }
    }

    private fun exists(tag: String): Boolean =
        rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    /** Emülatörde host 10.0.2.2; `adb reverse` çalışan cihazda 127.0.0.1. */
    private fun reachableBaseUrl(): String? = BASE_URLS.firstOrNull(::serverReachable)

    private fun serverReachable(baseUrl: String): Boolean = runCatching {
        val connection = URL("$baseUrl/health").openConnection() as HttpURLConnection
        connection.connectTimeout = 2_000
        connection.readTimeout = 2_000
        connection.responseCode == 200
    }.getOrDefault(false)

    private companion object {
        val BASE_URLS = listOf("http://10.0.2.2:8089", "http://127.0.0.1:8089")
    }
}
