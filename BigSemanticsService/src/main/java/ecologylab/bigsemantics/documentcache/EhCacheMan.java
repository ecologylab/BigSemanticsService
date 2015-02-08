package ecologylab.bigsemantics.documentcache;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;

/**
 * To make sure that we only have one CacheManager.
 * 
 * @author quyin
 */
public class EhCacheMan
{

  private static CacheManager cacheManager;

  public static CacheManager getSingleton()
  {
    if (cacheManager == null)
    {
      synchronized (EhCacheMan.class)
      {
        if (cacheManager == null)
        {
          CacheConfiguration defaultCacheConfig = getDefaultCacheConfig();
          Configuration cacheManConfig = new Configuration();
          cacheManConfig.setDynamicConfig(true);
          cacheManConfig.setUpdateCheck(false);
          cacheManConfig.addDefaultCache(defaultCacheConfig);
          cacheManager = new CacheManager(cacheManConfig);
        }
      }
    }
    return cacheManager;
  }

  public static CacheConfiguration getDefaultCacheConfig()
  {
    CacheConfiguration defaultCacheConfig = new CacheConfiguration();
    defaultCacheConfig.setMaxEntriesLocalHeap(1000);
    defaultCacheConfig.setEternal(true);
    defaultCacheConfig.setMemoryStoreEvictionPolicy("LRU");
    return defaultCacheConfig;
  }

}
