/**
 * 
 */
package ecologylab.bigsemantics.service.utils;

import java.io.File;
import java.io.IOException;

import ecologylab.bigsemantics.documentcache.DiskPersistentDocumentCache;

/**
 * @author quyin
 * 
 */
public class WgetBulkRequestMaker extends AbstractBulkRequestMaker
{

  static String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.60 Safari/537.17";

  public WgetBulkRequestMaker(String serviceBase)
  {
    super(serviceBase);
  }

  @Override
  public int request(String url)
  {
    String fn = "/tmp/cache/" + DiskPersistentDocumentCache.getDocId(url) + ".html";
    ProcessBuilder pb = new ProcessBuilder();
    pb.command("/usr/local/bin/wget", "-O", fn, "-U", userAgent, url);
    Process p = null;
    try
    {
      p = pb.start();
      p.waitFor();
      if (p.exitValue() == 0)
      {
        File f = new File(fn);
        if (f.exists() && f.length() > 0)
          return 200;
      }
    }
    catch (IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    catch (InterruptedException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return 0;
  }

}
