package me.saket.telephoto.zoomable.internal

import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

internal actual fun CompositionLocalConsumerModifierNode.hapticFeedbackPerformer(): HapticFeedbackPerformer {
  // Documentation for all available feedback types can be found here:
  // https://developer.apple.com/design/human-interface-guidelines/playing-haptics#Impact
  val impactGenerator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
  return HapticFeedbackPerformer {
    impactGenerator.impactOccurred()
  }
}
