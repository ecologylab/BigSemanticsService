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
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.bigsemantics.service.SemanticsServiceScope;
import ecologylab.net.ParsedURL;
import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.SimplTypesScope;
import ecologylab.serialization.formatenums.StringFormat;

/**
 * Metadata related services.
 * 
 * @author ajit
 * @author quyin
 */
@Path("/")
public class MetadataService extends BaseService
{

  static Logger         logger = LoggerFactory.getLogger(MetadataService.class);

  @Context
  HttpServletRequest    request;

  String                clientIp;

  @QueryParam("aid")
  String                appId;

  @QueryParam("aver")
  String                appVer;

  @QueryParam("uid")
  String                userId;

  @QueryParam("sid")
  String                sessionId;

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
  Response getResponse(StringFormat format, String mediaType, int ver)
  {
    clientIp = request.getRemoteAddr();
    String msg =
        String.format("Request from %s: %s, in %s, reload=%s", clientIp, docUrl, format, reload);
    byte[] fpBytes = Utils.fingerprintBytes("" + System.currentTimeMillis() + "|" + msg);
    requestId = Utils.base64urlNoPaddingEncode(fpBytes);
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
          String serializedMetadata = helper.serializeResultDocument(format);

          String respBody = "";
          if (ver == 2) {
            respBody = serializedMetadata;
          } else if (ver == 3) {
            if (format == StringFormat.XML)
              throw new RuntimeException("V3 XML support is not implemented");
            respBody = "{" + join(new String[] {
              keyValuePair("request", reqStr(), false),
              keyValuePair("metadata", serializedMetadata, false),
            }, ",") + "}";
          } else {
            throw new IllegalArgumentException("Unrecognized version: " + ver);
          }
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
        logRecord.addErrorRecord(errMsg, e);
        resp = Response.status(Status.INTERNAL_SERVER_ERROR).entity(errMsg).build();
        logger.error(errMsg, e);
      }
    }
    else
    {
      String errMsg = "Parameter 'url' must be a valid URL.";
      logRecord.addErrorRecord(errMsg, null);
      resp = Response.status(Status.BAD_REQUEST).entity(errMsg).build();
    }

    if (resp == null)
    {
      String errMsg = "Unknown error, please contact admin with Req ID " + requestId;
      logRecord.addErrorRecord(errMsg, null);
      resp = Response.status(Status.INTERNAL_SERVER_ERROR).entity(errMsg).build();
    }

    NDC.remove();

    // at this point of time resp cannot be null
    logRecord.setResponseCode(resp.getStatus());
    logRecord.endPhase(Phase.WHOLE);
    logger.info("Response generated. Total time in BigSemantics: {}ms",
                logRecord.getTotalMs(Phase.WHOLE));
    return resp;
  }

  public Response getJsonp(int ver)
  {
    Response resp = getResponse(StringFormat.JSON, MediaType.APPLICATION_JSON, ver);
    if (resp.getStatus() >= 400) // client or server error
    {
      return resp;
    }
    String respEntity = callback + "(" + ((String) resp.getEntity()) + ");";
    Response jsonpResp = Response.status(resp.getStatus()).entity(respEntity).build();
    return jsonpResp;
  }
  @Path("/metadata.jsonp")
  @GET
  @Produces("application/javascript")
  public Response getJsonpV2()
  {
    return getJsonp(2);
  }
  @Path("/v3/{name:metadata|semantics}.jsonp")
  @GET
  @Produces("application/javascript")
  public Response getJsonpV3()
  {
    return getJsonp(3);
  }

  public Response getJson(int ver)
  {
    Response resp = getResponse(StringFormat.JSON, MediaType.APPLICATION_JSON, ver);
    return resp;
  }
  @Path("/metadata.json")
  @GET
  @Produces("application/json")
  public Response getJsonV2()
  {
    return getJson(2);
  }
  @Path("/v3/{name:metadata|semantics}.json")
  @GET
  @Produces("application/json")
  public Response getJsonV3()
  {
    return getJson(3);
  }

  public Response getXml(int ver)
  {
    Response resp = getResponse(StringFormat.XML, MediaType.APPLICATION_XML, ver);
    return resp;
  }
  @Path("/metadata.xml")
  @GET
  @Produces("application/xml")
  public Response getXmlV2()
  {
    return getXml(2);
  }
  @Path("/v3/{name:metadata|semantics}.xml")
  @GET
  @Produces("application/xml")
  public Response getXmlV3()
  {
    return getXml(3);
  }

  Response getMetadataOrStubResponse(StringFormat format, String mediaType)
  {
    docPurl = ParsedURL.getAbsolute(docUrl);
    if (docPurl != null)
    {
      try
      {
        Document doc = semanticsServiceScope.getOrConstructDocument(docPurl);
        String xml = SimplTypesScope.serialize(doc, format).toString();
        return Response.ok(xml).type(mediaType).build();
      }
      catch (SIMPLTranslationException e)
      {
        String errorMsg = String.format("Error serializing %s", docUrl);
        logger.error(errorMsg, e);
        return Response.serverError().entity(errorMsg).build();
      }
      catch (Exception e)
      {
        String errorMsg = String.format("Unknown exception when processing %s", docUrl);
        logger.error(errorMsg, e);
        return Response.serverError().entity(errorMsg).build();
      }
    }
    String errorMsg = String.format("Invalid request parameter: %s", docUrl);
    logger.error(errorMsg);
    return Response.serverError().entity(errorMsg).build();
  }

  @Path("/metadata_or_stub.xml")
  @GET
  @Produces("application/xml")
  public Response getMetadataOrStubXml()
  {
    return getMetadataOrStubResponse(StringFormat.XML, MediaType.APPLICATION_XML);
  }

  @Path("/metadata_or_stub.json")
  @GET
  @Produces("application/json")
  public Response getMetadataOrStubJson()
  {
    return getMetadataOrStubResponse(StringFormat.JSON, MediaType.APPLICATION_JSON);
  }

  @Path("/metadata_or_stub.jsonp")
  @GET
  @Produces("application/javascript")
  public Response getMetadataOrStubJsonp()
  {
    Response resp = getMetadataOrStubResponse(StringFormat.JSON, MediaType.APPLICATION_JSON);
    int status = resp.getStatus();
    if (status >= 400) // client or server error
    {
      return resp;
    }
    String respEntity = callback + "(" + ((String) resp.getEntity()) + ");";
    Response jsonpResp = Response.status(resp.getStatus()).entity(respEntity).build();
    return jsonpResp;
  }

}
