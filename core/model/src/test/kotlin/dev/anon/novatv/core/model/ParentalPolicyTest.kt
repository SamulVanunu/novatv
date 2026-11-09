package dev.anon.novatv.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentalPolicyTest {

    @Test
    fun numericRatingsAboveTheLimitAreBlocked() {
        assertTrue(ParentalPolicy.isBlocked("18", "7"))
        assertTrue(ParentalPolicy.isBlocked("12+", "7"))
        assertFalse(ParentalPolicy.isBlocked("0", "7"))
        assertFalse(ParentalPolicy.isBlocked("7", "13"))
    }

    @Test
    fun missingRatingOrLimitNeverBlocks() {
        assertFalse(ParentalPolicy.isBlocked(null, "7"))
        assertFalse(ParentalPolicy.isBlocked("18", null))
        assertFalse(ParentalPolicy.isBlocked("", "7"))
        // Sınır "yok" ise her şey açık.
        assertFalse(ParentalPolicy.isBlocked("18", "Yok"))
    }

    @Test
    fun nonNumericRatingBlocksWhenLimitExists() {
        assertTrue(ParentalPolicy.isBlocked("PG-13", "7"))
        assertFalse(ParentalPolicy.isBlocked("PG-13", null))
    }

    @Test
    fun movieScoreUsesProfileCap() {
        assertTrue(ParentalPolicy.isMovieBlocked(8.4, "7", kids = false))
        assertFalse(ParentalPolicy.isMovieBlocked(6.1, "7", kids = false))
        assertTrue(ParentalPolicy.isMovieBlocked(8.0, null, kids = true))
        assertFalse(ParentalPolicy.isMovieBlocked(7.2, null, kids = true))
        assertFalse(ParentalPolicy.isMovieBlocked(null, "7", kids = false))
        assertFalse(ParentalPolicy.isMovieBlocked(9.9, null, kids = false))
    }

    @Test
    fun kidsGenreFilterAllowsFamilyTitles() {
        assertTrue(ParentalPolicy.isKidsGenreAllowed(listOf("Animasyon")))
        assertTrue(ParentalPolicy.isKidsGenreAllowed(listOf("Çocuk", "Macera")))
        assertTrue(ParentalPolicy.isKidsGenreAllowed(emptyList()))
        assertFalse(ParentalPolicy.isKidsGenreAllowed(listOf("Korku", "Gerilim")))
    }
}
