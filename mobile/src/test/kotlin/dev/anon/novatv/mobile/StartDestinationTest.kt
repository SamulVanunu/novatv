package dev.anon.novatv.mobile

import dev.anon.novatv.app.common.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
