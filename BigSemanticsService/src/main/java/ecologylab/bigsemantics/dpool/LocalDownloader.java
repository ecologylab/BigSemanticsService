package ecologylab.bigsemantics.dpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.httpclient.HttpClientUtils;
import ecologylab.bigsemantics.httpclient.SimplHttpResponse;
import ecologylab.serialization.annotations.simpl_inherit;

/**
 * A downloader that does downloading locally.
 * 
 * @author quyin
 */
@simpl_inherit
public class LocalDownloader extends Downloader
{

  static Logger logger = LoggerFactory.getLogger(LocalDownloader.class);

  /**
   * For deserialization only.
   */
  public LocalDownloader()
  {
    super();
  }

  public LocalDownloader(String id, int numThreads)
  {
    super(id, numThreads);
  }

  public LocalDownloader(String id, int numThreads, int priority)
  {
    super(id, numThreads, priority);
  }

  @Override
  public int doPerformDownload(DownloadTask task) throws Exception
  {
    String userAgent = task.getUserAgent();
    String url = task.getUrl();
    SimplHttpResponse resp = HttpClientUtils.doGet(userAgent, url, null);
    task.setResponse(resp);
    return resp.getCode();
  }

}
