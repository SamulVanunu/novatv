package dev.anon.novatv.app

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import dev.anon.novatv.feature.player.PlayerPip
import dev.anon.novatv.feature.player.enterPlayerPictureInPicture
import dev.anon.novatv.ui.theme.NovaTheme

class MainActivity : ComponentActivity() {

    private val graph: NovaGraph
        get() = (application as NovaApplication).graph

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NovaTheme {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    NovaNavHost(graph)
                }
            }
        }
    }

    /**
     * API 26–30'da otomatik PiP yok: kullanıcı HOME'a bastığında oynatıcı
     * ekrandaysa küçük pencereye geç. API 31+ bunu kendisi yapar.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.O until Build.VERSION_CODES.S &&
            PlayerPip.active && PlayerPip.playing
        ) {
            enterPlayerPictureInPicture()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        PlayerPip.setInPip(isInPictureInPictureMode)
    }

    override fun onDestroy() {
        super.onDestroy()
        PlayerPip.setInPip(false)
    }
}
