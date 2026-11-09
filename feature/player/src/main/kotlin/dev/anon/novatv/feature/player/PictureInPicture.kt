package dev.anon.novatv.feature.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Oynatıcının PiP durumu. `MainActivity.onUserLeaveHint` (API 26–30) ve
 * `onPictureInPictureModeChanged` buradan beslenir; ekran da buradan okur.
 */
object PlayerPip {
    /** Oynatıcı ekranı görünür ve hata durumunda değil. */
    @Volatile
    var active: Boolean = false

    /** Oynatma sürüyor (ya da tamponlanıyor); PiP'e otomatik geçiş buna bağlı. */
    @Volatile
    var playing: Boolean = false

    private val _inPip = MutableStateFlow(false)
    val inPip: StateFlow<Boolean> = _inPip

    fun setInPip(value: Boolean) {
        _inPip.value = value
    }
}

/**
 * API 31+ otomatik PiP: kullanıcı HOME'a bastığında sistem oynatıcıyı küçültür.
 * API 26–30'da giriş `onUserLeaveHint` üzerinden yapılır.
 */
fun Activity.updatePictureInPictureParams(playing: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return
    setPictureInPictureParams(
        PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setAutoEnterEnabled(playing)
            .build(),
    )
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** API 26+ ve PiP destekli cihazlarda oynatıcıyı küçük pencereye alır. */
fun Activity.enterPlayerPictureInPicture(aspect: Rational = Rational(16, 9)) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    ) {
        enterPip(aspect)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun Activity.enterPip(aspect: Rational) {
    enterPictureInPictureMode(
        PictureInPictureParams.Builder()
            .setAspectRatio(aspect)
            .build(),
    )
}
