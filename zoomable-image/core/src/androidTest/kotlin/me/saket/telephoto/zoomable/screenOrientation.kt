package me.saket.telephoto.zoomable

import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.espresso.device.action.ScreenOrientation
import androidx.test.ext.junit.rules.ActivityScenarioRule

// TODO: replace with Espresso Device API once emulator.wtf adds support for it.
internal fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.setScreenOrientation(orientation: ScreenOrientation) {
  runOnUiThread {
    val currentOrientation = activity.resources.configuration.orientation
    val targetOrientation = when (orientation) {
      ScreenOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
      ScreenOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    if (currentOrientation != targetOrientation) {
      activity.requestedOrientation = targetOrientation
    }
  }
  this.waitForIdle()
}
