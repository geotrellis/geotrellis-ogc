package geotrellis.services.ogc

import geotrellis.process._

/**
 * Implements the GetCapabilities action for WCS.
 *
 * Given a sequence of layers, it will create an XML response listing them
 * along with their relevant metadata.
 */
object WCSGetCapabilities {
  def getTitle(layer:RasterLayer) = new java.io.File(layer.basePath).getName

  def layer(layer:RasterLayer) = {
    val re = layer.rasterExtent
    val e = re.extent

<wcs:CoverageOfferingBrief>
 <wcs:description>Generated from arcGridSample</wcs:description>
 <wcs:name>{getTitle(layer)}</wcs:name>
 <wcs:label>{layer.name}</wcs:label>
 <wcs:lonLatEnvelope srsName="urn:ogc:def:crs:EPSG::3857">
  <gml:pos>{e.xmin} {e.ymin}</gml:pos>
  <gml:pos>{e.xmax} {e.ymax}</gml:pos>
 </wcs:lonLatEnvelope>
 <wcs:keywords>
  <wcs:keyword>WCS</wcs:keyword>
 </wcs:keywords>
</wcs:CoverageOfferingBrief>

}

  def document(layers:Seq[RasterLayer], url:String) = {

<wcs:WCS_Capabilities xmlns:wcs="http://www.opengis.net/wcs" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:ogc="http://www.opengis.net/ogc" xmlns:ows="http://www.opengis.net/ows/1.1" xmlns:gml="http://www.opengis.net/gml" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="1.0.0" xsi:schemaLocation="http://www.opengis.net/wcs http://tr01.azavea.com:80/geoserver/schemas/wcs/1.0.0/wcsCapabilities.xsd" updateSequence="384">
 <wcs:Service>
  <wcs:metadataLink about="http://geoserver.sourceforge.net/html/index.php" metadataType="other"/>
  <wcs:description>This server implements the WCS specification 1.0.</wcs:description>
  <wcs:name>WCS</wcs:name>
  <wcs:label>Web Coverage Service</wcs:label>
  <wcs:keywords>
   <wcs:keyword>WCS</wcs:keyword>
  </wcs:keywords>
  <wcs:responsibleParty>
   <wcs:individualName>Claudius Ptolomaeus</wcs:individualName>
   <wcs:organisationName>The ancient geographes INC</wcs:organisationName>
   <wcs:positionName>Chief geographer</wcs:positionName>
   <wcs:contactInfo>
    <wcs:phone/>
    <wcs:address>
     <wcs:city>Alexandria</wcs:city>
     <wcs:country>Egypt</wcs:country>
     <wcs:electronicMailAddress>claudius.ptolomaeus@gmail.com</wcs:electronicMailAddress>
    </wcs:address>
   </wcs:contactInfo>
  </wcs:responsibleParty>
  <wcs:fees>NONE</wcs:fees>
  <wcs:accessConstraints>NONE</wcs:accessConstraints>
 </wcs:Service>
 <wcs:Capability>
  <wcs:Request>
   <wcs:GetCapabilities>
    <wcs:DCPType>
     <wcs:HTTP>
      <wcs:Get>
       <wcs:OnlineResource xlink:href={url}/>
      </wcs:Get>
     </wcs:HTTP>
    </wcs:DCPType>
    <wcs:DCPType>
     <wcs:HTTP>
      <wcs:Post>
       <wcs:OnlineResource xlink:href={url}/>
      </wcs:Post>
     </wcs:HTTP>
    </wcs:DCPType>
   </wcs:GetCapabilities>
   <wcs:DescribeCoverage>
    <wcs:DCPType>
     <wcs:HTTP>
      <wcs:Get>
       <wcs:OnlineResource xlink:href={url}/>
      </wcs:Get>
     </wcs:HTTP>
    </wcs:DCPType>
   <wcs:DCPType>
   <wcs:HTTP>
    <wcs:Post>
     <wcs:OnlineResource xlink:href={url}/>
      </wcs:Post>
     </wcs:HTTP>
    </wcs:DCPType>
   </wcs:DescribeCoverage>
   <wcs:GetCoverage>
    <wcs:DCPType>
     <wcs:HTTP>
      <wcs:Get>
       <wcs:OnlineResource xlink:href={url}/>
      </wcs:Get>
     </wcs:HTTP>
   </wcs:DCPType>
   <wcs:DCPType>
    <wcs:HTTP>
     <wcs:Post>
      <wcs:OnlineResource xlink:href={url}/>
       </wcs:Post>
      </wcs:HTTP>
     </wcs:DCPType>
    </wcs:GetCoverage>
   </wcs:Request>
  <wcs:Exception>
   <wcs:Format>application/vnd.ogc.se_xml</wcs:Format>
  </wcs:Exception>
 </wcs:Capability>
 <wcs:ContentMetadata>
{layers map layer}
 </wcs:ContentMetadata>
</wcs:WCS_Capabilities>

  }
}
