@file:Suppress("DataClassPrivateConstructor", "OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")

package me.saket.telephoto.zoomable.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import me.saket.telephoto.zoomable.ContentOffset
import me.saket.telephoto.zoomable.ContentZoomFactor
import me.saket.telephoto.zoomable.GestureState
import me.saket.telephoto.zoomable.GestureStateInputs
import me.saket.telephoto.zoomable.UserOffset
import me.saket.telephoto.zoomable.UserZoomFactor

@AndroidParcelize
internal data class ZoomableSavedState private constructor(
  private val userOffset: Long,
  private val userZoom: Float,
  private val centroid: Long,
  private val stateRestorerInfo: StateRestorerInfo?,
) : AndroidParcelable {

  @AndroidParcelize
  data class StateRestorerInfo(
    val viewportSize: Long,
    val contentOffsetAtViewportCenter: Long,  // Present in the content's coordinate space.
    val finalZoomFactor: Long,
  ) : AndroidParcelable

  companion object {
    fun from(
      gestureState: GestureState,
      gestureStateInputs: GestureStateInputs,
    ) = ZoomableSavedState(
      userOffset = gestureState.userOffset.value.packToLong(),
      userZoom = gestureState.userZoom.value,
      centroid = gestureState.lastCentroid.packToLong(),
      stateRestorerInfo = gestureStateInputs.contentLayoutSize
        .takeIf { it.isSpecifiedAndNonEmpty }
        ?.let { viewportSize ->
          StateRestorerInfo(
            viewportSize = viewportSize.packToLong(),
            contentOffsetAtViewportCenter = GestureStateRestorer.calculateContentOffsetAtViewportCenter(
              gestureStateInputs = gestureStateInputs,
              savedGestureState = gestureState,
              viewportSize = viewportSize,
            ).packToLong(),
            finalZoomFactor = ContentZoomFactor(
              baseZoom = gestureStateInputs.baseZoom,
              userZoom = gestureState.userZoom,
            ).finalZoom().packToLong(),
          )
        },
    )
  }

  fun asGestureState(
    inputs: GestureStateInputs,
    coerceWithinBounds: (ContentOffset, ContentZoomFactor) -> ContentOffset,
  ): GestureState {
    if (stateRestorerInfo == null || stateRestorerInfo.viewportSize.unpackAsSize() == inputs.contentLayoutSize) {
      return GestureState(
        userOffset = UserOffset(userOffset.unpackAsOffset()),
        userZoom = UserZoomFactor(userZoom),
        lastCentroid = centroid.unpackAsOffset(),
      )
    }

    // If the viewport size changes after state restoration (likely due to orientation change or
    // window resize), the content's _visual_ anchor needs to be restored to its original position.
    // Treat the content offset at the viewport's center as the anchor and adjust the gesture state
    // to maintain the anchor's position in the new viewport.
    val stateRestorer = GestureStateRestorer(
      oldFinalZoom = stateRestorerInfo.finalZoomFactor.unpackAsScaleFactor(),
      oldContentOffsetAtViewportCenter = stateRestorerInfo.contentOffsetAtViewportCenter.unpackAsOffset(),
    )
    return stateRestorer.calculateForNewViewportSize(inputs, coerceWithinBounds)
  }
}

private fun Offset.packToLong(): Long =
  packFloats(x, y)

private fun Size.packToLong(): Long =
  packFloats(width, height)

private fun ScaleFactor.packToLong(): Long =
  packFloats(scaleX, scaleY)

private fun Long.unpackAsOffset(): Offset =
  Offset(x = unpackFloat1(this), y = unpackFloat2(this))

private fun Long.unpackAsSize(): Size =
  Size(width = unpackFloat1(this), height = unpackFloat2(this))

private fun Long.unpackAsScaleFactor(): ScaleFactor =
  ScaleFactor(scaleX = unpackFloat1(this), scaleY = unpackFloat2(this))
