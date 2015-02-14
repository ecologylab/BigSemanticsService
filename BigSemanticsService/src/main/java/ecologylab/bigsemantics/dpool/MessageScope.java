package ecologylab.bigsemantics.dpool;

import ecologylab.logging.LogEventTypeScope;
import ecologylab.serialization.SimplTypesScope;

/**
 * The SIMPL scope for all the messages used by DPool.
 * 
 * @author quyin
 */
public class MessageScope
{

  public static final String     NAME    = "DPoolMessages";

  public static final Class<?>[] CLASSES =
                                         {
                                         DomainInfo.class,
                                         DomainRuntimeInfo.class,
                                         DownloadDispatcher.class,
                                         Downloader.class,
                                         DownloadTask.class,
                                         LocalDownloader.class,
                                         RemoteCurlDownloader.class,
                                         RemoteCurlDownloaderList.class,
                                         };

  public static SimplTypesScope get()
  {
    return SimplTypesScope.get(NAME, LogEventTypeScope.get(), CLASSES);
  }

}
