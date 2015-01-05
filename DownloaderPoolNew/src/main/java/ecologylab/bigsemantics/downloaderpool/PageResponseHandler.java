package ecologylab.bigsemantics.downloaderpool;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.downloaderpool.DownloaderResult.State;
import ecologylab.bigsemantics.httpclient.BasicResponseHandler;

/**
 * Extracts useful info from HttpResponse and fills them into the DwonloaderResult object for this
 * download.
 * 
 * @author quyin
 */
public class PageResponseHandler extends BasicResponseHandler
{

  public PageResponseHandler(DownloaderResult result)
  {
    super(result);
  }

  @Override
  public DownloaderResult handleResponse(HttpResponse resp)
  {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    DownloaderResult result = (DownloaderResult) super.result;
    result.setState(State.OK);
    try
    {
      super.handleResponse(resp);
    }
    catch (ParseException e)
    {
      logger.error("Exception when parsing content from " + result.getRequestedUrl(), e);
      result.setState(State.ERR_CONTENT);
    }
    catch (IOException e)
    {
      logger.error("Exception when reading from " + result.getRequestedUrl(), e);
      result.setState(State.ERR_IO);
    }

    // TODO anything else?

    return result;
  }

}
