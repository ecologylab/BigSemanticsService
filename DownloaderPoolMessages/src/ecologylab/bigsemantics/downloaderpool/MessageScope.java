package ecologylab.bigsemantics.downloaderpool;

import ecologylab.serialization.SimplTypesScope;

/**
 * The SIMPL scope for all the messages used by the controller and the downloaders.
 * 
 * @author quyin
 */
public class MessageScope
{

  public static final String  NAME    = "DownloaderPoolMessages";

  public static final Class[] CLASSES = {
      Task.class,
      Event.class,
      DownloaderRequest.class,
      AssignedTasks.class,
      BasicResponse.class,
      DownloaderResult.class,
  };

  public static SimplTypesScope get()
  {
    return SimplTypesScope.get(NAME, CLASSES);
  }

}
