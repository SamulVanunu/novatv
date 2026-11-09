package dev.anon.novatv.core.model

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Profil PIN'i düz metin saklanmaz: rastgele tuz + SHA-256.
 * Kayıt biçimi `v1$<tuz>$<özet>`; eski/bozuk kayıt doğrulanamaz.
 */
object PinHash {

    private const val VERSION = "v1"
    private const val SALT_BYTES = 16

    fun create(pin: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        return "$VERSION$" + Base64.getEncoder().encodeToString(salt) + "$" +
            Base64.getEncoder().encodeToString(digest(pin, salt))
    }

    fun verify(pin: String, stored: String?): Boolean {
        if (stored.isNullOrBlank()) return true
        val parts = stored.split('$')
        if (parts.size != 3 || parts[0] != VERSION) return false
        val salt = runCatching { Base64.getDecoder().decode(parts[1]) }.getOrNull() ?: return false
        val expected = runCatching { Base64.getDecoder().decode(parts[2]) }.getOrNull() ?: return false
        val actual = digest(pin, salt)
        return MessageDigest.isEqual(expected, actual)
    }

    fun isHashed(value: String?): Boolean =
        value.isNullOrBlank() || value.startsWith("$VERSION$")

    private fun digest(pin: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return digest.digest()
    }
}
