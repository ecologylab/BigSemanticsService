package ecologylab.bigsemantics.documentcache;

import java.io.IOException;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;

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
   * @throws CouchInterfaceException 
   * @throws IOException 
   * @throws ParseException 
   */
  public String getDoc(String docId, String tableId) throws CouchInterfaceException, ParseException, IOException;

  /**
   * @param docId
   *          Will be the id of the new document in the database. Must be a valid, unused id for a
   *          couchdb
   * @param docContent
   *          The new document as a json string
   * @param tableId
   *          The name of the table
   * 
   * @return boolean, true if document was put, false if not because there was a naming conflict
   * @throws IOException 
   * @throws ParseException 
   * @throws CouchInterfaceException 
   */
  public boolean putDoc(String docId, String docContent, String tableId) throws ParseException, IOException, CouchInterfaceException;

  /**
   * @param docId
   *          The id of the document to be updated, document must already exist
   * @param docContent
   *          What the document should be changed to as a json string
   * @param tableId
   *          The table where the document is.
   *
   * @return true if successful false if can't update because the document doesn't exist
   * @throws IOException 
   * @throws ClientProtocolException 
   * @throws CouchInterfaceException 
   */
  public boolean updateDoc(String docId, String docContent, String tableId) throws ClientProtocolException, IOException, CouchInterfaceException;

  /**
   * @param docId
   *          The id of the document to be deleted, the document should exist.
   * @param tableId
   *          The name of the table where the document is.
   *
   * @return true if successful false if can't delete document because it doesn't exist
   * @throws CouchInterfaceException 
   * @throws IOException 
   * @throws ClientProtocolException 
   */
  public boolean dropDoc(String docId, String tableId) throws CouchInterfaceException, ClientProtocolException, IOException;

  /**
   * 
   * @param docId
   * 				The id of the document to add an attachment to
   * @param tableId
   * 				The id of the table where the document is
   * @param content
   * 			  String content of data to be attached
   * @param mimeType
   * 	      of the attachment  
   * @param contentTitle
   * 				name of the title
   * @return true if successful
   * @throws IOException 
   * @throws ClientProtocolException 
   * @throws CouchInterfaceException 
   */
  public boolean putAttach(String docId,
                       String tableId,
                       String content,
                       String mimeType,
                       String contentTitle) throws ClientProtocolException, IOException, CouchInterfaceException;

  /**
   * 
   * @param docId
   * @param tableId
   * @param title
   * @return the byte array that makes up the attachement
   */
  public byte[] getAttach(String docId, String tableId, String title);

}
