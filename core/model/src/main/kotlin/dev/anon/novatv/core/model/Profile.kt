package dev.anon.novatv.core.model

data class Profile(
    val id: Long = 0,
    val name: String,
    val pinHash: String? = null,
    val maxRating: String? = null,
    val isKids: Boolean = false,
)
