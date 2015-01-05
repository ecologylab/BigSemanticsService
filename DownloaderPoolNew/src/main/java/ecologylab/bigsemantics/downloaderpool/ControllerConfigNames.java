package ecologylab.bigsemantics.downloaderpool;

/**
 * Constants that are used for configuration keys.
 * 
 * @author quyin
 */
public interface ControllerConfigNames
{

  static String WAIT_BETWEEN_COUNTDOWN         = "controller.wait_between_countdown";

  static String TASK_ID_LENGTH                 = "controller.task_id_length";

  static String MAX_TASKS_PER_DOWNLOADER       = "controller.max_tasks_per_downloader";

  static String MAX_CONNECTIONS_FOR_CONTROLLER = "controller.max_connections";

  static String CLIENT_REQUEST_TIMEOUT         = "controller.client_request_timeout";

}
