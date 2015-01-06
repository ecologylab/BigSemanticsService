package ecologylab.bigsemantics.downloaderpool.services;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import ecologylab.bigsemantics.downloaderpool.DPoolUtils;

/**
 * Test Jersey.
 * 
 * @author quyin
 */
@Path("/echo")
public class Echo
{

  @Path("/get")
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public Response echoGet(@Context HttpServletRequest request,
                          @QueryParam("msg") String message,
                          @QueryParam("delay") int delay)
  {
    String ip = request.getRemoteAddr();
    if (message == null)
      message = "<EMPTY MESSAGE>";
    message += "\nCalling from " + ip;
    if (delay > 0)
    {
      DPoolUtils.sleep(delay);
    }
    return Response.ok().entity(message).build();
  }

  @Path("/post")
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  public Response echoPost(@Context HttpServletRequest request,
                           @FormParam("msg") String message,
                           @FormParam("delay") int delay)
  {
    String ip = request.getRemoteAddr();
    if (message == null)
      message = "<EMPTY MESSAGE>";
    message += "\nCalling from " + ip;
    if (delay > 0)
    {
      DPoolUtils.sleep(delay);
    }
    return Response.ok().entity(message).build();
  }

}
