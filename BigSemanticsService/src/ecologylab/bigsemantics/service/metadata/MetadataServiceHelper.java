package ecologylab.bigsemantics.service.metadata;

import java.io.IOException;
import java.util.Date;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.Utils;
import ecologylab.bigsemantics.collecting.DownloadStatus;
import ecologylab.bigsemantics.filestorage.FileSystemStorage;
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.bigsemantics.metadata.builtins.DocumentClosure;
import ecologylab.bigsemantics.metametadata.MetaMetadata;
import ecologylab.bigsemantics.service.SemanticServiceErrorMessages;
import ecologylab.bigsemantics.service.SemanticServiceScope;
import ecologylab.bigsemantics.service.downloader.controller.DPoolDownloadControllerFactory;
import ecologylab.bigsemantics.service.logging.ServiceLogRecord;
import ecologylab.generic.Continuation;
import ecologylab.generic.Debug;
import ecologylab.net.ParsedURL;
import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.SimplTypesScope;
import ecologylab.serialization.formatenums.StringFormat;

/**
 * helper class for metadata.xml and metadata.json
 * 
 * @author ajit
 */
public class MetadataServiceHelper extends Debug
    implements Continuation<DocumentClosure>, SemanticServiceErrorMessages
{

  public static int                   CONTINUATION_TIMOUT = 60000;

  private static Logger               logger;

  private static Logger               perfLogger;

  private static SemanticServiceScope semanticsServiceScope;

  static
  {
    logger = LoggerFactory.getLogger(MetadataServiceHelper.class);
    perfLogger = LoggerFactory.getLogger("ecologylab.bigsemantics.service.PERF");
    semanticsServiceScope = SemanticServiceScope.get();
  }

  private Document                    document;

  private ServiceLogRecord            perfLogRecord;

  private Object                      sigDownloadDone     = new Object();

  public MetadataServiceHelper()
  {
    this.perfLogRecord = new ServiceLogRecord();
  }

  ServiceLogRecord getServiceLogRecord()
  {
    return perfLogRecord;
  }

  /**
   * The entry method that accepts a URL and returns a Response with extracted metadata.
   * 
   * @param purl
   * @param format
   * @param reload
   * @return
   */
  public Response getMetadataResponse(ParsedURL purl, StringFormat format, boolean reload)
  {
    perfLogRecord.setBeginTime(new Date());
    perfLogRecord.setRequestUrl(purl);
    Response resp = null;

    document = null;
    getMetadata(purl, reload);
    if (document == null)
    {
      logger.error("Can't construct Document for [%s]", purl);
      resp = Response
          .status(Status.NOT_FOUND)
          .entity(METADATA_NOT_FOUND)
          .type(MediaType.TEXT_PLAIN)
          .build();
    }
    else
    {
      DownloadStatus docStatus = document.getDownloadStatus();
      switch (docStatus)
      {
      case UNPROCESSED:
      case QUEUED:
      case CONNECTING:
      case PARSING:
        logger.error("Unfinished %s, status: %s", document, docStatus);
        break;
      case DOWNLOAD_DONE:
        try
        {
          long t0 = System.currentTimeMillis();
          String responseBody = SimplTypesScope.serialize(document, format).toString();
          perfLogRecord.setMsSerialization(System.currentTimeMillis() - t0);
          resp = Response.status(Status.OK).entity(responseBody).build();
        }
        catch (SIMPLTranslationException e)
        {
          logger.error("Exception while serializing " + document, e);
        }
        break;
      case IOERROR:
      case RECYCLED:
        logger.error("Bad Document status for [%s]: %s", purl, docStatus);
        break;
      }
    }

    if (resp == null)
    {
      resp = Response
          .status(Status.INTERNAL_SERVER_ERROR)
          .entity(INTERNAL_ERROR)
          .type(MediaType.TEXT_PLAIN)
          .build();
    }

    perfLogRecord.setMsTotal(System.currentTimeMillis() - perfLogRecord.getBeginTime().getTime());
    perfLogRecord.setResponseCode(resp.getStatus());
    perfLogger.info(Utils.serializeToString(perfLogRecord, StringFormat.JSON));

    return resp;
  }

  Document getMetadata(ParsedURL purl, boolean reload)
  {
    document = semanticsServiceScope.getOrConstructDocument(purl);
    assert document != null : "Null Document returned from the semantics scope!";

    ParsedURL docPurl = document.getLocation();
    perfLogRecord.setDocumentUrl(document.getLocation());
    if (!docPurl.equals(purl))
    {
      logger.info("Normalizing %s to %s", purl, docPurl);
    }

    DownloadStatus docStatus = document.getDownloadStatus();
    logger.debug("Download status of %s: %s", document, docStatus);
    if (docStatus == DownloadStatus.DOWNLOAD_DONE)
    {
      logger.info("%s found in service in-mem document cache", document);
      perfLogRecord.setInMemDocumentCacheHit(true);
    }

    // take actions based on the status of the document
    DocumentClosure closure = document.getOrConstructClosure(new DPoolDownloadControllerFactory());
    switch (docStatus)
    {
    case UNPROCESSED:
      download(closure);
      break;
    case QUEUED:
    case CONNECTING:
    case PARSING:
      addCallbackAndWaitForDownloadDone(closure);
      break;
    case IOERROR:
    case RECYCLED:
      reload = true;
      // intentionally fall through the next case.
      // the idea is: when the document is in state IOERROR or RECYCLED, it should be reloaded.
    case DOWNLOAD_DONE:
      if (reload)
      {
        removeFromPersistentDocumentCache(docPurl);
        // redownload and parse document
        closure.resetDownloadStatus();
        download(closure);
      }
      break;
    }

    // if no_cache is set, remove the document from local document collection
    MetaMetadata mmd = (MetaMetadata) document.getMetaMetadata();
    if (mmd.isNoCache())
    {
      removeFromLocalDocumentCollection(purl);
      removeFromLocalDocumentCollection(docPurl);
    }

    return this.document;
  }

  private void download(DocumentClosure closure)
  {
    try
    {
      synchronized (closure)
      {
        closure.performDownload();
      }

      if (closure.getDownloadStatus() == DownloadStatus.QUEUED
          || closure.getDownloadStatus() == DownloadStatus.CONNECTING
          || closure.getDownloadStatus() == DownloadStatus.PARSING)
      {
        logger.error("Closure status is not download done after calling performDownload()!");
      }
      else
      {
        callback(closure);
      }
    }
    catch (IOException e)
    {
      logger.error("Error in downloading " + document, e);
    }
  }

  private void addCallbackAndWaitForDownloadDone(DocumentClosure closure)
  {
    if (closure.addContinuationBeforeDownloadDone(this))
    {
      synchronized (sigDownloadDone)
      {
        try
        {
          sigDownloadDone.wait(CONTINUATION_TIMOUT);
        }
        catch (InterruptedException e)
        {
          logger.debug("Waiting for download done interrupted.");
        }
      }
    }
    else
    {
      callback(closure);
    }
  }

  @Override
  public synchronized void callback(DocumentClosure closure)
  {
    Document newDoc = closure.getDocument();
    if (document != null && document != newDoc)
    {
      logger.info("Remapping old %s to new %s", document, newDoc);
      semanticsServiceScope.getLocalDocumentCollection().remap(document, newDoc);
    }
    document = newDoc;

    synchronized (sigDownloadDone)
    {
      sigDownloadDone.notifyAll();
    }
  }

  /**
   * @param docPurl
   */
  private void removeFromLocalDocumentCollection(ParsedURL docPurl)
  {
    logger.debug("removing document [%s] from service local document collection", docPurl);
    semanticsServiceScope.getLocalDocumentCollection().remove(docPurl);
  }

  /**
   * @param docPurl
   */
  private void removeFromPersistentDocumentCache(ParsedURL docPurl)
  {
    logger.debug("removing document [%s] from persistent document caches", docPurl);
    semanticsServiceScope.getPersistentDocumentCache().removeDocument(docPurl);
    FileSystemStorage.getStorageProvider().removeFileAndMetadata(docPurl);
  }

}
