/**
 * 
 */
package ecologylab.bigsemantics.service;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.actions.SemanticsConstants;
import ecologylab.bigsemantics.collecting.SemanticsGlobalScope;
import ecologylab.bigsemantics.collecting.SemanticsSite;
import ecologylab.bigsemantics.cyberneko.CybernekoWrapper;
import ecologylab.bigsemantics.documentcache.DiskPersistentDocumentCache;
import ecologylab.bigsemantics.documentcache.DocumentCache;
import ecologylab.bigsemantics.documentcache.EhCacheDocumentCache;
import ecologylab.bigsemantics.documentcache.PersistentDocumentCache;
import ecologylab.bigsemantics.documentparsers.DefaultHTMLDOMParser;
import ecologylab.bigsemantics.documentparsers.DocumentParser;
import ecologylab.bigsemantics.downloadcontrollers.DPoolDownloadController;
import ecologylab.bigsemantics.downloadcontrollers.DownloadController;
import ecologylab.bigsemantics.generated.library.RepositoryMetadataTranslationScope;
import ecologylab.bigsemantics.html.dom.IDOMProvider;
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.bigsemantics.metadata.builtins.DocumentClosure;
import ecologylab.bigsemantics.service.logging.ServiceLogRecord;
import ecologylab.net.ParsedURL;
import ecologylab.serialization.SimplTypesScope;
import ecologylab.serialization.SimplTypesScope.GRAPH_SWITCH;

/**
 * Exclusive semantic scope for the semantic service Keeps a reference to DBDocumentProviderFactory
 * to facilitate DB lookup
 * 
 * @author ajit
 */
public class SemanticsServiceScope extends SemanticsGlobalScope
{

  static Logger                       logger = LoggerFactory.getLogger(SemanticsServiceScope.class);

  private DiskPersistentDocumentCache persistentDocCache;

  private String                      dpoolServiceUrl;

  private SemanticsServiceScope(SimplTypesScope metadataTScope,
                                Class<? extends IDOMProvider> domProviderClass)
  {
    super(metadataTScope, domProviderClass);
    persistentDocCache = new DiskPersistentDocumentCache(this);
  }

  public void configure(Configuration configs)
  {
    String cacheBaseDir = configs.getString("cache-dir", "cache");
    if (!persistentDocCache.configure(cacheBaseDir))
    {
      logger.error("Cannot configure cache! Will not cache anything.");
    }
    String[] dpoolServices = configs.getStringArray("dpool-service");
    dpoolServiceUrl = DPoolDownloadController.pickDpoolServiceUrl(dpoolServices);
    if (dpoolServiceUrl == null)
    {
      String msg = "Cannot locate DPool service!";
      logger.error(msg);
      throw new RuntimeException(msg);
    }
  }

  @Override
  protected DocumentCache<ParsedURL, Document> getDocumentCache()
  {
    return new EhCacheDocumentCache();
  }

  @Override
  public PersistentDocumentCache getPersistentDocumentCache()
  {
    return persistentDocCache;
  }

  @Override
  public DownloadController createDownloadController(DocumentClosure closure)
  {
    DPoolDownloadController result = new DPoolDownloadController(this, dpoolServiceUrl);
    result.setDocumentClosure(closure);
    return result;
  }
  
  @Override
  public ServiceLogRecord createLogRecord()
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

  private static SemanticsServiceScope THE_SERVICE_SCOPE = null;

  public static SemanticsServiceScope get()
  {
    if (THE_SERVICE_SCOPE == null)
    {
      PropertiesConfiguration configs = new PropertiesConfiguration();
      try
      {
        configs.load("service.properties");
      }
      catch (ConfigurationException e)
      {
        logger.error("Cannot load configurations!");
      }

      SimplTypesScope.graphSwitch = GRAPH_SWITCH.ON;
      SemanticsSite.disableDownloadInterval = true;

      THE_SERVICE_SCOPE = new SemanticsServiceScope(RepositoryMetadataTranslationScope.get(),
                                                    CybernekoWrapper.class);
      THE_SERVICE_SCOPE.configure(configs);

      // This will disable content body recognization and image-text clipping derivation on the
      // service.
      DocumentParser.register(SemanticsConstants.HTML_IMAGE_DOM_TEXT_PARSER,
                              DefaultHTMLDOMParser.class);
    }
    return THE_SERVICE_SCOPE;
  }

}
