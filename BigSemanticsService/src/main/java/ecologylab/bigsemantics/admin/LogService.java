package ecologylab.bigsemantics.admin;

import java.util.Iterator;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import ecologylab.bigsemantics.Utils;
import ecologylab.bigsemantics.logging.DocumentLogRecord;
import ecologylab.bigsemantics.logging.ServiceLogRecord;
import ecologylab.bigsemantics.logging.ServiceLogRecordCollection;
import ecologylab.bigsemantics.service.SemanticsServiceScope;
import ecologylab.serialization.SimplTypesScope;
import ecologylab.serialization.formatenums.StringFormat;

@Path("/")
public class LogService
{

  @Inject
  SemanticsServiceScope semanticsServiceScope;

  @GET
  @Path("/log/{id}.json")
  @Produces("application/json")
  public Response getLogJson(@PathParam("id") String id) throws Exception
  {
    return getLogResponse(id, StringFormat.JSON, MediaType.APPLICATION_JSON);
  }

  @GET
  @Path("/log/{id}.xml")
  @Produces("application/xml")
  public Response getLogXml(@PathParam("id") String id) throws Exception
  {
    return getLogResponse(id, StringFormat.XML, MediaType.APPLICATION_XML);
  }

  @GET
  @Path("/find-logs.json")
  @Produces("application/json")
  public Response findLogsJson(@QueryParam("id") String id,
                               @QueryParam("caid") String clientAttachedId,
                               @QueryParam("urlfrag") String urlFrag,
                               @QueryParam("n") int count) throws Exception
  {
    return findLogsResponse(clientAttachedId,
                            urlFrag,
                            count,
                            StringFormat.JSON,
                            MediaType.APPLICATION_JSON);
  }

  @GET
  @Path("/find-logs.xml")
  @Produces("application/xml")
  public Response findLogsXml(@QueryParam("id") String id,
                              @QueryParam("caid") String clientAttachedId,
                              @QueryParam("urlfrag") String urlFrag,
                              @QueryParam("n") int count) throws Exception
  {
    return findLogsResponse(clientAttachedId,
                            urlFrag,
                            count,
                            StringFormat.XML,
                            MediaType.APPLICATION_XML);
  }

  public Response getLogResponse(String id, StringFormat format, String mediaType)
  {
    Response resp = Response.status(Status.NOT_FOUND).entity("Not found.").build();

    try
    {
      DocumentLogRecord logRecord = semanticsServiceScope.getLogStore().getLogRecord(id);
      if (logRecord != null)
      {
        String result = SimplTypesScope.serialize(logRecord, format).toString();
        resp = Response.status(Status.OK).type(mediaType).entity(result).build();
      }
    }
    catch (Exception e)
    {
      resp = Response.serverError().entity(Utils.getStackTraceAsString(e)).build();
    }

    return resp;
  }

  public Response findLogsResponse(String clientAttachedId,
                                   String urlFrag,
                                   int n,
                                   StringFormat format,
                                   String mediaType)
  {
    if (n <= 0)
    {
      n = 20;
    }

    Response resp = Response.status(Status.SERVICE_UNAVAILABLE).entity("Unknown error.").build();

    try
    {
      ServiceLogRecordCollection logs = new ServiceLogRecordCollection();
      Iterator<ServiceLogRecord> iter =
          semanticsServiceScope.getLogStore().filter(clientAttachedId, urlFrag);
      for (int i = 0; i < n && iter.hasNext(); ++i)
      {
        logs.addLogRecord(iter.next());
      }

      String body = SimplTypesScope.serialize(logs, format).toString();
      resp = Response.status(Status.OK).entity(body).type(mediaType).build();
    }
    catch (Exception e)
    {
      resp = Response.serverError().entity(Utils.getStackTraceAsString(e)).build();
    }

    return resp;
  }

}
