@file:Suppress("NAME_SHADOWING")

package me.saket.telephoto.subsamplingimage

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEach
import me.saket.telephoto.subsamplingimage.internal.ViewportImageTile

/**
 * An Image composable that can render large bitmaps by diving them into tiles so that they
 * can be loaded lazily. This ensures that images maintain their intricate details even when
 * fully zoomed in, without causing any `OutOfMemory` exceptions.
 *
 * [SubSamplingImage] is automatically used by [ZoomableImage][me.saket.telephoto.zoomable.ZoomableImage].
 */
@Composable
fun SubSamplingImage(
  state: SubSamplingImageState,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  alpha: Float = DefaultAlpha,
  colorFilter: ColorFilter? = null,
) {
  check(state is RealSubSamplingImageState)

  Image(
    modifier = modifier.onSizeChanged {
      state.viewportSize = it
    },
    painter = remember(state) {
      SubSamplingImagePainter(state)
    },
    contentDescription = contentDescription,
    alpha = alpha,
    colorFilter = colorFilter,
    alignment = Alignment.TopStart,
    contentScale = ContentScale.FillBounds,
  )
}

@Composable
fun Image2(
  painter: Painter,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  alignment: Alignment = Alignment.Center,
  contentScale: ContentScale = ContentScale.Fit,
  alpha: Float = DefaultAlpha,
  colorFilter: ColorFilter? = null
) {
  val semantics = if (contentDescription != null) {
    Modifier.semantics {
      this.contentDescription = contentDescription
      this.role = Role.Image
    }
  } else {
    Modifier
  }

  // Explicitly use a simple Layout implementation here as Spacer squashes any non fixed
  // constraint with zero
  Layout(
    modifier
      .then(semantics)
      .clipToBounds()
      .paint(
        painter,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
        sizeToIntrinsics = false,
      )
  ) { _, constraints ->
    layout(constraints.minWidth, constraints.minHeight) {}
  }
}

private class SubSamplingImagePainter(val state: RealSubSamplingImageState) : Painter() {
  override val intrinsicSize: Size
    get() {
      return state.imageSize?.toSize() ?: state.imagePreviewSize ?: Size.Unspecified
    }

  private var alpha: Float = DefaultAlpha
  private var colorFilter: ColorFilter? = null

  override fun applyAlpha(alpha: Float): Boolean {
    this.alpha = alpha
    return true
  }

  override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
    this.colorFilter = colorFilter
    return true
  }

  override fun DrawScope.onDraw() {
    println("ondraw() -> size = $size")
    //state.viewportSize = size.discardFractionalParts()

    if (!state.isImageDisplayed) {
      return
    }

    state.viewportImageTiles.fastForEach { tile ->
      drawImageTile(
        tile = tile,
        alpha = alpha,
        colorFilter = colorFilter,
      )
      if (state.showTileBounds) {
        drawRect(
          color = Color.Yellow,
          topLeft = tile.bounds.topLeft.toOffset(),
          size = tile.bounds.size.toSize(),
          style = Stroke(width = 2.dp.toPx()),
        )
      }
    }
  }

  private fun DrawScope.drawImageTile(
    tile: ViewportImageTile,
    alpha: Float,
    colorFilter: ColorFilter?,
  ) {
    val painter = tile.painter ?: return
    withTransform(
      transformBlock = {
        translate(
          left = tile.bounds.topLeft.x.toFloat(),
          top = tile.bounds.topLeft.y.toFloat(),
        )
      },
      drawBlock = {
        with(painter) {
          draw(
            size = tile.bounds.size.toSize(),
            alpha = alpha,
            colorFilter = colorFilter,
          )
        }
      }
    )
  }
}

@SuppressLint("ComposeParameterOrder")
@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)  // For binary compatibility.
@Composable
fun SubSamplingImage(
  state: SubSamplingImageState,
  modifier: Modifier = Modifier,
  contentDescription: String?,
  alpha: Float = DefaultAlpha,
  colorFilter: ColorFilter? = null,
) {
  SubSamplingImage(
    state,
    contentDescription,
    modifier,
    alpha,
    colorFilter
  )
}
