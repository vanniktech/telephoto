package me.saket.telephoto.zoomable.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import me.saket.telephoto.zoomable.GestureState
import me.saket.telephoto.zoomable.UserZoomFactor

@AndroidParcelize
@Suppress("DataClassPrivateConstructor")
internal data class ZoomableSavedState private constructor(
  private val offset: Long?,
  private val centroid: Long?,
  private val userZoom: Float?,
) : AndroidParcelable {

  constructor(gestureState: GestureState?) : this(
    offset = gestureState?.offset?.packToLong(),
    centroid = gestureState?.lastCentroid?.packToLong(),
    userZoom = gestureState?.userZoom?.value,
  )

  fun asGestureState(): GestureState? {
    return GestureState(
      offset = offset?.unpackAsOffset() ?: return null,
      lastCentroid = centroid?.unpackAsOffset() ?: return null,
      userZoom = UserZoomFactor(value = userZoom ?: return null),
    )
  }
}

private fun Offset.packToLong(): Long =
  packFloats(x, y)

private fun Long.unpackAsOffset(): Offset =
  Offset(x = unpackFloat1(this), y = unpackFloat2(this))
