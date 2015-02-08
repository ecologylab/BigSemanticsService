package ecologylab.bigsemantics.logging;

import ecologylab.logging.LogEvent;
import ecologylab.serialization.annotations.simpl_inherit;
import ecologylab.serialization.annotations.simpl_scalar;

/**
 * When a dpool task fails. Will retry.
 * 
 * @author quyin
 */
@simpl_inherit
public class DownloadTaskFailed extends LogEvent
{

  @simpl_scalar
  private String message;

  public DownloadTaskFailed()
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
