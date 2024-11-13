package me.saket.telephoto.zoomable

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import me.saket.telephoto.util.waitUntil
import kotlin.time.Duration.Companion.seconds

// Workaround for https://issuetracker.google.com/issues/271226817.
// Based on https://issuetracker.google.com/issues/271226817#comment5.
class ActivityRecreationTester<A : ComponentActivity>(
  private val testRule: AndroidComposeTestRule<ActivityScenarioRule<A>, A>
) {
  private lateinit var content: @Composable () -> Unit

  fun setContent(block: @Composable () -> Unit) {
    content = block
    testRule.setContent(block)
  }

  fun recreateWith(block: () -> Unit) {
    val activityBefore = testRule.activity
    block()

    try {
      testRule.waitUntil(2.seconds) { activityBefore != testRule.activity }
    } catch (e: ComposeTimeoutException) {
      throw RuntimeException("Timed out waiting for the Activity to be recreated", e)
    }

    val composeTest = testRule::class.java.getDeclaredField("composeTest").also {
      it.isAccessible = true
    }.get(testRule)
    composeTest::class.java.getDeclaredField("disposeContentHook").also {
      it.isAccessible = true
    }.set(composeTest, null)
    testRule.setContent(content)
  }
}
