package ecologylab.bigsemantics.downloaderpool.services;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import ecologylab.bigsemantics.downloaderpool.AssignedTasks;
import ecologylab.bigsemantics.downloaderpool.Controller;
import ecologylab.bigsemantics.downloaderpool.DownloaderRequest;
import ecologylab.bigsemantics.downloaderpool.DownloaderResult;
import ecologylab.bigsemantics.downloaderpool.Event;
import ecologylab.bigsemantics.downloaderpool.Task;
import ecologylab.bigsemantics.downloaderpool.DPoolUtils;
import ecologylab.serialization.formatenums.StringFormat;

/**
 * Requests that are related to tasks.
 * 
 * @author quyin
 */
@Path("/task")
public class TaskRequest extends RequestHandlerForController
{
  
  private static Logger heartBeatLogger;
  
  private static String EMPTY_ASSIGNMENT_XML;

  private static String EMPTY_ASSIGNMENT_JSON;
  
  static
  {
    heartBeatLogger = LoggerFactory.getLogger("dpool_downloader_heart_beat");
    EMPTY_ASSIGNMENT_XML = DPoolUtils.serialize(AssignedTasks.EMPTY_ASSIGNMENT, StringFormat.XML);
    EMPTY_ASSIGNMENT_JSON = DPoolUtils.serialize(AssignedTasks.EMPTY_ASSIGNMENT, StringFormat.JSON);
  }

  /**
   * Assign one or more tasks to a downloader.
   * 
   * @param remoteIp
   * @param workerId
   * @param blakclist
   * @param maxTaskCount
   * @return
   */
  public AssignedTasks assign(String remoteIp,
                              String workerId,
                              String blacklist,
                              int maxTaskCount)
  {
    heartBeatLogger.debug("Downloader[{}]@{} asks for tasks; blacklist: {}; count: {}.",
                          workerId,
                          remoteIp,
                          blacklist,
                          maxTaskCount);
    Controller ctrl = getController();

    DownloaderRequest req = new DownloaderRequest();
    req.setWorkerId(workerId);
    if (maxTaskCount > 0)
    {
      req.setMaxTaskCount(maxTaskCount);
    }
    if (blacklist != null && blacklist.length() > 0)
    {
      ArrayList<String> blist = Lists.newArrayList(Splitter.on(',').split(blacklist));
      req.setBlacklist(blist);
    }

    List<Task> tasks = ctrl.getTasksForWork(req);
    int n = tasks == null ? 0 : tasks.size();
    if (n > 0)
    {
      logger.debug("{} task(s) will be assigned to Downloader[{}]@{}, blacklist: [{}], max: {}.",
                   n,
                   workerId,
                   remoteIp,
                   blacklist,
                   maxTaskCount);
      for (Task task : tasks)
      {
        logger.info("{} will be assigned to Downloader[{}]@{}", task, workerId, remoteIp);
        Event e = new Event("assigned");
        e.addParam("downloader IP: " + remoteIp);
        task.addEvent(e);
      }
    }

    AssignedTasks result = AssignedTasks.EMPTY_ASSIGNMENT;
    if (n > 0)
    {
      result = new AssignedTasks();
      result.setTasks(tasks);
    }

    return result;
  }

  @Path("/assign.json")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response assignJson(@Context HttpServletRequest request,
                             @QueryParam("id") String workerId,
                             @QueryParam("blacklist") String blacklist,
                             @QueryParam("ntask") int maxTaskCount)
  {
    String remoteIp = request.getRemoteAddr();
    AssignedTasks result = this.assign(remoteIp, workerId, blacklist, maxTaskCount);
    
    if (result == AssignedTasks.EMPTY_ASSIGNMENT)
    {
      return Response.ok(EMPTY_ASSIGNMENT_JSON, MediaType.APPLICATION_JSON).build();
    }

    return generateResponse(result,
                            StringFormat.JSON,
                            MediaType.APPLICATION_JSON,
                            "Tasks Cannot Be Assigned.");
  }

  @Path("/assign.xml")
  @GET
  @Produces(MediaType.APPLICATION_XML)
  public Response assignXml(@Context HttpServletRequest request,
                            @QueryParam("id") String workerId,
                            @QueryParam("blacklist") String blacklist,
                            @QueryParam("ntask") int maxTaskCount)
  {
    String remoteIp = request.getRemoteAddr();
    AssignedTasks result = this.assign(remoteIp, workerId, blacklist, maxTaskCount);
    
    if (result == AssignedTasks.EMPTY_ASSIGNMENT)
    {
      return Response.ok(EMPTY_ASSIGNMENT_XML, MediaType.APPLICATION_XML).build();
    }

    return generateResponse(result,
                            StringFormat.XML,
                            MediaType.APPLICATION_XML,
                            "Tasks Cannot Be Assigned.");
  }

  /**
   * Find a task by ID or URL.
   * 
   * @param id
   * @param url
   * @return
   */
  public Task find(String id, String url)
  {
    Controller ctrl = getController();
    if (id != null && id.length() > 0)
    {
      return ctrl.getTask(id);
    }
    else if (url != null && url.length() > 0)
    {
      return ctrl.getTaskByUri(url);
    }
    return null;
  }

  /**
   * View the details of a task, in JSON.
   * 
   * @param id
   * @param url
   * @return
   */
  @Path("/view.json")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response viewJson(@QueryParam("id") String id,
                           @QueryParam("url") String url,
                           @QueryParam("with_result") boolean withResult)
  {
    Task t = find(id, url);
    Task t1 = (Task) t.clone();
    DownloaderResult dResult = t1.getResult();
    if (!withResult)
    {
      t1.setResult(null);
    }
    Response result =
        generateResponse(t1, StringFormat.JSON, MediaType.APPLICATION_JSON, "Cannot Find Task.");
    if (!withResult)
    {
      t1.setResult(dResult);
    }
    return result;
  }

  /**
   * View the details of a task, in XML.
   * 
   * @param workerId
   * @param url
   * @return
   */
  @Path("/view.xml")
  @GET
  @Produces(MediaType.APPLICATION_XML)
  public Response viewXml(@QueryParam("id") String id,
                          @QueryParam("url") String url,
                          @QueryParam("with_result") boolean withResult)
  {
    Task t = find(id, url);
    Task t1 = (Task) t.clone();
    DownloaderResult dResult = t1.getResult();
    if (!withResult)
    {
      t1.setResult(null);
    }
    Response result =
        generateResponse(t1, StringFormat.XML, MediaType.APPLICATION_XML, "Cannot Find Task.");
    if (!withResult)
    {
      t1.setResult(dResult);
    }
    return result;
  }

}
