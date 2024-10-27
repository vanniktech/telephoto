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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import me.saket.telephoto.subsamplingimage.internal.BitmapCache
import me.saket.telephoto.subsamplingimage.internal.ImageSampleSize
import me.saket.telephoto.subsamplingimage.internal.ImageRegionDecoder
import me.saket.telephoto.subsamplingimage.internal.ImageRegionTile
import me.saket.telephoto.subsamplingimage.internal.ImageRegionTileGrid
import me.saket.telephoto.subsamplingimage.internal.RotatedBitmapPainter
import me.saket.telephoto.subsamplingimage.internal.ViewportImageTile
import me.saket.telephoto.subsamplingimage.internal.ViewportTile
import me.saket.telephoto.subsamplingimage.internal.calculateFor
import me.saket.telephoto.subsamplingimage.internal.contains
import me.saket.telephoto.subsamplingimage.internal.fastFilter
import me.saket.telephoto.subsamplingimage.internal.fastMapNotNull
import me.saket.telephoto.subsamplingimage.internal.generate
import me.saket.telephoto.subsamplingimage.internal.isNotEmpty
import me.saket.telephoto.subsamplingimage.internal.maxScale
import me.saket.telephoto.subsamplingimage.internal.overlaps
import me.saket.telephoto.subsamplingimage.internal.scaledAndOffsetBy
import me.saket.telephoto.zoomable.ZoomableContentTransformation

/** State for [SubSamplingImage]. */
@Stable
internal class RealSubSamplingImageState(
  private val imageSource: SubSamplingImageSource,
  private val contentTransformation: () -> ZoomableContentTransformation,
) : SubSamplingImageState {

  override val imageSize: IntSize?
    get() = imageRegionDecoder?.imageSize

  /**
   * Whether the image is loaded and displayed (not necessarily in its full quality).
   *
   * Also see [isImageLoadedInFullQuality].
   */
  override val isImageLoaded: Boolean by derivedStateOf {
    isReadyToBeDisplayed && viewportImageTiles.isNotEmpty() &&
      (viewportImageTiles.fastAny { it.tile.isBase } || viewportImageTiles.fastAll { it.painter != null })
  }

  /** Whether the image is loaded and displayed in its full quality. */
  override val isImageLoadedInFullQuality: Boolean by derivedStateOf {
    isImageLoaded && viewportImageTiles.fastAll { it.painter != null }
  }

  internal var imageRegionDecoder: ImageRegionDecoder? by mutableStateOf(null)
  internal var canvasSize: IntSize? by mutableStateOf(null)
  internal var showTileBounds = false  // Only used by tests.

  private val isReadyToBeDisplayed: Boolean by derivedStateOf {
    val canvasSize = canvasSize
    val imageSize = imageRegionDecoder?.imageSize
    canvasSize?.isNotEmpty() == true && imageSize?.isNotEmpty() == true
  }

  // Note to self: This is not inlined in viewportTiles to
  // avoid creating a new grid on every transformation change.
  private val tileGrid by derivedStateOf {
    if (isReadyToBeDisplayed) {
      ImageRegionTileGrid.generate(
        canvasSize = canvasSize!!,  // todo: rename both to viewportSize.
        unscaledImageSize = imageSize!!,
      )
    } else null
  }

  private val viewportTiles: ImmutableList<ViewportTile> by derivedStateOf {
    val tileGrid = tileGrid
    if (tileGrid == null) {
      persistentListOf()
    } else {
      // todo:
      // Fill any missing gaps in tiles by drawing the low-res base tile underneath as
      // a fallback. The base tile will hide again when all bitmaps have been loaded.
      val canDrawBaseTile = true //foregroundRegions.isEmpty() || foregroundRegions.fastAny { it !in loadedBitmaps }

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
            isVisible = if (isBaseTile) canDrawBaseTile else drawBounds.overlaps(canvasSize!!),
            isBase = isBaseTile,
          )
        }
        .toImmutableList()
    }
  }

  private var tileImages: ImmutableMap<ImageRegionTile, ImageBitmap> by mutableStateOf(persistentMapOf())

  internal val viewportImageTiles: ImmutableList<ViewportImageTile> by derivedStateOf {
    viewportTiles
      .fastFilter { it.isVisible }
      .fastMapNotNull { tile ->
        val image = tileImages[tile.region] ?: if (tile.isBase) imageSource.preview else null
        ViewportImageTile(
          tile = tile,
          painter = image?.let(::RotatedBitmapPainter),
        )
      }
      .toImmutableList()
  }

  @Composable
  fun LoadImageTilesEffect() {
    if (!isReadyToBeDisplayed) {
      return
    }

    val scope = rememberCoroutineScope()
    val bitmapCache = remember(this) {
      BitmapCache(scope, imageRegionDecoder!!)
    }

    LaunchedEffect(bitmapCache, viewportTiles) {
      bitmapCache.loadOrUnloadForTiles(
        regions = viewportTiles.fastMapNotNull { if (it.isVisible) it.region else null }
      )
    }
    LaunchedEffect(bitmapCache) {
      bitmapCache.observeCachedBitmaps().collect {
        tileImages = it
      }
    }
  }
}
