package me.saket.telephoto.zoomable.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.layout.ScaleFactor
import me.saket.telephoto.zoomable.ContentOffset
import me.saket.telephoto.zoomable.ContentZoomFactor
import me.saket.telephoto.zoomable.GestureState
import me.saket.telephoto.zoomable.GestureStateInputs
import me.saket.telephoto.zoomable.ZoomableContentTransformation
import me.saket.telephoto.zoomable.ZoomableState

/**
 * Used when [ZoomableState]'s saved gesture state cannot be restored due to viewport size changes.
 * Adjusts zoom and pan values to maintain the content's centroid position in the new viewport.
 */
internal class GestureStateAdjuster(
  private val oldFinalZoom: ScaleFactor,
  private val oldContentOffsetAtViewportCenter: Offset, // Present in the content's coordinate space.
) {

  fun adjustForNewViewportSize(
    inputs: GestureStateInputs,
    coerceWithinBounds: (ContentOffset, ContentZoomFactor) -> ContentOffset,
  ): GestureState {
    // Retain the same zoom level. This will change the user zoom level, but that's okay.
    // Switching from a smaller to a larger screen should display more content, not the same.
    val newZoom = ContentZoomFactor.forFinalZoom(inputs.baseZoom, finalZoom = oldFinalZoom)

    // Find the offset needed to move the old anchor (i.e., the content offset at the viewport
    // center) back to the viewport's center. The anchor is present in the content's coordinate
    // space so it will be be transformed to the viewport space for the scope of this calculation.
    val newUserOffset = oldContentOffsetAtViewportCenter.withZoom(newZoom.finalZoom()) { anchorInViewportSpace ->
      anchorInViewportSpace - inputs.viewportSize.center
    }
    val proposedContentOffset = ContentOffset.forFinalOffset(
      baseOffset = inputs.baseOffset,
      finalOffset = newUserOffset,
    )

    return GestureState(
      userOffset = coerceWithinBounds(proposedContentOffset, newZoom).userOffset,
      userZoom = newZoom.userZoom,
      lastCentroid = inputs.viewportSize.center
    )
  }

  companion object {
    /** Calculates the coordinate in the content that's present at the center of the viewport. */
    internal fun calculateContentOffsetAtViewportCenter(
      gestureStateInputs: GestureStateInputs,
      savedGestureState: GestureState,
      viewportSize: Size,
    ): Offset {
      val transformation = RealZoomableContentTransformation.calculateFrom(
        gestureStateInputs = gestureStateInputs,
        gestureState = savedGestureState,
      )
      // Find the viewport center and locate it in the content's coordinate space.
      return CoordinateSpaceConverter.viewportToContent(
        viewportPoint = viewportSize.center,
        transformation = transformation,
        unscaledContentBounds = gestureStateInputs.unscaledContentBounds,
      )
    }
  }
}

private object CoordinateSpaceConverter {
  /** Converts a point in viewport coordinates to content coordinates. */
  fun viewportToContent(
    viewportPoint: Offset,
    unscaledContentBounds: Rect,
    transformation: ZoomableContentTransformation,
  ): Offset {
    val scale = transformation.scale

    // The transformation applied to content to get to viewport coordinates is:
    // 1. Shift by -contentBounds.topLeft (to handle content not at 0,0)
    // 2. Scale by scale factor
    // 3. Shift by transformed content bounds
    //
    // So to go to content coordinates, this walks backwards:
    // 1. Shift back by -transformedBounds.topLeft
    // 2. Divide by scale
    // 3. Shift back by +contentBounds.topLeft
    val transformedContentBounds = unscaledContentBounds.scaledAndOffsetBy(scale, transformation.offset)
    return ((viewportPoint - transformedContentBounds.topLeft) / scale) + unscaledContentBounds.topLeft
  }
}
