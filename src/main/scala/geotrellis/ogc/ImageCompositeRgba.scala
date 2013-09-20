package geotrellis.services.ogc

import geotrellis._
import geotrellis.raster._
import geotrellis.raster.op._
import geotrellis.process._

/**
 * Composite an array of images into a single image raster.
 *
 * If the value of the first raster is transparent, use the value of the second
 * raster in the output raster.
 */
case class ImageCompositeRgba(op:Op[Array[Raster]]) extends local.MultiLocalArray {
  final def handle(a:Int, b:Int) = if (a == 0) b else a
  final def handleDouble(a:Double, b:Double) = if (java.lang.Double.isNaN(a)) b else a
}
