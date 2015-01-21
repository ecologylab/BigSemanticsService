package ecologylab.bigsemantics.downloadcontrollers;

import java.io.IOException;
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
import ecologylab.bigsemantics.downloaderpool.BasicResponse;
import ecologylab.bigsemantics.downloaderpool.DownloaderResult;
import ecologylab.bigsemantics.downloaderpool.MessageScope;
import ecologylab.bigsemantics.httpclient.BasicResponseHandler;
import ecologylab.bigsemantics.httpclient.HttpClientFactory;
import ecologylab.bigsemantics.httpclient.ModifiedHttpClientUtils;
import ecologylab.bigsemantics.logging.DocumentLogRecord;
import ecologylab.bigsemantics.logging.DpoolServiceError;
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.bigsemantics.metadata.builtins.DocumentClosure;
import ecologylab.net.ParsedURL;
import ecologylab.serialization.SIMPLTranslationException;
import ecologylab.serialization.formatenums.StringFormat;

/**
 * Connect to the DPool service for downloading a web page.
 * 
 * @author quyin
 */
public class DPoolDownloadController extends AbstractDownloadController
{

  private static Logger            logger;

  private static HttpClientFactory httpClientFactory;

  static
  {
    logger = LoggerFactory.getLogger(DPoolDownloadController.class);
    httpClientFactory = new HttpClientFactory();
  }

  public static int                HTTP_DOWNLOAD_REQUEST_TIMEOUT = 60000;

  private String                   dpoolServiceUrl;

  private DocumentClosure          closure;

  private DownloaderResult         result;

  public DPoolDownloadController(String dpoolServiceUrl)
  {
    this.dpoolServiceUrl = dpoolServiceUrl;
  }

  public void setDocumentClosure(DocumentClosure closure)
  {
    this.closure = closure;
  }

  @Override
  public boolean accessAndDownload(ParsedURL location) throws IOException
  {
    setLocation(location);

    Document document = closure.getDocument();
    SemanticsGlobalScope semanticScope = document.getSemanticsScope();
    SemanticsSite site = document.getSite();

    if (location.isFile())
    {
      logger.error("File URL: " + location);
      return false;
    }
    setIsGood(doDownload(semanticScope, location, site));
    return isGood();
  }

  private boolean doDownload(SemanticsGlobalScope semanticScope,
                             ParsedURL location,
                             SemanticsSite site)
  {
    // init log record
    DocumentLogRecord logRecord = closure.getLogRecord();

    result = downloadPage(site, location, getUserAgent());

    if (result == null)
    {
      logRecord.logPost().addEventNow(new DpoolServiceError("Null response"));
      logger.error("Failed to download {}: null result from downloadPage()", location);
      return false;
    }

    logRecord.logPost().addEvents(result.getLogPost());

    if (result.getHttpRespCode() == HttpStatus.SC_OK)
    {
      setLocation(ParsedURL.getAbsolute(result.getRequestedUrl()));

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
      
      setCharset(result.getCharset());
      setMimeType(result.getMimeType());
      setStatus(result.getHttpRespCode());
      setStatusMessage(result.getHttpRespMsg());

      setContent(result.getContent());

      return true;
    }
    else
    {
      logRecord.logPost().addEventNow(new DpoolServiceError("Failed to download " + location,
                                      result.getHttpRespCode()));
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
    addRedirectedLocation(redirectedLocation);
    Document newDocument = semanticScope.getOrConstructDocument(redirectedLocation);
    newDocument.addAdditionalLocation(originalPurl);
    documentClosure.changeDocument(newDocument);
  }

  @Override
  public String getHeader(String name)
  {
    throw new RuntimeException("Not implemented.");
  }

  private static String TEST_STR = "TEST_STR";

  public static String pickDpoolServiceUrl(Integer port, String... dpoolHosts)
  {
    BasicResponseHandler handler = new BasicResponseHandler();
    for (String dpoolHost : dpoolHosts)
    {
      logger.info("Trying dpool service at " + dpoolHost);
      String testLoc =
          "http://" + dpoolHost + ":" + port + "/DownloaderPool/echo/get?msg=" + TEST_STR;
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
            logger.info("Picked dpool service at " + dpoolHost);
            return "http://" + dpoolHost + ":" + port + "/DownloaderPool/page/download.xml";
          }
        }
      }
      catch (Throwable t)
      {
        logger.warn("Dpool service not reachable at: " + dpoolHost);
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
