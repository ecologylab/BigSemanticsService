package ecologylab.bigsemantics.downloadcontrollers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.AbstractHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.collecting.SemanticsGlobalScope;
import ecologylab.bigsemantics.collecting.SemanticsSite;
import ecologylab.bigsemantics.documentcache.DiskPersistentDocumentCache;
import ecologylab.bigsemantics.documentcache.PersistenceMetadata;
import ecologylab.bigsemantics.documentcache.PersistentDocumentCache;
import ecologylab.bigsemantics.downloadcontrollers.DownloadController;
import ecologylab.bigsemantics.downloaderpool.BasicResponse;
import ecologylab.bigsemantics.downloaderpool.DownloaderResult;
import ecologylab.bigsemantics.downloaderpool.MessageScope;
import ecologylab.bigsemantics.httpclient.BasicResponseHandler;
import ecologylab.bigsemantics.httpclient.HttpClientFactory;
import ecologylab.bigsemantics.httpclient.ModifiedHttpClientUtils;
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.bigsemantics.metadata.builtins.DocumentClosure;
import ecologylab.bigsemantics.service.logging.DpoolServiceError;
import ecologylab.bigsemantics.service.logging.ServiceLogRecord;
import ecologylab.concurrent.DownloadableLogRecord;
import ecologylab.net.ParsedURL;
import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.formatenums.StringFormat;

/**
 * Connect to the DPool service for downloading a web page.
 * 
 * @author quyin
 */
public class DPoolDownloadController implements DownloadController
{

  private static Logger            logger;

  private static HttpClientFactory httpClientFactory;

  static
  {
    logger = LoggerFactory.getLogger(DPoolDownloadController.class);
    httpClientFactory = new HttpClientFactory();
  }

  public static int                HTTP_DOWNLOAD_REQUEST_TIMEOUT = 60000;

  private SemanticsGlobalScope     semanticsScope;

  private String                   dpoolServiceUrl;

  private String                   userAgent;

  private DocumentClosure          closure;

  private ParsedURL                location;

  private List<ParsedURL>          redirectedLocations;

  private DownloaderResult         result;

  private String                   content;

  private String                   mimeType;

  private boolean                  good;

  private InputStream              inputStream;

  public DPoolDownloadController(SemanticsGlobalScope semanticsScope, String dpoolServiceUrl)
  {
    this.semanticsScope = semanticsScope;
    this.dpoolServiceUrl = dpoolServiceUrl;
  }

  public void setDocumentClosure(DocumentClosure closure)
  {
    this.closure = closure;
  }

  @Override
  public void setUserAgent(String userAgent)
  {
    this.userAgent = userAgent;
  }

  @Override
  public boolean accessAndDownload(ParsedURL location) throws IOException
  {
    this.location = location;

    Document document = closure.getDocument();
    SemanticsGlobalScope semanticScope = document.getSemanticsScope();
    SemanticsSite site = document.getSite();

    ServiceLogRecord logRecord = ServiceLogRecord.DUMMY;
    DownloadableLogRecord downloadableLogRecord = closure.getLogRecord();
    if (downloadableLogRecord instanceof ServiceLogRecord)
    {
      logRecord = (ServiceLogRecord) downloadableLogRecord;
    }
    logRecord.setId(DiskPersistentDocumentCache.getDocId(location.toString()));

    PersistentDocumentCache pCache = semanticsScope.getPersistentDocumentCache();
    if (location.isFile())
    {
      logger.error("File URL: " + location);
      return false;
    }
    else
    {
      long t0Lookup = System.currentTimeMillis();
      PersistenceMetadata pMetadata = pCache.getMetadata(location);
      content = pCache.retrieveRaw(location);
      logRecord.setMsPageCacheLookup(System.currentTimeMillis() - t0Lookup);

      if (pMetadata == null || content == null)
      {
        if (!doDownload(semanticScope, location, site, logRecord))
        {
          return false;
        }
      }
      else
      {
        // cached:
        logger.info("Cached: " + location);
        logRecord.setHtmlCacheHit(true);

        redirectedLocations = pMetadata.getAdditionalLocations();
        if (redirectedLocations != null)
        {
          for (ParsedURL redirect : redirectedLocations)
          {
            handleRedirectLocation(semanticScope, closure, location, redirect);
          }
        }

        mimeType = pMetadata.getMimeType();
      }
    }

    if (content != null)
    {
      if (inputStream == null)
      {
        inputStream = new ByteArrayInputStream(content.getBytes());
      }
      good = true;
    }

    return good;
  }

  private boolean doDownload(SemanticsGlobalScope semanticScope,
                             ParsedURL location,
                             SemanticsSite site,
                             ServiceLogRecord logRecord)
  {
    // not cached, network download:
    logger.info("Not cached: " + location);
    logRecord.setHtmlCacheHit(false);

    result = downloadPage(site, location, userAgent);

    DpoolServiceError e = new DpoolServiceError();

    if (result == null)
    {
      e.setMessage("Dpool service null response.");
      logRecord.logPost().addEventNow(e);
      logger.error("Failed to download {}: null result from downloadPage()", location);
      return false;
    }

    logRecord.logPost().addEvents(result.getLogPost());

    if (result.getHttpRespCode() == HttpStatus.SC_OK)
    {
      content = result.getContent();

      // handle other locations (e.g. redirects)
      List<String> otherLocations = result.getOtherLocations();
      if (otherLocations != null)
      {
        for (String otherLocation : otherLocations)
        {
          ParsedURL redirectedLocation = ParsedURL.getAbsolute(otherLocation);
          handleRedirectLocation(semanticScope, closure, location, redirectedLocation);
        }
      }

      return true;
    }
    else
    {
      e.setMessage("Dpool service failed to download " + location);
      logRecord.logPost().addEventNow(e);
      logger.error("Failed to download {}: {}", location, result.getHttpRespCode());
      return false;
    }
  }

  private DownloaderResult downloadPage(SemanticsSite site,
                                        ParsedURL origLoc,
                                        String userAgentString)
  {
    Map<String, String> params = new HashMap<String, String>();
    params.put("url", origLoc.toString());
    params.put("agent", userAgentString);
    params.put("int", String.valueOf((int) (site.getMinDownloadInterval() * 1000)));
    params.put("natt", "3");
    params.put("tatt", "60000");
    HttpGet get = ModifiedHttpClientUtils.generateGetRequest(dpoolServiceUrl, params);

    AbstractHttpClient client = httpClientFactory.get();
    client.getParams().setParameter("http.connection.timeout", HTTP_DOWNLOAD_REQUEST_TIMEOUT);
    BasicResponseHandler handler = new BasicResponseHandler();
    try
    {
      client.execute(get, handler);
    }
    catch (IOException e)
    {
      logger.error("Error downloading " + origLoc + " using DPool!", e);
    }
    finally
    {
      get.releaseConnection();
    }

    BasicResponse resp = handler.getResponse();
    if (resp != null && resp.getHttpRespCode() == HttpStatus.SC_OK)
    {
      String resultStr = resp.getContent();
      DownloaderResult result = null;
      try
      {
        result = (DownloaderResult) MessageScope.get().deserialize(resultStr, StringFormat.XML);
        assert result != null : "Deserialization results in null!";
        String content = result.getContent();
        logger.info("Received DPool result for {}: tid={}, state={}, status={}, content_len={}",
                    origLoc,
                    result.getTaskId(),
                    result.getState(),
                    result.getHttpRespCode(),
                    content == null ? 0 : content.length());
      }
      catch (SIMPLTranslationException e)
      {
        logger.error("Error deserializing DPool result for " + origLoc, e);
      }
      return result;
    }
    else
    {
      logger.error("DPool controller error status when downloading {}: {} {}",
                   origLoc,
                   resp.getHttpRespCode(),
                   resp.getHttpRespMsg());
    }

    return null;
  }

  private void handleRedirectLocation(SemanticsGlobalScope semanticScope,
                                      DocumentClosure documentClosure,
                                      ParsedURL originalPurl,
                                      ParsedURL redirectedLocation)
  {
    redirectedLocations().add(redirectedLocation);
    Document newDocument = semanticScope.getOrConstructDocument(redirectedLocation);
    newDocument.addAdditionalLocation(originalPurl);
    documentClosure.changeDocument(newDocument);
  }

  @Override
  public boolean isGood()
  {
    return good;
  }

  @Override
  public int getStatus()
  {
    return result == null ? -1 : result.getHttpRespCode();
  }

  @Override
  public String getStatusMessage()
  {
    return result == null ? null : result.getHttpRespMsg();
  }

  @Override
  public ParsedURL getLocation()
  {
    return location;
  }

  private List<ParsedURL> redirectedLocations()
  {
    if (redirectedLocations == null)
    {
      redirectedLocations = new ArrayList<ParsedURL>();
    }
    return redirectedLocations;
  }

  @Override
  public List<ParsedURL> getRedirectedLocations()
  {
    return redirectedLocations;
  }

  @Override
  public String getMimeType()
  {
    if (mimeType != null)
    {
      return mimeType;
    }
    return result == null ? null : result.getMimeType();
  }

  @Override
  public String getCharset()
  {
    return result == null ? null : result.getCharset();
  }

  @Override
  public String getHeader(String name)
  {
    throw new RuntimeException("Not implemented.");
  }

  @Override
  public InputStream getInputStream()
  {
    return isGood() ? inputStream : null;
  }

  @Override
  public String getContent() throws IOException
  {
    return isGood() ? content : null;
  }

  private static String TEST_STR = "TEST_STR";

  public static String pickDpoolServiceUrl(String... dpoolServices)
  {
    BasicResponseHandler handler = new BasicResponseHandler();
    for (String dpoolService : dpoolServices)
    {
      logger.info("Trying dpool service at " + dpoolService);
      String testLoc = dpoolService.replace("page/download.xml", "echo/get?msg=" + TEST_STR);
      AbstractHttpClient client = httpClientFactory.get();
      HttpGet get = new HttpGet(testLoc);
      try
      {
        client.execute(get, handler);
        BasicResponse resp = handler.getResponse();
        if (resp != null && resp.getHttpRespCode() == HttpStatus.SC_OK)
        {
          String content = resp.getContent();
          if (content.contains(TEST_STR))
          {
            logger.info("Picked dpool service at " + dpoolService);
            return dpoolService;
          }
        }
      }
      catch (Throwable t)
      {
        logger.warn("Dpool service not reachable at: " + dpoolService);
      }
      finally
      {
        get.releaseConnection();
      }
    }
    logger.error("Cannot locate the DPool service!");
    return null;
  }

}
