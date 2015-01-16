package ecologylab.bigsemantics.documentcache;

/**
 * Apache CouchDB Return Codes:
 * <ul>
 * <li>200 - OK</li>
 * <li>201 - Created</li>
 * <li>202 - Accepted</li>
 * <li>304 - Not Modified</li>
 * <li>400 - Bad Request</li>
 * <li>401 - Unauthorized</li>
 * <li>403 - Forbidden</li>
 * <li>404 - Not Found</li>
 * <li>405 - Resource Not Allowed</li>
 * <li>406 - Not Acceptable</li>
 * <li>409 - Conflict</li>
 * <li>412 - Precondition Failed</li>
 * <li>415 - Bad Content Type</li>
 * <li>416 - Requested Range Not Satisfiable</li>
 * <li>417 - Expectation Failed</li>
 * <li>500 - Internal Server Error</li>
 * </ul>
 * 
 * CouchInterface Return Codes:
 * <ul>
 * <li>null - getDoc failure code</li>
 * <li>1000 - UnsupportedEncodingException</li>
 * <li>1100 - ParseException</li>
 * <li>1200 - IOException</li>
 * <li>1300 - Illegal dropDoc id i.e the id was empty and would have deleted the database</li>
 * </ul>
 *
 * @author Zach Brown
 */
public interface CouchInterface
{

  /**
   * @param docId
   *          id of the document desired
   * @param databaseId
   *          The name of the table
   * 
   * @return Will return the document from the database in json form The json will now also contain
   *         the entries id, and _rev Will be null if the document was unattainable
   */
  public String getDoc(String docId, String tableId);

  /**
   * @param docId
   *          Will be the id of the new document in the database. Must be a valid, unused id for a
   *          couchdb
   * @param docContent
   *          The new document as a json string
   * @param tableId
   *          The name of the table
   * 
   * @return Will be 201 if successful, otherwise some error code
   */
  public int putDoc(String docId, String docContent, String tableId);

  /**
   * @param docId
   *          The id of the document to be updated, document must already exist
   * @param docContent
   *          What the document should be changed to as a json string
   * @param tableId
   *          The table where the document is.
   *
   * @return Will be 201 if successful, otherwise some error code
   */
  public int updateDoc(String docId, String docContent, String tableId);

  /**
   * @param docId
   *          The id of the document to be deleted, the document should exist.
   * @param tableId
   *          The name of the table where the document is.
   *
   * @return 200 if successful, otherwise some error code
   */
  public int dropDoc(String docId, String tableId);

  public int putAttach(String docId,
                       String tableId,
                       String content,
                       String mimeType,
                       String contentTitle);

  public byte[] getAttach(String docId, String tableId, String title);

}
