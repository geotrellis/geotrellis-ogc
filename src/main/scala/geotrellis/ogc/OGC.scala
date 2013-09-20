package geotrellis.services.ogc

import com.typesafe.config.ConfigFactory

import java.io.File

import geotrellis._
import geotrellis.process._
import geotrellis.data.geotiff._

/**
 * Server and common code for various OGC services.
 */
object OGC {
  val config = ConfigFactory.load()
  val dataPath = config.getString("ogc.path")
  val server = Server.empty("ogc")

  val geoTiffSettings = Settings(ByteSample, Floating, true, Lzw)

  println("Started OGC service with data path: %s" format dataPath)

  def argPath(name:String) = dataPath + name + ".arg"

  def getRasterLayer(name:String) = RasterLayer.fromPath(dataPath + name + ".json")

  def getRasterFiles:Seq[File] = new File(dataPath)
    .listFiles
    .filter(_.getName.endsWith(".json"))

  def getRasterNames:Seq[String] = getRasterFiles
    .map(_.getName)
    .map(s => s.substring(0, s.length - 5))

  def getRasterPaths:Seq[String] = getRasterFiles.map(_.getPath)

  def getRasterLayers:Seq[RasterLayer] = getRasterPaths
    .map(RasterLayer.fromPath(_))

  def errorMsg(msg:String, trace:Failure) = {
    "failed: %s\n\ntrace:\n%s" format (msg, trace)
  }
}
