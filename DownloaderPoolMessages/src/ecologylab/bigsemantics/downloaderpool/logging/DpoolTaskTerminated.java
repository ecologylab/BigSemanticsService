package ecologylab.bigsemantics.downloaderpool.logging;

import ecologylab.logging.LogEvent;
import ecologylab.serialization.annotations.simpl_inherit;

/**
 * When a dpool task is eventually marked as terminated after multiple unsuccessful trials.
 * 
 * @author quyin
 */
@simpl_inherit
public class DpoolTaskTerminated extends LogEvent
{

  public DpoolTaskTerminated()
  {
    super();
  }

}
