package ecologylab.bigsemantics.downloaderpool.logging;

import ecologylab.logging.LogEventTypeScope;
import ecologylab.logging.LogPost;
import ecologylab.serialization.ClassDescriptor;
import ecologylab.serialization.FieldDescriptor;

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
    
    ClassDescriptor cd = ClassDescriptor.getClassDescriptor(LogPost.class);
    FieldDescriptor fd = cd.getFieldDescriptorByFieldName("events");
    fd.reevaluateScopeAnnotation();
  }

}
