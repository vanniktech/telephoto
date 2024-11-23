package me.saket.telephoto.zoomable.internal

internal enum class HostPlatform {
  Android,
  Desktop,
  iOS,
  ;

  companion object;
}

internal expect val HostPlatform.Companion.current: HostPlatform
