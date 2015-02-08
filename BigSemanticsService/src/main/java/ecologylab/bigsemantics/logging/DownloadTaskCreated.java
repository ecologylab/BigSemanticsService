package ecologylab.bigsemantics.logging;

import ecologylab.logging.LogEvent;
import ecologylab.serialization.annotations.simpl_inherit;

/**
 * When a dpool task is created.
 * 
 * @author quyin
 */
@simpl_inherit
public class DownloadTaskCreated extends LogEvent
{

  public DownloadTaskCreated()
  {
    super();
  }

}
