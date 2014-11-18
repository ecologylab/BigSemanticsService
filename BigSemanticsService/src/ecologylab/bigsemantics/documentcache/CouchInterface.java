package ecologylab.bigsemantics.documentcache;
/*
 * @author Zach Brown
 * 
 */
public interface CouchInterface {

	/*
	 * @param docId id of the document desired
	 * @param databaseId The name of the table
	 * 
	 * @return Will return the document from the database in json form
	 * 		   The json will now also contain the entries id, and _rev
	 * 		   Will be null if the document was unattainable
	 */
	public String   getDoc(String docId , String tableId);
	
	/*
	 *  @param docId      Will be the id of the new document in the database. Must be a valid, unused id for a couchdb
	 *  @param docContent The new document as a json string
	 *  @param tableId    The name of the table
	 *  
	 *  @return           Will be 201 if successful, otherwise some error code
	 */
	public int      putDoc(String docId , String docContent , String tableId );
	
	/*
	 * @param docId       The id of the document to be updated, document must already exist
	 * @param docContent  What the document should be changed to as a json string
	 * @param tableId     The table where the document is.
	 *
	 * @return Will be 201 if successful, otherwise some error code
	 */
	public int      updateDoc(String docId , String docContent, String tableId); 
	
	/*
	 *	@param docId   The id of the document to be deleted, the document should exist.
	 *	@param tableId The name of the table where the document is. 
	 *
	 *	@return        200 if successful, otherwise some error code
	 */
	public int      dropDoc(String docId , String tableId);
	
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
	 *  null - getDoc failure code
	 *  1000 - UnsupportedEncodingException
	 *  1100 - ParseException
	 *  1200 - IOException
	 *  1300 - Illegal dropDoc id i.e the id was empty and would have deleted the database
	 *  
	 */
}
