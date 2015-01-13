package ecologylab.bigsemantics.exceptions;

public class ProcessingUnfinished extends Exception
{

  public ProcessingUnfinished()
  {
    super();
  }

  public ProcessingUnfinished(String message,
                              Throwable cause,
                              boolean enableSuppression,
                              boolean writableStackTrace)
  {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public ProcessingUnfinished(String message, Throwable cause)
  {
    super(message, cause);
  }

  public ProcessingUnfinished(String message)
  {
    super(message);
  }

  public ProcessingUnfinished(Throwable cause)
  {
    super(cause);
  }

}
