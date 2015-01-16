
package ecologylab.bigsemantics.documentcache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.BaseEncoding;

import ecologylab.bigsemantics.Utils;
import ecologylab.bigsemantics.collecting.SemanticsGlobalScope;
import ecologylab.bigsemantics.cyberneko.CybernekoWrapper;
import ecologylab.bigsemantics.generated.library.RepositoryMetadataTypesScope;
import ecologylab.bigsemantics.generated.library.primitives.CachedHtml;
import ecologylab.bigsemantics.generated.library.primitives.CouchdbEntry;
import ecologylab.bigsemantics.metadata.MetadataDeserializationHookStrategy;
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.bigsemantics.metadata.builtins.PersistenceMetaInfo;
import ecologylab.bigsemantics.service.SemanticsServiceScope;
import ecologylab.net.ParsedURL;
import ecologylab.serialization.DeserializationHookStrategy;
import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.SimplTypesScope;
import ecologylab.serialization.formatenums.Format;
import ecologylab.serialization.formatenums.StringFormat;
/**
 * A persistent document cache that uses the disk.
 *  
 * @author zach
 */

	public class CouchPersistentDocumentCache implements PersistentDocumentCache<Document> {

	
	  private static final String htmlTable     = "html";
	  private static final String metaDataTable = "metadata";
	  private static final String metaInfoTable = "metainfo";
	  private static final String dataBaseUrl   = "ecoarray0:7054";
	  private static CouchInterface couchInterface;
	 
	  static Logger                logger;

	  static
	  {
	    logger = LoggerFactory.getLogger(CouchPersistentDocumentCache.class);
	  }
	  
	  private SimplTypesScope      entryTScope;

	  private SemanticsGlobalScope semanticsScope;

	  public CouchPersistentDocumentCache(SemanticsGlobalScope ss)
	  {
	    semanticsScope = ss;
		  
		couchInterface = new HttpCouchInterface(dataBaseUrl);
		
	    // entryTScope    = SimplTypesScope.get("CouchdbEntry", CouchdbEntry.class);
	    entryTScope = ss.getMetadataTypesScope();
	  
	  }
	  
	  public CouchPersistentDocumentCache()
	  {
	    semanticsScope = new SemanticsGlobalScope(RepositoryMetadataTypesScope.get(), CybernekoWrapper.class);
		 
		couchInterface = new HttpCouchInterface(dataBaseUrl);
	    entryTScope    = SimplTypesScope.get("CouchdbEntry", CouchdbEntry.class);
	  
	  }


	  private static String getDocId(ParsedURL purl){
	    return getDocId(purl.toString());
	  }

	  public static String getDocId(String purl)
	  {
	    if (purl == null)
	    {
	      return null;
	    }

	    MessageDigest md;
	    try
	    {
	      md = MessageDigest.getInstance("SHA-256");
	      md.update(purl.toString().getBytes("UTF-8"));
	      byte[] digest = md.digest();

	      BaseEncoding be = BaseEncoding.base64Url();
	      return be.encode(digest, 0, 9);
	    }
	    catch (Exception e)
	    {
	      logger.error("Cannot hash " + purl, e);
	    }

	    return "ERROR_DOC_ID";
	  }
	  



	  private String getDoc( String docId , String tableId ){
		  
    	  String encodedDocId = Utils.base64urlEncode(Utils.secureHashBytes(docId));
    	  encodedDocId        = new String("A" + encodedDocId);
		  String json = couchInterface.getDoc(encodedDocId, tableId);
		  if ( json == null)
		  {
			  return null;
			  
		  }
		  else
		  {
			  json = "{ \"couchdb_entry\" : " + json + " } ";
			  return json;
		  }  
	  }
	  
	  private byte[] getAttach( String docId , String tableId , String title ){
		  
    	  String encodedDocId = Utils.base64urlEncode(Utils.secureHashBytes(docId));
    	  encodedDocId        = new String("A" + encodedDocId);
		  byte[] bytes = couchInterface.getAttach(encodedDocId, tableId , title);
		  if ( bytes == null)
		  {
			  return null;
			  
		  }
		  else
		  {
			 
			  return bytes;
		  }  
	  }
	  
      private int couchDoc(String docId, String tableId, Object doc) throws SIMPLTranslationException {
    	  
    	  String encodedDocId = Utils.base64urlEncode(Utils.secureHashBytes(docId));
    	  encodedDocId        = new String("A" + encodedDocId);
    	  OutputStream output = new ByteArrayOutputStream();
    	  SimplTypesScope.serialize(doc , output , Format.JSON );
    	  String valueJSON = output.toString(); //I need to figure out which character encoding to use
    	  String docJSON = "{\"value\":" + valueJSON + "}";
    	  int code = couchInterface.putDoc(encodedDocId,  docJSON, tableId);
  

    	  return code;
	  }
		
      private int couchAttach(String docId, String tableId, String attachment , String mimeType, String contentTitle) throws SIMPLTranslationException {
    	  String encodedDocId = Utils.base64urlEncode(Utils.secureHashBytes(docId));
    	  encodedDocId        = new String("A" + encodedDocId);
    	  int code = couchInterface.putAttach(encodedDocId,  tableId , attachment , mimeType , contentTitle);
    	  return code;
  
      }
      
      private int updateDoc(String docId, String tableId ,Object metaInfo) throws SIMPLTranslationException {
		
    	 String encodedDocId = Utils.base64urlEncode(Utils.secureHashBytes(docId));
    	 encodedDocId        = new String("A" + encodedDocId);
		 OutputStream output = new ByteArrayOutputStream();
		 SimplTypesScope.serialize(metaInfo,  output, Format.JSON);
   	     String valueJSON = output.toString(); //I need to figure out which character encoding to use
   	     String docJSON = "{ \"value\" : " + valueJSON + " }";
   	     int code = couchInterface.updateDoc(encodedDocId,  docJSON, tableId);
   	     return code;
	  }

  	  private int unCouch(String docId, String tableId) {
  		String encodedDocId = Utils.base64urlEncode(Utils.secureHashBytes(docId));
  		encodedDocId        = new String("A" + encodedDocId);
  		int code = couchInterface.dropDoc(encodedDocId, tableId);
  		return code;
  	  }
  	  private int couchOrOverWrite(String docId, String tableId,  Object doc) throws SIMPLTranslationException {
  		  
  		  int result = couchDoc(docId, tableId, doc);
  		  if( result == 201  || result == 200)	  
  		  {
  			  return 200;
  		  }
  		  else
  		  {
  			  result = updateDoc(docId, tableId, doc);
  			  if( result == 200 || result == 201 )
  				  
  				  return 200;
  			  else
  				  return result;
  		  }
  	  }
  	  
	  @Override
	  public PersistenceMetaInfo getMetaInfo(ParsedURL location)
	  {
	    String docId = getDocId(location.toString());
	    String couchEntryJson = getDoc( docId , metaInfoTable);
	    
	    if (couchEntryJson != null)
	    {
	      PersistenceMetaInfo metaInfo = null;
	      try
	      {
	    	InputStream input = new ByteArrayInputStream(couchEntryJson.getBytes() );
	        CouchdbEntry entry = (CouchdbEntry) entryTScope.deserialize(input, Format.JSON);
	        metaInfo = (PersistenceMetaInfo) entry.getValue();
	      }
	      catch (SIMPLTranslationException e)
	      {
	        logger.error("Cannot deserialize metadata retrived from " + "/" + metaInfoTable + "/" + docId, e);
	        return null;
	      }
	      return metaInfo;
	    }
	    else
	    {
	    	logger.error("Failed to retrieve get document from " + "/" + metaInfoTable + "/" + docId);
	    	return null;
	    }
	  }

	  @Override
	  public PersistenceMetaInfo store(Document document,
	                                   String rawContent,
	                                   String charset,
	                                   String mimeType,
	                                   String mmdHash)
	  {
	    if (document == null || document.getLocation() == null)
	    {
	      return null;
	    }
	    
	    
	    ParsedURL location = document.getLocation();
	    String docId = getDocId(location);
	    
	    CachedHtml cachedHtml = new CachedHtml();
	    //rawContent = Utils.base64urlEncode(rawContent.getBytes());
	    
	    
	    //cachedHtml.setContent(rawContent);
	    cachedHtml.setLocation(location);
	    
	    Date now = new Date();
	    PersistenceMetaInfo metaInfo = getMetaInfo(location);
	   
	    if (metaInfo == null)
	    {
	      metaInfo = new PersistenceMetaInfo();
	    }
	    metaInfo.setDocId(docId);
	    metaInfo.setLocation(location);
	    metaInfo.setMimeType(mimeType);
	    metaInfo.setAccessTime(now);
	    metaInfo.setPersistenceTime(now);
	    metaInfo.setMmdHash(document.getMetaMetadata().getHashForExtraction());
	    
	    
	    
	    int result;
	    try {
			
			result = couchOrOverWrite( docId , htmlTable , cachedHtml);
		    if ( result != 200 && result != 201 )
		    {
		    	
		    	  logger.error("Cannot store " + docId + " in " + htmlTable + " Error code " + result);
		    	  return null;
		    }
		    
		    //ci.putAttach("test" , "html" , "Working?" , "text/plain" , "My_example")
		    result = couchAttach(docId , htmlTable , rawContent, mimeType,  docId);
		    if ( result != 200 && result != 201 )
		    {
		    	
		    	  logger.error("Cannot store attachment for " + docId + " in " + htmlTable + " Error code " + result);
		    	  return null;
		    }
		    
		    
		    
		    result = couchOrOverWrite( docId , metaDataTable ,document);
		    if( result != 200 && result != 201 )
		    {
		    	  logger.error("Cannot store " + docId + " in " + metaDataTable + " Error code " + result);
		    	  return null;
		    }
		    result = couchOrOverWrite( docId , metaInfoTable , metaInfo);
		    if( result != 200 && result != 201 )
		    {
		    	logger.error("Cannot store " + docId + " in " + metaInfoTable + " Error code " + result);
		    	return null;
		    }
		    
		} 
		catch (SIMPLTranslationException e) {
				
				logger.error("Failure to serilize while storing " + document + ", doc_id=" + docId, e);
				return null;
		}
	    
	      
	   return metaInfo;
	      
	      
	  }

	@Override
	  public boolean updateDoc(PersistenceMetaInfo metaInfo, Document newDoc)
	  {
	    if (newDoc != null && newDoc.getLocation() != null)
	    {
	      metaInfo.setPersistenceTime(new Date());
	      metaInfo.setMmdHash(newDoc.getMetaMetadata().getHashForExtraction());
	      try
	      {
	    	String docId = metaInfo.getLocation().toString();
	    	int result1 = updateDoc( docId , metaDataTable , newDoc);
	    	int result2 = updateDoc( docId , metaInfoTable , metaInfo);
	    	if ( result1 != 200 || result2 != 200)
	    		return false;
	    	else
	    		return true;
	      }
	      catch (SIMPLTranslationException e)
	      {
	        logger.error("Cannot store " + newDoc + ", doc_id=" + metaInfo.getDocId(), e);
	      }
	    }

	    return false;
	  }

	@Override
	  public Document retrieveDoc(PersistenceMetaInfo metaInfo)
	  {
	    String docId = metaInfo.getDocId();
	    
	    String couchEntryJson = getDoc( docId , metaDataTable);
	    if ( couchEntryJson != null)
	    {

	      Document document = null;
	      try
	      { 
	    	//InputStream input = new ByteArrayInputStream(couchEntryJson.getBytes() );
	    	  
    	    //TranslationContext translationContext = new TranslationContext();
	    	DeserializationHookStrategy deserializationHookStrategy = new MetadataDeserializationHookStrategy(semanticsScope);
			CouchdbEntry entry = (CouchdbEntry) entryTScope.deserialize(couchEntryJson,deserializationHookStrategy , StringFormat.JSON);
	    	
	        // CouchdbEntry entry = (CouchdbEntry) entryTScope.deserialize(input, Format.JSON);
	        document = (Document) entry.getValue();
	      }
	      catch (SIMPLTranslationException e)
	      {
	        logger.error("Cannot deserialize document retrived from " + metaDataTable + "/" + docId, e);
	      }
	      return document;
	    }
	    else
	    {
	    	logger.error("Cannot retrive document from " + metaDataTable + "/" + docId);
	    }

	    return null;
	  }

	  @Override
	  public String retrieveRawContent(PersistenceMetaInfo metaInfo)
	  {
		  

		 
		 
	    String docId = metaInfo.getDocId();
	    byte[] bytes = getAttach(docId , htmlTable , docId);
	    return new String( bytes);
	    /*
	    String couchEntryJSON = getDoc(docId , htmlTable);
	    if (couchEntryJSON != null)
	    {
	      CachedHtml rawDocument = null;
	      try
	      {
	    	InputStream input = new ByteArrayInputStream(couchEntryJSON.getBytes());

	    	//CouchdbEntry entry = (CouchdbEntry) entryTScope.deserialize(input,  Format.JSON);
	        //rawDocument = (CachedHtml) entry.getValue();
	      }
	      catch (SIMPLTranslationException e) {
	    	logger.error("Cannot deserialize raw document from " + htmlTable + "/" + docId, e);
		  }
	      return new String(Utils.base64urlDecode(rawDocument.getContent()));
	    }
	    else
	    {
	    	logger.error("Cannot retrive raw document from " + htmlTable + "/" + docId);
	    }
		*/
	  }

	  @Override
	  public boolean remove(PersistenceMetaInfo metaInfo)
	  {
	    String docId = metaInfo.getDocId();
	    int result1 = unCouch( docId , htmlTable);
	    int result2 = unCouch( docId , metaDataTable);
	    int result3 = unCouch( docId , metaInfoTable);
	    if ( result1 == 200 && result2 == 200 && result3 == 200)
	    	return true;
	    else
	    	return false;
	  }


	
	
	public static void main(String args[]) throws SIMPLTranslationException, IOException
	{
		
		SemanticsServiceScope sss = SemanticsServiceScope.get();
		
		/*
		SimplTypesScope tscope = sss.getMetadataTypesScope();
		Document doc = (Document) tscope.deserialize(new FileInputStream(new File("C:\\Users\\ZachB\\AppData\\Local\\Temp\\test-json7459270573011231817.txt")),
				new MetadataDeserializationHookStrategy(sss), Format.JSON);
		*/
		//System.out.println(doc.toString());
		
	    /*PrintWriter writer = null;
		try {
			writer = new PrintWriter("C:/Users/ZachB/Desktop/source.txt" , "UTF-8");
		    writer.println(rawContent);
		    System.out.println("HERE");
		    writer.close();
			System.out.println("Opened the file");
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		*/

		    
			CouchPersistentDocumentCache dc = new CouchPersistentDocumentCache();
			Document doc = dc.semanticsScope.getOrConstructDocument(ParsedURL.getAbsolute("http://www.amazon.com/Discovery-Daft-Punk/dp/B000069MEK"));
			
			dc.couchDoc("test", "html", doc);
			System.out.println(dc.couchAttach("test", "html", "HI", "text/plain", "trial"));
			

	}
	


	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	}

	
	
	
	
	
	