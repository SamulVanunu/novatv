package dev.anon.novatv.mobile

import dev.anon.novatv.ui.layout.NovaWindow
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Mobil kabuk `Expanded` sınıfına çıkmaz: TV gövdeleri tv-material'ın
 * `ClickableSurface`'ını kullanır ve yalnız kumanda girdisini işler. Telefon
 * yatayda 891 dp'ye çıktığında kütüphane posteri, canlı kanal satırı ve EPG
 * satırı dokunmayla açılmıyordu.
 */
class TouchWindowTest {

    @Test
    fun genisPencereEnFazlaOrtaSinifOlarakKalir() {
        assertEquals(NovaWindow.Medium, touchWindow(NovaWindow.Expanded))
    }

    @Test
    fun kompaktVeOrtaSiniflarAynenKalir() {
        assertEquals(NovaWindow.Compact, touchWindow(NovaWindow.Compact))
        assertEquals(NovaWindow.Medium, touchWindow(NovaWindow.Medium))
    }

    /**
     * Gerçek genişlikler tek kapıdan (`mobileWindow`) geçer. Telefon dikey
     * 411 dp, yatay 891 dp; tablet 1066 dp — hiçbiri televizyon düzenine
     * (`Expanded`) çıkmamalı.
     */
    @Test
    fun gercekGenisliklerTelevizyonDuzenineCikmaz() {
        assertEquals(NovaWindow.Compact, mobileWindow(411))
        assertEquals(NovaWindow.Medium, mobileWindow(600))
        assertEquals(NovaWindow.Medium, mobileWindow(891))
        assertEquals(NovaWindow.Medium, mobileWindow(1066))
    }
}
