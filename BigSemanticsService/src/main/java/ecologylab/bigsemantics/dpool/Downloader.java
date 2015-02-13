package ecologylab.bigsemantics.dpool;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.Utils;
import ecologylab.bigsemantics.distributed.TaskEventHandler;
import ecologylab.bigsemantics.distributed.Worker;
import ecologylab.bigsemantics.httpclient.SimplHttpResponse;
import ecologylab.bigsemantics.logging.DownloadTaskDied;
import ecologylab.bigsemantics.logging.DownloadTaskDispatched;
import ecologylab.bigsemantics.logging.DownloadTaskFailed;
import ecologylab.bigsemantics.logging.DownloadTaskSucceeded;
import ecologylab.serialization.annotations.simpl_inherit;

/**
 * 
 * @author quyin
 */
@simpl_inherit
public abstract class Downloader extends Worker<DownloadTask>
{

  static Logger                  logger = LoggerFactory.getLogger(Downloader.class);

  private DomainRuntimeInfoTable domainInfoTable;

  /**
   * For deserialization only.
   */
  public Downloader()
  {
    super();
  }

  public Downloader(String id, int numThreads)
  {
    super(id, numThreads);
  }

  public Downloader(String id, int numThreads, int priority)
  {
    super(id, numThreads, priority);
  }

  public void initializeDomainInfoTable(ConcurrentHashMap<String, DomainInfo> domainInfos)
  {
    this.domainInfoTable = new DomainRuntimeInfoTable(domainInfos);
  }

  protected DomainRuntimeInfo getDomainRuntimeInfo(DownloadTask task) throws DpoolException
  {
    if (domainInfoTable == null)
    {
      throw new DpoolException("Domain Info Table must be initialized before use!");
    }
    return domainInfoTable.getOrCreate(task.getDomain());
  }

  @Override
  protected void onSubmitAccept(DownloadTask task, TaskEventHandler<DownloadTask> handler)
  {
    task.setDownloader(this);
    DownloadTaskDispatched event = new DownloadTaskDispatched();
    event.setDownloaderId(this.getId());
    task.getLogPost().addEventNow(event);
  }

  @Override
  protected void onTaskPerformed(DownloadTask task, TaskEventHandler<DownloadTask> handler)
  {
    task.setDownloader(null);
  }

  public boolean performDownload(DownloadTask task) throws Exception
  {
    DomainRuntimeInfo domainRuntimeInfo = getDomainRuntimeInfo(task);
    try
    {
      domainRuntimeInfo.beginAccess();
      int code = doPerformDownload(task);
      domainRuntimeInfo.endAccess(code);
      SimplHttpResponse httpResp = task.getResponse();

      if (code < 400)
      {
        String content = httpResp.getContent();
        if (task.getFailRegex() != null && task.getFailPattern().matcher(content).find())
        {
          logger.warn("Fail pattern found in {}", task);
          DownloadTaskFailed event = new DownloadTaskFailed();
          event.setMessage("Failure pattern found.");
          task.getLogPost().addEventNow(event);
          return false;
        }
        if (task.getBanRegex() != null && task.getBanPattern().matcher(content).find())
        {
          domainRuntimeInfo.setBanned();
          logger.warn("Ban pattern found in {}", task);
          DownloadTaskFailed event = new DownloadTaskFailed();
          event.setMessage("Ban pattern found.");
          task.getLogPost().addEventNow(event);
          return false;
        }
        DownloadTaskSucceeded event = new DownloadTaskSucceeded();
        event.setDownloaderId(this.getId());
        event.setContentLength(httpResp.getContentLength());
        task.getLogPost().addEventNow(event);
        return true;
      }
    }
    catch (Exception e)
    {
      DownloadTaskDied event = new DownloadTaskDied();
      event.setStacktrace(Utils.getStackTraceAsString(e));
      task.getLogPost().addEventNow(event);
      domainRuntimeInfo.endAccess(601);
      throw e;
    }
    return false;
  }

  /**
   * Do downloading, and return an error code. If there is a HTTP status code, return that; if not,
   * return an error code >= 600.
   * 
   * @param task
   * @return HTTP status code, or custom error code >= 600.
   * @throws Exception
   */
  abstract public int doPerformDownload(DownloadTask task) throws Exception;

  @Override
  public boolean canHandle(DownloadTask task) throws DpoolException
  {
    DomainRuntimeInfo domainRuntimeInfo = getDomainRuntimeInfo(task);
    return domainRuntimeInfo.canAccess();
  }

}
