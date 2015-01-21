package ecologylab.bigsemantics.service.resources;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.Utils;
import ecologylab.bigsemantics.collecting.DownloadStatus;
import ecologylab.bigsemantics.documentcache.PersistentDocumentCache;
import ecologylab.bigsemantics.exceptions.DocumentRecycled;
import ecologylab.bigsemantics.exceptions.ProcessingUnfinished;
import ecologylab.bigsemantics.logging.MemoryCacheHit;
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

  Document              document;

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
    ParsedURL docPurl = metadataService.docPurl;
    document = semanticsServiceScope.getOrConstructDocument(docPurl);
    if (document == null)
    {
      throw new NullPointerException("WEIRD: Null Document returned from SemanticsServiceScope!");
    }
    logger.info("{} returned from SemanticsServiceScope.", document);

    boolean reload = metadataService.reload;
    MetaMetadata mmd = (MetaMetadata) document.getMetaMetadata();
    boolean noCache = mmd.isNoCache();

    DownloadStatus docStatus = document.getDownloadStatus();
    boolean errorBefore =
        docStatus == DownloadStatus.RECYCLED || docStatus == DownloadStatus.IOERROR;
    if (reload || noCache || errorBefore)
    {
      document.resetRecycleStatus();
      removeFromLocalDocumentCollection(docPurl);
      removeFromPersistentDocumentCache(docPurl);
    }

    docStatus = document.getDownloadStatus();
    if (docStatus == DownloadStatus.DOWNLOAD_DONE)
    {
      logger.info("{} found in service in-mem document cache", document);
      logRecord.logPost().addEventNow(new MemoryCacheHit());
    }

    document.setLogRecord(logRecord);

    DocumentClosure closure = document.getOrConstructClosure();
    if (closure == null)
    {
      throw new NullPointerException("DocumentClosure is null: " + document);
    }
    if (reload || noCache)
    {
      closure.setReload(true);
    }

    download(closure);

    if (document == null)
    {
      throw new NullPointerException("Null Document after downloading and parsing: " + docPurl);
    }

    docStatus = document.getDownloadStatus();
    switch (docStatus)
    {
    case UNPROCESSED:
    case QUEUED:
    case CONNECTING:
    case PARSING:
      throw new ProcessingUnfinished("Returned before finishing downloading and parsing: "
                                     + document + ", status: " + docStatus);
    case DOWNLOAD_DONE:
      logger.info("{} downloaded and parsed.", document);
      break;
    case IOERROR:
      errorMessage = "I/O error when downloading " + document;
      return 404;
    case RECYCLED:
      throw new DocumentRecycled("Document is recycled after downloading and parsing: " + document);
    }

    perfLogger.info(Utils.serializeToString(logRecord, StringFormat.JSON));

    return 200;
  }

  private void download(DocumentClosure closure) throws IOException
  {
    logger.info("performing downloading on {}", document);

    closure.performDownloadSynchronously();
    Document newDoc = closure.getDocument();
    logger.info("download status of {}: {}", document, closure.getDownloadStatus());
    if (document != null && document != newDoc)
    {
      logger.info("Remapping old {} to new {}", document, newDoc);
      semanticsServiceScope.getLocalDocumentCollection().remap(document, newDoc);
      document = newDoc;
    }
    if (closure.getDownloadStatus() == DownloadStatus.IOERROR)
    {
      logger.warn("I/O error when downloading {}", document);
    }
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
