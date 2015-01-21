package ecologylab.bigsemantics.logging;

import ecologylab.serialization.annotations.simpl_inherit;
import ecologylab.serialization.annotations.simpl_scalar;

/**
 * An error has occurred for the dpool service.
 * 
 * @author quyin
 */
@simpl_inherit
public class DpoolServiceError extends ErrorEvent
{

  @simpl_scalar
  int dpoolStatusCode;

  public DpoolServiceError()
  {
    super();
  }

  public DpoolServiceError(Throwable throwable)
  {
    super(throwable);
  }

  public DpoolServiceError(String message)
  {
    super(message);
  }

  public DpoolServiceError(Throwable throwable, int dpoolStatusCode)
  {
    super(throwable);
    this.dpoolStatusCode = dpoolStatusCode;
  }

  public DpoolServiceError(String message, int dpoolStatusCode)
  {
    super(message);
    this.dpoolStatusCode = dpoolStatusCode;
  }

}
