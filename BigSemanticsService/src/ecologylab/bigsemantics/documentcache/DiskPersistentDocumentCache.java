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

  private long                 repositoryVersion;

  private String               repositoryHash;

  public DiskPersistentDocumentCache(SemanticsGlobalScope semanticsScope)
  {
    metaTScope = SimplTypesScope.get("PersistenceMetadata", PersistenceMetadata.class);
    docTScope = RepositoryMetadataTranslationScope.get();
    this.semanticsScope = semanticsScope;
    repositoryHash = semanticsScope.getMetaMetadataRepositoryHash();
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
   * Write raw document when it doesn't exist or has expired.
   * 
   * @param rawDocument
   * @param metadata
   * @return True if raw document actually written, otherwise false.
   * @throws IOException
   */
  private boolean writeRawDocumentIfNeeded(String rawDocument, PersistenceMetadata metadata)
      throws IOException
  {
    File rawDocFile = getFilePath(rawDocDir, metadata.getDocId(), RAW_DOC_SUFFIX);
    if (!rawDocFile.exists())
    {
      // TODO Check for cache life.
      Utils.writeToFile(rawDocFile, rawDocument);
      return true;
    }
    return false;
  }

  /**
   * Write document when it doesn't exist or is stale.
   * 
   * @param document
   * @param metadata
   * @return True if docuemnt actually written, otherwise false.
   * @throws SIMPLTranslationException
   */
  private boolean writeDocumentIfNeeded(Document document, PersistenceMetadata metadata)
      throws SIMPLTranslationException
  {
    File docFile = getFilePath(docDir, metadata.getDocId(), DOC_SUFFIX);
    if (!docFile.exists() || !repositoryHash.equals(metadata.getRepositoryHash()))
    {
      metadata.setRepositoryVersion(repositoryVersion);
      metadata.setRepositoryHash(repositoryHash);
      SimplTypesScope.serialize(document, docFile, Format.XML);
      return true;
    }
    return false;
  }

  private void writeMetadata(PersistenceMetadata metadata) throws SIMPLTranslationException
  {
    File metadataFile = getFilePath(metadataDir, metadata.getDocId(), METADATA_SUFFIX);
    SimplTypesScope.serialize(metadata, metadataFile, Format.XML);
  }

  @Override
  public boolean store(Document document, String rawDocument, PersistenceMetadata pMetadata)
  {
    if (document == null || document.getLocation() == null)
    {
      return false;
    }

    ParsedURL purl = document.getLocation();
    String docId = getDocId(purl);
    Date now = new Date();

    PersistenceMetadata metadata = getMetadata(purl);
    if (metadata == null)
    {
      metadata = new PersistenceMetadata();
      metadata.setDocId(docId);
      metadata.setLocation(purl);
      metadata.setMimeType(pMetadata.getMimeType());
      metadata.setAccessTime(now);
      metadata.setPersistenceTime(now);
      metadata.setRepositoryVersion(repositoryVersion);
      metadata.setRepositoryHash(repositoryHash);
    }

    try
    {
      boolean update = false;
      update |= writeRawDocumentIfNeeded(rawDocument, metadata);
      update |= writeDocumentIfNeeded(document, metadata);
      if (update)
      {
        writeMetadata(metadata);
      }
      return true;
    }
    catch (Exception e)
    {
      logger.error("Cannot store " + document + ", doc_id=" + docId, e);
    }

    return false;
  }

  @Override
  public PersistenceMetadata getMetadata(String docId)
  {
    File metadataFile = getFilePath(metadataDir, docId, METADATA_SUFFIX);
    if (metadataFile.exists() && metadataFile.isFile())
    {
      PersistenceMetadata metadata = null;
      try
      {
        metadata = (PersistenceMetadata) metaTScope.deserialize(metadataFile, Format.XML);
      }
      catch (SIMPLTranslationException e)
      {
        logger.error("Cannot load metadata from " + metadataFile, e);
      }
      return metadata;
    }
    return null;
  }

  @Override
  public PersistenceMetadata getMetadata(ParsedURL location)
  {
    return getMetadata(getDocId(location));
  }

  @Override
  public Document retrieve(String docId)
  {
    if (getMetadata(docId) == null)
    {
      return null;
    }

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
  public Document retrieve(ParsedURL location)
  {
    return retrieve(getDocId(location));
  }

  @Override
  public String retrieveRaw(String docId)
  {
    if (getMetadata(docId) == null)
    {
      return null;
    }

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
  public String retrieveRaw(ParsedURL location)
  {
    return retrieveRaw(getDocId(location));
  }

  @Override
  public boolean remove(String docId)
  {
    File metadataFile = getFilePath(metadataDir, docId, METADATA_SUFFIX);
    File docFile = getFilePath(docDir, docId, DOC_SUFFIX);
    File rawDocFile = getFilePath(rawDocDir, docId, RAW_DOC_SUFFIX);
    return metadataFile.delete() && docFile.delete() && rawDocFile.delete();
  }

  @Override
  public boolean remove(ParsedURL location)
  {
    return remove(getDocId(location));
  }

}
