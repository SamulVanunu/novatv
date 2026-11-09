package dev.anon.novatv.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlsTest {

    @Test
    fun dropsQueryFragmentAndUserInfo() {
        assertEquals(
            "https://panel.example:8443/get.php",
            redactUrl("https://panel.example:8443/get.php?username=u&password=p#top"),
        )
        assertEquals(
            "http://panel.example/list.m3u",
            redactUrl("http://user:secret@panel.example/list.m3u?token=abc"),
        )
    }

    @Test
    fun keepsUrlWithoutSecretsUnchanged() {
        assertEquals("https://panel.example:7000/list.m3u", redactUrl("https://panel.example:7000/list.m3u"))
        assertEquals("http://10.0.2.2:8089/live.ts", redactUrl("http://10.0.2.2:8089/live.ts"))
    }

    @Test
    fun malformedInputFallsBackToTextBeforeQuery() {
        assertEquals("not a url", redactUrl("not a url?username=x"))
    }
}
