package ecologylab.bigsemantics.dpool;

import java.util.Random;

import ecologylab.concurrent.Site;
import ecologylab.serialization.annotations.simpl_scalar;

/**
 * 
 * @author quyin
 */
public class DomainInfo
{

  public static final int DEFAULT_LONG_DELAY           = 5000;

  public static final int DEFAULT_LONG_DELAY_THRESHOLD = 3;

  static Random           rand                         = new Random(System.currentTimeMillis());

  @simpl_scalar
  private String          domain;

  @simpl_scalar
  private float           minDelay;

  /**
   * Max number of consecutive failures before doing long delays.
   */
  @simpl_scalar
  private int             longDelayThreshold           = DEFAULT_LONG_DELAY_THRESHOLD;

  @simpl_scalar
  private float           longDelay                    = DEFAULT_LONG_DELAY;

  private long            currentDelay;

  private int             consecutiveFailures;

  private long            lastAccessTime;

  /**
   * for deserialization.
   */
  public DomainInfo()
  {
    super();
  }

  public DomainInfo(String domain)
  {
    this.domain = domain;
  }

  public DomainInfo(Site site)
  {
    this(site.domain());
    setMinDelay(site.getDownloadInterval());
  }

  public String getDomain()
  {
    return domain;
  }

  void setDomain(String domain)
  {
    this.domain = domain;
  }

  public float getMinDelay()
  {
    return minDelay;
  }

  public void setMinDelay(float minDelay)
  {
    this.minDelay = minDelay;
  }

  public int getLongDelayThreshold()
  {
    return longDelayThreshold;
  }

  public void setLongDelayThreshold(int longDelayThreshold)
  {
    this.longDelayThreshold = longDelayThreshold;
  }

  public float getLongDelay()
  {
    return longDelay > minDelay ? longDelay : minDelay;
  }

  public void setLongDelay(float longDelay)
  {
    this.longDelay = longDelay;
  }

  public long getCurrentDelay()
  {
    return currentDelay;
  }

  public int getConsecutiveFailures()
  {
    return consecutiveFailures;
  }

  public long getLastAccessTime()
  {
    return lastAccessTime;
  }

  protected long getCurrentTime()
  {
    return System.currentTimeMillis();
  }

  public long getDecentDelay()
  {
    if (minDelay > 0)
    {
      long delay = (long) (minDelay * 1000);
      int halfDelay = (int) (delay / 2);
      if (halfDelay > 0)
      {
        return delay + rand.nextInt(halfDelay);
      }
      return delay;
    }
    return 0;
  }

  public synchronized void beginAccess()
  {
    // no op
  }

  /**
   * 
   * @param httpResponseCode
   *          The HTTP response code, or 6XX if a non-HTTP-related error happened.
   */
  public synchronized void endAccess(int httpResponseCode)
  {
    if (httpResponseCode < 500)
    {
      consecutiveFailures = 0;
      currentDelay = getDecentDelay();
    }
    else
    {
      consecutiveFailures++;
      if (consecutiveFailures < longDelayThreshold)
      {
        currentDelay = getDecentDelay();
      }
      if (consecutiveFailures == longDelayThreshold)
      {
        setBanned();
      }
      else if (consecutiveFailures > longDelayThreshold)
      {
        currentDelay *= 2;
      }
    }
    lastAccessTime = getCurrentTime();
  }

  public void setBanned()
  {
    consecutiveFailures = longDelayThreshold;
    currentDelay = (int) (getLongDelay() * 1000);
  }

  public boolean canAccess()
  {
    return getCurrentTime() - lastAccessTime > currentDelay;
  }

}
