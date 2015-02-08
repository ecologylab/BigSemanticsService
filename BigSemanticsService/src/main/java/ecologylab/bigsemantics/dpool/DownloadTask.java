package ecologylab.bigsemantics.dpool;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.distributed.Task;
import ecologylab.bigsemantics.httpclient.SimplHttpResponse;
import ecologylab.logging.LogPost;
import ecologylab.net.ParsedURL;
import ecologylab.serialization.annotations.Hint;
import ecologylab.serialization.annotations.simpl_composite;
import ecologylab.serialization.annotations.simpl_hints;
import ecologylab.serialization.annotations.simpl_scalar;

/**
 * The representation of a downloading task. This representation is transferred from clients to the
 * controller and then to distributed downloaders, for communication.
 * 
 * @author quyin
 * 
 */
public class DownloadTask extends Task
{

  private static final int  DEFAULT_ATTEMPT_TIME = 60 * 1000;

  public static final int   DEFAULT_MAX_ATTEMPTS = 1;

  private static Logger     logger               = LoggerFactory.getLogger(DownloadTask.class);

  /**
   * The URL to download.
   */
  @simpl_scalar
  @simpl_hints(Hint.XML_LEAF)
  private String            url;

  /**
   * The user agent string that should be used.
   */
  @simpl_scalar
  private String            userAgent;

  /**
   * Maximum number of attempts that are allowed. In each attempt, the task is assigned to a
   * downloader for accessing and downloading. If more than this number of attempts have been made
   * and it didn't succeed, the task is seen as undoable, thus terminated.
   */
  @simpl_scalar
  private int               maxAttempts          = DEFAULT_MAX_ATTEMPTS;

  /**
   * The time for each attempt, in millisecond.
   */
  @simpl_scalar
  private int               attemptTime          = DEFAULT_ATTEMPT_TIME;

  /**
   * Some websites returns an error page with status code 200. This regex helps detect such cases,
   * where the pattern will be searched in the downloaded page (in HTML) to determine if it is an
   * error page.
   */
  @simpl_scalar
  private String            failRegex;

  /**
   * Similar to failRegex, but specifically to detect a page that says we are banned from the site
   * :(, so that we can back from the site.
   */
  @simpl_scalar
  private String            banRegex;

  @simpl_composite
  private SimplHttpResponse response;

  @simpl_composite
  private LogPost           logPost;

  /**
   * The same as uri, for convenience.
   */
  private ParsedURL         purl;

  private Pattern           pFail;

  private Pattern           pBan;

  private int               attempts;

  private boolean           banned;

  private Downloader        downloader;

  /**
   * (for simpl)
   * 
   * @throws DpoolException
   */
  public DownloadTask() throws DpoolException
  {
    this(null, null);
  }

  public DownloadTask(String id, String url) throws DpoolException
  {
    this(id, url, 0);
  }

  public DownloadTask(String id, String url, int priority) throws DpoolException
  {
    super(id, priority);
    setUrl(url);
    this.logPost = new LogPost();
    logger.info("Task created: id={}, url={}", id, url);
  }

  public String getUrl()
  {
    return url;
  }

  public void setUrl(String url) throws DpoolException
  {
    if (url != null)
    {
      if (!url.matches("\\w+://.*"))
      {
        url = "http://" + url;
      }
      url = url.replace(" ", "%20");
      purl = ParsedURL.getAbsolute(url);
      if (purl == null)
      {
        throw new DpoolException("Cannot create ParsedURL from " + url);
      }
    }
    this.url = url;
  }

  public String getUserAgent()
  {
    return userAgent;
  }

  public void setUserAgent(String userAgent)
  {
    this.userAgent = userAgent;
  }

  public int getMaxAttempts()
  {
    return maxAttempts;
  }

  public void setMaxAttempts(int attempts)
  {
    this.maxAttempts = attempts;
  }

  public int getAttemptTime()
  {
    return attemptTime;
  }

  public void setAttemptTime(int attemptTime)
  {
    this.attemptTime = attemptTime;
  }

  public String getFailRegex()
  {
    return failRegex;
  }

  public Pattern getFailPattern()
  {
    if (pFail == null && failRegex != null)
    {
      pFail = Pattern.compile(failRegex);
    }
    return pFail;
  }

  public void setFailRegex(String failRegex)
  {
    this.failRegex = failRegex;
    this.getFailPattern();
  }

  public String getBanRegex()
  {
    return banRegex;
  }

  public Pattern getBanPattern()
  {
    if (pBan == null && banRegex != null)
    {
      pBan = Pattern.compile(banRegex);
    }
    return pBan;
  }

  public void setBanRegex(String banRegex)
  {
    this.banRegex = banRegex;
    this.getBanPattern();
  }

  public SimplHttpResponse getResponse()
  {
    return response;
  }

  protected void setResponse(SimplHttpResponse response)
  {
    this.response = response;
  }

  public LogPost getLogPost()
  {
    return logPost;
  }

  public ParsedURL getPurl()
  {
    if (purl == null && url != null)
    {
      purl = ParsedURL.getAbsolute(url);
    }
    return purl;
  }

  public String getDomain()
  {
    ParsedURL purl = getPurl();
    return purl.domain();
  }

  public int getAttempts()
  {
    return attempts;
  }

  protected void setAttempts(int attempts)
  {
    this.attempts = attempts;
  }

  public boolean isBanned()
  {
    return banned;
  }

  protected void setBanned(boolean banned)
  {
    this.banned = banned;
  }

  public String toString()
  {
    return String.format("%s[%s](%s)", DownloadTask.class.getSimpleName(), getId(), getState());
  }

  public Object clone()
  {
    try
    {
      return super.clone();
    }
    catch (CloneNotSupportedException e)
    {
      logger.error("Cannot clone task: " + this, e);
    }
    return this;
  }

  public void setDownloader(Downloader downloader)
  {
    this.downloader = downloader;
  }

  /**
   * Note that this will happen in an individual thread, so no need to create a new thread.
   */
  @Override
  public boolean perform() throws Exception
  {
    return downloader.performDownload(this);
  }

}
