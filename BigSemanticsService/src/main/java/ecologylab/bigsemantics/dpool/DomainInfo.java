package ecologylab.bigsemantics.dpool;

import ecologylab.serialization.annotations.simpl_scalar;

/**
 * 
 * @author quyin
 */
public class DomainInfo
{

  public static final int DEFAULT_LONG_DELAY           = 5000;

  public static final int DEFAULT_LONG_DELAY_THRESHOLD = 3;

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

  public DomainInfo()
  {
    this(null);
  }

  public DomainInfo(String domain)
  {
    this.domain = domain;
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
    return longDelayThreshold;
  }

  public void setLongDelayThreshold(int longDelayThreshold)
  {
    this.longDelayThreshold = longDelayThreshold;
  }

  public float getLongDelay()
  {
    return longDelay;
  }

  public void setLongDelay(float longDelay)
  {
    this.longDelay = longDelay;
  }

}
