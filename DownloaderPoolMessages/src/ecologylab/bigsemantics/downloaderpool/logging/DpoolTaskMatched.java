package ecologylab.bigsemantics.downloaderpool.logging;

import ecologylab.logging.LogEvent;
import ecologylab.serialization.annotations.simpl_inherit;

/**
 * When a dpool task is matched to a downloader, but not yet assigned.
 * 
 * @author quyin
 */
@simpl_inherit
public class DpoolTaskMatched extends LogEvent
{

  public DpoolTaskMatched()
  {
    super();
  }

}
