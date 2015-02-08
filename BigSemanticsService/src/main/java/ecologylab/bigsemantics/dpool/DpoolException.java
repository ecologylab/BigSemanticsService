package ecologylab.bigsemantics.dpool;

/**
 * 
 * @author quyin
 */
public class DpoolException extends Exception
{

  private static final long serialVersionUID = -8366585640119006045L;

  public DpoolException()
  {
    super();
  }

  public DpoolException(String message)
  {
    super(message);
  }

  public DpoolException(Throwable cause)
  {
    super(cause);
  }

  public DpoolException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public DpoolException(String message,
                        Throwable cause,
                        boolean enableSuppression,
                        boolean writableStackTrace)
  {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
