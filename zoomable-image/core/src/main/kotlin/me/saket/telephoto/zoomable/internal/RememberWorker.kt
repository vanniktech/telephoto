package me.saket.telephoto.zoomable.internal

import androidx.compose.runtime.RememberObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal abstract class RememberWorker : RememberObserver {
  private var scope: CoroutineScope? = null

  abstract suspend fun work()

  override fun onRemembered() {
    // todo: Dispatchers.Main may not be available? see https://github.com/coil-kt/coil/pull/2699/files#diff-5a05d039d66bb86d3c317f17ca1b0ccf5be11215461f2811beb3b07cfa9c945e
    scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    scope!!.launch { work() }
  }

  override fun onForgotten() {
    scope?.cancel()
  }

  override fun onAbandoned() {
    check(scope == null) {
      "onRemembered() shouldn't have been called as per RememberObserver's documentation"
    }
  }
}
