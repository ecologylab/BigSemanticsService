package ecologylab.bigsemantics.dpool;

import java.util.concurrent.ConcurrentHashMap;

import ecologylab.concurrent.Site;

/**
 * 
 * @author quyin
 */
public class DomainInfoTable
{

  private ConcurrentHashMap<String, DomainInfo> table;

  public DomainInfoTable()
  {
    super();
    table = new ConcurrentHashMap<String, DomainInfo>();
  }

  /**
   * 
   * @param site
   * @return true iff successfully added site.
   */
  public boolean addSite(Site site)
  {
    DomainInfo domainInfo = new DomainInfo(site);
    DomainInfo existing = table.putIfAbsent(domainInfo.getDomain(), domainInfo);
    return existing == null;
  }

  public DomainInfo getOrCreate(String domain)
  {
    if (!table.containsKey(domain))
    {
      DomainInfo domainInfo = new DomainInfo(domain);
      DomainInfo existing = table.putIfAbsent(domain, domainInfo);
      return existing == null ? domainInfo : existing;
    }
    return table.get(domain);
  }

}
