package ecologylab.bigsemantics.downloaderpool.logging;

import ecologylab.logging.LogEvent;
import ecologylab.serialization.annotations.simpl_inherit;

/**
 * When a dpool task is reported from a downloader.
 * 
 * @author quyin
 */
@simpl_inherit
public class DpoolTaskReported extends LogEvent
{

  public DpoolTaskReported()
  {
    super();
  }

}
