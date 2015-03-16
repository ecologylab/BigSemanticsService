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

  private DomainInfo                                   defaultDomainInfo;

  public DomainRuntimeInfoTable()
  {
    this(null);
  }

  public DomainRuntimeInfoTable(ConcurrentHashMap<String, DomainInfo> domainInfos)
  {
    super();
    this.domainInfos =
        domainInfos == null ? new ConcurrentHashMap<String, DomainInfo>() : domainInfos;
    this.table = new ConcurrentHashMap<String, DomainRuntimeInfo>();
    this.defaultDomainInfo = domainInfos.get(DomainInfo.DEFAULT_DOMAIN);
  }

  public DomainRuntimeInfo getOrCreate(String domain)
  {
    DomainRuntimeInfo result = table.get(domain);
    if (result == null)
    {
      // first, find or generate the DomainInfo
      DomainInfo domainInfo = domainInfos.get(domain);
      if (domainInfo == null)
      {
        domainInfo = defaultDomainInfo == null
            ? new DomainInfo(domain)
            : new DomainInfo(domain, defaultDomainInfo);
        DomainInfo existingDomainInfo = domainInfos.putIfAbsent(domain, domainInfo);
        domainInfo = existingDomainInfo == null ? domainInfo : existingDomainInfo;
      }

      // second, generate a DomainRuntimeInfo.
      result = new DomainRuntimeInfo(domainInfo);
      DomainRuntimeInfo existingDomainRuntimeInfo = table.putIfAbsent(domain, result);
      result = existingDomainRuntimeInfo == null ? result : existingDomainRuntimeInfo;
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
