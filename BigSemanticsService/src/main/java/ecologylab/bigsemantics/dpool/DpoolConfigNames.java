package ecologylab.bigsemantics.dpool;

/**
 * Constants that are used as configuration keys.
 * 
 * @author quyin
 */
public interface DpoolConfigNames
{

  static final String DEFAULT_USER_AGENT              = "dpool.default_user_agent";

  static final String DEFAULT_NUM_ATTEMPTS            = "dpool.default_num_attempts";

  static final String DEFAULT_TIMEOUT                 = "dpool.default_timeout";

  static final String CONTROLLER_HOST                 = "dpool.controller.host";

  static final String CONTROLLER_PORT                 = "dpool.controller.port";

  static final String DOWNLOADERS_FILE                = "dpool.downloaders.file";

  static final String MAX_WORKER_CONSECUTIVE_FAILURES = "dpool.downloaders.max_consecutive_failures";

}
