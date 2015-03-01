package ecologylab.bigsemantics.documentcache;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;

import org.apache.commons.configuration.Configuration;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.Utils;
import ecologylab.bigsemantics.collecting.SemanticsGlobalScope;
import ecologylab.bigsemantics.generated.library.primitives.CachedHtml;
import ecologylab.bigsemantics.generated.library.primitives.CouchdbEntry;
import ecologylab.bigsemantics.metadata.MetadataDeserializationHookStrategy;
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.bigsemantics.metadata.builtins.PersistenceMetaInfo;
import ecologylab.bigsemantics.service.SemanticsServiceConfigNames;
import ecologylab.net.ParsedURL;
import ecologylab.serialization.DeserializationHookStrategy;
import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.SimplTypesScope;
import ecologylab.serialization.formatenums.StringFormat;

/**
 * A persistent document cache that uses the disk.
 * 
 * @author zach
 */
public class CouchPersistentDocumentCache
    implements PersistentDocumentCache<Document>, SemanticsServiceConfigNames
{

  private static final String  htmlTable     = "html";

  private static final String  metaDataTable = "metadata";

  private static final String  metaInfoTable = "metainfo";

  static Logger                logger;

  static
  {
    logger = LoggerFactory.getLogger(CouchPersistentDocumentCache.class);
  }

  private SimplTypesScope      entryTScope;

  private SemanticsGlobalScope semanticsScope;

  private CouchAccessor        couchInterface;

  public CouchPersistentDocumentCache(SemanticsGlobalScope semanticsScope)
  {
    this.semanticsScope = semanticsScope;
    entryTScope = semanticsScope.getMetadataTypesScope();
  }

  @Override
  public void configure(Configuration config)
  {
    String databaseUrl = config.getString(COUCHDB_URL);
    couchInterface = new CouchHttpAccessor(databaseUrl);
  }

  private String getDoc(String docId, String tableId)
      throws ParseException, CouchAccessorException, IOException
  {
    String json = couchInterface.getDoc(docId, tableId);
    return json == null ? null : "{ \"couchdb_entry\" : " + json + " } ";
  }

  private byte[] getAttach(String docId, String tableId, String title)
  {
    byte[] bytes = couchInterface.getAttach(docId, tableId, title);
    return bytes;
  }

  private boolean couchDoc(String docId, String tableId, Object doc)
      throws SIMPLTranslationException, ParseException, IOException, CouchAccessorException
  {
    String valueJSON = SimplTypesScope.serialize(doc, StringFormat.JSON).toString();
    String docJSON = "{\"value\":" + valueJSON + "}";
    boolean result = couchInterface.putDoc(docId, docJSON, tableId);
    return result;
  }

  private boolean couchAttach(String docId,
                              String tableId,
                              String attachment,
                              String mimeType,
                              String contentTitle)
      throws SIMPLTranslationException, ClientProtocolException, IOException,
      CouchAccessorException
  {
    boolean result = couchInterface.putAttach(docId, tableId, attachment, mimeType, contentTitle);
    return result;
  }

  private boolean updateDoc(String docId, String tableId, Object doc)
      throws SIMPLTranslationException, ClientProtocolException, IOException,
      CouchAccessorException
  {
    String valueJSON = SimplTypesScope.serialize(doc, StringFormat.JSON).toString();
    String docJSON = "{ \"value\" : " + valueJSON + " }";
    boolean result = couchInterface.updateDoc(docId, docJSON, tableId);
    return result;
  }

  private boolean unCouch(String docId, String tableId)
      throws ClientProtocolException, CouchAccessorException, IOException
  {
    boolean code = couchInterface.dropDoc(docId, tableId);
    return code;
  }

  private void couchOrOverWrite(String docId, String tableId, Object doc)
      throws SIMPLTranslationException, ParseException, IOException, CouchAccessorException
  {
    boolean result = couchDoc(docId, tableId, doc);
    if (result)
    {
      return;
    }
    else
    {
      result = updateDoc(docId, tableId, doc);
      if (result)
      {
        return;
      }
      else
      // There should never be a case where you can't create, or update a document
      {
        throw new CouchAccessorException("Failed to couchOrOverWrite", 0, "", tableId, docId);
      }
    }
  }

  @Override
  public PersistenceMetaInfo getMetaInfo(ParsedURL location)
      throws CouchAccessorException, ParseException, IOException, SIMPLTranslationException
  {
    String docId = Utils.getLocationHash(location);
    String couchEntryJson = getDoc(docId, metaInfoTable);

    if (couchEntryJson != null)
    {
      PersistenceMetaInfo metaInfo = null;

      CouchdbEntry entry =
          (CouchdbEntry) entryTScope.deserialize(couchEntryJson, StringFormat.JSON);
      metaInfo = (PersistenceMetaInfo) entry.getValue();
      return metaInfo;
    }
    else
    {
      return null;
    }

  }

  @Override
  public PersistenceMetaInfo store(Document document,
                                   String rawContent,
                                   String charset,
                                   String mimeType,
                                   String mmdHash)
      throws ParseException, IOException, CouchAccessorException, SIMPLTranslationException
  {
    if (document == null || document.getLocation() == null)
    {
      return null;
    }

    ParsedURL location = document.getLocation();
    String docId = Utils.getLocationHash(location);

    CachedHtml cachedHtml = new CachedHtml();
    // rawContent = Utils.base64urlEncode(rawContent.getBytes());

    // cachedHtml.setContent(rawContent);
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

    couchOrOverWrite(docId, htmlTable, cachedHtml);
    couchAttach(docId, htmlTable, rawContent, mimeType, docId);
    couchOrOverWrite(docId, metaDataTable, document);
    couchOrOverWrite(docId, metaInfoTable, metaInfo);

    return metaInfo;
  }

  @Override
  public boolean updateDoc(PersistenceMetaInfo metaInfo, Document newDoc)
      throws ClientProtocolException, IOException, CouchAccessorException,
      SIMPLTranslationException
  {
    if (newDoc != null && newDoc.getLocation() != null)
    {
      metaInfo.setPersistenceTime(new Date());
      metaInfo.setMmdHash(newDoc.getMetaMetadata().getHashForExtraction());

      String docId = metaInfo.getLocation().toString();
      boolean result1 = updateDoc(docId, metaDataTable, newDoc);
      boolean result2 = updateDoc(docId, metaInfoTable, metaInfo);
      if (!result1 || !result2)
      {
        return false;
      }
      else
      {
        return true;
      }

    }

    return false;
  }

  @Override
  public Document retrieveDoc(PersistenceMetaInfo metaInfo)
      throws ParseException, CouchAccessorException, IOException, SIMPLTranslationException
  {
    String docId = metaInfo.getDocId();

    String couchEntryJson = getDoc(docId, metaDataTable);
    if (couchEntryJson != null)
    {
      Document document = null;
      DeserializationHookStrategy deserializationHookStrategy =
          new MetadataDeserializationHookStrategy(semanticsScope);
      CouchdbEntry entry = (CouchdbEntry) entryTScope.deserialize(couchEntryJson,
                                                                  deserializationHookStrategy,
                                                                  StringFormat.JSON);

      document = (Document) entry.getValue();
      return document;
    }
    else
    {
      logger.error("Cannot retrive document from " + metaDataTable + "/" + docId);
      return null;
    }

  }

  @Override
  public String retrieveRawContent(PersistenceMetaInfo metaInfo)
  {
    String docId = metaInfo.getDocId();
    byte[] bytes = getAttach(docId, htmlTable, docId);
    return new String(bytes, Charset.forName("UTF-8"));
  }

  @Override
  public boolean remove(PersistenceMetaInfo metaInfo)
      throws ClientProtocolException, CouchAccessorException, IOException
  {
    String docId = metaInfo.getDocId();
    boolean result1 = unCouch(docId, htmlTable);
    boolean result2 = unCouch(docId, metaDataTable);
    boolean result3 = unCouch(docId, metaInfoTable);
    if (result1 && result2 && result3)
    {
      return true;
    }
    else
    {
      return false;
    }
  }

}
