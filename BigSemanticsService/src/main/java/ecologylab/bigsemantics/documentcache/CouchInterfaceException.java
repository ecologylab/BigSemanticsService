package ecologylab.bigsemantics.documentcache;

public class CouchInterfaceException extends Exception {

	
		private String tableID;
		private String docID;
		private String databaseUrl;
		private int    httpCode;
		private String message;
		
    public CouchInterfaceException () 
    {

    }

    public CouchInterfaceException (String message)
    {
        super (message);
    }

    public CouchInterfaceException (Throwable cause) 
    {
        super (cause);
    }

    public CouchInterfaceException (String message, Throwable cause) 
    {
        super (message, cause);
    }
    
    public CouchInterfaceException (String message, int httpCode , String databaseUrl,String tableId ,String docId )
    {
    		super( message );
    		this.databaseUrl = databaseUrl;
    		this.docID       = docId;
    		this.tableID     = tableId;
    		this.httpCode    = httpCode;
    		this.message     = message;
    }
    
    @Override
    public String toString()
    {
    	 //Fix?
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
