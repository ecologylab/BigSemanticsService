package ecologylab.bigsemantics.documentcache;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.BaseEncoding;

import ecologylab.bigsemantics.Utils;
import ecologylab.bigsemantics.collecting.SemanticsGlobalScope;
import ecologylab.bigsemantics.generated.library.RepositoryMetadataTranslationScope;
import ecologylab.bigsemantics.metadata.MetadataDeserializationHookStrategy;
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.net.ParsedURL;
import ecologylab.serialization.DeserializationHookStrategy;
import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.SimplTypesScope;
import ecologylab.serialization.TranslationContext;
import ecologylab.serialization.TranslationContextPool;
import ecologylab.serialization.formatenums.Format;

/**
 * A persistent document cache that uses the disk.
 * 
 * @author quyin
 */
public class DiskPersistentDocumentCache implements PersistentDocumentCache<Document>
{

  private static final String  METADATA_SUFFIX = ".meta";

  private static final String  DOC_SUFFIX      = ".xml";

  private static final String  RAW_DOC_SUFFIX  = ".html";

  static Logger                logger;

  static
  {
    logger = LoggerFactory.getLogger(DiskPersistentDocumentCache.class);
  }

  private SimplTypesScope      metaTScope;

  private SimplTypesScope      docTScope;

  private SemanticsGlobalScope semanticsScope;

  private File                 metadataDir;

  private File                 rawDocDir;

  private File                 docDir;

  public DiskPersistentDocumentCache(SemanticsGlobalScope semanticsScope)
  {
    metaTScope = SimplTypesScope.get("PersistenceMetadata", PersistenceMetaInfo.class);
    docTScope = RepositoryMetadataTranslationScope.get();
    this.semanticsScope = semanticsScope;
  }

  public boolean configure(String cacheBaseDir)
  {
    cacheBaseDir = expandHomeDir(cacheBaseDir);
    logger.info("cache base dir: " + cacheBaseDir);
    File baseDir = new File(cacheBaseDir);
    if (mkdirsIfNeeded(baseDir))
    {
      logger.info("Cache directory: " + baseDir.getAbsolutePath());
      metadataDir = new File(baseDir, "metadata");
      rawDocDir = new File(baseDir, "raw");
      docDir = new File(baseDir, "semantics");
      return mkdirsIfNeeded(metadataDir) && mkdirsIfNeeded(rawDocDir) && mkdirsIfNeeded(docDir);
    }
    else
    {
      logger.warn("Cannot create cache directory at: " + baseDir);
    }
    return false;
  }

  static String expandHomeDir(String dir)
  {
    String homeDir = System.getProperty("user.home");
    return dir.replaceFirst("\\$HOME", homeDir.replace("\\", "/"));
  }

  /**
   * Do mkdirs(), but returns false only when cannot create those dirs.
   * 
   * @param dir
   * @return
   */
  private boolean mkdirsIfNeeded(File dir)
  {
    if (dir.exists() && dir.isDirectory())
    {
      return true;
    }
    return dir.mkdirs();
  }

  private static String getDocId(ParsedURL purl)
  {
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

  private File getFilePath(File dir, String name, String suffix)
  {
    File intermediateDir = new File(dir, name.substring(0, 2));
    intermediateDir.mkdirs();
    return new File(intermediateDir, name + suffix);
  }

  /**
   * Write raw document.
   * 
   * @param rawPageContent
   * @param metaInfo
   * @return True if raw document actually written, otherwise false.
   * @throws IOException
   */
  private boolean writeRawPageContent(String rawPageContent, PersistenceMetaInfo metaInfo)
      throws IOException
  {
    File rawDocFile = getFilePath(rawDocDir, metaInfo.getDocId(), RAW_DOC_SUFFIX);
    Utils.writeToFile(rawDocFile, rawPageContent);
    return true;
  }

  /**
   * Write document.
   * 
   * @param document
   * @param metaInfo
   * @return True if docuemnt actually written, otherwise false.
   * @throws SIMPLTranslationException
   */
  private boolean writeDocument(Document document, PersistenceMetaInfo metaInfo)
      throws SIMPLTranslationException
  {
    String currentHash = document.getMetaMetadata().getHashForExtraction();
    File docFile = getFilePath(docDir, metaInfo.getDocId(), DOC_SUFFIX);
    metaInfo.setMmdHash(currentHash);
    SimplTypesScope.serialize(document, docFile, Format.XML);
    return true;
  }

  private void writeMetaInfo(PersistenceMetaInfo metadata) throws SIMPLTranslationException
  {
    File metadataFile = getFilePath(metadataDir, metadata.getDocId(), METADATA_SUFFIX);
    SimplTypesScope.serialize(metadata, metadataFile, Format.XML);
  }

  @Override
  public PersistenceMetaInfo getMetaInfo(ParsedURL location)
  {
    String docId = getDocId(location);
    File metadataFile = getFilePath(metadataDir, docId, METADATA_SUFFIX);
    if (metadataFile.exists() && metadataFile.isFile())
    {
      PersistenceMetaInfo metaInfo = null;
      try
      {
        metaInfo = (PersistenceMetaInfo) metaTScope.deserialize(metadataFile, Format.XML);
      }
      catch (SIMPLTranslationException e)
      {
        logger.error("Cannot load metadata from " + metadataFile, e);
      }
      return metaInfo;
    }
    return null;
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

    try
    {
      writeRawPageContent(rawContent, metaInfo);
      writeDocument(document, metaInfo);
      writeMetaInfo(metaInfo);
      return metaInfo;
    }
    catch (Exception e)
    {
      logger.error("Cannot store " + document + ", doc_id=" + docId, e);
    }

    return null;
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
        writeDocument(newDoc, metaInfo);
        writeMetaInfo(metaInfo);
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
    File docFile = getFilePath(docDir, docId, DOC_SUFFIX);
    if (docFile.exists() && docFile.isFile())
    {
      TranslationContext translationContext = TranslationContextPool.get().acquire();
      DeserializationHookStrategy deserializationHookStrategy =
          new MetadataDeserializationHookStrategy(semanticsScope);
      Document document = null;
      try
      {
        document = (Document) docTScope.deserialize(docFile,
                                                    translationContext,
                                                    deserializationHookStrategy,
                                                    Format.XML);
      }
      catch (SIMPLTranslationException e)
      {
        logger.error("Cannot load document from " + docFile, e);
      }
      finally
      {
        TranslationContextPool.get().release(translationContext);
      }
      return document;
    }

    return null;
  }

  @Override
  public String retrieveRawContent(PersistenceMetaInfo metaInfo)
  {
    String docId = metaInfo.getDocId();
    File rawDocFile = getFilePath(rawDocDir, docId, RAW_DOC_SUFFIX);
    if (rawDocFile.exists() && rawDocFile.isFile())
    {
      String rawDoc = null;
      try
      {
        rawDoc = Utils.readInputStream(new FileInputStream(rawDocFile));
      }
      catch (IOException e)
      {
        logger.error("Cannot load raw document from " + rawDocFile, e);
      }
      return rawDoc;
    }

    return null;
  }

  @Override
  public boolean remove(PersistenceMetaInfo metaInfo)
  {
    String docId = metaInfo.getDocId();
    File metadataFile = getFilePath(metadataDir, docId, METADATA_SUFFIX);
    File docFile = getFilePath(docDir, docId, DOC_SUFFIX);
    File rawDocFile = getFilePath(rawDocDir, docId, RAW_DOC_SUFFIX);
    return metadataFile.delete() && docFile.delete() && rawDocFile.delete();
  }

}
