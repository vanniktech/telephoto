package me.saket.telephoto.zoomable.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import me.saket.telephoto.zoomable.GestureState
import me.saket.telephoto.zoomable.UserOffset
import me.saket.telephoto.zoomable.UserZoomFactor

@AndroidParcelize
@Suppress("DataClassPrivateConstructor")
internal data class ZoomableSavedState private constructor(
  private val userOffset: Long?,
  private val userZoom: Float?,
  private val centroid: Long?,
  private val packedContentLayoutSize: Long?,
) : AndroidParcelable {

  constructor(gestureState: GestureState?, contentLayoutSize: Size) : this(
    userOffset = gestureState?.userOffset?.value?.packToLong(),
    userZoom = gestureState?.userZoom?.value,
    centroid = gestureState?.lastCentroid?.packToLong(),
    packedContentLayoutSize = if (contentLayoutSize.isSpecified) contentLayoutSize.packToLong() else null,
  )

  fun asGestureState(expectedContentLayoutSize: Size): GestureState? {
    val contentLayoutSize = packedContentLayoutSize?.unpackAsSize() ?: return null
    if (expectedContentLayoutSize != contentLayoutSize) {
      return null
    }

    return GestureState(
      userOffset = UserOffset(userOffset?.unpackAsOffset() ?: return null),
      userZoom = UserZoomFactor(userZoom ?: return null),
      lastCentroid = centroid?.unpackAsOffset() ?: return null,
    )
  }
}

private fun Offset.packToLong(): Long =
  packFloats(x, y)

private fun Long.unpackAsOffset(): Offset =
  Offset(x = unpackFloat1(this), y = unpackFloat2(this))

private fun Size.packToLong(): Long =
  packFloats(width, height)

private fun Long.unpackAsSize(): Size =
  Size(width = unpackFloat1(this), height = unpackFloat2(this))
