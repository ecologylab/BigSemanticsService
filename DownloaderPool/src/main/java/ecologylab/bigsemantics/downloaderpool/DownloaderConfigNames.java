package ecologylab.bigsemantics.downloaderpool;

/**
 * Constants that are used as configuration keys.
 * 
 * @author quyin
 */
public interface DownloaderConfigNames
{

  static String CONTROLLER_BASE_URL            = "downloader.controller_base_url";

  static String NAME                           = "downloader.name";

  static String NUM_DOWNLOADING_THREADS        = "downloader.num_downloading_threads";

  static String MAX_TASK_COUNT                 = "downloader.max_task_count";

  static String MAX_CONNECTIONS_FOR_DOWNLOADER = "downloader.max_connections";

  static String MAX_ERROR                      = "downloader.max_error";

}
