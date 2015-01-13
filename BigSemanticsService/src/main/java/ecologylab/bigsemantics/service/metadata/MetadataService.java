package ecologylab.bigsemantics.service.metadata;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.NDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.Utils;
import ecologylab.bigsemantics.metadata.Metadata;
import ecologylab.bigsemantics.service.SemanticsServiceErrorMessages;
import ecologylab.net.ParsedURL;
import ecologylab.serialization.SimplTypesScope;
import ecologylab.serialization.formatenums.StringFormat;

/**
 * Metadata related services.
 * 
 * @author ajit
 * @author quyin
 */
@Path("/")
public class MetadataService
{

  Logger                     logger = LoggerFactory.getLogger(MetadataService.class);

  @Context
  private HttpServletRequest request;

  @QueryParam("url")
  private String             docUrl;

  @QueryParam("callback")
  private String             callback;

  @QueryParam("reload")
  private boolean            reload;

  /**
   * The utility method that actually generate the response; used by getJsonp(), getJson(), and
   * getXml().
   * 
   * @param format
   * @return
   */
  Response getResponse(StringFormat format)
  {
    String clientIp = request.getRemoteAddr();
    String msg =
        String.format("Request from %s: %s, in %s, reload=%s", clientIp, docUrl, format, reload);
    byte[] fpBytes = Utils.fingerprintBytes("" + System.currentTimeMillis() + "|" + msg);
    String fp = Utils.base64urlEncode(fpBytes);
    NDC.push(String.format("[Task %s] ", fp));
    logger.info(msg);

    long requestTime = System.currentTimeMillis();

    Response resp = null;
    Status errorStatus = Status.INTERNAL_SERVER_ERROR;
    String errorMsg = "Unknown error.";
    ParsedURL docPurl = ParsedURL.getAbsolute(docUrl);
    if (docPurl != null)
    {
      MetadataServiceHelper helper = new MetadataServiceHelper();
      resp = helper.getMetadataResponse(clientIp, docPurl, format, reload);
    }
    else
    {
      errorStatus = Status.BAD_REQUEST;
      errorMsg = "Parameter 'url' must be a valid URL.";
    }

    if (resp == null)
    {
      resp = Response.status(errorStatus).entity(errorMsg).type(MediaType.TEXT_PLAIN).build();
    }

    logger.info("Response generated. Total time in BigSemantics: {}ms",
                System.currentTimeMillis() - requestTime);
    NDC.remove();

    // at this point of time resp cannot be null
    return resp;
  }

  @Path("/metadata.jsonp")
  @GET
  @Produces("application/javascript")
  public Response getJsonp()
  {
    Response resp = getResponse(StringFormat.JSON);
    int status = resp.getStatus();
    if (status >= 400) // client or server error
    {
      return resp;
    }
    String respEntity = callback + "(" + ((String) resp.getEntity()) + ");";
    Response jsonpResp = Response.status(resp.getStatus()).entity(respEntity).build();
    return jsonpResp;
  }

  @Path("/metadata.json")
  @GET
  @Produces("application/json")
  public Response getJson()
  {
    Response resp = getResponse(StringFormat.JSON);
    return resp;
  }

  @Path("/metadata.xml")
  @GET
  @Produces("application/xml")
  public Response getXml()
  {
    Response resp = getResponse(StringFormat.XML);
    return resp;
  }

}
