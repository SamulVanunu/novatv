package dev.anon.novatv.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHashTest {

    @Test
    fun pinVerifiesOnlyWithExactValue() {
        val stored = PinHash.create("1234")
        assertTrue(PinHash.verify("1234", stored))
        assertFalse(PinHash.verify("1235", stored))
        assertFalse(PinHash.verify("", stored))
    }

    @Test
    fun samePinGetsDifferentSaltEachTime() {
        val first = PinHash.create("0000")
        val second = PinHash.create("0000")
        assertNotEquals(first, second)
        assertTrue(PinHash.verify("0000", first))
        assertTrue(PinHash.verify("0000", second))
    }

    @Test
    fun missingPinMeansNoLock() {
        assertTrue(PinHash.verify("anything", null))
        assertTrue(PinHash.verify("anything", ""))
    }

    @Test
    fun malformedRecordIsRejected() {
        assertFalse(PinHash.verify("1234", "duz-metin"))
        assertFalse(PinHash.verify("1234", "v1\$abc"))
        assertFalse(PinHash.verify("1234", "v9\$YWJj\$ZGVm"))
    }
}
