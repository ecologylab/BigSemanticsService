package ecologylab.bigsemantics.service;

/**
 * 
 * @author quyin
 */
public class ServiceParams
{

  public int maxThreads;

  public int minThreads;

  public int port;

  public int nAcceptors;

  public int nSelectors;

  public ServiceParams(int maxThreads, int minThreads, int port, int nAcceptors, int nSelectors)
  {
    this.maxThreads = maxThreads;
    this.minThreads = minThreads;
    this.port = port;
    this.nAcceptors = nAcceptors;
    this.nSelectors = nSelectors;
  }

}