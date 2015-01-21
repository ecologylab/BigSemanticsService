package ecologylab.bigsemantics.documentcache;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.Utils;
import ecologylab.bigsemantics.collecting.SemanticsGlobalScope;
import ecologylab.bigsemantics.cyberneko.CybernekoWrapper;
import ecologylab.bigsemantics.generated.library.RepositoryMetadataTypesScope;
import ecologylab.bigsemantics.generated.library.primitives.CachedHtml;
import ecologylab.bigsemantics.generated.library.primitives.CouchdbEntry;
import ecologylab.bigsemantics.metadata.MetadataDeserializationHookStrategy;
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.bigsemantics.metadata.builtins.PersistenceMetaInfo;
import ecologylab.bigsemantics.service.SemanticsServiceConfigNames;
import ecologylab.bigsemantics.service.SemanticsServiceScope;
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

  private CouchInterface       couchInterface;

  public CouchPersistentDocumentCache(SemanticsGlobalScope semanticsScope)
  {
    this.semanticsScope = semanticsScope;
    entryTScope = semanticsScope.getMetadataTypesScope();
  }

  public void configure(Configuration config)
  {
    String databaseUrl = config.getString(COUCHDB_URL);
    couchInterface = new HttpCouchInterface(databaseUrl);
  }

  private String getDoc(String docId, String tableId)
  {
    String json = couchInterface.getDoc(docId, tableId);
    return json == null ? null : "{ \"couchdb_entry\" : " + json + " } ";
  }

  private byte[] getAttach(String docId, String tableId, String title)
  {
    byte[] bytes = couchInterface.getAttach(docId, tableId, title);
    return bytes;
  }

  private int couchDoc(String docId, String tableId, Object doc) throws SIMPLTranslationException
  {
    String valueJSON = SimplTypesScope.serialize(doc, StringFormat.JSON).toString();
    String docJSON = "{\"value\":" + valueJSON + "}";
    int code = couchInterface.putDoc(docId, docJSON, tableId);
    return code;
  }

  private int couchAttach(String docId,
                          String tableId,
                          String attachment,
                          String mimeType,
                          String contentTitle) throws SIMPLTranslationException
  {
    int code = couchInterface.putAttach(docId, tableId, attachment, mimeType, contentTitle);
    return code;
  }

  private int updateDoc(String docId, String tableId, Object metaInfo)
      throws SIMPLTranslationException
  {
    String valueJSON = SimplTypesScope.serialize(metaInfo, StringFormat.JSON).toString();
    String docJSON = "{ \"value\" : " + valueJSON + " }";
    int code = couchInterface.updateDoc(docId, docJSON, tableId);
    return code;
  }

  private int unCouch(String docId, String tableId)
  {
    int code = couchInterface.dropDoc(docId, tableId);
    return code;
  }

  private int couchOrOverWrite(String docId, String tableId, Object doc)
      throws SIMPLTranslationException
  {
    int result = couchDoc(docId, tableId, doc);
    if (result == 201 || result == 200)
    {
      return 200;
    }
    else
    {
      result = updateDoc(docId, tableId, doc);
      if (result == 200 || result == 201)
      {
        return 200;
      }
      else
      {
        return result;
      }
    }
  }

  @Override
  public PersistenceMetaInfo getMetaInfo(ParsedURL location)
  {
    String docId = Utils.getLocationHash(location);
    String couchEntryJson = getDoc(docId, metaInfoTable);

    if (couchEntryJson != null)
    {
      PersistenceMetaInfo metaInfo = null;
      try
      {
        CouchdbEntry entry =
            (CouchdbEntry) entryTScope.deserialize(couchEntryJson, StringFormat.JSON);
        metaInfo = (PersistenceMetaInfo) entry.getValue();
      }
      catch (SIMPLTranslationException e)
      {
        logger.error("Cannot deserialize metadata retrived from "
                     + "/"
                     + metaInfoTable
                     + "/"
                     + docId, e);
        return null;
      }
      return metaInfo;
    }
    else
    {
      logger.error("Failed to retrieve document from " + "/" + metaInfoTable + "/" + docId);
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

    int result;
    try
    {
      result = couchOrOverWrite(docId, htmlTable, cachedHtml);
      if (result != 200 && result != 201)
      {
        logger.error("Cannot store " + docId + " in " + htmlTable + " Error code " + result);
        return null;
      }

      result = couchAttach(docId, htmlTable, rawContent, mimeType, docId);
      if (result != 200 && result != 201)
      {
        logger.error("Cannot store attachment for "
                     + docId
                     + " in "
                     + htmlTable
                     + " Error code "
                     + result);
        return null;
      }

      result = couchOrOverWrite(docId, metaDataTable, document);
      if (result != 200 && result != 201)
      {
        logger.error("Cannot store " + docId + " in " + metaDataTable + " Error code " + result);
        return null;
      }
      result = couchOrOverWrite(docId, metaInfoTable, metaInfo);
      if (result != 200 && result != 201)
      {
        logger.error("Cannot store " + docId + " in " + metaInfoTable + " Error code " + result);
        return null;
      }
    }
    catch (SIMPLTranslationException e)
    {
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
        int result1 = updateDoc(docId, metaDataTable, newDoc);
        int result2 = updateDoc(docId, metaInfoTable, metaInfo);
        if (result1 != 200 || result2 != 200)
        {
          return false;
        }
        else
        {
          return true;
        }
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

    String couchEntryJson = getDoc(docId, metaDataTable);
    if (couchEntryJson != null)
    {
      Document document = null;
      try
      {
        DeserializationHookStrategy deserializationHookStrategy =
            new MetadataDeserializationHookStrategy(semanticsScope);
        CouchdbEntry entry = (CouchdbEntry) entryTScope.deserialize(couchEntryJson,
                                                                    deserializationHookStrategy,
                                                                    StringFormat.JSON);

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
    byte[] bytes = getAttach(docId, htmlTable, docId);
    return new String(bytes, Charset.forName("UTF-8"));
  }

  @Override
  public boolean remove(PersistenceMetaInfo metaInfo)
  {
    String docId = metaInfo.getDocId();
    int result1 = unCouch(docId, htmlTable);
    int result2 = unCouch(docId, metaDataTable);
    int result3 = unCouch(docId, metaInfoTable);
    if (result1 == 200 && result2 == 200 && result3 == 200)
    {
      return true;
    }
    else
    {
      return false;
    }
  }

  public static void main(String args[]) throws SIMPLTranslationException, IOException
  {
    SemanticsServiceScope sss =
        new SemanticsServiceScope(RepositoryMetadataTypesScope.get(), CybernekoWrapper.class);

    CouchPersistentDocumentCache dc = new CouchPersistentDocumentCache(sss);
    ParsedURL purl =
        ParsedURL.getAbsolute("http://www.amazon.com/Discovery-Daft-Punk/dp/B000069MEK");
    Document doc = sss.getOrConstructDocument(purl);

    dc.couchDoc("test", "html", doc);
    System.out.println(dc.couchAttach("test", "html", "HI", "text/plain", "trial"));
  }

}
