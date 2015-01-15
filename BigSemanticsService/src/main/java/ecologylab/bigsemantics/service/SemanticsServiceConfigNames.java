package ecologylab.bigsemantics.service;

/**
 * Config names.
 * 
 * @author quyin
 */
public interface SemanticsServiceConfigNames
{

  static final String PORT                   = "service.port";

  static final String MAX_THREADS            = "service.threads.max";

  static final String MIN_THREADS            = "service.threads.min";

  static final String NUM_ACCEPTORS          = "service.acceptors";

  static final String NUM_SELECTORS          = "service.selectors";

  static final String STATIC_DIR             = "service.static_dir";

  static final String PERSISTENT_CACHE_CLASS = "service.persistent_cache.class";

  static final String CACHE_DIR              = "service.persistent_cache.dir";

  static final String NO_BUILTIN_DPOOL       = "service.no_builtin_dpool";

}
