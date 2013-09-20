package geotrellis.services.ogc

import java.io.File

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response
import javax.ws.rs.{GET, Path, DefaultValue, QueryParam, WebApplicationException}
import javax.ws.rs.core.{Response, Context}

import com.typesafe.config.ConfigFactory

import geotrellis._
import geotrellis.data.MultiColorRangeChooser
import geotrellis.raster.op._
import geotrellis.process._
import geotrellis.rest.op._
import geotrellis.statistics.op._


@Path("/wms")
class WMS {
  // Execute a geoprocessing operation given a layer name and a raster extent.
  // Override this method to include your own operation.
  //
  // The final result should be an raster with RGBA color values.
  def getOperation(name:String, reOp:Op[RasterExtent]):Op[Raster] = {
    io.SimpleRender(io.LoadRaster(name, reOp))
  }

  @GET
  def get(

    // WMS request parameters
    @DefaultValue("1")
    @QueryParam("VERSION")
    version:String,

    @DefaultValue("GetCapabilities")
    @QueryParam("REQUEST")
    request:String,

    @DefaultValue("")
    @QueryParam("STYLES")
    styles:String,

    @DefaultValue("-8379782.57151,4846436.32082,-8360582.57151,4865636.32082")
    @QueryParam("BBOX")
    bbox:String,

    @DefaultValue("256")
    @QueryParam("WIDTH")
    cols:String,

    @DefaultValue("256")
    @QueryParam("HEIGHT")
    rows:String,

    @DefaultValue("")
    @QueryParam("LAYERS")
    layers:String,

    @DefaultValue("info")
    @QueryParam("FORMAT")
    format:String,

    @DefaultValue("true")
    @QueryParam("TRANSPARENT")
    transparent:String,

    @DefaultValue("")
    @QueryParam("SRS")
    srs:String,

    @DefaultValue("")
    @QueryParam("BGCOLOR")
    bgcolor:String,

    // Custom parameters
    @DefaultValue("ff0000,ffff00,00ff00,0000ff")
    @QueryParam("palette")
    palettex:String,

    @DefaultValue("http://192.168.16.41:8888/wms")
    @QueryParam("url")
    url:String,

    @Context req:HttpServletRequest
  ) = {

    val query = req.getQueryString()
    println(query)

    // make sure raster layer exists
    if (layers != "") {
      val f = new java.io.File(OGC.dataPath + layers + ".json")
      if (! f.exists ) {
        throw new WebApplicationException(404);
      }
    }

    if (request != "GetMap") {
      val rasterLayers:Seq[RasterLayer] = if (layers != "") {
        List(OGC.getRasterLayer(layers))
      } else { 
        new File(OGC.dataPath)
          .listFiles
          .filter(_.getName.endsWith(".json"))
          .map { 
            f => RasterLayer.fromPath(f.getPath) 
          }
      }
      val xml = WMSGetCapabilities.document(rasterLayers, url)
      Response.ok(xml.toString).`type`("text/xml").build()
    } else {
      // Create operations for WMS operation.  Note that we do not run these operations
      // until server.run is called.

      /**
       * First, let's figure out what geographical area we're interested in, as
       * well as the resolution we want to use.
       */
      val colsOp = string.ParseInt(cols)
      val rowsOp = string.ParseInt(rows)
      val extentOp = string.ParseExtent(bbox)
      val reOp = extent.GetRasterExtent(extentOp, colsOp, rowsOp)

      // Create a collection of layer rasters, which store the rendered RGBA color 
      // values for each layer. 
      val layerOps:Op[Array[Raster]] = logic.ForEach(string.SplitOnComma(layers)) (getOperation(_, reOp))

      // Composite the images of each layer, creating a single layer.
      val colorRaster = ImageCompositeRgba(layerOps)

      /**
       * Render the acutal PNG image.
       */
      val pngOp = io.RenderPngRgba(colorRaster)

      if (format == "hello") {
        Response.ok("hello world").`type`("text/plain").build()
      } else if (format == "info") {
        OGC.server.getResult(pngOp) match {
          case Complete(img, h) => {
            val ms = h.elapsedTime
            val query = req.getQueryString
            val url = "/wms?" + query.replaceAll("format=info", "format=png")
            val tree = h.toPretty

            val html = WMS.infoPage(cols.toInt, rows.toInt, ms, url, tree)
            Response.ok(html).`type`("text/html").build()
          }

          case Error(msg, trace) => {
            val output = "There has been a system error.\n\ntrace:\n%s".format(trace)
            Response.serverError().entity(output).`type`("text/plain").build()
          }
        }
      } else {
        OGC.server.getResult(pngOp) match {
          case Complete(img, _) => Response.ok(img).`type`("image/png").build()
        
          case Error(msg, trace) => {
            val output = "There has been a system error.\n\nTrace message:\n%s".format(trace)
            Response.serverError().entity(output).`type`("text/plain").build()
          }
        }
      }
    }
  }
}


object WMS {

  // Information page for debugging.
  def infoPage(cols:Int, rows:Int, ms:Long, url:String, tree:String) = """
<html>
<head>
 <script type="text/javascript">
 </script>
</head>
<body>
 <h2>WMS service template</h2>

 <h3>rendered %dx%d image (%d pixels) in %d ms</h3>

 <table>
  <tr>
   <td style="vertical-align:top"><img style="vertical-align:top" src="%s" /></td>
   <td><pre>%s</pre></td>
  </tr>
 </table>

</body>
</html>
""" format(cols, rows, cols * rows, ms, url, tree)
}

