package dev.anon.novatv.feature.player

import dev.anon.novatv.core.model.Channel

/**
 * Kanal zapping sırası: listede [delta] kadar ilerler, uçlarda başa döner.
 *
 * Kanal listesi çağıran taraftan gelir (görünen kanallar, numara/ad sırası);
 * burada yalnızca sıra aritmetiği var ki D-pad davranışı test edilebilsin.
 *
 * @return hedef kanal; liste boşsa, kanal listede yoksa, tek kanal varsa veya
 *   [delta] sıfırsa null (yeniden yükleme tetiklenmez).
 */
fun channelStep(channels: List<Channel>, currentId: Long, delta: Int): Channel? {
    if (delta == 0 || channels.size < 2) return null
    val index = channels.indexOfFirst { it.id == currentId }
    if (index < 0) return null
    val size = channels.size
    val target = ((index + delta) % size + size) % size
    return channels[target]
}
