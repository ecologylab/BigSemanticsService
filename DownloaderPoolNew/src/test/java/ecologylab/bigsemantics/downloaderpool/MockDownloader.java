package ecologylab.bigsemantics.downloaderpool;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;

/**
 * A mock downloader for testing
 * 
 * @author quyin
 */
public class MockDownloader extends Downloader
{

  // collaborating objects:


  /**
   * A preset responder (that will typically not actually connect to a controller on a server).
   */
  MockDownloaderResponder presetResponder;

  // properties:

  int                     numTasksRequested  = 0;

  int                     numPagesQueued     = 0;

  /**
   * Use this to preset some tasks, as if they are retrieved from the controller.
   */
  List<Task>              presetTasks        = new ArrayList<Task>();

  /**
   * If this is set to true, the preset tasks will be used only once. This is useful to test the
   * downloader's behavior with a single set of preset tasks. If you need to test its behavior with
   * a serials of tasks, you can set this to false.
   */
  boolean                 usePresetTasksOnce = false;
  
  public MockDownloader(Configuration configs)
  {
    super(configs);
    // TODO Auto-generated constructor stub
  }

  @Override
  public List<Task> requestTasks()
  {
    // use preset tasks
    numTasksRequested++;
    List<Task> result = presetTasks;
    if (usePresetTasksOnce)
    {
      presetTasks = null;
    }
    return result;
  }

  @Override
  protected Page createPage(Task task)
  {
    // creates a MockPage instead of Page
    Page page = new MockPage(task.getId(), task.getPurl(), task.getUserAgent());
    page.clientFactory = this.clientFactory;
    page.siteTable = this.siteTable;
    return page;
  }

  @Override
  protected void queuePageToDownload(Page pageToDownload, Task associatedTask)
  {
    // queue and count
    super.queuePageToDownload(pageToDownload, associatedTask);
    System.err.println("Queued page to download: " + pageToDownload);
    numPagesQueued++;
  }

  @Override
  protected DownloaderResponder createDownloaderResponder(Task associatedTask)
  {
    // use the preset responder, if there is one.
    if (presetResponder == null)
    {
      presetResponder = new MockDownloaderResponder();
    }
    return presetResponder;
  }

}
