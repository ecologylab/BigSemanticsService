package ecologylab.bigsemantics.documentcache;

/**
 * 
 * @author Zach Brown
 */
public class CouchAccessorException extends Exception
{

  private static final long serialVersionUID = -6999991539679691236L;

  private String            tableID;

  private String            docID;

  private String            databaseUrl;

  private int               httpCode;

  private String            message;

  public CouchAccessorException()
  {

  }

  public CouchAccessorException(String message)
  {
    super(message);
  }

  public CouchAccessorException(Throwable cause)
  {
    super(cause);
  }

  public CouchAccessorException(String message, Throwable cause)
  {
    super(message, cause);
  }

  public CouchAccessorException(String message,
                                 int httpCode,
                                 String databaseUrl,
                                 String tableId,
                                 String docId)
  {
    super(message);
    this.databaseUrl = databaseUrl;
    this.docID = docId;
    this.tableID = tableId;
    this.httpCode = httpCode;
    this.message = message;
  }

  @Override
  public String toString()
  {
    // Fix?
    return message;
  }

  public String getTableId()
  {
    return tableID;
  }

  public String getDocId()
  {
    return docID;
  }

  public String getDatabaseUrl()
  {
    return databaseUrl;
  }

  public int getHttpCode()
  {
    return httpCode;
  }

  public String getMessage()
  {
    return message;
  }

}
