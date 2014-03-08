package ecologylab.bigsemantics.downloaderpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class abstracting a runnable (loopy) routine that can be started, paused, and stopped.
 * 
 * @author quyin
 */
public abstract class Routine implements Runnable
{

  static Logger logger = LoggerFactory.getLogger(Routine.class);

  /**
   * The status of a Routine.
   * 
   * @author quyin
   */
  public static enum Status
  {
    NEW, READY, RUNNING, PAUSED, STOP_PENDING, STOPPED,
  }

  /**
   * If rountineBody() results in exceptions in a sequence of this many times, the thread will exit.
   * If this is set to 0, ignore exceptions resulted from routineBody().
   */
  private int    maxError;

  /**
   * The time to sleep between looping the routine body, in milliseconds.
   */
  private long   sleepBetweenLoop = 300;

  /**
   * The status of this Routine
   */
  private Status status;

  private Object lockStatus       = new Object();

  private Thread thread;

  public Status getStatus()
  {
    return status;
  }

  public int getMaxError()
  {
    return maxError;
  }

  public void setMaxError(int maxError)
  {
    this.maxError = maxError;
  }

  public long getSleepBetweenLoop()
  {
    return sleepBetweenLoop;
  }

  public void setSleepBetweenLoop(long sleepBetweenLoop)
  {
    this.sleepBetweenLoop = sleepBetweenLoop;
  }

  /**
   * The constructor. Will call init() for initialization.
   */
  public Routine()
  {
    super();
    status = Status.NEW;
  }

  /**
   * The Routine implementation should call this method after getting ready to run.
   */
  protected void setReady()
  {
    status = Status.READY;
  }

  /**
   * The body of the routine loop. This method will be invoked repeatedly to do routine work, unless
   * the Routine is paused or stopped.
   */
  abstract void routineBody() throws Exception;

  @Override
  public void run()
  {
    int seqErrors = 0;

    while (status == Status.RUNNING || status == Status.PAUSED)
    {
      if (status == Status.RUNNING)
      {
        try
        {
          routineBody();
        }
        catch (Exception e)
        {
          logger.error("Error in routineBody(); # of errors in sequence: " + seqErrors, e);
          if (maxError > 0)
          {
            seqErrors += 1;
            if (seqErrors >= maxError)
            {
              status = Status.STOP_PENDING;
            }
          }
        }
      }

      DPoolUtils.sleep(sleepBetweenLoop);
    }

    synchronized (lockStatus)
    {
      status = Status.STOPPED;
      lockStatus.notifyAll();
    }
  }

  public void start()
  {
    if (status == Status.READY)
    {
      synchronized (lockStatus)
      {
        if (status == Status.READY)
        {
          status = Status.RUNNING;
          thread = new Thread(this);
          thread.start();
        }
      }
    }
  }

  public void pause()
  {
    if (status == Status.RUNNING)
    {
      synchronized (lockStatus)
      {
        if (status == Status.RUNNING)
        {
          status = Status.PAUSED;
        }
      }
    }
  }

  public void stop()
  {
    if (status == Status.RUNNING || status == Status.PAUSED)
    {
      synchronized (lockStatus)
      {
        if (status == Status.RUNNING || status == Status.PAUSED)
        {
          status = Status.STOP_PENDING;
          try
          {
            lockStatus.wait();
            if (status == Status.STOPPED)
            {
              return;
            }
            else
            {
              throw new RuntimeException("Cannot stop " + this + "!");
            }
          }
          catch (InterruptedException e)
          {
            e.printStackTrace();
          }
        }
      }
    }
  }

  public void join() throws InterruptedException
  {
    if (thread != null)
    {
      thread.join();
    }
  }

}
