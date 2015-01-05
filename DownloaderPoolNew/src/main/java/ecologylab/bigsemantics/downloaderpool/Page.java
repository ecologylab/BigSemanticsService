package ecologylab.bigsemantics.downloaderpool;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.AbstractHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.downloaderpool.DownloaderResult.State;
import ecologylab.bigsemantics.httpclient.HttpClientFactory;
import ecologylab.bigsemantics.httpclient.ModifiedHttpClientUtils;
import ecologylab.concurrent.Downloadable;
import ecologylab.concurrent.DownloadableLogRecord;
import ecologylab.concurrent.Site;
import ecologylab.net.ParsedURL;

/**
 * Represent a page to download. It is the actual class that the DownloadMonitor works with.
 * 
 * @author quyin
 */
public class Page implements Downloadable
{

  private static Logger    logger = LoggerFactory.getLogger(Page.class);

  // collaborating objects:

  HttpClientFactory        clientFactory;

  SimpleSiteTable          siteTable;

  // properties:

  private ParsedURL        location;

  private String           userAgent;

  private DownloaderResult result;

  public Page(String taskId, ParsedURL location, String userAgent)
  {
    this.location = location;
    this.userAgent = userAgent;
    result = new DownloaderResult();
    result.setTaskId(taskId);
  }

  @Override
  public ParsedURL location()
  {
    return location;
  }

  @Override
  public ParsedURL getDownloadLocation()
  {
    return location;
  }

  public void setDownloadLocation(ParsedURL location)
  {
    this.location = location;
  }

  @Override
  public Site getSite()
  {
    if (location != null)
    {
      String domain = location.domain();
      return siteTable.getSite(domain);
    }
    return null;
  }

  @Override
  public Site getDownloadSite()
  {
    return getSite();
  }

  @Override
  public DownloadableLogRecord getLogRecord()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void performDownload()
  {
    AbstractHttpClient client = null;
    if (userAgent != null && userAgent.length() > 0)
    {
      client = clientFactory.get(userAgent);
    }
    else
    {
      client = clientFactory.get();
    }
    PageResponseHandler handler = new PageResponseHandler(result);
    PageRedirectStrategy redirectStrategy = new PageRedirectStrategy(result);
    client.setRedirectStrategy(redirectStrategy);

    HttpGet httpGet = ModifiedHttpClientUtils.generateGetRequest(location.toString(), null);
    try
    {
      client.execute(httpGet, handler);
      result.setState(State.OK);
    }
    catch (ClientProtocolException e)
    {
      logger.error("Exception when connecting to " + location, e);
      result.setState(State.ERR_PROTOCOL);
      httpGet.abort();
    }
    catch (IOException e)
    {
      logger.error("Exception when reading from " + location, e);
      result.setState(State.ERR_IO);
      httpGet.abort();
    }
    finally
    {
      httpGet.releaseConnection();
    }
  }

  public DownloaderResult getResult()
  {
    return result;
  }

  @Override
  public void handleIoError(Throwable e)
  {
    logger.error("I/O error occurred!", e);
  }

  @Override
  public boolean isCached()
  {
    return false;
  }

  @Override
  public boolean isImage()
  {
    // TODO determine if it is image using cues such as suffix, mime-type, etc.
    return false;
  }

  @Override
  public boolean isRecycled()
  {
    return false;
  }

  @Override
  public void recycle()
  {
    // TODO recycle mechanism for Pages.
  }

  @Override
  public String message()
  {
    return toString();
  }

  public String toString()
  {
    return String.format("%s[%s, result=%s]", this.getClass().getSimpleName(), location, result);
  }

}
