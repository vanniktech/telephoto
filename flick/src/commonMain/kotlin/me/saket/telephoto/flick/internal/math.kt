package me.saket.telephoto.flick.internal

/**
 * Converts an angle measured in degrees to an approximately equivalent angle
 * measured in radians. The conversion from degrees to radians is generally inexact.
 *
 * Copied from `java.lang.Math.toRadians`.
 */
internal fun Float.toRadians(): Float {
  val degreesToRadians = 0.017453292519943295
  return (this * degreesToRadians).toFloat()
}
