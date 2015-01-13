package ecologylab.bigsemantics.service.metadata;

import java.io.IOException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.Utils;
import ecologylab.bigsemantics.collecting.DownloadStatus;
import ecologylab.bigsemantics.documentcache.PersistentDocumentCache;
import ecologylab.bigsemantics.exceptions.DocumentRecycled;
import ecologylab.bigsemantics.exceptions.ProcessingUnfinished;
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.bigsemantics.metadata.builtins.DocumentClosure;
import ecologylab.bigsemantics.metadata.builtins.PersistenceMetaInfo;
import ecologylab.bigsemantics.metametadata.MetaMetadata;
import ecologylab.bigsemantics.service.SemanticsServiceErrorMessages;
import ecologylab.bigsemantics.service.SemanticsServiceScope;
import ecologylab.bigsemantics.service.logging.ServiceLogRecord;
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

  ServiceLogRecord      serviceLogRecord;

  Document              document;

  String                errorMessage;

  public MetadataServiceHelper(MetadataService metadataService)
  {
    this.metadataService = metadataService;
    this.semanticsServiceScope = metadataService.semanticsServiceScope;
    this.serviceLogRecord = new ServiceLogRecord();
    this.document = semanticsServiceScope.getOrConstructDocument(metadataService.docPurl);
    if (this.document == null)
    {
      throw new NullPointerException("Null Document returned from SemanticsServiceScope!");
    }
    logger.info("{} returned from SemanticsServiceScope.", this.document);
    document.setLogRecord(this.serviceLogRecord);
    this.serviceLogRecord.setRequesterIp(metadataService.clientIp);
    this.serviceLogRecord.setRequestUrl(metadataService.docPurl);
  }

  public String serializeResultDocument(StringFormat format) throws SIMPLTranslationException
  {
    long t1 = System.currentTimeMillis();
    String result = SimplTypesScope.serialize(document, format).toString();
    serviceLogRecord.setMsSerialization(System.currentTimeMillis() - t1);
    return result;
  }

  /**
   * The entry method that accepts a URL and returns a Response with extracted metadata.
   * 
   * @param purl
   * @param format
   * @param reload
   * @return
   * @throws DocumentRecycled
   * @throws IOException
   * @throws ProcessingUnfinished
   */
  public int getMetadata() throws DocumentRecycled, IOException, ProcessingUnfinished
  {
    long t0 = System.currentTimeMillis();
    serviceLogRecord.setBeginTime(new Date(t0));

    ParsedURL docPurl = metadataService.docPurl;
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
      serviceLogRecord.setInMemDocumentCacheHit(true);
    }

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

    document = null;
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
      serviceLogRecord.setMsTotal(System.currentTimeMillis() - t0);
      break;
    case IOERROR:
      errorMessage = "I/O error when downloading " + document;
      return 404;
    case RECYCLED:
      throw new DocumentRecycled("Document is recycled after downloading and parsing: " + document);
    }

    perfLogger.info(Utils.serializeToString(serviceLogRecord, StringFormat.JSON));

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
    PersistenceMetaInfo metaInfo = pCache.getMetaInfo(docPurl);
    if (metaInfo != null)
    {
      pCache.remove(metaInfo);
    }
  }

}
