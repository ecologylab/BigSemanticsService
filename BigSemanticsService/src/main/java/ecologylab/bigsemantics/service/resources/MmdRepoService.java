package ecologylab.bigsemantics.service.resources;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.NDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.Utils;
import ecologylab.bigsemantics.namesandnums.SemanticsAssetVersions;
import ecologylab.bigsemantics.service.SemanticsServiceErrorMessages;
import ecologylab.bigsemantics.service.SemanticsServiceScope;
import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.SimplTypesScope;
import ecologylab.serialization.formatenums.StringFormat;

/**
 * Meta-metadata repository related services.
 * 
 * @author ajit
 * @author quyin
 */
@Path("/")
public class MmdRepoService extends BaseService
{

  static Logger                    logger      = LoggerFactory.getLogger(MmdRepoService.class);

  static Map<StringFormat, String> cachedRepos = new HashMap<StringFormat, String>();

  @QueryParam("callback")
  String                           callback;

  @Inject
  SemanticsServiceScope            semanticsServiceScope;

  public Response getResponse(StringFormat format, String mediaType, int ver)
  {
    NDC.push("mmdrepository | format: " + format);
    long requestTime = System.currentTimeMillis();
    logger.debug("Requested at: " + (new Date(requestTime)));

    Response resp = null;

    Status status = Status.NOT_FOUND;
    String msg = SemanticsServiceErrorMessages.METAMETADATA_NOT_FOUND;

    if (semanticsServiceScope.getMetaMetadataRepository() != null)
    {
      try
      {
        String repoStr = getCachedRepoStr(format);
        
        String respBody = "";
        if (ver == 2) {
          respBody = repoStr;
        } else if (ver == 3) {
          if (format == StringFormat.XML)
            throw new RuntimeException("V3 XML support is not implemented");
          respBody = "{" + join(new String[] {
            keyValuePair("request", reqStr(), false),
            keyValuePair("repository", repoStr, false),
          }, ",") + "}";
        } else {
          throw new IllegalArgumentException("Unrecognized version: " + ver);
        }
        resp = Response.status(Status.OK).entity(respBody).type(mediaType).build();
      }
      catch (Exception e)
      {
        logger.error("Cannot serve mmd repository!", e);
        status = Status.INTERNAL_SERVER_ERROR;
        msg = SemanticsServiceErrorMessages.SERVICE_UNAVAILABLE;
        msg += "\nDetails: " + e.getMessage() + "\n" + Utils.getStackTraceAsString(e);
      }
    }

    if (resp == null)
    {
      resp = Response.status(status).entity(msg).type(MediaType.TEXT_PLAIN).build();
    }

    logger.debug("Time taken (ms): " + (System.currentTimeMillis() - requestTime));
    NDC.remove();

    return resp;
  }

  private String getCachedRepoStr(StringFormat format) throws SIMPLTranslationException
  {
    if (!cachedRepos.containsKey(format))
    {
      synchronized (cachedRepos)
      {
        if (!cachedRepos.containsKey(format))
        {
          String repoBody = SimplTypesScope.serialize(semanticsServiceScope.getMetaMetadataRepository(), format).toString();
          cachedRepos.put(format, repoBody);
        }
      }
    }
    return cachedRepos.get(format);
  }

  public Response getJsonp(int ver)
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

  @Path("/mmdrepository.jsonp")
  @GET
  @Produces("application/javascript")
  public Response getJsonpV2()
  {
    return getJsonp(2);
  }

  @Path("/v3/{name:repository|mmdrepository}.jsonp")
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

  @Path("/mmdrepository.json")
  @GET
  @Produces("application/json")
  public Response getJsonV2()
  {
    return getJson(2);
  }

  @Path("/v3/{name:repository|mmdrepository}.json")
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

  @Path("/mmdrepository.xml")
  @GET
  @Produces("application/xml")
  public Response getXmlV2()
  {
    return getXml(2);
  }

  @Path("/v3/{name:repository|mmdrepository}.xml")
  @GET
  @Produces("application/xml")
  public Response getXmlV3()
  {
    return getXml(3);
  }

  @Path("/mmdrepository.version")
  @GET
  @Produces("text/plain")
  public Response getVersion()
  {
    String version = String.valueOf(SemanticsAssetVersions.METAMETADATA_ASSET_VERSION);
    return Response.status(Status.OK).entity(version).type(MediaType.TEXT_PLAIN).build();
  }

}
