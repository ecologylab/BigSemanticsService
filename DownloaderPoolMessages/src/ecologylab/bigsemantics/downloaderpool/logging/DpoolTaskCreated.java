package ecologylab.bigsemantics.downloaderpool.logging;

import ecologylab.logging.LogEvent;
import ecologylab.serialization.annotations.simpl_inherit;

/**
 * When a dpool task is created.
 * 
 * @author quyin
 */
@simpl_inherit
public class DpoolTaskCreated extends LogEvent
{

  public DpoolTaskCreated()
  {
    super();
  }

}
