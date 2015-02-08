package ecologylab.bigsemantics.dpool.resources;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.Configurable;
import ecologylab.bigsemantics.Utils;
import ecologylab.bigsemantics.distributed.Task.State;
import ecologylab.bigsemantics.dpool.Controller;
import ecologylab.bigsemantics.dpool.DownloadTask;
import ecologylab.bigsemantics.dpool.DpoolConfigNames;
import ecologylab.bigsemantics.httpclient.SimplHttpResponse;
import ecologylab.bigsemantics.logging.DownloadTaskCreated;
import ecologylab.generic.StringBuilderBaseUtils;
import ecologylab.serialization.formatenums.StringFormat;

/**
 * Requests that are related to a web page.
 * 
 * @author quyin
 */
@Path("/page")
public class PageService implements Configurable, DpoolConfigNames
{

  static Logger              logger = LoggerFactory.getLogger(PageService.class);

  @Context
  private HttpServletRequest request;

  @QueryParam("url")
  private String             url;

  @QueryParam("agent")
  private String             userAgent;

  @QueryParam("n")
  private int                numOfAttempts;

  @QueryParam("t")
  private int                timeout;

  @QueryParam("rfail")
  private String             failRegex;

  @QueryParam("rban")
  private String             banRegex;

  @Inject
  private Controller         controller;

  private Configuration      configuration;

  private String             remoteIp;

  private DownloadTask       task;

  private Response           response;

  public void configure(Configuration configuration)
  {
    this.configuration = configuration;
    if (userAgent == null)
    {
      userAgent = configuration.getString(DEFAULT_USER_AGENT);
    }
    if (numOfAttempts <= 0)
    {
      numOfAttempts = configuration.getInt(DEFAULT_NUM_ATTEMPTS);
    }
    if (timeout <= 0)
    {
      timeout = configuration.getInt(DEFAULT_TIMEOUT);
    }
  }

  @Override
  public Configuration getConfiguration()
  {
    return configuration;
  }

  /**
   * Download a web page.
   * 
   * @param format
   * @param mediaType
   */
  void doDownload(StringFormat format, String mediaType)
  {
    configure(controller.getConfiguration());

    remoteIp = request.getRemoteAddr();

    StringBuilder sb = StringBuilderBaseUtils.acquire();
    sb.append(remoteIp).append("|").append(System.currentTimeMillis()).append("|").append(url);
    byte[] hash = Utils.fingerprintBytes(sb.toString());
    String taskId = Utils.base64urlNoPaddingEncode(hash);
    StringBuilderBaseUtils.release(sb);

    try
    {
      task = new DownloadTask(taskId, url);

      if (userAgent != null)
      {
        task.setUserAgent(userAgent);
      }
      task.setMaxAttempts(numOfAttempts);
      task.setAttemptTime(timeout);
      task.setFailRegex(failRegex);
      task.setBanRegex(banRegex);

      task.getLogPost().addEventNow(new DownloadTaskCreated());

      logger.debug("DownloadTask created: {}", task);

      controller.getDispatcher().queueTask(task, null);
      logger.debug("DownloadTask queued: {}", task);

      task.waitForDone(timeout);

      State state = task.getState();
      SimplHttpResponse httpResp = task.getResponse();
      int code = httpResp == null ? 500 : httpResp.getCode();
      String taskDetail = getTaskDetail(format);
      logger.debug("DownloadTask {} done, state={}", task, state);
      if (state == State.SUCCEEDED)
      {
        response = Response.ok(taskDetail, mediaType).build();
      }
      else
      {
        response = Response.status(code).entity(taskDetail).build();
      }
    }
    catch (Exception e)
    {
      logger.error("Exception occurred when downloading " + url, e);
      String stacktrace = Utils.getStackTraceAsString(e);
      String taskDetail = getTaskDetail(format);
      response = Response.serverError().entity(stacktrace + "\n\n\n" + taskDetail).build();
    }
  }

  private String getTaskDetail(StringFormat format)
  {
    String taskDetail = Utils.serializeToString(task, format);
    return taskDetail == null ? "null" : taskDetail;
  }

  @Path("/download.json")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response downloadJson()
  {
    doDownload(StringFormat.JSON, MediaType.APPLICATION_JSON);
    return response;
  }

  @Path("/download.xml")
  @GET
  @Produces(MediaType.APPLICATION_XML)
  public Response downloadXml()
  {
    doDownload(StringFormat.XML, MediaType.APPLICATION_XML);
    return response;
  }

  @Path("/raw")
  @GET
  public Response raw()
  {
    // TODO need implementation
    return Response.serverError().entity("NOT YET IMPLEMENTED").build();
  }

}
