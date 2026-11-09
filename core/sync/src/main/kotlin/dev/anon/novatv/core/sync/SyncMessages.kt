package dev.anon.novatv.core.sync

/**
 * Senkron hatasını kullanıcıya gösterilebilir metne çevirir.
 *
 * Adresler, kimlik bilgileri ve MAC gibi değerler mesajdan çıkarılır; kaynak
 * kartında yalnızca neden görünür. Boş sonuç `null` döner.
 */
fun sanitizeSyncMessage(raw: String?): String? {
    val text = raw?.takeIf { it.isNotBlank() } ?: return null
    val cleaned = text
        .replace(USER_INFO, "://")
        .replace(URL, "[adres]")
        .replace(SECRET_PARAM, "$1=***")
        .trim()
        .take(MAX_LENGTH)
    return cleaned.ifBlank { null }
}

private const val MAX_LENGTH = 160

private val USER_INFO = Regex("""://[^/@\s]+@""")
private val URL = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)
private val SECRET_PARAM = Regex("""(?i)\b(username|password|pass|token|mac)=([^&\s]+)""")
