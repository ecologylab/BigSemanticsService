package ecologylab.bigsemantics.downloaderpool;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.concurrent.Site;

/**
 * A table that records all the sites that we are accessing.
 * 
 * @author quyin
 */
public class SimpleSiteTable
{

  private static Logger                         logger;

  static
  {
    logger = LoggerFactory.getLogger(SimpleSiteTable.class);
  }

  /**
   * The real table, from domain to Site object.
   */
  private ConcurrentHashMap<String, SimpleSite> sites = new ConcurrentHashMap<String, SimpleSite>();

  /**
   * Get the Site object by domain. Lazily create the object with the given downloadInterval if
   * needed.
   * 
   * @param domain
   * @param downloadInterval
   *          Interval between requests to this domain, in millisecond. Will be ignored if the Site
   *          object for this domain already exists.
   * @return
   */
  public Site getSite(String domain)
  {
    SimpleSite site;
    if (sites.containsKey(domain))
    {
      site = sites.get(domain);
    }
    else
    {
      logger.info("Creating Site object for " + domain);
      site = new SimpleSite(domain);
      SimpleSite existingSite = sites.putIfAbsent(domain, site);
      if (existingSite != null)
      {
        site = existingSite;
      }
    }
    return site;
  }
  
  public boolean updateSiteDownloadInterval(String domain, long downloadInterval)
  {
    SimpleSite site = (SimpleSite) getSite(domain);
    if (site.getDownloadInterval() != downloadInterval)
    {
      logger.info("Updating downloading interval for " + domain + " to " + downloadInterval);
      site.setDownloadInterval(downloadInterval);
      return true;
    }
    return false;
  }

  /**
   * Get the set of sites that are too busy to download from at this moment.
   * 
   * @return
   */
  public Set<Site> getBusySites()
  {
    Set<Site> results = new HashSet<Site>();

    long t0 = System.currentTimeMillis();
    // logger.info("current time t0=" + t0);
    for (SimpleSite site : sites.values())
    {
      long t = site.getNextAvailableTime();
      // logger.info("Site[{}]: next available at {} (t{}{})",
      // site,
      // t,
      // t > t0 ? "+" : "-",
      // Math.abs(t - t0));
      // if the site is down or ignored, we treat it as busy because we don't want to download from
      // them anyway.
      if (site.isDown() || site.isIgnored())
      {
        results.add(site);
      }
      if (site.isDownloadingConstrained() && t > t0)
      {
        results.add(site);
      }
    }

    return results;
  }

}
