package ecologylab.bigsemantics.dpool;

import javax.inject.Singleton;

import org.apache.commons.configuration.Configuration;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.Configurable;

/**
 * 
 * @author quyin
 */
@Singleton
@Service
public class Controller implements Configurable, DpoolConfigNames
{

  static Logger              logger = LoggerFactory.getLogger(Controller.class);

  private Configuration      configs;

  private DownloadDispatcher dispatcher;

  private Thread             dispatcherThread;

  private boolean            dispatcherThreadStopRequested;

  public Controller()
  {
    super();
    dispatcher = new DownloadDispatcher();
    dispatcherThread = new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        dispatcherLoop();
      }
    });
  }

  @Override
  public void configure(Configuration configs)
  {
    this.configs = configs;
    int maxWorkerConsecFailures = configs.getInt(MAX_WORKER_CONSECUTIVE_FAILURES);
    dispatcher.setMaxConsecutiveWorkerFailures(maxWorkerConsecFailures);
  }

  @Override
  public Configuration getConfiguration()
  {
    return configs;
  }

  public DownloadDispatcher getDispatcher()
  {
    return dispatcher;
  }

  private void dispatcherLoop()
  {
    while (!dispatcherThreadStopRequested)
    {
      try
      {
        dispatcher.dispatchTask();
      }
      catch (InterruptedException e)
      {
        logger.warn("Dispatcher interrupted.", e);
      }
      catch (Exception e)
      {
        logger.warn("Exception on dispatching task.", e);
      }
    }
  }

  public void startDispatcher()
  {
    dispatcherThreadStopRequested = false;
    dispatcherThread.start();
  }

  public void stopDispatcher()
  {
    dispatcherThreadStopRequested = true;
    try
    {
      dispatcherThread.join();
    }
    catch (InterruptedException e)
    {
      logger.warn("Interruptted when waiting for dispatcher thread to stop", e);
    }
  }

}
