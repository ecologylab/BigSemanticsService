package ecologylab.bigsemantics.logging;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
    cacheConfig.setName(LogStore.class.getName() + ".logsById");
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
    GlobalCacheManager.getSingleton().addCache(logsById); // necessary for initialization
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
    List<ServiceLogRecord> sortedLogs = new LinkedList<ServiceLogRecord>(logs.keySet());
    Comparator<ServiceLogRecord> logComparator = new Comparator<ServiceLogRecord>()
    {
      @Override
      public int compare(ServiceLogRecord r1, ServiceLogRecord r2)
      {
        return -r1.getRequestTime().compareTo(r2.getRequestTime());
      }
    };
    Collections.sort(sortedLogs, logComparator);
    Iterator<ServiceLogRecord> result = sortedLogs.iterator();

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
          String docUrl = logRecord.getDocumentLocation().toString().toLowerCase();
          return docUrl.contains(urlFrag.toLowerCase());
        }
      };
      result = newIter;
    }

    return result;
  }
}
