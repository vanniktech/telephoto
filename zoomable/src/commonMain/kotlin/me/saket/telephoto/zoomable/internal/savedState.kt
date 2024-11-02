package me.saket.telephoto.zoomable.internal

import androidx.compose.ui.geometry.Offset
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
) : AndroidParcelable {

  constructor(gestureState: GestureState?) : this(
    userOffset = gestureState?.userOffset?.value?.packToLong(),
    userZoom = gestureState?.userZoom?.value,
    centroid = gestureState?.lastCentroid?.packToLong(),
  )

  fun asGestureState(): GestureState? {
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
