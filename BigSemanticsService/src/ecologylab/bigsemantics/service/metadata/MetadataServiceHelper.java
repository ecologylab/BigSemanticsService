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
import ecologylab.bigsemantics.documentcache.PersistenceMetaInfo;
import ecologylab.bigsemantics.documentcache.PersistentDocumentCache;
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.bigsemantics.metadata.builtins.DocumentClosure;
import ecologylab.bigsemantics.metametadata.MetaMetadata;
import ecologylab.bigsemantics.service.SemanticServiceErrorMessages;
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
 */
public class MetadataServiceHelper extends Debug
    implements SemanticServiceErrorMessages
{

  public static int                    CONTINUATION_TIMOUT = 60000;

  private static Logger                logger;

  private static Logger                perfLogger;

  private static SemanticsServiceScope semanticsServiceScope;

  static
  {
    logger = LoggerFactory.getLogger(MetadataServiceHelper.class);
    perfLogger = LoggerFactory.getLogger("ecologylab.bigsemantics.service.PERF");
    semanticsServiceScope = SemanticsServiceScope.get();
  }

  private Document                     document;

  private ServiceLogRecord             perfLogRecord;

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
  public Response getMetadataResponse(String requesterIp,
                                      ParsedURL purl,
                                      StringFormat format,
                                      boolean reload)
  {
    perfLogRecord.setRequesterIp(requesterIp);
    perfLogRecord.setRequestUrl(purl);
    perfLogRecord.setBeginTime(new Date());
    Response resp = null;

    document = null;
    getMetadata(purl, reload);
    if (document == null)
    {
      logger.error("Can't construct Document for [{}]", purl);
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
        logger.error("Unfinished {}, status: {}", document, docStatus);
        break;
      case DOWNLOAD_DONE:
        try
        {
          logger.info("{} downloaded and parsed, generating response", document);
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
        logger.error("Bad Document status for [{}]: {}", purl, docStatus);
        removeFromLocalDocumentCollection(purl);
        break;
      }
    }

    if (resp == null)
    {
      String respBody = "<error>" + INTERNAL_ERROR + "</error>";
      if (document != null)
      {
        try
        {
          respBody = SimplTypesScope.serialize(document, StringFormat.XML).toString();
        }
        catch (SIMPLTranslationException e)
        {
          logger.error("Exception while serializing " + document, e);
        }
      }
      resp = Response
          .status(Status.INTERNAL_SERVER_ERROR)
          .entity(respBody)
          .type(MediaType.APPLICATION_XML)
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
    if (document == null)
    {
      logger.error("Null Document returned from the semantics scope!");
      return null;
    }

    ParsedURL docPurl = document.getLocation();
    perfLogRecord.setDocumentUrl(document.getLocation());
    if (!docPurl.equals(purl))
    {
      logger.info("Normalizing {} to {}", purl, docPurl);
    }

    DownloadStatus docStatus = document.getDownloadStatus();
    logger.debug("Download status of {}: {}", document, docStatus);
    if (!reload && docStatus == DownloadStatus.DOWNLOAD_DONE)
    {
      logger.info("{} found in service in-mem document cache", document);
      perfLogRecord.setInMemDocumentCacheHit(true);
    }

    // take actions based on the status of the document
    DocumentClosure closure = document.getOrConstructClosure();
    if (closure == null && docStatus != DownloadStatus.DOWNLOAD_DONE)
    {
      logger.error("DocumentClosure is null for " + purl);
      return null;
    }
    switch (docStatus)
    {
    case UNPROCESSED:
    case QUEUED:
    case CONNECTING:
    case PARSING:
      download(closure);
      break;
    case IOERROR:
    case RECYCLED:
      reload = true;
      // intentionally fall through the next case.
      // the idea is: when the document is in state IOERROR or RECYCLED, it should be reloaded.
    case DOWNLOAD_DONE:
      if (reload)
      {
        logger.info("Reloading {}", document);
        removeFromPersistentDocumentCache(docPurl);
        // redownload and parse document
        document.resetRecycleStatus();
        closure.reset();
        download(closure);
      }
      break;
    }

    // if no_cache is set, remove the document from local document collection
    MetaMetadata mmd = (MetaMetadata) document.getMetaMetadata();
    if (mmd.isNoCache())
    {
      logger.info("Meta-metadata specified no_cache, purging {}", document);
      removeFromLocalDocumentCollection(purl);
      removeFromLocalDocumentCollection(docPurl);
    }

    return this.document;
  }

  private void download(DocumentClosure closure)
  {
    try
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
    catch (IOException e)
    {
      logger.error("Error in downloading " + document, e);
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
