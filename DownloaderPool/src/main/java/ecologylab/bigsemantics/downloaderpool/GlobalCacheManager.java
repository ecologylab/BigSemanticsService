package ecologylab.bigsemantics.downloaderpool;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;

/**
 * To make sure that we only have one CacheManager.
 * 
 * @author quyin
 */
public class GlobalCacheManager
{

  private static CacheManager cacheManager;

  public static CacheManager getSingleton()
  {
    if (cacheManager == null)
    {
      synchronized (GlobalCacheManager.class)
      {
        if (cacheManager == null)
        {
          CacheConfiguration defaultCacheConfig = new CacheConfiguration();
          defaultCacheConfig.setMaxEntriesLocalHeap(1000);
          defaultCacheConfig.setEternal(true);
          defaultCacheConfig.setMemoryStoreEvictionPolicy("LRU");
          net.sf.ehcache.config.Configuration cacheManConfig = new net.sf.ehcache.config.Configuration();
          cacheManConfig.setDynamicConfig(true);
          cacheManConfig.setUpdateCheck(false);
          cacheManConfig.addDefaultCache(defaultCacheConfig);
          cacheManager = new CacheManager(cacheManConfig);
        }
      }
    }
    return cacheManager;
  }

}
