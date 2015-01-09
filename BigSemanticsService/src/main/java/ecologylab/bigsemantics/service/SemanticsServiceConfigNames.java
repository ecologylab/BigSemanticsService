package ecologylab.bigsemantics.service;

public interface SemanticsServiceConfigNames
{

  static String MAX_THREADS   = "threads.max_count";

  static String MIN_THREADS   = "threads.min_count";

  static String NUM_ACCEPTORS = "acceptors.max_count";

  static String NUM_SELECTORS = "selectors.max_count";

  static String PORT          = "port";

  static String STATIC_DIR    = "static_dir";
  
  static String PERSISTENT_CACHE_CLASS = "ecologylab.bigsemantics.documentcache.DiskPersistentDocumentCache";

}
