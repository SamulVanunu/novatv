package dev.anon.novatv.feature.player

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PictureInPictureTest {

    private fun activity(): Activity =
        Robolectric.buildActivity(Activity::class.java).setup().get()

    private fun Activity.setPipFeature(enabled: Boolean) {
        shadowOf(packageManager).setSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE, enabled)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    fun helperEntersPipWhenDeviceSupportsIt() {
        val activity = activity()
        activity.setPipFeature(true)

        activity.enterPlayerPictureInPicture()

        assertTrue("PiP'e girilmeliydi", activity.isInPictureInPictureMode)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O])
    fun helperStaysPutWithoutPipFeature() {
        val activity = activity()
        activity.setPipFeature(false)

        activity.enterPlayerPictureInPicture()

        assertFalse("PiP özelliği yokken pencere küçültülmemeli", activity.isInPictureInPictureMode)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun updateParamsWithoutPipFeatureDoesNotCrash() {
        val activity = activity()
        activity.setPipFeature(false)

        activity.updatePictureInPictureParams(playing = true)

        assertFalse(activity.isInPictureInPictureMode)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun updateParamsWithPipFeatureIsAccepted() {
        val activity = activity()
        activity.setPipFeature(true)

        // Otomatik giriş bayrağı sistemde; API 31+ yolunun çağrısı patlamamalı.
        activity.updatePictureInPictureParams(playing = true)

        assertFalse(activity.isInPictureInPictureMode)
    }

    @Test
    fun playerPipFlagsRoundTrip() {
        PlayerPip.active = true
        PlayerPip.playing = true
        PlayerPip.setInPip(true)

        assertTrue(PlayerPip.active)
        assertTrue(PlayerPip.playing)
        assertTrue(PlayerPip.inPip.value)

        PlayerPip.active = false
        PlayerPip.playing = false
        PlayerPip.setInPip(false)

        assertFalse(PlayerPip.active)
        assertFalse(PlayerPip.playing)
        assertFalse(PlayerPip.inPip.value)
    }
}
