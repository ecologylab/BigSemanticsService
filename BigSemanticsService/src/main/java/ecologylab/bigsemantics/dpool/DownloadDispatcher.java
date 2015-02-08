package ecologylab.bigsemantics.dpool;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.distributed.Dispatcher;
import ecologylab.bigsemantics.distributed.TaskEventHandler;
import ecologylab.bigsemantics.documentcache.EhCacheMan;
import ecologylab.bigsemantics.logging.DownloadTaskDispatching;
import ecologylab.bigsemantics.logging.DownloadTaskQueued;
import ecologylab.bigsemantics.logging.DpoolEventTypeScope;

/**
 * The central controller that works with a set of downloaders. The controller accepts requests from
 * clients as tasks, assigns tasks to distributed downloaders, collects results, and returns to
 * clients. The controller doesn't remember previous tasks (memoryless), for simplicity.
 * 
 * @author quyin
 */
public class DownloadDispatcher extends Dispatcher<DownloadTask, Downloader>
    implements DpoolConfigNames
{

  static Logger logger = LoggerFactory.getLogger(DownloadDispatcher.class);

  static
  {
    DpoolEventTypeScope.init();
  }

  /**
   * Indexing all tasks by ID. We regard conflicts of keys as impossible.
   */
  private Cache allTasksById;

  /**
   * Indexing all tasks by URL. When there is conflict this only stores the latest one.
   */
  private Cache allTasksByUrl;

  public DownloadDispatcher()
  {
    super();

    CacheManager cacheManager = EhCacheMan.getSingleton();

    cacheManager.addCacheIfAbsent("tasks-by-id");
    allTasksById = cacheManager.getCache("tasks-by-id");

    cacheManager.addCacheIfAbsent("tasks-by-uri");
    allTasksByUrl = cacheManager.getCache("tasks-by-uri");

    logger.info("Controller is constructed and ready.");
  }

  public DownloadTask getTask(String id)
  {
    Element element = allTasksById.get(id);
    return element == null ? null : (DownloadTask) element.getObjectValue();
  }

  public DownloadTask getTaskByUri(String uri)
  {
    Element element = allTasksByUrl.get(uri);
    return element == null ? null : (DownloadTask) element.getObjectValue();
  }

  /**
   * Queue a new task to this controller.
   * 
   * @param task
   */
  public void queueTask(DownloadTask task, TaskEventHandler<DownloadTask> handler)
  {
    allTasksById.put(new Element(task.getId(), task));
    logger.info("enqueuing task " + task);
    super.queueTask(task, handler);
  }

  @Override
  protected void onQueued(DownloadTask task)
  {
    task.getLogPost().addEventNow(new DownloadTaskQueued());
  };

  @Override
  protected void onDispatch(DownloadTask task)
  {
    task.getLogPost().addEventNow(new DownloadTaskDispatching());
  }

  @Override
  protected boolean isTooManyFail(DownloadTask task)
  {
    return task.getFailCount() >= task.getMaxAttempts();
  }

}
