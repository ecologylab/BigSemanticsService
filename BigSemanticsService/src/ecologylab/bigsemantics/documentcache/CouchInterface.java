package ecologylab.bigsemantics.documentcache;

/*
 * Author Zach Brown
 * 
 */
public interface CouchInterface {

		
	//Returns and empty string if unsuccessful
	public String   getDoc(String id , String dbid);
	
	//Returns an code, if successful 201 otherwise see codes below
	public int      putDoc(String id , String json , String dbid );
	
	//Returns an code, if successful 201 otherwise see codes below
	public int      updateDoc(String id , String json, String dbid); 
	
	//Returns an code, if successful 200, otherwise see codes below
	public int      dropDoc(String id , String dbid);
	
	/*
	 *  Apache CouchDB Return Codes 
	 *  200 - OK
	 *  201 - Created
	 *  202 - Accepted
	 *  304 - Not Modified
	 *  400 - Bad Request
	 *  401 - Unauthorized
	 *  403 - Forbidden
	 *  404 - Not Found
	 *  405 - Resource Not Allowed
	 *  406 - Not Acceptable
	 *  409 - Conflict
	 *  412 - Precondition Failed
	 *  415 - Bad Content Type
	 *  416 - Requested Range Not Satisfiable
	 *  417 - Expectation Failed
	 *  500 - Internal Server Error
	 *  
	 *  CouchInterface Return Codes
	 *  "" - getDoc failure code
	 *  1000 - UnsupportedEncodingException
	 *  1100 - ParseException
	 *  1200 - IOException
	 *  1300 - Illegal dropDoc id i.e the id was empty and would have deleted the database
	 *  
	 */
}
