package ecologylab.bigsemantics.exceptions;

public class DocumentRecycled extends Exception
{

  public DocumentRecycled()
  {
    super();
  }

  public DocumentRecycled(String message)
  {
    super(message);
  }

  public DocumentRecycled(Throwable cause)
  {
    super(cause);
  }

  public DocumentRecycled(String message, Throwable cause)
  {
    super(message, cause);
  }

  public DocumentRecycled(String message,
                          Throwable cause,
                          boolean enableSuppression,
                          boolean writableStackTrace)
  {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
