package ecologylab.bigsemantics.service.resources;

import java.util.Date;

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
import ecologylab.bigsemantics.logging.Phase;
import ecologylab.bigsemantics.logging.ServiceLogRecord;
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

  static Logger         logger = LoggerFactory.getLogger(MetadataService.class);

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

  @QueryParam("caid")
  String                clientAttachedId;

  @Inject
  SemanticsServiceScope semanticsServiceScope;

  String                requestId;

  ServiceLogRecord      logRecord;

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
    requestId = Utils.base64urlEncode(fpBytes);
    NDC.push(String.format("[Task %s] ", requestId));
    logger.info(msg);

    logRecord = new ServiceLogRecord();
    logRecord.setId(requestId);
    logRecord.setRequesterIp(clientIp);
    logRecord.setRequestTime(new Date());
    logRecord.setRequestUrl(request.getRequestURI());
    semanticsServiceScope.getLogStore().addLogRecord(requestId, clientAttachedId, logRecord);

    logRecord.beginPhase(Phase.WHOLE);

    Response resp = null;
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
          resp = Response.status(statusCode).entity(helper.errorMessage).build();
        }
      }
      catch (Exception e)
      {
        String errMsg = String.format("Exception happened for %s (Req ID: %s).", docUrl, requestId);
        logRecord.setErrorRecord(errMsg, e);
        resp = Response.status(Status.INTERNAL_SERVER_ERROR).entity(errMsg).build();
        logger.error(errMsg, e);
      }
    }
    else
    {
      String errMsg = "Parameter 'url' must be a valid URL.";
      resp = Response.status(Status.BAD_REQUEST).entity(errMsg).build();
    }

    if (resp == null)
    {
      String errMsg = "Unknown error, please contact admin with Req ID " + requestId;
      resp = Response.status(Status.INTERNAL_SERVER_ERROR).entity(errMsg).build();
    }

    NDC.remove();

    // at this point of time resp cannot be null
    logRecord.setResponseCode(resp.getStatus());
    logRecord.beginPhase(Phase.WHOLE);
    logger.info("Response generated. Total time in BigSemantics: {}ms",
                logRecord.getTotalMs(Phase.WHOLE));
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
