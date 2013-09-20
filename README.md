# GeoTrellis OGC

This project provides libraries for creating WMS or WCS services with GeoTrellis and a simple example template service.

This project provides JAX-RS classes that implement limited WMS and WCS services that can be used to provide the results of a geoprocessing operation.  Creating a new service involves subclassing the WMS or WCS class (which is a Jersey
resource) and overriding the getOperation method to return your own custom
operation (given the layer name and raster extent).

In the future, this functionality will be integrated directly into the GeoTrellis server project.
