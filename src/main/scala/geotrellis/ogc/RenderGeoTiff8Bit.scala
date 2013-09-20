package geotrellis.services.ogc

import geotrellis._
import geotrellis.data.geotiff._
import geotrellis.process._

/**
 * Render a raster to an 8-bit GeoTIFF.
 */
case class RenderGeoTiff8Bit(r:Op[Raster]) extends Op1(r)({
  r => Result(Encoder.writeBytes(r, OGC.geoTiffSettings))
})


/*
case class RenderGeoTiff(r:Op[Raster], compression:Compression) extends Op1(r) ({
  r => {
    val settings = r.data.getType match {
      case TypeBit | TypeByte => Settings(ByteSample, Signed, true, compression)
      case TypeShort => Settings(ShortSample, Signed, true, compression)
      case TypeInt => Settings(IntSample, Signed, true, compression)
      case TypeFloat => Settings(IntSample, Floating, true, compression)
      case TypeDouble => Settings(LongSample, Floating, true, compression)
    }
    Result(Encoder.writeBytes(r, settings))
  }
})*/
