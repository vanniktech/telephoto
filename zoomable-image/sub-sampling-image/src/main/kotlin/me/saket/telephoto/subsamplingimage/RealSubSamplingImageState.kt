package me.saket.telephoto.subsamplingimage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import me.saket.telephoto.subsamplingimage.internal.ImageCache
import me.saket.telephoto.subsamplingimage.internal.ImageRegionDecoder
import me.saket.telephoto.subsamplingimage.internal.ImageRegionTile
import me.saket.telephoto.subsamplingimage.internal.ImageRegionTileGrid
import me.saket.telephoto.subsamplingimage.internal.ImageSampleSize
import me.saket.telephoto.subsamplingimage.internal.ViewportImageTile
import me.saket.telephoto.subsamplingimage.internal.ViewportTile
import me.saket.telephoto.subsamplingimage.internal.calculateFor
import me.saket.telephoto.subsamplingimage.internal.contains
import me.saket.telephoto.subsamplingimage.internal.discardFractionalParts
import me.saket.telephoto.subsamplingimage.internal.fastMapNotNull
import me.saket.telephoto.subsamplingimage.internal.generate
import me.saket.telephoto.subsamplingimage.internal.isNotEmpty
import me.saket.telephoto.subsamplingimage.internal.maxScale
import me.saket.telephoto.subsamplingimage.internal.overlaps
import me.saket.telephoto.subsamplingimage.internal.scaledAndOffsetBy
import me.saket.telephoto.zoomable.ZoomableContentTransformation

/** State for [SubSamplingImage]. Created using [rememberSubSamplingImageState]. */
@Stable
internal class RealSubSamplingImageState(
  imageSource: SubSamplingImageSource,
  private val contentTransformation: () -> ZoomableContentTransformation,
) : SubSamplingImageState {

  override val imageSize: IntSize?
    get() = imageRegionDecoder?.imageSize

  // todo: it isn't great that the preview image remains in memory even after the full image is loaded.
  private val imagePreview: Painter? =
    imageSource.preview?.let(::BitmapPainter)

  internal val imagePreviewSize: IntSize?
    get() = imagePreview?.intrinsicSize?.discardFractionalParts()

  override val isImageDisplayed: Boolean by derivedStateOf {
    isReadyToBeDisplayed && viewportImageTiles.isNotEmpty() &&
      (viewportImageTiles.fastAny { it.tile.isBase } || viewportImageTiles.fastAll { it.painter != null })
  }

  override val isImageDisplayedInFullQuality: Boolean by derivedStateOf {
    isImageDisplayed && viewportImageTiles.fastAll { it.painter != null }
  }

  internal var imageRegionDecoder: ImageRegionDecoder? by mutableStateOf(null)
  internal var viewportSize: IntSize? by mutableStateOf(null)
  internal var showTileBounds = false  // Only used by tests.

  /**
   * Images collected from [ImageCache].
   *
   * Loaded images are kept in a separate state instead of being combined with viewport tiles
   * because images are collected asynchronously, whereas viewport tiles are updated synchronously
   * during the layout pass.
   *
   * This separation enables layout changes to be rendered immediately. In previous
   * versions, layout changes caused image flickering because tile updates were asynchronous
   * and lagged by one frame.
   */
  private var loadedImages: ImmutableMap<ImageRegionTile, Painter> by mutableStateOf(persistentMapOf())

  private val isReadyToBeDisplayed: Boolean by derivedStateOf {
    val viewportSize = viewportSize
    val imageSize = imageRegionDecoder?.imageSize
    viewportSize?.isNotEmpty() == true && imageSize?.isNotEmpty() == true
  }

  // Note to self: This is not inlined in viewportTiles to
  // avoid creating a new grid on every transformation change.
  private val tileGrid by derivedStateOf {
    if (isReadyToBeDisplayed) {
      ImageRegionTileGrid.generate(
        viewportSize = viewportSize!!,
        unscaledImageSize = imageSize!!,
      )
    } else null
  }

  private val viewportTiles: ImmutableList<ViewportTile> by derivedStateOf {
    val tileGrid = tileGrid ?: return@derivedStateOf persistentListOf()
    val transformation = contentTransformation()
    val baseSampleSize = tileGrid.base.sampleSize

    val currentSampleSize = ImageSampleSize
      .calculateFor(zoom = transformation.scale.maxScale)
      .coerceAtMost(baseSampleSize)

    val isBaseSampleSize = currentSampleSize == baseSampleSize
    val foregroundRegions = if (isBaseSampleSize) emptyList() else tileGrid.foreground[currentSampleSize]!!

    (listOf(tileGrid.base) + foregroundRegions)
      .sortedByDescending { it.bounds.contains(transformation.centroid) }
      .fastMapNotNull { region ->
        val isBaseTile = region == tileGrid.base
        val drawBounds = region.bounds.scaledAndOffsetBy(transformation.scale, transformation.offset)
        ViewportTile(
          region = region,
          bounds = drawBounds,
          isBase = isBaseTile,
          isVisible = drawBounds.overlaps(viewportSize!!),
        )
      }
      .toImmutableList()
  }

  internal val viewportImageTiles: ImmutableList<ViewportImageTile> by derivedStateOf {
    // Fill any missing gaps in tiles by drawing the low-res base tile underneath as
    // a fallback. The base tile will hide again when all bitmaps have been loaded.
    val hasNoForeground = viewportTiles.fastAll { it.isBase }
    val hasGapsInForeground = { viewportTiles.fastAny { !it.isBase && it.region !in loadedImages } }
    val canDrawBaseTile = hasNoForeground || hasGapsInForeground()

    viewportTiles.fastMapNotNull { tile ->
      if (tile.isVisible) {
        ViewportImageTile(
          tile = tile,
          painter = if (tile.isBase) {
            if (canDrawBaseTile) {
              loadedImages[tile.region] ?: imagePreview
            } else {
              null
            }
          } else {
            loadedImages[tile.region]
          }
        )
      } else null
    }.toImmutableList()
  }

  @Composable
  fun LoadImageTilesEffect() {
    if (!isReadyToBeDisplayed) {
      return
    }

    val scope = rememberCoroutineScope()
    val imageCache = remember(this) {
      ImageCache(scope, imageRegionDecoder!!)
    }

    LaunchedEffect(imageCache, viewportTiles) {
      imageCache.loadOrUnloadForTiles(
        regions = viewportTiles.fastMapNotNull { if (it.isVisible) it.region else null }
      )
    }
    LaunchedEffect(imageCache) {
      imageCache.observeCachedImages().collect {
        loadedImages = it
      }
    }
  }
}
