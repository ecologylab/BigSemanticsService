package ecologylab.bigsemantics.downloaderpool.logging;

import ecologylab.logging.LogEventTypeScope;

/**
 * Initialize the type scope of dpool events.
 * 
 * @author quyin
 */
public class DpoolEventTypeScope
{

  public static void init()
  {
    LogEventTypeScope.addEventClass(DpoolTaskAssigned.class);
    LogEventTypeScope.addEventClass(DpoolTaskCreated.class);
    LogEventTypeScope.addEventClass(DpoolTaskFailed.class);
    LogEventTypeScope.addEventClass(DpoolTaskMatched.class);
    LogEventTypeScope.addEventClass(DpoolTaskQueued.class);
    LogEventTypeScope.addEventClass(DpoolTaskReported.class);
    LogEventTypeScope.addEventClass(DpoolTaskSuccess.class);
    LogEventTypeScope.addEventClass(DpoolTaskTerminated.class);
  }

}
