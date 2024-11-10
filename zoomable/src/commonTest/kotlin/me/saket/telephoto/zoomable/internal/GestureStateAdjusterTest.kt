package me.saket.telephoto.zoomable.internal

import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.unit.LayoutDirection
import assertk.assertThat
import assertk.assertions.isEqualTo
import me.saket.telephoto.zoomable.BaseZoomFactor
import me.saket.telephoto.zoomable.ContentZoomFactor
import me.saket.telephoto.zoomable.GestureStateInputs
import kotlin.test.Test

class GestureStateAdjusterTest {
  @Test fun `retain zoom level for a proportionate base zoom`() {
    // Proportional base zoom will be created for
    // content scales such as ContentScale.Fit.
    val savedFinalZoom = ScaleFactor(5.5f, 5.5f)

    val adjuster = GestureStateAdjuster(
      oldFinalZoom = savedFinalZoom,
      oldContentOffsetAtViewportCenter = Offset(800f, 800f),
    )
    val gestureStateInputs = GestureStateInputs(
      contentLayoutSize = Size(2400f, 1080f),
      baseZoom = BaseZoomFactor(ScaleFactor(0.8f, 0.8f)),
      unscaledContentBounds = Rect(00f, 0f, 1000f, 1328f),
      contentAlignment = Alignment.Center,
      layoutDirection = LayoutDirection.Ltr,
    )
    val restoredGestureState = adjuster.calculateForNewViewportSize(
      inputs = gestureStateInputs,
      coerceWithinBounds = { offset, _ -> offset },
    )

    val restoredZoom = ContentZoomFactor(
      baseZoom = gestureStateInputs.baseZoom,
      userZoom = restoredGestureState.userZoom,
    )
    assertThat(restoredZoom.finalZoom()).isEqualTo(savedFinalZoom)
  }

  @Test fun `retain zoom level for a disproportionate base zoom`() {
    // Disproportional base zoom will be created for
    // content scales such as ContentScale.FillBounds.
    val savedFinalZoom = ScaleFactor(3.3f, 5.5f)

    val adjuster = GestureStateAdjuster(
      oldFinalZoom = savedFinalZoom,
      oldContentOffsetAtViewportCenter = Offset(800f, 800f),
    )
    val newGestureStateInputs = GestureStateInputs(
      contentLayoutSize = Size(2400f, 1080f),
      baseZoom = BaseZoomFactor(ScaleFactor(2.4f, 0.8f)),
      unscaledContentBounds = Rect(00f, 0f, 10_000f, 13_280f),
      contentAlignment = Alignment.Center,
      layoutDirection = LayoutDirection.Ltr,
    )
    val restoredGestureState = adjuster.calculateForNewViewportSize(
      inputs = newGestureStateInputs,
      coerceWithinBounds = { offset, _ -> offset },
    )

    val restoredZoom = ContentZoomFactor(
      baseZoom = newGestureStateInputs.baseZoom,
      userZoom = restoredGestureState.userZoom,
    )
    assertThat(restoredZoom.finalZoom().maxScale).isEqualTo(savedFinalZoom.maxScale)
  }
}
