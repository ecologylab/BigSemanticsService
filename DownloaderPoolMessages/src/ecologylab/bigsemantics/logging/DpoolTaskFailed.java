package ecologylab.bigsemantics.logging;

import ecologylab.logging.LogEvent;
import ecologylab.serialization.annotations.simpl_inherit;

/**
 * When a dpool task fails. Will retry.
 * 
 * @author quyin
 */
@simpl_inherit
public class DpoolTaskFailed extends LogEvent
{

  public DpoolTaskFailed()
  {
    super();
  }

}
