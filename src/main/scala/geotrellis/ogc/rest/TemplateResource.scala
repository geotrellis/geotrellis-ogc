package geotrellis.ogc

import javax.ws.rs._

/**
 * Simple hello world rest service that responds to "/hello"
 */
@Path("/hello")
class TemplateResource {
  @GET
  def hello() = "<h2>Hello OGC</h2>"
}
