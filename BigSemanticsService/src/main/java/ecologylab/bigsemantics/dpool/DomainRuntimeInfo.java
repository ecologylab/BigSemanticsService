package ecologylab.bigsemantics.dpool;

import java.util.Random;

import ecologylab.concurrent.Site;
import ecologylab.serialization.annotations.simpl_composite;
import ecologylab.serialization.annotations.simpl_scalar;

/**
 * 
 * @author quyin
 */
public class DomainRuntimeInfo
{

  static Random      rand = new Random(System.currentTimeMillis());

  @simpl_composite
  private DomainInfo domainInfo;

  @simpl_scalar
  private long       currentDelay;

  @simpl_scalar
  private int        consecutiveFailures;

  @simpl_scalar
  private long       lastAccessTime;

  /**
   * for deserialization.
   */
  public DomainRuntimeInfo()
  {
    super();
  }

  public DomainRuntimeInfo(DomainInfo domainInfo)
  {
    this.domainInfo = domainInfo;
  }

  public DomainRuntimeInfo(Site site)
  {
    this();
    domainInfo = new DomainInfo(site.domain());
    domainInfo.setMinDelay(site.getDownloadInterval());
  }

  public DomainInfo getDomainInfo()
  {
    return domainInfo;
  }

  void setDomainInfo(DomainInfo domainInfo)
  {
    this.domainInfo = domainInfo;
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
    float minDelay = domainInfo.getMinDelay();
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
      int longDelayThreshold = domainInfo.getLongDelayThreshold();
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
    consecutiveFailures = domainInfo.getLongDelayThreshold();
    currentDelay = (int) (domainInfo.getLongDelay() * 1000);
  }

  public boolean canAccess()
  {
    return getCurrentTime() - lastAccessTime > currentDelay;
  }

}
