package ecologylab.bigsemantics.admin;

import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.Utils;
import ecologylab.bigsemantics.dpool.Controller;
import ecologylab.bigsemantics.dpool.Downloader;
import ecologylab.bigsemantics.dpool.RemoteCurlDownloader;
import ecologylab.bigsemantics.dpool.RemoteCurlDownloaderList;
import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.SimplTypesScope;
import ecologylab.serialization.formatenums.StringFormat;

@Path("/downloader")
public class DownloaderService
{

  static Logger      logger = LoggerFactory.getLogger(DownloaderService.class);

  @Inject
  private Controller controller;

  @GET
  @Path("/all.xml")
  @Produces(MediaType.APPLICATION_XML)
  public Response allXml()
  {
    return all(StringFormat.XML, MediaType.APPLICATION_XML);
  }

  @GET
  @Path("/all.json")
  @Produces(MediaType.APPLICATION_JSON)
  public Response allJson()
  {
    return all(StringFormat.JSON, MediaType.APPLICATION_JSON);
  }

  public Response all(StringFormat format, String mediaType)
  {
    RemoteCurlDownloaderList downloaders = new RemoteCurlDownloaderList();
    Map<String, Downloader> workers = controller.getDispatcher().getWorkers();
    for (Downloader downloader : workers.values())
    {
      if (downloader instanceof RemoteCurlDownloader)
      {
        downloaders.addDownloader((RemoteCurlDownloader) downloader);
      }
    }
    try
    {
      String result = SimplTypesScope.serialize(downloaders, format).toString();
      return Response.ok(result).type(mediaType).build();
    }
    catch (SIMPLTranslationException e)
    {
      logger.error("Cannot serialize downloader list!", e);
      String error = Utils.getStackTraceAsString(e);
      return Response.serverError().entity(error).build();
    }
  }

  @Path("/{id}")
  public Response get(@PathParam("id") String id)
  {
    // TODO
    return null;
  }

}
