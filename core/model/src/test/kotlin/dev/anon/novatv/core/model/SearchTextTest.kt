package dev.anon.novatv.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTextTest {

    @Test
    fun turkishUppercaseFolds() {
        assertEquals("istanbul", SearchText.normalize("İSTANBUL"))
        assertEquals("igdir", SearchText.normalize("IĞDIR"))
        assertEquals("izmir", SearchText.normalize("İZMİR"))
        assertEquals("samsun", SearchText.normalize("SAMSUN"))
    }

    @Test
    fun keepsDigitsAndCollapsesSpace() {
        assertEquals("trt 1 hd", SearchText.normalize("  TRT   1   HD "))
    }

    @Test
    fun stripsPunctuation() {
        assertEquals("bein sports 1", SearchText.normalize("beIN Sports-1!"))
    }
}
