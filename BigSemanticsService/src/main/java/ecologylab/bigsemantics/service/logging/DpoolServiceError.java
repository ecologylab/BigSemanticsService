package ecologylab.bigsemantics.service.logging;

import ecologylab.logging.LogEvent;
import ecologylab.serialization.annotations.simpl_inherit;
import ecologylab.serialization.annotations.simpl_scalar;

/**
 * An error has occurred for the dpool service.
 * 
 * @author quyin
 */
@simpl_inherit
public class DpoolServiceError extends LogEvent
{

  @simpl_scalar
  private String message;

  public DpoolServiceError()
  {
    super();
  }

  public String getMessage()
  {
    return message;
  }

  public void setMessage(String message)
  {
    this.message = message;
  }

}
