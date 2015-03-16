package ecologylab.bigsemantics.dpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.concurrent.Site;
import ecologylab.serialization.annotations.simpl_scalar;

/**
 * 
 * @author quyin
 */
public class DomainInfo
{

  public static final String  DEFAULT_DOMAIN               = "DEFAULT";

  private static final Logger logger;

  static
  {
    logger = LoggerFactory.getLogger(DomainInfo.class);
  }

  public static final int     DEFAULT_LONG_DELAY           = 5000;

  public static final int     DEFAULT_LONG_DELAY_THRESHOLD = 3;

  @simpl_scalar
  private String              domain;

  @simpl_scalar
  private float               minDelay;

  /**
   * Max number of consecutive failures before doing long delays.
   */
  @simpl_scalar
  private int                 longDelayThreshold;

  @simpl_scalar
  private float               longDelay;

  /**
   * One token for a downloading thread.
   */
  @simpl_scalar
  private int                 maxTokens;

  /**
   * we only want to serialize this for monitoring purposes.
   */
  @simpl_scalar
  private int                 numTokens;

  private Object              tokenLock                    = new Object();

  public DomainInfo()
  {
    this((String) null);
  }

  public DomainInfo(String domain)
  {
    this.domain = domain;
  }

  public DomainInfo(Site site)
  {
    this(site.domain());
    setMinDelay(site.getDownloadInterval() / 1000f);
    setMaxTokens(site.getMaxDownloaders());
  }

  public DomainInfo(String domain, DomainInfo copy)
  {
    this.setDomain(domain);
    copyFrom(copy);
  }

  public void copyFrom(DomainInfo copy)
  {
    if (this.minDelay == 0)
    {
      this.setMinDelay(copy.minDelay);
    }
    if (this.longDelay == 0)
    {
      this.setLongDelay(copy.longDelay);
    }
    if (this.longDelayThreshold == 0)
    {
      this.setLongDelayThreshold(copy.longDelayThreshold);
    }
    if (this.maxTokens == 0)
    {
      this.setMaxTokens(copy.maxTokens);
    }
  }

  public String getDomain()
  {
    return domain;
  }

  public void setDomain(String domain)
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
    return longDelayThreshold == 0 ? DEFAULT_LONG_DELAY_THRESHOLD : longDelayThreshold;
  }

  public void setLongDelayThreshold(int longDelayThreshold)
  {
    this.longDelayThreshold = longDelayThreshold;
  }

  public float getLongDelay()
  {
    return longDelay == 0 ? DEFAULT_LONG_DELAY : longDelay;
  }

  public void setLongDelay(float longDelay)
  {
    this.longDelay = longDelay;
  }

  public int getMaxTokens()
  {
    return maxTokens;
  }

  public void setMaxTokens(int maxTokens)
  {
    this.maxTokens = maxTokens;
    setNumTokens(maxTokens);
  }

  public int getNumTokens()
  {
    return numTokens;
  }

  private void setNumTokens(int numTokens)
  {
    this.numTokens = numTokens;
  }

  public boolean acquireToken()
  {
    if (maxTokens > 0)
    {
      synchronized (tokenLock)
      {
        if (numTokens <= 0)
        {
          return false;
        }
        numTokens--;
        logger.debug("[{}] \t assigned token, current # tokens: {}", domain, numTokens);
      }
    }
    return true;
  }

  public void releaseToken()
  {
    if (maxTokens > 0)
    {
      synchronized (tokenLock)
      {
        numTokens++;
        logger.debug("[{}] \t received token, current # tokens: {}", domain, numTokens);
        if (numTokens > maxTokens)
        {
          logger.error("[{}] \t Releasing more tokens than possible!", domain);
        }
      }
    }
  }

  @Override
  public String toString()
  {
    return DomainInfo.class.getSimpleName() + "[" + domain + "]";
  }

}
