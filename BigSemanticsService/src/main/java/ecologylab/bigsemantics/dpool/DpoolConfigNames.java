package ecologylab.bigsemantics.dpool;

/**
 * Constants that are used as configuration keys.
 * 
 * @author quyin
 */
public interface DpoolConfigNames
{

  static final String DEFAULT_USER_AGENT             = "dpool.default_user_agent";

  static final String DEFAULT_NUM_ATTEMPTS           = "dpool.default_num_attempts";

  static final String DEFAULT_TIMEOUT                = "dpool.default_timeout";

  static String       CONTROLLER_PORT                = "dpool.controller.port";

  static final String WAIT_BETWEEN_COUNTDOWN         = "dpool.controller.wait_between_countdown";

  static final String TASK_ID_LENGTH                 = "dpool.controller.task_id_length";

  static final String MAX_TASKS_PER_DOWNLOADER       = "dpool.controller.max_tasks_per_downloader";

  static final String MAX_CONNECTIONS                = "dpool.controller.max_connections";

  static final String CLIENT_REQUEST_TIMEOUT         = "dpool.controller.client_request_timeout";

  static String       CONTROLLER_HOST                = "dpool.controller.host";

  static String       DOWNLOADER_NAME                = "dpool.downloader.name";

  static String       NUM_DOWNLOADING_THREADS        = "dpool.downloader.num_downloading_threads";

  static String       MAX_TASK_COUNT                 = "dpool.downloader.max_task_count";

  static String       MAX_CONNECTIONS_FOR_DOWNLOADER = "dpool.downloader.max_connections";

  static String       MAX_ERROR                      = "dpool.downloader.max_error";

}
