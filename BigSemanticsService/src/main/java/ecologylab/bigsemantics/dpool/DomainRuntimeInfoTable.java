package ecologylab.bigsemantics.dpool;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author quyin
 */
public class DomainRuntimeInfoTable
{

  private ConcurrentHashMap<String, DomainInfo>        domainInfos;

  private ConcurrentHashMap<String, DomainRuntimeInfo> table;

  public DomainRuntimeInfoTable()
  {
    this(null);
  }

  public DomainRuntimeInfoTable(ConcurrentHashMap<String, DomainInfo> domainInfos)
  {
    super();
    this.domainInfos =
        domainInfos == null ? new ConcurrentHashMap<String, DomainInfo>() : domainInfos;
    table = new ConcurrentHashMap<String, DomainRuntimeInfo>();
  }

  public DomainRuntimeInfo getOrCreate(String domain)
  {
    DomainInfo domainInfo = domainInfos.get(domain);
    if (domainInfo == null)
    {
      return new DomainRuntimeInfo(new DomainInfo(domain));
    }

    DomainRuntimeInfo result = table.get(domain);
    if (result == null)
    {
      DomainRuntimeInfo domainRuntimeInfo = new DomainRuntimeInfo(domainInfo);
      result = table.putIfAbsent(domain, domainRuntimeInfo);
      if (result == null)
      {
        result = domainRuntimeInfo;
      }
    }
    return result;
  }

  public void releaseAllTokens()
  {
    for (DomainRuntimeInfo domainRuntimeInfo : table.values())
    {
      if (domainRuntimeInfo.isHoldingToken())
      {
        domainRuntimeInfo.getDomainInfo().releaseToken();
      }
    }
  }

}
