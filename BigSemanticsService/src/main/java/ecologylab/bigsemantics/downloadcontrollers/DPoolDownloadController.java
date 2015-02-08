package ecologylab.bigsemantics.downloadcontrollers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.collecting.SemanticsGlobalScope;
import ecologylab.bigsemantics.collecting.SemanticsSite;
import ecologylab.bigsemantics.dpool.DownloadTask;
import ecologylab.bigsemantics.dpool.MessageScope;
import ecologylab.bigsemantics.httpclient.HttpClientUtils;
import ecologylab.bigsemantics.httpclient.SimplHttpResponse;
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

  private static Logger       logger = LoggerFactory.getLogger(DPoolDownloadController.class);

  private static StringFormat format = StringFormat.JSON;

  private String              dpoolServiceUrl;

  private DocumentClosure     closure;

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
    setOriginalLocation(location);

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

    DownloadTask task = downloadPage(location, getUserAgent());
    SimplHttpResponse resp = task == null ? null : task.getResponse();
    if (resp == null)
    {
      logRecord.logPost().addEventNow(new DpoolServiceError("Null response"));
      logger.error("Failed to download {}: null result from downloadPage()", location);
      return false;
    }

    logRecord.logPost().addEvents(task.getLogPost());
    setHttpResponse(resp);

    if (resp.getCode() != HttpStatus.SC_OK)
    {
      logRecord.logPost().addEventNow(new DpoolServiceError("Failed to download " + location,
                                                            resp.getCode()));
      logger.error("Failed to download {}: {}", location, resp.getCode());
      return false;
    }
    return true;
  }

  private DownloadTask downloadPage(ParsedURL origLoc, String userAgentString)
  {
    Map<String, String> params = new HashMap<String, String>();
    params.put("url", origLoc.toString());
    params.put("agent", userAgentString);
    params.put("t", "60000");
    try
    {
      SimplHttpResponse dpoolResp = HttpClientUtils.doGet(userAgentString, dpoolServiceUrl, params);
      if (dpoolResp != null && dpoolResp.getCode() == HttpStatus.SC_OK)
      {
        String taskStr = dpoolResp.getContent();
        DownloadTask task =
            (DownloadTask) MessageScope.get().deserialize(taskStr, format);
        SimplHttpResponse httpResp = task.getResponse();
        String content = httpResp == null ? null : httpResp.getContent();
        logger.info("Received DPool result for {}: tid={}, state={}, content_len={}",
                    origLoc, task.getId(), task.getState(), content == null ? 0 : content.length());
        return task;
      }
      else
      {
        logger.error("DPool controller error status when downloading {}: {} {}",
                     origLoc, dpoolResp.getCode(), dpoolResp.getMessage());
      }
    }
    catch (SIMPLTranslationException e)
    {
      logger.error("Error deserializing DPool result for " + origLoc, e);
    }
    catch (Exception e)
    {
      logger.error("Error downloading " + origLoc + " using DPool!", e);
    }
    return null;
  }

  private static String TEST_STR = "TEST_STR";

  public static String pickDpoolServiceUrl(Integer port, String... dpoolHosts)
  {
    for (String dpoolHost : dpoolHosts)
    {
      logger.info("Trying dpool service at " + dpoolHost);
      String testLoc =
          "http://" + dpoolHost + ":" + port + "/DownloaderPool/echo/get?msg=" + TEST_STR;
      try
      {
        SimplHttpResponse resp = HttpClientUtils.doGet(null, testLoc, null);
        if (resp != null && resp.getCode() == HttpStatus.SC_OK)
        {
          String content = resp.getContent();
          if (content.contains(TEST_STR))
          {
            logger.info("Picked dpool service at " + dpoolHost);
            String fmt = null;
            switch (format)
            {
            case JSON:
              fmt = "json";
              break;
            case XML:
              fmt = "xml";
              break;
            default:
              logger.error("Format not supported: " + format);
              break;
            }
            return "http://" + dpoolHost + ":" + port + "/DownloaderPool/page/download." + fmt;
          }
        }
      }
      catch (Exception e)
      {
        logger.warn("Dpool service not reachable at: " + dpoolHost);
      }
    }
    logger.error("Cannot locate the DPool service!");
    return null;
  }

}
