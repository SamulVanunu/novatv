package dev.anon.novatv.core.model

import java.util.Locale

object SearchText {

    fun normalize(input: String): String {
        val lowered = input.lowercase(Locale.ROOT)
        val mapped = buildString(lowered.length) {
            for (ch in lowered) {
                when (ch) {
                    '\u0307' -> Unit                 // İ'nin root-locale lowercase artığı
                    'ı', 'î' -> append('i')
                    'ş' -> append('s')
                    'ğ' -> append('g')
                    'ü', 'û' -> append('u')
                    'ö' -> append('o')
                    'ç' -> append('c')
                    'â' -> append('a')
                    else -> append(ch)
                }
            }
        }
        val cleaned = buildString(mapped.length) {
            for (ch in mapped) {
                append(if (ch.isLetterOrDigit()) ch else ' ')
            }
        }
        return cleaned.trim().replace(Regex("\\s+"), " ")
    }
}
