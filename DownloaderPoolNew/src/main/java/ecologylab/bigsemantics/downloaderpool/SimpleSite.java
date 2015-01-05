package ecologylab.bigsemantics.downloaderpool;

import java.util.Random;

import ecologylab.concurrent.Site;
import ecologylab.net.ParsedURL;

/**
 * Represents a website and the waiting interval for this site. Records last downloading time and
 * number of errors encountered etc.
 * 
 * @author quyin
 */
public class SimpleSite implements Site
{

  private static final int TWELVE_HOURS_IN_MILLIS = 1000 * 60 * 60 * 12;

  private static Random    rand                   = new Random(System.currentTimeMillis());

  /**
   * The domain of this site.
   */
  private String           domain;

  /**
   * If this site should be ignored.
   */
  private boolean          ignored;

  /**
   * If this site is down.
   */
  private boolean          down;

  /**
   * If this site is being downloaded.
   */
  private boolean          downloading;

  /**
   * The last time this site has been downloaded.
   */
  private long             lastDownloadingTime;

  /**
   * The minimum interval between downloading requests to this site.
   */
  private long             downloadInterval;

  /**
   * The next time that this site will be available for downloading.
   */
  private long             nextAvailableTime;

  /**
   * Number of normal downloads for this site.
   */
  private int              cNormal;

  /**
   * Number of timed out downloads for this site.
   */
  private int              cTimeout;

  /**
   * Number of file-not-found downloads for this site.
   */
  private int              cNotFound;

  /**
   * Number of downloads with other errors for this site.
   */
  private int              cOther;

  public SimpleSite(String domain)
  {
    this.domain = domain;
  }

  @Override
  public String domain()
  {
    return domain;
  }

  @Override
  public boolean isIgnored()
  {
    return ignored;
  }

  @Override
  public void setIgnored(boolean ignored)
  {
    this.ignored = ignored;
  }

  @Override
  public boolean isDown()
  {
    return down;
  }

  @Override
  public boolean isDownloading()
  {
    return downloading;
  }

  @Override
  public long getLastDownloadAt()
  {
    return lastDownloadingTime;
  }

  @Override
  public boolean isDownloadingConstrained()
  {
    return downloadInterval > 0;
  }

  public long getDownloadInterval()
  {
    return downloadInterval;
  }

  protected synchronized void setDownloadInterval(long downloadInterval)
  {
    this.downloadInterval = downloadInterval;
  }

  @Override
  public long getDecentDownloadInterval()
  {
    if (isDownloadingConstrained())
    {
      return downloadInterval + rand.nextInt((int) downloadInterval / 2);
    }
    else
    {
      return 0;
    }
  }

  @Override
  public long getNextAvailableTime()
  {
    return nextAvailableTime;
  }

  @Override
  public void advanceNextAvailableTime()
  {
    if (isDownloadingConstrained())
    {
      nextAvailableTime = System.currentTimeMillis() + getDecentDownloadInterval();
    }
  }

  @Override
  public void setAbnormallyLongNextAvailableTime()
  {
    nextAvailableTime = System.currentTimeMillis() + TWELVE_HOURS_IN_MILLIS;
  }

  @Override
  public void queueDownload(ParsedURL location)
  {
    // no-op
  }

  @Override
  public void beginDownload(ParsedURL location)
  {
    lastDownloadingTime = System.currentTimeMillis();
    downloading = true;

  }

  @Override
  public void endDownload(ParsedURL location)
  {
    downloading = false;
  }

  @Override
  public void countNormalDownload(ParsedURL location)
  {
    cNormal++;
  }

  @Override
  public void countTimeout(ParsedURL location)
  {
    cTimeout++;
  }

  @Override
  public void countFileNotFound(ParsedURL location)
  {
    cNotFound++;
  }

  @Override
  public void countOtherIoError(ParsedURL location)
  {
    cOther++;
  }

  public int numOfNormalDownloads()
  {
    return cNormal;
  }

  public int numOfTimeouts()
  {
    return cTimeout;
  }

  public int numOfNotFounds()
  {
    return cNotFound;
  }

  public int numOfOtherIoError()
  {
    return cOther;
  }
  
  public String toString()
  {
    return this.getClass().getSimpleName() + "[" + domain + "]";
  }

}
