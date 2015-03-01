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
import ecologylab.bigsemantics.metametadata.MetaMetadataRepository;
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
public class MmdRepoService
{

  static Logger                    logger      = LoggerFactory.getLogger(MmdRepoService.class);

  static Map<StringFormat, String> cachedRepos = new HashMap<StringFormat, String>();

  @QueryParam("callback")
  String                           callback;

  @Inject
  SemanticsServiceScope            semanticsServiceScope;

  public Response getMmdRepository(StringFormat format, String mediaType)
  {
    NDC.push("mmdrepository | format: " + format);
    long requestTime = System.currentTimeMillis();
    logger.debug("Requested at: " + (new Date(requestTime)));

    Response resp = null;

    Status status = Status.NOT_FOUND;
    String msg = SemanticsServiceErrorMessages.METAMETADATA_NOT_FOUND;

    MetaMetadataRepository mmdRepository = semanticsServiceScope.getMetaMetadataRepository();
    if (mmdRepository != null)
    {
      try
      {
        if (!cachedRepos.containsKey(format))
        {
          synchronized (cachedRepos)
          {
            if (!cachedRepos.containsKey(format))
            {
              String repoString = SimplTypesScope.serialize(mmdRepository, format).toString();
              cachedRepos.put(format, repoString);
            }
          }
        }
        String respBody = cachedRepos.get(format);
        resp = Response.status(Status.OK).entity(respBody).type(mediaType).build();
      }
      catch (SIMPLTranslationException e)
      {
        logger.error("Cannot serialize mmd repository!", e);
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

  @Path("/mmdrepository.jsonp")
  @GET
  @Produces("application/javascript")
  public Response getJsonp()
  {
    Response resp = getMmdRepository(StringFormat.JSON, "application/javascript");
    String respEntity = callback + "(" + (String) resp.getEntity() + ");";
    Response jsonpResp =
        Response.status(resp.getStatus()).entity(respEntity).type("application/javascript").build();
    return jsonpResp;
  }

  @Path("/mmdrepository.json")
  @GET
  @Produces("application/json")
  public Response getJson()
  {
    Response resp = getMmdRepository(StringFormat.JSON, MediaType.APPLICATION_JSON);
    return resp;
  }

  @Path("/mmdrepository.xml")
  @GET
  @Produces("application/xml")
  public Response getXml()
  {
    Response resp = getMmdRepository(StringFormat.XML, MediaType.APPLICATION_XML);
    return resp;
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
