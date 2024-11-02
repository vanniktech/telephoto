@file:Suppress("NOTHING_TO_INLINE")

package me.saket.telephoto.subsamplingimage.internal

import android.graphics.Matrix
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import me.saket.telephoto.subsamplingimage.internal.ExifMetadata.ImageOrientation
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.math.roundToInt

/**
 * Calculate the position of this rectangle inside [unRotatedParent]
 * after its parent is rotated clockwise by [degrees].
 */
internal fun IntRect.rotateBy(degrees: Int, unRotatedParent: IntRect): IntRect {
  if (degrees == 0) {
    return this
  }

  // There is probably a better (and simpler) way to find the rectangle after
  // rotation, but I'm brute forcing my way through this by manually mapping points.
  val newTopLeft = when (degrees) {
    -270, 90 -> {
      val offsetFromBottomLeft = unRotatedParent.bottomLeft - bottomLeft
      IntOffset(
        x = offsetFromBottomLeft.flip().x,
        y = -offsetFromBottomLeft.flip().y,
      )
    }

    -180, 180 -> {
      unRotatedParent.bottomRight - bottomRight
    }

    -90, 270 -> {
      val offsetFromTopRight = unRotatedParent.topRight - topRight
      IntOffset(
        x = -offsetFromTopRight.flip().x,
        y = offsetFromTopRight.flip().y,
      )
    }

    0, 360 -> topLeft
    else -> error("unsupported orientation = $degrees")
  }

  return IntRect(
    offset = newTopLeft,
    size = when (degrees) {
      -270, 90 -> size.flip()
      -180, 180 -> size
      -90, 270 -> size.flip()
      0, 360 -> size
      else -> error("unsupported orientation = $degrees")
    },
  )
}

// The same instance is shared by all calls to createRotationMatrix()
// because it's always called on the same (main) thread.
private val matrix by lazy(NONE) { Matrix() }

/**
 * Creates a [Matrix] that can be used for drawing this tile's rotated bitmap such that
 * it appears straight on the canvas.
 *
 * Code adapted from [subsampling-scale-image-view](https://github.com/davemorrissey/subsampling-scale-image-view).
 */
internal inline fun createRotationMatrix(
  bitmapSize: Size,
  orientation: ImageOrientation,
  bounds: Size,
): Matrix {
  matrix.reset()

  // Calculate a scale to fill the bounds. For rotated orientations (90, 270),
  // the width/height will be swapped since the bitmap will be rotated.
  val rotationDegrees = when (orientation) {
    ImageOrientation.None -> 0f
    ImageOrientation.Orientation90 -> 90f
    ImageOrientation.Orientation180 -> 180f
    ImageOrientation.Orientation270 -> 270f
  }

  // Calculate scale to fill bounds completely (based on rotated dimensions).
  // This scale happens from (0,0). This ensures a uniform scaling before any
  // translations that could affect the scale ratios.
  val rotatedSize = if (rotationDegrees % 180 == 0f) bitmapSize else bitmapSize.flip()
  val scaleX = bounds.width / rotatedSize.width
  val scaleY = bounds.height / rotatedSize.height
  matrix.postScale(scaleX, scaleY)

  // Calculate points for rotation around the image's center.
  val bitmapCenterX = (bitmapSize.width * scaleX / 2f).round()
  val bitmapCenterY = (bitmapSize.height * scaleY / 2f).round()

  // Post* matrix operations happen in reverse order, so reading bottom to top:
  // 1. Translate to final bounds center
  // 2. Rotate by required degrees
  // 3. Translate to origin for rotation around center
  // (The scale operation was already done first, above)

  matrix.postTranslate(-bitmapCenterX, -bitmapCenterY)  // 3.
  matrix.postRotate(rotationDegrees)  // 2.

  val left = 0f
  val top = 0f
  matrix.postTranslate( // 1.
    (left + bounds.width / 2f).round(),
    (top + bounds.height / 2f).round()
  )

  return matrix
}

/**
 * Used for matching the precise calculations of Bitmap.createBitmap()
 * when it's used for creating rotated bitmaps.
 */
private inline fun Float.round(): Float {
  return roundToInt().toFloat()
}

private fun IntOffset.flip(): IntOffset = IntOffset(x = y, y = x)
private fun IntSize.flip(): IntSize = IntSize(width = height, height = width)
private fun Size.flip(): Size = Size(width = height, height = width)
