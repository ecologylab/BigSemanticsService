package ecologylab.bigsemantics.dpool;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.serialization.annotations.simpl_composite;
import ecologylab.serialization.annotations.simpl_scalar;

/**
 * 
 * @author quyin
 */
public class DomainRuntimeInfo
{

  static final Logger logger = LoggerFactory.getLogger(DomainRuntimeInfo.class);

  static Random       rand   = new Random(System.currentTimeMillis());

  @simpl_composite
  private DomainInfo  domainInfo;

  @simpl_scalar
  private long        currentDelay;

  @simpl_scalar
  private int         consecutiveFailures;

  @simpl_scalar
  private long        lastAccessTime;

  private boolean     holdingToken;

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

  public boolean isHoldingToken()
  {
    return holdingToken;
  }

  public synchronized void beginAccess()
  {
    // no op
  }

  /**
   * 
   * @param httpResponseCode
   *          The HTTP response code, or 0 if a non-HTTP-related error happened.
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
    if (getCurrentTime() - lastAccessTime <= currentDelay)
    {
      return false;
    }

    if (holdingToken)
    {
      // if this downloader is holding the token, release it, and give next downloader a chance.
      domainInfo.releaseToken();
      holdingToken = false;
      logger.debug("[{}] \t token released", domainInfo.getDomain());
      return false;
    }
    else
    {
      // if this downloader is not holding the token, try to get it and do the work.
      holdingToken = domainInfo.acquireToken();
      if (holdingToken)
      {
        logger.debug("[{}] \t token acquired", domainInfo.getDomain());
      }
      return holdingToken;
    }
  }

  @Override
  public String toString()
  {
    return DomainRuntimeInfo.class.getSimpleName() + "[" + domainInfo.getDomain() + "]";
  }

}
