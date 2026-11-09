package dev.anon.novatv.core.model

/**
 * Ebeveyn kilidi kuralları.
 *
 * Sınır değeri yaş olarak saklanır ("7", "13", "18"). Rehberdeki reyting
 * alanı çoğu kaynakta sayısal yaş taşır; sayısal olmayan reytingler sınır
 * varsa kilitli sayılır (temkinli taraf).
 */
object ParentalPolicy {

    val LIMITS: List<String> = listOf("7", "13", "18")

    fun limitValue(limit: String?): Int? = limit?.trim()?.toIntOrNull()

    fun isBlocked(rating: String?, limit: String?): Boolean {
        val cap = limitValue(limit) ?: return false
        val raw = rating?.trim().orEmpty()
        if (raw.isEmpty()) return false
        val value = raw.filter { it.isDigit() }.toIntOrNull() ?: return true
        return value > cap
    }

    /** Film/dizi puanı (0..10) sınırı aşıyor mu? Puan yoksa engellenmez. */
    fun isMovieBlocked(rating: Double?, limit: String?, kids: Boolean): Boolean {
        val cap = scoreCap(kids, limit) ?: return false
        val value = rating ?: return false
        return value > cap
    }

    /** Çocuk profili için kaba tür süzgeci; tür listesi boşsa içerik görünür. */
    fun isKidsGenreAllowed(genres: List<String>): Boolean {
        if (genres.isEmpty()) return true
        val normalized = genres.map { SearchText.normalize(it) }
        return normalized.any { genre ->
            KIDS_GENRES.any { keyword -> genre.contains(keyword) }
        }
    }

    /** Film/dizi puanı (0..10) yaş sınırına çevrilir; çocuk profilinde kullanılır. */
    fun scoreCap(kids: Boolean, limit: String?): Double? = when {
        kids -> 7.5
        else -> when (limitValue(limit)) {
            7 -> 6.5
            13 -> 8.0
            18 -> 9.5
            else -> null
        }
    }

    private val KIDS_GENRES = listOf("animation", "animasyon", "kids", "cocuk", "family", "aile")
}
