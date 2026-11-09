package dev.anon.novatv.core.player

import android.net.Uri
import androidx.media3.common.MediaItem

/** Kullanıcının seçtiği harici altyazı dosyası (SRT/VTT/SSA). */
data class SubtitleSpec(
    val uri: Uri,
    val mimeType: String,
    val language: String = "und",
)

/** Medya öğesi: akış adresi + varsa yan yüklenen altyazı. */
fun mediaItemFor(url: String, subtitle: SubtitleSpec?): MediaItem =
    MediaItem.Builder()
        .setUri(url)
        .apply {
            subtitle?.let { spec ->
                setSubtitleConfigurations(
                    listOf(
                        MediaItem.SubtitleConfiguration.Builder(spec.uri)
                            .setMimeType(spec.mimeType)
                            .setLanguage(spec.language)
                            .build(),
                    ),
                )
            }
        }
        .build()
