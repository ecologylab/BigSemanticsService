package ecologylab.bigsemantics.downloaderpool.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * View service log.
 * 
 * @author quyin
 */
@Path("/log")
public class LogRequest extends RequestHandlerForController
{

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public Response viewLog(@QueryParam("prefix") String prefix,
                          @QueryParam("from") String from,
                          @QueryParam("to") String to)
  {
    // TODO
    return null;
  }

}
