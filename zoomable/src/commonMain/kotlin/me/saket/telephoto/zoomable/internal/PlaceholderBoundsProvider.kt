package me.saket.telephoto.zoomable.internal

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import me.saket.telephoto.zoomable.RealZoomableState
import me.saket.telephoto.zoomable.ZoomableState
import me.saket.telephoto.zoomable.RelativeContentLocation
import me.saket.telephoto.zoomable.ZoomableContentLocation

/**
 * Used by [me.saket.telephoto.zoomable.ZoomableImage] to provide a fallback value for
 * [ZoomableState.transformedContentBounds] before the full quality image is loaded. This
 * ensures that the bounds aren't empty while a placeholder image is visible.
 */
internal data class PlaceholderBoundsProvider(val contentSize: Size) {
  var viewportSize: IntSize? by mutableStateOf(null)

  @Stable
  fun calculate(state: RealZoomableState): Rect? {
    val viewportSize = viewportSize ?: return null

    val locationProvider = if (contentSize.isSpecified) {
      RelativeContentLocation(
        size = contentSize,
        scale = state.contentScale,
        alignment = state.contentAlignment,
      )
    } else {
      ZoomableContentLocation.SameAsLayoutBounds
    }
    return locationProvider.location(
      layoutSize = viewportSize.toSize(),
      direction = state.layoutDirection,
    )
  }
}
