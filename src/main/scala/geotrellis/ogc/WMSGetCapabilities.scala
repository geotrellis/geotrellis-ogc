package geotrellis.services.ogc

import geotrellis.process._

/**
 * Implements the GetCapabilities action for WMS.
 *
 * Given a sequence of layers, it will create an XML response listing
 * them along with their relevant metadata.
 */
object WMSGetCapabilities {
  def layer(layer:RasterLayer) = 
      <Layer queryable="1">
        <Name>{ new java.io.File(layer.basePath).getName}</Name>
        <Title>{layer.name}</Title>
        <Abstract/>
        <KeywordList/>
        <CRS>EPSG:3857</CRS>

        <BoundingBox CRS="EPSG:3857" minx={layer.rasterExtent.extent.xmin.toString} miny={layer.rasterExtent.extent.ymin.toString} maxx={layer.rasterExtent.extent.xmax.toString} maxy={layer.rasterExtent.extent.ymax.toString}  />
<EX_GeographicBoundingBox>
<westBoundLongitude>{ metersToLatLng(layer.rasterExtent.extent.xmin,layer.rasterExtent.extent.ymin)._2 }</westBoundLongitude>
<eastBoundLongitude>{ metersToLatLng(layer.rasterExtent.extent.xmax,layer.rasterExtent.extent.ymin)._2 }</eastBoundLongitude>
<southBoundLatitude>{ metersToLatLng(layer.rasterExtent.extent.xmin,layer.rasterExtent.extent.ymin)._1 }</southBoundLatitude>
<northBoundLatitude>{ metersToLatLng(layer.rasterExtent.extent.xmax,layer.rasterExtent.extent.ymax)._1 }</northBoundLatitude>
</EX_GeographicBoundingBox>

      </Layer>

  def metersToLatLng(x:Double,y:Double):(Double,Double) = {
    val origin = 2 * math.Pi * 6378137 / 2.0
    val lng = (x / origin) * 180.0
    val lat1 = (y / origin) * 180.0
    val lat = 180 / math.Pi * (2 * math.atan( math.exp( lat1 * math.Pi / 180.0)) - math.Pi / 2.0)
    (lat, lng)
  }

  def document(layers:Seq[RasterLayer],url:String) = 
  <WMS_Capabilities version="1.3.0" updateSequence="280" xmlns="http://www.opengis.net/wms" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/wms http://tr01.azavea.com:80/geoserver/schemas/wms/1.3.0/capabilities_1_3_0.xsd">
  <Service>
    <Name>WMS</Name>
    <Title>GeoTrellis Web Map Service</Title>
    <Abstract>GeoTrellis Web Map Service</Abstract>
    <KeywordList>
      <Keyword>WMS</Keyword>
      <Keyword>GEOTRELLIS</Keyword>
    </KeywordList>
    <OnlineResource xlink:type="simple" xlink:href={url} />
    <ContactInformation>
      <ContactPersonPrimary>
        <ContactPerson></ContactPerson>
        <ContactOrganization></ContactOrganization>
      </ContactPersonPrimary>
      <ContactPosition></ContactPosition>
      <ContactAddress>
        <AddressType></AddressType>
        <Address/>
        <City></City>
        <StateOrProvince/>
        <PostCode/>
        <Country></Country>
      </ContactAddress>
      <ContactVoiceTelephone/>
      <ContactFacsimileTelephone/>
      <ContactElectronicMailAddress></ContactElectronicMailAddress>
    </ContactInformation>
    <Fees>NONE</Fees>
    <AccessConstraints>NONE</AccessConstraints>
  </Service>
  <Capability>
    <Request>
      <GetCapabilities>
        <Format>text/xml</Format>
        <DCPType>
          <HTTP>
            <Get>
              <OnlineResource xlink:type="simple" xlink:href={url} />
            </Get>
            <Post>
              <OnlineResource xlink:type="simple" xlink:href={url} />
            </Post>
          </HTTP>
        </DCPType>
      </GetCapabilities>
      <GetMap>
        <Format>image/png</Format>
        <DCPType>
          <HTTP>
            <Get>
              <OnlineResource xlink:type="simple" xlink:href={url} />
            </Get>
          </HTTP>
        </DCPType>
      </GetMap>
    </Request>
    <Exception>
      <Format>XML</Format>
      <Format>INIMAGE</Format>
      <Format>BLANK</Format>
    </Exception>

    <Layer>
      <Title>GeoTrellis Result Layer</Title>
      <Abstract>GeoTrellis Result Layer</Abstract>
      <CRS>EPSG:3857</CRS>
      <EX_GeographicBoundingBox>
        <westBoundLongitude>-180.0</westBoundLongitude>
        <eastBoundLongitude>180.0</eastBoundLongitude>
        <southBoundLatitude>-90.0</southBoundLatitude>
        <northBoundLatitude>90.0</northBoundLatitude>
      </EX_GeographicBoundingBox>
      {layers map layer}
    </Layer>
  </Capability>
</WMS_Capabilities>
}
