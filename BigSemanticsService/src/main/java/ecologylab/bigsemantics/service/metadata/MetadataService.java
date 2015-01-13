package ecologylab.bigsemantics.service.metadata;

import javax.inject.Inject;
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
import ecologylab.bigsemantics.service.SemanticsServiceScope;
import ecologylab.net.ParsedURL;
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

  Logger                logger = LoggerFactory.getLogger(MetadataService.class);

  @Context
  HttpServletRequest    request;

  String                clientIp;

  @QueryParam("url")
  String                docUrl;

  ParsedURL             docPurl;

  @QueryParam("callback")
  String                callback;

  @QueryParam("reload")
  boolean               reload;

  @Inject
  SemanticsServiceScope semanticsServiceScope;

  /**
   * The utility method that actually generate the response; used by getJsonp(), getJson(), and
   * getXml().
   * 
   * @param format
   * @param mediaType
   * @return
   */
  Response getResponse(StringFormat format, String mediaType)
  {
    clientIp = request.getRemoteAddr();
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
    docPurl = ParsedURL.getAbsolute(docUrl);
    if (docPurl != null)
    {
      try
      {
        MetadataServiceHelper helper = new MetadataServiceHelper(this);
        int statusCode = helper.getMetadata();
        if (statusCode == 200)
        {
          String respBody = helper.serializeResultDocument(format);
          resp = Response.status(Status.OK).entity(respBody).type(mediaType).build();
        }
        else
        {
          errorStatus = Status.fromStatusCode(statusCode);
          errorMsg = helper.errorMessage;
        }
      }
      catch (Exception e)
      {
        errorMsg = "Exception happened: " + e.getMessage() + "; Details:\n"
                   + Utils.getStackTraceAsString(e);
        logger.error("Exception when processing " + docPurl, e);
      }
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
    Response resp = getResponse(StringFormat.JSON, MediaType.APPLICATION_JSON);
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
    Response resp = getResponse(StringFormat.JSON, MediaType.APPLICATION_JSON);
    return resp;
  }

  @Path("/metadata.xml")
  @GET
  @Produces("application/xml")
  public Response getXml()
  {
    Response resp = getResponse(StringFormat.XML, MediaType.APPLICATION_XML);
    return resp;
  }

}
