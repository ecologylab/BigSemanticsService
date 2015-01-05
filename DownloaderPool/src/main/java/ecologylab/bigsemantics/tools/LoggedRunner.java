package ecologylab.bigsemantics.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Run given commands as a sub-process, and log all its outputs.
 * 
 * @author quyin
 */
public class LoggedRunner
{

  static Logger          logger = LoggerFactory.getLogger(LoggedRunner.class);

  private ProcessBuilder processBuilder;

  private Process        process;

  private BufferedReader outputReader;

  private BufferedReader errorReader;

  private Logger         ologger;

  private Logger         elogger;

  private boolean        running;

  private boolean        stopRequested;

  /**
   * Constructor.
   * 
   * @param processName
   * @param cmds
   */
  public LoggedRunner(String processName, String... cmds)
  {
    ologger = LoggerFactory.getLogger("loggedrunner.stdout." + processName);
    elogger = LoggerFactory.getLogger("loggedrunner.stderr." + processName);
    processBuilder = new ProcessBuilder(cmds);
  }

  /**
   * Start the target process.
   * 
   * @throws IOException
   * @throws InterruptedException
   */
  public void start() throws IOException, InterruptedException
  {
    if (!running)
    {
      synchronized (this)
      {
        if (!running)
        {
          running = true;
          process = processBuilder.start();
          outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
          errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
          new Thread(new Runnable()
          {
            @Override
            public void run()
            {
              runProcess();
            }
          }).start();
        }
      }

      int exitValue = process.waitFor();
      stopRequested = true;
      ologger.info("Exit value: " + exitValue);
    }
  }

  /**
   * Private method for collecting outputs from the target process when it is running.
   */
  private void runProcess()
  {
    while (running)
    {
      synchronized (this)
      {
        if (running)
        {
          boolean readOut = logLineIfReady(outputReader, ologger);
          boolean readErr = logLineIfReady(errorReader, elogger);
          if (!readOut && !readErr && stopRequested)
          {
            running = false;
          }
        }
      }
    }
  }

  /**
   * Log one line from the reader to the given logger only if it is ready. If it is not ready,
   * nothing happens.
   * 
   * @param reader
   * @param logger
   * @return true if something has been read, otherwise false.
   */
  private boolean logLineIfReady(BufferedReader reader, Logger logger)
  {
    try
    {
      if (reader.ready())
      {
        String line = reader.readLine();
        if (line != null)
        {
          logger.info(line);
          return true;
        }
      }
    }
    catch (IOException e)
    {
      logger.error("Cannot read process stream!", e);
    }
    return false;
  }

  public static void main(String[] args)
  {
    if (args == null || args.length < 2)
    {
      System.out.println("args: <process_name> <args...>");
      System.exit(-1);
    }
    LoggedRunner lr = new LoggedRunner(args[0], Arrays.copyOfRange(args, 1, args.length));
    try
    {
      lr.start();
    }
    catch (Exception e)
    {
      logger.error("Error happened when running the process.", e);
    }

  }

}
