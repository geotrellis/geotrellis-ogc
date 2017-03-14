# DEPRECATED - no longer maintained, and will not work with current versions of GeoTrellis. See https://github.com/locationtech/geotrellis/issues/2069 for updates on OGC support in GeoTrellis.

## GeoTrellis OGC

This project provides libraries for creating WMS or WCS services with GeoTrellis and a simple example template service.

See [the core GeoTrellis documentation](http://geotrellis.github.io/) for information about using GeoTrellis.

This project provides JAX-RS classes that implement limited WMS and WCS services that can be used to provide the results of a geoprocessing operation.  Creating a new service involves subclassing the WMS or WCS class (which is a Jersey
resource) and overriding the getOperation method to return your own custom
operation (given the layer name and raster extent).

In the future, this functionality will be integrated directly into the GeoTrellis server project.


## USAGE

```scala
  @Path("/customWCS")
  class MyWCSService extends WCSResource {
    /**
     * Given a layer name and a raster extent, return the operation to execute.
     * The operation should output a GeoTiff.
     */
    override def getOperation(name:String, reOp:Op[RasterExtent]) = {
      val layerOp = io.LoadRaster(name, reOp)
      ## implement your geoprocessing operation here
      val tiffOp = RenderGeoTiff8Bit(resultOp)
    }


  @Path("customWMS")
  class MyWMSService extends WMSResource {
    /** Execute a geoprocessing operation given a layer name and a raster extent.
     *  Override this method to include your own operation.
     *  The final result should be an raster with RGBA color values.
     */ 
    def getOperation(name:String, reOp:Op[RasterExtent]):Op[Raster] = {
      val data = io.LoadRaster(name, reOp)
      ## implement your geoprocessing operation here
      io.SimpleRender(result)
    }
  }
```
