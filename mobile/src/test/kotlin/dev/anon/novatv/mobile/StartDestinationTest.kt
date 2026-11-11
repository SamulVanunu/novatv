package dev.anon.novatv.mobile

import dev.anon.novatv.app.common.Routes
import dev.anon.novatv.core.model.Playlist
import dev.anon.novatv.core.model.PlaylistType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Test listesi; adres bilerek yer tutucu, gerçek kaynak taşımaz. */
private fun playlist(id: Long, active: Boolean) = Playlist(
    id = id,
    type = PlaylistType.M3U,
    name = "liste-$id",
    baseUrl = "http://example.invalid/liste-$id.m3u",
    isActive = active,
)

/**
 * Açılış hedefi kararı saf tutulur: navigasyon yığını, veritabanı ve akış
 * zamanlaması bu fonksiyonların dışında kalır, böylece kural tek yerde ve
 * test edilebilir olur.
 */
class StartDestinationTest {

    @Test
    fun kaynakYokkenCanliAciliyorVeYonlendirmeGerekli() {
        assertEquals(Routes.LIVE, startDestination(hasSources = false, lastChannelRoute = null))
        assertTrue(needsSourceOnboarding(hasSources = false))
    }

    @Test
    fun kaynakVeSonKanalVarsaSonKanalAciliyor() {
        val route = "player?channelId=7&catchupStart=-1&catchupMinutes=0"
        assertEquals(route, startDestination(hasSources = true, lastChannelRoute = route))
        assertFalse(needsSourceOnboarding(hasSources = true))
    }

    @Test
    fun kaynakVarSonKanalYoksaCanliAciliyor() {
        assertEquals(Routes.LIVE, startDestination(hasSources = true, lastChannelRoute = null))
        assertFalse(needsSourceOnboarding(hasSources = true))
    }

    @Test
    fun kaynakYokkenSonKanalRotuAcilmiyor() {
        // Kanal geçmişi silinmiş ya da etkin kaynaktan düşmüş olabilir; kaynak
        // yokken oynatıcıya girmek boş ekrana götürür.
        val route = "player?channelId=7&catchupStart=-1&catchupMinutes=0"
        assertEquals(Routes.LIVE, startDestination(hasSources = false, lastChannelRoute = route))
        assertTrue(needsSourceOnboarding(hasSources = false))
    }

    @Test
    fun bosListedeEtkinKaynakYok() {
        val count = activeSourceCount(emptyList())
        assertEquals(0, count)
        // Kaynak yokken yönlendirme gerekir.
        assertTrue(needsSourceOnboarding(count > 0))
    }

    @Test
    fun etkinOlmayanListelerSayilmaz() {
        val playlists = listOf(playlist(1, active = false), playlist(2, active = false))
        assertEquals(0, activeSourceCount(playlists))
    }

    @Test
    fun etkinListePasiflerArasindaBulunuyor() {
        val playlists = listOf(
            playlist(1, active = false),
            playlist(2, active = true),
            playlist(3, active = false),
        )
        val count = activeSourceCount(playlists)
        assertEquals(1, count)
        // Kaynak varken yönlendirme kalkar.
        assertFalse(needsSourceOnboarding(count > 0))
    }

    @Test
    fun birdenCokEtkinListeSayilir() {
        val playlists = listOf(
            playlist(1, active = true),
            playlist(2, active = true),
            playlist(3, active = false),
        )
        assertEquals(2, activeSourceCount(playlists))
    }
}
