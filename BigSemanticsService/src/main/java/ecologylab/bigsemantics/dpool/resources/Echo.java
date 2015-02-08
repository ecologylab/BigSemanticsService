package ecologylab.bigsemantics.dpool.resources;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Test Jersey.
 * 
 * @author quyin
 */
@Path("/echo")
public class Echo
{

  @Context
  private HttpServletRequest request;

  @QueryParam("msg")
  private String             message;

  @QueryParam("delay")
  private int                delay;

  @Path("/get")
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public Response echoGet() throws InterruptedException
  {
    String ip = request.getRemoteAddr();
    if (message == null)
      message = "<EMPTY MESSAGE>";
    message += "\nCalling from " + ip;
    if (delay > 0)
    {
      Thread.sleep(delay);
    }
    return Response.ok().entity(message).build();
  }

  @Path("/post")
  @POST
  @Produces(MediaType.TEXT_PLAIN)
  public Response echoPost() throws InterruptedException
  {
    String ip = request.getRemoteAddr();
    if (message == null)
      message = "<EMPTY MESSAGE>";
    message += "\nCalling from " + ip;
    if (delay > 0)
    {
      Thread.sleep(delay);
    }
    return Response.ok().entity(message).build();
  }

}
