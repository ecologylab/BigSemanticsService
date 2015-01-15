package ecologylab.bigsemantics.downloaderpool;

import java.util.ArrayList;
import java.util.List;

import ecologylab.generic.StringTools;
import ecologylab.net.ParsedURL;
import ecologylab.serialization.annotations.simpl_collection;
import ecologylab.serialization.annotations.simpl_scalar;

/**
 * The request of tasks sent from a downloader to the controller.
 * 
 * @author quyin
 */
public class DownloaderRequest
{

  /**
   * The ID of the downloader
   */
  @simpl_scalar
  private String       workerId;

  /**
   * Blacklisted domains. The controller will not return tasks with any of the domains in this
   * collection.
   */
  @simpl_collection("domain")
  private List<String> blacklist;

  /**
   * Maximum number of tasks that can be accepted by this request.
   */
  @simpl_scalar
  private int          maxTaskCount;

  public DownloaderRequest()
  {
    super();
  }

  public String getWorkerId()
  {
    return workerId;
  }

  public void setWorkerId(String workerId)
  {
    this.workerId = workerId;
  }

  public List<String> getBlacklist()
  {
    return blacklist;
  }

  public void setBlacklist(List<String> blacklist)
  {
    this.blacklist = blacklist;
  }

  public int getMaxTaskCount()
  {
    return maxTaskCount;
  }

  public void setMaxTaskCount(int maxTaskCount)
  {
    this.maxTaskCount = maxTaskCount;
  }

  private List<String> blacklist()
  {
    if (blacklist == null)
    {
      synchronized (this)
      {
        if (blacklist == null)
        {
          blacklist = new ArrayList<String>();
        }
      }
    }
    return blacklist;
  }

  public void addToBlacklist(String domain)
  {
    if (domain != null && domain.length() > 0)
      this.blacklist().add(domain);
  }

  public String getBlacklistString()
  {
    if (blacklist != null)
    {
      return StringTools.join(",", blacklist);
    }
    return null;
  }

  /**
   * Test if a given URL can be accepted by this request, considering the blacklist.
   * 
   * @param purl
   *          The URL to test.
   * @return true if the URL can be accepted (domain not in the blacklist), otherwise false.
   */
  public boolean accept(ParsedURL purl)
  {
    if (purl == null)
      return false;
    if (blacklist != null)
    {
      for (String domain : blacklist)
      {
        if (purl.domain().equals(domain))
          return false;
      }
    }
    return true;
  }

}
