/**
 * 
 */
package ecologylab.bigsemantics.service;

import org.apache.commons.configuration.Configuration;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.actions.SemanticsConstants;
import ecologylab.bigsemantics.collecting.SemanticsGlobalScope;
import ecologylab.bigsemantics.collecting.SemanticsSite;
import ecologylab.bigsemantics.documentcache.DocumentCache;
import ecologylab.bigsemantics.documentcache.EhCacheDocumentCache;
import ecologylab.bigsemantics.documentcache.PersistentDocumentCache;
import ecologylab.bigsemantics.documentparsers.DefaultHTMLDOMParser;
import ecologylab.bigsemantics.documentparsers.DocumentParser;
import ecologylab.bigsemantics.downloadcontrollers.DPoolDownloadController;
import ecologylab.bigsemantics.downloadcontrollers.DownloadController;
import ecologylab.bigsemantics.downloaderpool.DpoolConfigNames;
import ecologylab.bigsemantics.downloaderpool.GlobalCacheManager;
import ecologylab.bigsemantics.html.dom.IDOMProvider;
import ecologylab.bigsemantics.logging.DocumentLogRecord;
import ecologylab.bigsemantics.logging.DocumentLogRecordScope;
import ecologylab.bigsemantics.logging.DpoolEventTypeScope;
import ecologylab.bigsemantics.logging.DpoolServiceError;
import ecologylab.bigsemantics.logging.LogStore;
import ecologylab.bigsemantics.logging.ServiceLogRecord;
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.bigsemantics.metadata.builtins.DocumentClosure;
import ecologylab.generic.ReflectionTools;
import ecologylab.logging.LogEventTypeScope;
import ecologylab.net.ParsedURL;
import ecologylab.serialization.SimplTypesScope;
import ecologylab.serialization.SimplTypesScope.GRAPH_SWITCH;

/**
 * Exclusive semantic scope for the semantic service Keeps a reference to DBDocumentProviderFactory
 * to facilitate DB lookup
 * 
 * @author ajit
 */
@Service
public class SemanticsServiceScope extends SemanticsGlobalScope
    implements SemanticsServiceConfigNames
{

  static Logger                   logger = LoggerFactory.getLogger(SemanticsServiceScope.class);

  static
  {
    SimplTypesScope.graphSwitch = GRAPH_SWITCH.ON;
    SemanticsSite.disableDownloadInterval = true;

    DpoolEventTypeScope.init();
    LogEventTypeScope.addEventClass(DpoolServiceError.class);
    DocumentLogRecordScope.addType(ServiceLogRecord.class);

    // This will disable content body recognization and image-text clipping derivation.
    DocumentParser.register(SemanticsConstants.HTML_IMAGE_DOM_TEXT_PARSER,
                            DefaultHTMLDOMParser.class);
  }

  private Configuration           configs;

  private String                  dpoolServiceUrl;

  private PersistentDocumentCache persistentDocCache;

  private LogStore                logStore;

  public SemanticsServiceScope(SimplTypesScope metadataTScope,
                               Class<? extends IDOMProvider> domProviderClass)
  {
    super(metadataTScope, domProviderClass);
  }

  public Configuration getConfigs()
  {
    return configs;
  }

  public void configure(Configuration configs) throws Exception
  {
    this.configs = configs;

    String pCacheClass = configs.getString(PERSISTENT_CACHE_CLASS);
    if (pCacheClass != null)
    {
      Class<? extends PersistentDocumentCache> clazz =
          (Class<? extends PersistentDocumentCache>) Class.forName(pCacheClass);
      Class<?>[] ctorArgType = new Class<?>[] { SemanticsGlobalScope.class };
      Object[] ctorArg = new Object[] { this };
      persistentDocCache = ReflectionTools.getInstance(clazz, ctorArgType, ctorArg);
      persistentDocCache.configure(configs);
    }

    logStore = new LogStore();
    logStore.configure(configs);
  }

  private void configureDpoolServiceUrl()
  {
    String[] dpoolHosts = configs.getStringArray(DpoolConfigNames.CONTROLLER_HOST);
    int port = configs.getInt(DpoolConfigNames.CONTROLLER_PORT);
    dpoolServiceUrl = DPoolDownloadController.pickDpoolServiceUrl(port, dpoolHosts);
    if (dpoolServiceUrl == null)
    {
      String msg = "Cannot locate DPool service!";
      logger.error(msg);
    }
  }

  @Override
  protected DocumentCache<ParsedURL, Document> getDocumentCache()
  {
    return new EhCacheDocumentCache(GlobalCacheManager.getSingleton());
  }

  @Override
  public PersistentDocumentCache getPersistentDocumentCache()
  {
    return persistentDocCache;
  }

  @Override
  public DownloadController createDownloadController(DocumentClosure closure)
  {
    while (dpoolServiceUrl == null)
    {
      configureDpoolServiceUrl();
    }
    DPoolDownloadController result = new DPoolDownloadController(dpoolServiceUrl);
    result.setDocumentClosure(closure);
    return result;
  }

  @Override
  public DocumentLogRecord createLogRecord()
  {
    return new ServiceLogRecord();
  }

  @Override
  public boolean isService()
  {
    return true;
  }

  @Override
  public boolean ifAutoUpdateDocRefs()
  {
    return false;
  }

  @Override
  public boolean ifLookForFavicon()
  {
    return false;
  }

  public LogStore getLogStore()
  {
    return logStore;
  }

}
