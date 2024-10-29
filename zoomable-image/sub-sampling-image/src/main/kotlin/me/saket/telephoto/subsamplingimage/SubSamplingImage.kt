@file:Suppress("NAME_SHADOWING")

package me.saket.telephoto.subsamplingimage

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEach
import me.saket.telephoto.subsamplingimage.internal.ViewportImageTile
import me.saket.telephoto.subsamplingimage.internal.toCeilInt

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

  val onDraw: DrawScope.() -> Unit = {
    if (state.isImageDisplayed) {
      state.viewportImageTiles.fastForEach { tile ->
        drawImageTile(
          tile = tile,
          alpha = alpha,
          colorFilter = colorFilter,
        )

        if (state.showTileBounds) {
          drawRect(
            color = Color.Black,
            topLeft = tile.bounds.topLeft.toOffset(),
            size = tile.bounds.size.toSize(),
            style = Stroke(width = 2.dp.toPx()),
          )
        }
      }
    }
  }

  Layout(
    modifier = modifier
      .contentDescription(contentDescription)
      .drawBehind(onDraw),
    measurePolicy = WrapContentSizeIfNeededPolicy(
      imageSize = { state.imageSize },
      onMeasured = { state.viewportSize = it },
    )
  )
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

@Stable
private fun Modifier.contentDescription(contentDescription: String?): Modifier {
  return if (contentDescription != null) {
    semantics {
      this.contentDescription = contentDescription
      this.role = Role.Image
    }
  } else {
    this
  }
}

@Immutable
private data class WrapContentSizeIfNeededPolicy(
  val imageSize: () -> IntSize?,
  val onMeasured: (IntSize) -> Unit,
) : MeasurePolicy {
  override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
    val imageSize = imageSize()
    val constraints = if (constraints.hasFixedWidth && constraints.hasFixedHeight) {
      constraints
    } else if (imageSize != null) {
      val imageAspectRatio = imageSize.width / imageSize.height.toFloat()
      val targetWidth = when {
        constraints.hasFixedWidth -> constraints.maxWidth
        constraints.hasBoundedWidth -> imageSize.width
        else -> minOf(constraints.maxWidth, imageSize.width)  // To handle Constraints.Infinity
      }
      val targetHeight = when {
        constraints.hasFixedHeight -> constraints.maxHeight
        constraints.hasBoundedHeight -> imageSize.height
        else -> minOf(constraints.maxHeight, imageSize.height) // To handle Constraints.Infinity
      }
      val heightBasedOnWidth = (targetWidth / imageAspectRatio).toCeilInt()
      if (heightBasedOnWidth <= targetHeight) {
        Constraints.fixed(width = targetWidth, height = heightBasedOnWidth)
      } else {
        val widthBasedOnHeight = (targetHeight * imageAspectRatio).toCeilInt()
        Constraints.fixed(width = widthBasedOnHeight, height = targetHeight)
      }
    } else {
      constraints
    }
    val layoutSize = IntSize(constraints.minWidth, constraints.minHeight)
    onMeasured(layoutSize)
    return layout(width = layoutSize.width, height = layoutSize.height) {}
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
