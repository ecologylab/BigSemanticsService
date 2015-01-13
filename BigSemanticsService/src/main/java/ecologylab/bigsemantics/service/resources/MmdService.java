package ecologylab.bigsemantics.service.resources;

import java.net.URI;
import java.util.Date;

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
import ecologylab.serialization.formatenums.StringFormat;

/**
 * Meta-metadata related services.
 * 
 * @author ajit
 * @author quyin
 */
@Path("/")
public class MmdService
{

  static final String   NAME     = "name";

  static final String   URL      = "url";

  static final String   CALLBACK = "callback";

  static final String   WITH_URL = "withurl";

  static Logger         logger   = LoggerFactory.getLogger(MmdService.class);

  @Inject
  SemanticsServiceScope semanticsServiceScope;

  // request specific UriInfo object to get absolute query path
  @Context
  UriInfo               uriInfo;

  @QueryParam(URL)
  String                url;

  @QueryParam(NAME)
  String                name;

  @QueryParam(CALLBACK)
  String                callback;

  @QueryParam(WITH_URL)
  String                withUrl;

  Response getMmdResponse(StringFormat format, String mediaType)
  {
    NDC.push("MMD REQ format: " + format + " | url:" + url + " | name:" + name);
    long requestTime = System.currentTimeMillis();
    logger.debug("Requested at: " + (new Date(requestTime)));

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
            if (withUrl != null)
            {
              // Here, note that whatever the value of withurl is, in the redirection we use the
              // real URL. in this way you don't need to repeat the URL, since it is already
              // available through the url parameter.
              uriBuilder = uriBuilder.queryParam(WITH_URL, ServiceUtils.urlencode(url));
            }
          }
          URI nameURI = uriBuilder.build();
          resp = Response.status(Status.SEE_OTHER).location(nameURI).build();
        }
      }
    }
    else if (name != null)
    {
      MetaMetadata mmd = getMmdByName(name);
      if (mmd != null)
      {
        String mmdJson = ServiceUtils.serialize(mmd, format);
        String respString = mmdJson;
        if (callback != null)
        {
          String locParam = "";
          if (withUrl != null)
          {
            logger.info("withUrl = {}", withUrl);
            locParam = "\"" + withUrl + "\", ";
          }
          respString = callback + "(" + locParam + mmdJson + ");";
        }
        resp = Response.status(Status.OK).entity(respString).type(mediaType).build();
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

    logger.debug("Time taken (ms): " + (System.currentTimeMillis() - requestTime));
    NDC.remove();

    return resp;
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

  @Path("/mmd.jsonp")
  @GET
  @Produces("application/javascript")
  public Response getJsonp()
  {
    Response resp = getMmdResponse(StringFormat.JSON, "application/javascript");
    return resp;
  }

  @Path("/mmd.json")
  @GET
  @Produces("application/json")
  public Response getJson()
  {
    Response resp = getMmdResponse(StringFormat.JSON, "application/json");
    return resp;
  }

  @Path("/mmd.xml")
  @GET
  @Produces("application/xml")
  public Response getMmd()
  {
    Response resp = getMmdResponse(StringFormat.XML, "application/xml");
    return resp;
  }

}
