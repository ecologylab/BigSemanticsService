package ecologylab.bigsemantics.logging;

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
    LogEventTypeScope.addEventClass(DownloadTaskCreated.class);
    LogEventTypeScope.addEventClass(DownloadTaskDied.class);
    LogEventTypeScope.addEventClass(DownloadTaskDispatched.class);
    LogEventTypeScope.addEventClass(DownloadTaskFailed.class);
    LogEventTypeScope.addEventClass(DownloadTaskQueued.class);
    LogEventTypeScope.addEventClass(DownloadTaskSucceeded.class);

    ClassDescriptor<?> cd = ClassDescriptor.getClassDescriptor(LogPost.class);
    FieldDescriptor fd = cd.getFieldDescriptorByFieldName("events");
    fd.reevaluateScopeAnnotation();
  }

}
