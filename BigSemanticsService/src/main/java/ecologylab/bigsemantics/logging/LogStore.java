package ecologylab.bigsemantics.logging;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.event.CacheEventListenerAdapter;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.apache.commons.configuration.Configuration;

import ecologylab.bigsemantics.downloaderpool.GlobalCacheManager;
import ecologylab.bigsemantics.service.SemanticsServiceConfigNames;
import ecologylab.collections.FilteredIterator;

/**
 * 
 * @author quyin
 */
public class LogStore implements SemanticsServiceConfigNames
{

  static final String           EMPTY_CLIENT_ATTACHED_ID = "EMPTY_CLIENT_ATTACHED_ID";

  Cache                         logsById;

  Map<ServiceLogRecord, String> logs;

  public LogStore()
  {
    super();
  }

  public void configure(Configuration configs)
  {
    int size = configs.getInt(LOG_CACHE_SIZE);

    logs = new ConcurrentHashMap<ServiceLogRecord, String>(size);

    CacheConfiguration cacheConfig = GlobalCacheManager.getDefaultCacheConfig();
    cacheConfig.setMaxEntriesLocalHeap(size);
    cacheConfig.memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.FIFO);
    logsById = new Cache(cacheConfig);
    logsById.getCacheEventNotificationService().registerListener(new CacheEventListenerAdapter()
    {
      @Override
      public void notifyElementEvicted(Ehcache cache, Element element)
      {
        ServiceLogRecord logRecord = (ServiceLogRecord) element.getObjectValue();
        logs.remove(logRecord);
      }
    });
  }

  public void addLogRecord(String id, String clientAttachedId, ServiceLogRecord log)
  {
    if (log != null)
    {
      logsById.put(new Element(id, log));
      logs.put(log, clientAttachedId == null ? EMPTY_CLIENT_ATTACHED_ID : clientAttachedId);
    }
  }

  public ServiceLogRecord getLogRecord(String id)
  {
    Element element = logsById.get(id);
    return element == null ? null : (ServiceLogRecord) element.getObjectValue();
  }

  public Iterator<ServiceLogRecord> filter(final String caid, final String urlFrag)
  {
    Iterator<ServiceLogRecord> result = logs.keySet().iterator();

    if (caid != null)
    {
      Iterator<ServiceLogRecord> newIter = new FilteredIterator<ServiceLogRecord>(result)
      {
        @Override
        protected boolean keepElement(ServiceLogRecord logRecord)
        {
          return caid.equals(logs.get(logRecord));
        }
      };
      result = newIter;
    }

    if (urlFrag != null)
    {
      Iterator<ServiceLogRecord> newIter = new FilteredIterator<ServiceLogRecord>(result)
      {
        @Override
        protected boolean keepElement(ServiceLogRecord logRecord)
        {
          String docUrl = logRecord.getDocumentLocation().toString();
          return docUrl.contains(urlFrag);
        }
      };
      result = newIter;
    }

    return result;
  }

}
