package me.saket.telephoto.zoomable

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import kotlinx.coroutines.flow.Flow
import me.saket.telephoto.zoomable.ZoomableImageSource.ResolveResult
import org.junit.Rule
import org.junit.Test

class ZoomableImagePreviewTest {
  @get:Rule val paparazzi = Paparazzi(
    deviceConfig = DeviceConfig.PIXEL_5
  )

  @Test fun `layout preview`() {
    paparazzi.snapshot {
      CompositionLocalProvider(LocalInspectionMode provides true) {
        ZoomableImage(
          modifier = Modifier.fillMaxSize(),
          image = remember {
            object : ZoomableImageSource {
              @Composable override fun resolve(canvasSize: Flow<Size>) =
                ResolveResult(
                  delegate = ZoomableImageSource.PainterDelegate(ColorPainter(Color.Yellow))
                )
            }
          },
          contentDescription = null,
        )
      }
    }
  }
}
