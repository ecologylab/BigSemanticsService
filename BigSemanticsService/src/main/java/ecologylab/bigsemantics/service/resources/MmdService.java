package ecologylab.bigsemantics.service.resources;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.NDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.bigsemantics.metametadata.MetaMetadata;
import ecologylab.bigsemantics.service.SemanticsServiceErrorMessages;
import ecologylab.bigsemantics.service.SemanticsServiceScope;
import ecologylab.bigsemantics.service.ServiceUtils;
import ecologylab.net.ParsedURL;
import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.SimplTypesScope;
import ecologylab.serialization.formatenums.StringFormat;

/**
 * Meta-metadata related services.
 * 
 * @author ajit
 * @author quyin
 */
@Path("/")
public class MmdService extends BaseService
{

  static final String        NAME       = "name";

  static final String        URL        = "url";

  static final String        CALLBACK   = "callback";

  static Logger              logger     = LoggerFactory.getLogger(MmdService.class);

  /**
   * key: (mmd_name)$(format), e.g. rich_document$JSON, scholarly_article$XML. value: serialized
   * mmd.
   */
  static Map<String, String> cachedMmds = new HashMap<String, String>();

  @Inject
  SemanticsServiceScope      semanticsServiceScope;

  // request specific UriInfo object to get absolute query path
  @Context
  UriInfo                    uriInfo;

  @QueryParam("aid")
  String                     appId;

  @QueryParam("aver")
  String                     appVer;

  @QueryParam("uid")
  String                     userId;

  @QueryParam("sid")
  String                     sessionId;

  @QueryParam(URL)
  String                     url;

  @QueryParam(NAME)
  String                     name;

  @QueryParam(CALLBACK)
  String                     callback;

  Response getResponse(StringFormat format, String mediaType, int ver) throws SIMPLTranslationException
  {
    NDC.push("MMD REQ format: " + format + " | url:" + url + " | name:" + name);
    long requestTime = System.currentTimeMillis();
    logger.info("Requested at: " + (new Date(requestTime)));

    Response resp = null;
    if (url != null)
    {
      ParsedURL purl = ParsedURL.getAbsolute(url);
      if (purl != null)
      {
        Document initialDoc = getInitialDocument(purl);
        if (initialDoc != null)
        {
          MetaMetadata mmd = (MetaMetadata) initialDoc.getMetaMetadata();
          UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder().queryParam(NAME, mmd.getName());
          if (callback != null)
          {
            uriBuilder = uriBuilder.queryParam(CALLBACK, callback);
          }
          URI nameURI = uriBuilder.build();
          resp = Response.status(Status.SEE_OTHER).location(nameURI).build();
        }
      }
    }
    else if (name != null)
    {
      String serializedMmd = getSerializedMmd(name, format);
      if (serializedMmd != null)
      {
        String respBody = "";
        if (ver == 2) {
          respBody = serializedMmd;
        } else if (ver == 3) {
          if (format == StringFormat.XML)
            throw new RuntimeException("V3 XML support is not implemented");
          respBody = "{" + join(new String[] {
            keyValuePair("request", reqStr(), false),
            keyValuePair("wrapper", serializedMmd, false),
          }, ",") + "}";
        } else {
          throw new IllegalArgumentException("Unrecognized version: " + ver);
        }
        resp = Response.status(Status.OK).entity(respBody).type(mediaType).build();
      }
    }
    else
    {
      resp = Response
          .status(Status.BAD_REQUEST)
          .entity(SemanticsServiceErrorMessages.BAD_REQUEST)
          .type(MediaType.TEXT_PLAIN)
          .build();
    }

    if (resp == null)
    {
      resp = Response
          .status(Status.NOT_FOUND)
          .entity(SemanticsServiceErrorMessages.METAMETADATA_NOT_FOUND)
          .type(MediaType.TEXT_PLAIN)
          .build();
    }

    logger.info("Time taken (ms): " + (System.currentTimeMillis() - requestTime));
    NDC.remove();

    return resp;
  }

  String getSerializedMmd(String name, StringFormat format) throws SIMPLTranslationException
  {
    String cacheKey = name + "$" + format.toString();
    if (!cachedMmds.containsKey(cacheKey))
    {
      synchronized (cachedMmds)
      {
        if (!cachedMmds.containsKey(cacheKey))
        {
          MetaMetadata mmd = getMmdByName(name);
          if (mmd != null)
          {
            String serializedMmd = SimplTypesScope.serialize(mmd, format).toString();
            cachedMmds.put(cacheKey, serializedMmd);
          }
        }
      }
    }
    return cachedMmds.get(cacheKey);
  }

  MetaMetadata getMmdByName(String mmdName)
  {
    MetaMetadata docMM = semanticsServiceScope.getMetaMetadataRepository().getMMByName(mmdName);
    return docMM;
  }

  Document getInitialDocument(ParsedURL requestedUrl)
  {
    return semanticsServiceScope.getOrConstructDocument(requestedUrl);
  }

  public Response getJsonp(int ver) throws SIMPLTranslationException
  {
    Response resp = getResponse(StringFormat.JSON, "application/javascript", ver);
    if (resp.getStatus() >= 400) // client or server error
    {
      return resp;
    }
    String respEntity = callback + "(" + (String) resp.getEntity() + ");";
    Response jsonpResp =
        Response.status(resp.getStatus()).entity(respEntity).type("application/javascript").build();
    return jsonpResp;
  }

  @Path("/mmd.jsonp")
  @GET
  @Produces("application/javascript")
  public Response getJsonpV2() throws SIMPLTranslationException
  {
    return getJsonp(2);
  }

  @Path("/v3/{name:wrapper|mmd|meta_metadata}.jsonp")
  @GET
  @Produces("application/javascript")
  public Response getJsonpV3() throws SIMPLTranslationException
  {
    return getJsonp(3);
  }

  public Response getJson(int ver) throws SIMPLTranslationException
  {
    Response resp = getResponse(StringFormat.JSON, MediaType.APPLICATION_JSON, ver);
    return resp;
  }

  @Path("/mmd.json")
  @GET
  @Produces("application/json")
  public Response getJsonV2() throws SIMPLTranslationException
  {
    return getJson(2);
  }

  @Path("/v3/{name:wrapper|mmd|meta_metadata}.json")
  @GET
  @Produces("application/json")
  public Response getJsonV3() throws SIMPLTranslationException
  {
    return getJson(3);
  }
  
  public Response getMmd(int ver) throws SIMPLTranslationException
  {
    Response resp = getResponse(StringFormat.XML, MediaType.APPLICATION_XML, ver);
    return resp;
  }

  @Path("/mmd.xml")
  @GET
  @Produces("application/xml")
  public Response getMmdV2() throws SIMPLTranslationException
  {
    return getMmd(2);
  }
  
  @Path("/v3/{name:wrapper|mmd|meta_metadata}.xml")
  @GET
  @Produces("application/xml")
  public Response getMmdV3() throws SIMPLTranslationException
  {
    return getMmd(3);
  }

}
