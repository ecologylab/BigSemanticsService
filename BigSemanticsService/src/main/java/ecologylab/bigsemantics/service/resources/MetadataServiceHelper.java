package ecologylab.bigsemantics.service.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.Utils;
import ecologylab.bigsemantics.collecting.DownloadStatus;
import ecologylab.bigsemantics.documentcache.PersistentDocumentCache;
import ecologylab.bigsemantics.exceptions.ProcessingUnfinished;
import ecologylab.bigsemantics.logging.MemoryCacheHit;
import ecologylab.bigsemantics.logging.MemoryCacheMiss;
import ecologylab.bigsemantics.logging.ServiceLogRecord;
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.bigsemantics.metadata.builtins.DocumentClosure;
import ecologylab.bigsemantics.metadata.builtins.PersistenceMetaInfo;
import ecologylab.bigsemantics.metametadata.MetaMetadata;
import ecologylab.bigsemantics.service.SemanticsServiceErrorMessages;
import ecologylab.bigsemantics.service.SemanticsServiceScope;
import ecologylab.generic.Debug;
import ecologylab.net.ParsedURL;
import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.SimplTypesScope;
import ecologylab.serialization.formatenums.StringFormat;

/**
 * helper class for metadata.xml and metadata.json
 * 
 * @author ajit
 * @author quyin
 */
public class MetadataServiceHelper extends Debug
    implements SemanticsServiceErrorMessages
{

  public static int     CONTINUATION_TIMOUT = 60000;

  private static Logger logger;

  private static Logger perfLogger;

  static
  {
    logger = LoggerFactory.getLogger(MetadataServiceHelper.class);
    perfLogger = LoggerFactory.getLogger("ecologylab.bigsemantics.service.PERF");
  }

  MetadataService       metadataService;

  SemanticsServiceScope semanticsServiceScope;

  ServiceLogRecord      logRecord;

  ParsedURL             docPurl;

  Document              document;

  DocumentClosure       closure;

  MetaMetadata          metaMetadata;

  String                errorMessage;

  public MetadataServiceHelper(MetadataService metadataService)
  {
    this.metadataService = metadataService;
    this.semanticsServiceScope = metadataService.semanticsServiceScope;
    this.logRecord = metadataService.logRecord;
  }

  public String serializeResultDocument(StringFormat format) throws SIMPLTranslationException
  {
    String result = SimplTypesScope.serialize(document, format).toString();
    return result;
  }

  /**
   * @return Status code.
   * @throws Exception
   */
  public int getMetadata() throws Exception
  {
    // initialize
    docPurl = metadataService.docPurl;
    document = semanticsServiceScope.getOrConstructDocument(docPurl);
    if (document == null)
    {
      throw new NullPointerException("WEIRD: Got null from SemanticsServiceScope for " + docPurl);
    }
    closure = document.getOrConstructClosure();
    if (closure == null)
    {
      throw new NullPointerException("DocumentClosure is null: " + document);
    }
    metaMetadata = (MetaMetadata) document.getMetaMetadata();
    if (metaMetadata == null)
    {
      throw new NullPointerException("MetaMetadata is null: " + document);
    }

    // deal with special flags
    boolean reload = metadataService.reload;
    boolean noCache = metaMetadata.isNoCache();
    if (reload || noCache)
    {
      prepForReload(docPurl, closure);
      return downloadAndFinish(reload);
    }

    // take actions based on download status
    DownloadStatus downloadStatus = document.getDownloadStatus();
    switch (downloadStatus)
    {
    case DOWNLOAD_DONE:
    {
      logger.info("{} found in service in-mem document cache", document);
      logRecord.logPost().addEventNow(new MemoryCacheHit());
      return 200;
    }
    case IOERROR:
    case RECYCLED:
    {
      prepForReload(docPurl, closure);
      // intentionally fall through
    }
    case UNPROCESSED:
    {
      logRecord.logPost().addEventNow(new MemoryCacheMiss());
      // intentionally fall through
    }
    case QUEUED:
    case CONNECTING:
    case PARSING:
    {
      return downloadAndFinish(false);
    }
    default:
    {
      errorMessage = "Unexpected closure download status: " + downloadStatus;
      logger.error(errorMessage);
      return 500;
    }
    }
  }

  private void prepForReload(ParsedURL docPurl, DocumentClosure closure)
  {
    document.resetRecycleStatus();
    removeFromLocalDocumentCollection(docPurl);
    removeFromPersistentDocumentCache(docPurl);
  }

  private int downloadAndFinish(boolean reload) throws Exception
  {
    int result = 500;

    document.setLogRecord(logRecord);
    logger.info("performing downloading on {}", document);
    DownloadStatus downloadStatus = closure.performDownloadSynchronously(reload, false);
    logger.info("resulting status of downloading {}: {}", document, downloadStatus);
    switch (downloadStatus)
    {
    case UNPROCESSED:
    case QUEUED:
    case CONNECTING:
    case PARSING:
    {
      throw new ProcessingUnfinished("Returned before finishing downloading and parsing: "
                                     + document + ", status: " + downloadStatus);
    }
    case DOWNLOAD_DONE:
    {
      Document newDoc = closure.getDocument();
      if (newDoc == null)
      {
        throw new NullPointerException("Null Document after downloading and parsing: " + docPurl);
      }
      if (document != newDoc)
      {
        logger.info("Remapping old {} to new {}", document, newDoc);
        semanticsServiceScope.getLocalDocumentCollection().addMapping(docPurl, newDoc);
      }
      document = newDoc;
      logger.info("{} downloaded and parsed.", document);
      result = 200;
      break;
    }
    case IOERROR:
    {
      errorMessage = "I/O error when downloading " + document;
      logger.warn(errorMessage);
      break;
    }
    case RECYCLED:
    {
      errorMessage = "Document is recycled after downloading and parsing: " + document;
      logger.warn(errorMessage);
      break;
    }
    }

    perfLogger.info(Utils.serializeToString(logRecord, StringFormat.JSON));
    return result;
  }

  /**
   * @param docPurl
   */
  private void removeFromLocalDocumentCollection(ParsedURL docPurl)
  {
    logger.debug("Removing document [{}] from service local document collection", docPurl);
    semanticsServiceScope.getLocalDocumentCollection().remove(docPurl);
  }

  /**
   * @param docPurl
   */
  private void removeFromPersistentDocumentCache(ParsedURL docPurl)
  {
    logger.debug("Removing document [{}] from persistent document caches", docPurl);
    PersistentDocumentCache pCache = semanticsServiceScope.getPersistentDocumentCache();
    try
    {
      PersistenceMetaInfo metaInfo = pCache.getMetaInfo(docPurl);
      if (metaInfo != null)
      {
        pCache.remove(metaInfo);
      }
    }
    catch (Exception e)
    {
      logger.error("Cannot remove from persistent doc cache: " + docPurl, e);
    }
  }

}
