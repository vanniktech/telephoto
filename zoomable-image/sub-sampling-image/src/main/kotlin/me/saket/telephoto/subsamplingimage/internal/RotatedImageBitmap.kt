package me.saket.telephoto.subsamplingimage.internal

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

@Immutable
internal data class RotatedImageBitmap(
  val delegate: ImageBitmap,
  val orientation: ExifMetadata.ImageOrientation,
) : ImageBitmap by delegate

@Immutable
internal data class RotatedBitmapPainter(
  private val image: ImageBitmap,
) : Painter() {
  private val orientation: ExifMetadata.ImageOrientation = when (image) {
    is RotatedImageBitmap -> image.orientation
    else -> ExifMetadata.ImageOrientation.None
  }

  override val intrinsicSize: Size
    get() = Size(image.width.toFloat(), image.height.toFloat())

  private val paint = Paint().also {
    it.isAntiAlias = true
  }

  override fun applyAlpha(alpha: Float): Boolean {
    this.paint.alpha = alpha
    return true
  }

  override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
    this.paint.colorFilter = colorFilter
    return true
  }

  override fun DrawScope.onDraw() {
    val rotationMatrix = createRotationMatrix(
      bitmapSize = intrinsicSize,
      orientation = orientation,
      bounds = size,
    )
    val actualImage = if (image is RotatedImageBitmap) image.delegate else image

    drawIntoCanvas {
      it.nativeCanvas.drawBitmap(
        /* bitmap = */ actualImage.asAndroidBitmap(),
        /* matrix = */ rotationMatrix,
        /* paint = */ paint.asFrameworkPaint(),
      )
    }
  }
}
