package ecologylab.bigsemantics.logging;

import ecologylab.logging.LogEvent;
import ecologylab.serialization.annotations.simpl_inherit;
import ecologylab.serialization.annotations.simpl_scalar;

/**
 * When a dpool task is eventually marked as terminated after multiple unsuccessful trials.
 * 
 * @author quyin
 */
@simpl_inherit
public class DownloadTaskDied extends LogEvent
{

  @simpl_scalar
  private String stacktrace;

  public DownloadTaskDied()
  {
    super();
  }

  public String getStacktrace()
  {
    return stacktrace;
  }

  public void setStacktrace(String stacktrace)
  {
    this.stacktrace = stacktrace;
  }

}
