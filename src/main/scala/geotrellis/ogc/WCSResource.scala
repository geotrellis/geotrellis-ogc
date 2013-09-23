package geotrellis.services.ogc

import java.io.File

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response
import javax.ws.rs.{GET, POST, Consumes, Path, DefaultValue, QueryParam}
import javax.ws.rs.core.{Response, Context}

import com.typesafe.config.ConfigFactory

import scala.xml._

import geotrellis._
import geotrellis.data.MultiColorRangeChooser
import geotrellis.raster._
//import geotrellis.op.util._
import geotrellis.raster.op._
import geotrellis.process._
import geotrellis.rest.op._
import geotrellis.statistics.op._

/**
 * Implements basic WCS services.
 * 
 * Right now GetCapabilities and GetCoverage are supported.
 */
@Path("/wcs")
class WCSResource {

  /**
   * Given a layer name and a raster extent, return the operation to execute.
   * The operation should output a GeoTiff.
   * Override this method to include your geoprocessing operation.
   */ 
  def getOperation(name:String, reOp:Op[RasterExtent]) = {
    val nameOp = string.Concat(OGC.dataPath, name, ".arg")
    val layerOp = io.LoadFileWithRasterExtent(nameOp, reOp)
    val tiffOp = RenderGeoTiff8Bit(layerOp)
  }


  @GET def capabilities(
    @DefaultValue("") @QueryParam("LAYERS") layers:String,
    @DefaultValue("http://192.168.16.45:8888/wcs") @QueryParam("url") url:String,
    @DefaultValue("GetCapabilities") @QueryParam("REQUEST") mode:String,
    @DefaultValue("") @QueryParam("COVERAGE") coverage:String,
    @DefaultValue("") @QueryParam("coverage") _coverage:String,
    @DefaultValue("") @QueryParam("BBOX") bbox:String,
    @DefaultValue("") @QueryParam("WIDTH") width:String,
    @DefaultValue("") @QueryParam("HEIGHT") height:String,
    @DefaultValue("wcs") @QueryParam("DEST") dest:String,
    @Context req:HttpServletRequest
  ) = {
    println("GET %s?%s" format (url, req.getQueryString))
    mode match {
      case "GetCapabilities" => request("<GetCapabilities />", layers, url, req)
      case "DescribeCoverage" => {
        val names = if (coverage != "") List(coverage)
          else if (_coverage != "") List(_coverage)
          else OGC.getRasterNames

        describeCoverages(names)
      }
      case "GetCoverage" => {
        val reOp = extent.GetRasterExtent(string.ParseExtent(bbox),
                                          string.ParseInt(width),
                                          string.ParseInt(height))
        getCoverage(coverage, dest, reOp)
      }
      case s => sys.error("can't handle REQUEST=%s" format s)
    }
  }

  def splitPos(s:String):(Double, Double) = s.split(" ").map(_.toDouble) match {
    case Array(a, b) => (a, b)
    case _ => sys.error("couldn't parse pos: %s" format s)
  }


  def getCoverage(name:String, dest:String, reOp:Op[RasterExtent]) = {
    // build the operation to load/encode the raster
    val tiffOp = getOperation(name, reOp)

    // run it!
    OGC.server.getResult(tiffOp) match {
      case Complete(img, _) => Response.ok(img)
        .`type`("image/geotiff")
        .header("Content-Disposition", "attachment; filename=%s" format dest)
        .build()

      case Error(msg, trace) => Response.ok("error " + OGC.errorMsg(msg, trace))
        .`type`("text/plain")
        .build()
    }
  }

  def getCoverage1_0_0(xml:Elem) = {
    // layer stuff
    val name = (xml \ "sourceCoverage" text)

    // envelope stuff
    val env = (xml \ "domainSubset" \ "spatialSubset" \ "Envelope")
    val srs = (env \ "@srsName" text)
    val pts = (env \ "pos")
    val (xmin, ymin) = splitPos(pts(0).text)
    val (xmax, ymax) = splitPos(pts(1).text)

    // grid stuff
    val grid = (xml \ "domainSubset" \ "spatialSubset" \ "Grid")
    val (col0, row0) = splitPos(grid \ "limits" \ "GridEnvelope" \ "low" text)
    val (cols, rows) = splitPos(grid \ "limits" \ "GridEnvelope" \ "high" text)

    // output stuff
    val crs = (xml \ "output" \ "crs" text)
    val fmt = (xml \ "output" \ "format" text)

    // assertions
    if (srs != "EPSG:3857") sys.error("unsupported srs: %s" format srs)
    if (crs != "EPSG:3857") sys.error("unsupported crs: %s" format srs)
    if (col0 != 0.0 || row0 != 0.0) sys.error("unsupported grid origin: %s, %s" format (col0, row0))
    if (fmt != "GeoTIFF") sys.error("unsupported format: %s" format fmt)

    // debugging
    val s = "name=%s\nsrs=%s xmin=%s ymin=%s xmax=%s ymax=%s\ncols=%s rows=%s crs=%s format=%s\n"
    val diag = s format (name, srs, xmin, ymin, xmax, ymax, cols, rows, crs, fmt)
    //println(diag)

    // figure out what kind of resampling we need
    val dx = xmax - xmin
    val dy = ymax - ymin
    val cw = dx / cols
    val ch = dy / rows
    val re = RasterExtent(Extent(xmin, ymin, xmax, ymax), cw, ch, cols.toInt, rows.toInt)

    getCoverage(name, name, re)
  }

  def describeCoverage1_0_0(xml:Elem) = {
    println("xml was %s" format xml)
    describeCoverage(xml \ "Coverage" text)
  }

  def describeCoverage(coverage:String) = {
    val xml = buildDescription(List(buildOffering(coverage)))
    Response.ok(xml.toString).`type`("application/xml").build()
  }

  def describeCoverages(coverages:Seq[String]) = {
    val xml = buildDescription(coverages.map(s => buildOffering(s)))
    Response.ok(xml.toString).`type`("application/xml").build()
  }

  def buildOffering(coverage:String) = {
    val layer = OGC.getRasterLayer(coverage)
    val re = layer.rasterExtent
    val e = re.extent

    val name = WCSGetCapabilities.getTitle(layer)
    val label = layer.name

 <CoverageOffering>
  <name>{name}</name>
  <label>{label}</label>
  <lonLatEnvelope srsName="EPSG:3857">
   <gml:pos dimension="2">{e.xmin} {e.ymin}</gml:pos>
   <gml:pos dimension="2">{e.xmax} {e.ymax}</gml:pos>
  </lonLatEnvelope>
  <domainSet>
   <spatialDomain>
    <gml:Envelope srsName="EPSG:3857">
     <gml:pos dimension="2">{e.xmin} {e.ymin}</gml:pos>
     <gml:pos dimension="2">{e.xmax} {e.ymax}</gml:pos>
    </gml:Envelope>
    <gml:RectifiedGrid dimension="2">
     <gml:limits>
      <gml:GridEnvelope>
       <gml:low>0 0</gml:low>
       <gml:high>{re.cols - 1} {re.rows - 1}</gml:high>
      </gml:GridEnvelope>
     </gml:limits>
     <gml:axisName>x</gml:axisName>
     <gml:axisName>y</gml:axisName>
     <gml:origin>
      <gml:pos>{e.xmin} {e.ymax}</gml:pos>
     </gml:origin>
     <gml:offsetVector>{re.cellwidth} 0</gml:offsetVector>
     <gml:offsetVector>0 -{re.cellheight}</gml:offsetVector>
    </gml:RectifiedGrid>
   </spatialDomain>
  </domainSet>
  <rangeSet>
   <RangeSet>
    <name>Bands</name>
    <label>Data Band</label>
    <axisDescription>
     <AxisDescription>
      <name>Bands</name>
      <label>Band numbers</label>
      <values>
       <singleValue>1</singleValue>
      </values>
     </AxisDescription>
    </axisDescription>
   </RangeSet>
  </rangeSet>
  <supportedCRSs>
   <requestResponseCRSs>EPSG:3857</requestResponseCRSs>
   <nativeCRSs>EPSG:3857</nativeCRSs>
  </supportedCRSs>
  <supportedFormats nativeFormat="GeoTIFF">
   <formats>GeoTIFF</formats>
  </supportedFormats>
  <supportedInterpolations default="nearest neighbor">
   <interpolationMethod>nearest neighbor</interpolationMethod>
  </supportedInterpolations>
 </CoverageOffering>

  }

  def buildDescription(covs:Seq[Node]) = {
    val Elem(prefix, label, attrs, scope, child @ _*) = 
<CoverageDescription
   version="1.0.0" 
   xmlns="http://www.opengis.net/wcs" 
   xmlns:xlink="http://www.w3.org/1999/xlink" 
   xmlns:gml="http://www.opengis.net/gml" 
   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xsi:schemaLocation="http://www.opengis.net/wcs http://schemas.opengis.net/wcs/1.0.0/describeCoverage.xsd">
</CoverageDescription>

    Elem(prefix, label, attrs, scope, covs:_*)
  }

  @POST def request(
    data:String,
    @DefaultValue("") @QueryParam("LAYERS") layers:String,
    @DefaultValue("http://192.168.16.45:8888/wcs") @QueryParam("url") url:String,
    @Context req:HttpServletRequest
  ) = {
    println("POST %s?%s" format (url, req.getQueryString))
    val xml = XML.loadString(data)

    xml.label match {
      case "GetCoverage" => (xml \ "@version" text) match {
        case "1.0" | "1.0.0" => getCoverage1_0_0(xml)
        case v => sys.error("bad version: %s" format v)
      }
      case "DescribeCoverage" => (xml \ "@version" text) match {
        case "1.0" | "1.0.0" => describeCoverage1_0_0(xml)
        case v => sys.error("bad version: %s" format v)
      }

      case "GetCapabilities" => {
        val rasterLayers = if (layers != "")
          List(OGC.getRasterLayer(layers))
        else
          OGC.getRasterLayers

        val xml = WCSGetCapabilities.document(rasterLayers, url)
        Response.ok(xml.toString).`type`("text/xml").build()
      }

      case s => sys.error("can't handle %s" format s)
    }
  }
}
