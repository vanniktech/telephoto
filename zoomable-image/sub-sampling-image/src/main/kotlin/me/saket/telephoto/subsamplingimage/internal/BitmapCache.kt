package me.saket.telephoto.subsamplingimage.internal

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.util.fastForEach
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import me.saket.telephoto.subsamplingimage.internal.BitmapCache.LoadingState.InFlight
import me.saket.telephoto.subsamplingimage.internal.BitmapCache.LoadingState.Loaded
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class BitmapCache(
  scope: CoroutineScope,
  private val decoder: ImageRegionDecoder,
  private val throttleEvery: Duration = 100.milliseconds,
) {
  private val visibleRegions = Channel<List<ImageRegionTile>>(capacity = 10)
  private val cachedBitmaps = MutableStateFlow(emptyMap<ImageRegionTile, LoadingState>())

  private sealed interface LoadingState {
    data class Loaded(val bitmap: ImageBitmap) : LoadingState
    data class InFlight(val job: Job) : LoadingState
  }

  init {
    scope.launch {
      visibleRegions.consumeAsFlow()
        .distinctUntilChanged()
        .throttleLatest(throttleEvery)  // In case the image is animating its zoom.
        .collect { tiles ->
          val tilesToLoad = tiles.fastFilter { it !in cachedBitmaps.value }
          tilesToLoad.fastForEach { tile ->
            // CoroutineStart.UNDISPATCHED is used to ensure that the coroutines are executed
            // in the same order they were launched. Otherwise, the tiles may load in a different
            // order than what was requested. SubSamplingImageTest#draw_tile_under_centroid_first()
            // test will also become flaky.
            launch(start = CoroutineStart.UNDISPATCHED) {
              cachedBitmaps.update {
                check(tile !in it)
                it + (tile to InFlight(currentCoroutineContext().job))
              }
              val bitmap = decoder.decodeRegion(tile)
              cachedBitmaps.update {
                it + (tile to Loaded(bitmap))
              }
            }
          }

          val tilesToUnload = cachedBitmaps.value.keys.filter { it !in tiles }
          tilesToUnload.fastForEach { region ->
            val inFlight = cachedBitmaps.value[region] as? InFlight
            inFlight?.job?.cancel()
          }
          cachedBitmaps.update { it - tilesToUnload.toSet() }
        }
    }
  }

  fun observeCachedBitmaps(): Flow<ImmutableMap<ImageRegionTile, ImageBitmap>> {
    return cachedBitmaps.map { map ->
      buildMap(capacity = map.size) {
        map.forEach { (region, state) ->
          if (state is Loaded) {
            put(region, state.bitmap)
          }
        }
      }.toImmutableMap()
    }.distinctUntilChanged()
  }

  fun loadOrUnloadForTiles(regions: List<ImageRegionTile>) {
    visibleRegions.trySend(regions)
  }

  // Copied from https://github.com/Kotlin/kotlinx.coroutines/issues/1446#issuecomment-1198103541.
  private fun <T> Flow<T>.throttleLatest(delay: Duration): Flow<T> {
    return conflate().transform {
      emit(it)
      delay(delay)
    }
  }
}
