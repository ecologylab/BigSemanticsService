package ecologylab.bigsemantics.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.Slf4jRequestLog;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.AllowSymLinkAliasChecker;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.Configs;
import ecologylab.bigsemantics.Configurable;
import ecologylab.bigsemantics.Utils;
import ecologylab.bigsemantics.admin.AdminServiceApplication;
import ecologylab.bigsemantics.admin.DownloaderService;
import ecologylab.bigsemantics.admin.LogService;
import ecologylab.bigsemantics.collecting.SemanticsSiteMap;
import ecologylab.bigsemantics.cyberneko.CybernekoWrapper;
import ecologylab.bigsemantics.dpool.Controller;
import ecologylab.bigsemantics.dpool.DomainInfo;
import ecologylab.bigsemantics.dpool.DownloadDispatcher;
import ecologylab.bigsemantics.dpool.DownloaderPoolApplication;
import ecologylab.bigsemantics.generated.library.RepositoryMetadataTypesScope;
import ecologylab.bigsemantics.service.resources.MetadataService;
import ecologylab.bigsemantics.service.resources.MmdRepoService;
import ecologylab.bigsemantics.service.resources.MmdService;
import ecologylab.bigsemantics.service.resources.OntoVizService;
import ecologylab.concurrent.Site;

/**
 * Glues different components of the service together.
 * 
 * @author quyin
 */
public class BigSemanticsServiceApplication extends AbstractServiceApplication
    implements Configurable, SemanticsServiceConfigNames
{

  static final Logger               logger;

  static
  {
    logger = LoggerFactory.getLogger(BigSemanticsServiceApplication.class);
  }

  Configuration                     configs;

  SemanticsServiceScope             semanticsServiceScope;

  private DownloaderPoolApplication dpoolApp;

  private AdminServiceApplication   adminApp;

  @Override
  public void configure(Configuration configuration) throws Exception
  {
    this.configs = configuration;

    semanticsServiceScope =
        new SemanticsServiceScope(RepositoryMetadataTypesScope.get(), CybernekoWrapper.class);
    semanticsServiceScope.configure(configs);

    if (configs.getBoolean(DPOOL_RUN_BUILTIN_SERVICE, true))
    {
      Configuration dpoolConfig = Configs.loadProperties("dpool.properties");
      CompositeConfiguration compositeConfig = new CompositeConfiguration();
      compositeConfig.addConfiguration(configuration);
      compositeConfig.addConfiguration(dpoolConfig);

      dpoolApp = new DownloaderPoolApplication();
      dpoolApp.configure(compositeConfig);

      DownloadDispatcher dispatcher = dpoolApp.getController().getDispatcher();
      SemanticsSiteMap siteMap =
          semanticsServiceScope.getMetaMetadataRepository().getSemanticsSiteMap();
      if (siteMap != null)
      {
        for (Site site : siteMap.values())
        {
          DomainInfo domainInfo = new DomainInfo(site);
          dispatcher.addDomainInfoIfAbsent(domainInfo);
        }
        dispatcher.configureDomainInfos();
      }
    }

    adminApp = new AdminServiceApplication();
    int adminPort = configuration.getInt(ADMIN_PORT);
    adminApp.setPort(adminPort);
    ResourceConfig adminResourceConfig = adminApp.getResourceConfig();
    adminResourceConfig.register(LogService.class);
    adminResourceConfig.register(new AbstractBinder()
    {
      @Override
      protected void configure()
      {
        bind(semanticsServiceScope).to(SemanticsServiceScope.class);
      }
    });
    if (configs.getBoolean(DPOOL_RUN_BUILTIN_SERVICE))
    {
      adminResourceConfig.register(DownloaderService.class);
      adminResourceConfig.register(new AbstractBinder()
      {
        @Override
        protected void configure()
        {
          bind(dpoolApp.getController()).to(Controller.class);
        }
      });
    }
  }

  @Override
  public Configuration getConfiguration()
  {
    return configs;
  }

  public SemanticsServiceScope getSemanticsServiceScope()
  {
    return semanticsServiceScope;
  }

  public void setupServer() throws Exception
  {
    int maxThreads = configs.getInt(MAX_THREADS, 500);
    int minThreads = configs.getInt(MIN_THREADS, 50);
    int port = configs.getInt(PORT);
    // num of acceptors: num of cores - 1
    // num of selectors: use default guess. in practice, depend on cores, load, etc.
    int cores = Runtime.getRuntime().availableProcessors();
    int nAcceptors = configs.getInt(NUM_ACCEPTORS, cores - 1);
    int nSelectors = configs.getInt(NUM_SELECTORS, -1);
    super.setupServer(new ServiceParams(maxThreads, minThreads, port, nAcceptors, nSelectors));
    
    int securePort = configs.getInt(SECURE_PORT, -1);
    if (securePort > 0)
    {
      SslContextFactory sslContextFactory = new SslContextFactory();
      sslContextFactory.setKeyStorePath(configs.getString(KEYSTORE_PATH));
      sslContextFactory.setKeyStorePassword(configs.getString(KEYSTORE_PASSWORD));
      sslContextFactory.setKeyManagerPassword(configs.getString(KEYMANAGER_PASSWORD));
      sslContextFactory.setTrustStorePath(configs.getString(KEYSTORE_PATH));
      sslContextFactory.setTrustStorePassword(configs.getString(KEYSTORE_PASSWORD));
      sslContextFactory.setIncludeCipherSuites("TLS_DHE_RSA.*", "TLS_ECDHE.*");
      sslContextFactory.setExcludeCipherSuites(".*NULL.*", ".*RC4.*", ".*MD5.*", ".*DES.*", ".*DSS.*");
      sslContextFactory.setExcludeProtocols("SSLv3");
      sslContextFactory.setRenegotiationAllowed(false);
      SslConnectionFactory sslConnFactory = new SslConnectionFactory(sslContextFactory, "HTTP/1.1");

      HttpConfiguration httpsConfig = new HttpConfiguration();
      httpsConfig.addCustomizer(new SecureRequestCustomizer());
      HttpConnectionFactory httpConnFactory = new HttpConnectionFactory(httpsConfig);

      Server server = getServer();
      ServerConnector secureConnector =
          new ServerConnector(server, nAcceptors, nSelectors, sslConnFactory, httpConnFactory);
      secureConnector.setPort(securePort);
      server.addConnector(secureConnector);
    }

    adminApp.setupServer();
  }

  @Override
  public Handler createHandler() throws Exception
  {
    ServletContextHandler dpoolHandler = null;
    if (dpoolApp != null)
    {
      dpoolHandler = (ServletContextHandler) dpoolApp.createHandler();
    }

    ServletContextHandler bssHandler = createBSSHandler();

    ContextHandler staticResourceHandler = createStaticResourceHandler();
    
    Handler webRootHandler = new AbstractHandler()
    {
      @Override
      public void handle(String target,
                         Request baseRequest,
                         HttpServletRequest request,
                         HttpServletResponse response)
          throws IOException, ServletException
      {
        if ("/".equals(target))
        {
          String wikiUrl = configs.getString(WIKI_URL);
          logger.debug("Redirecting to Wiki at {} ...", wikiUrl);
          response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
          response.setHeader("Location", wikiUrl);
          response.flushBuffer();
        }
      }
    };

    RequestLogHandler requestLogHandler = createRequestLogHandler();
    
    HandlerCollection handlerCollection = new HandlerCollection();
    if (dpoolHandler != null)
    {
      handlerCollection.addHandler(dpoolHandler);
    }
    handlerCollection.addHandler(bssHandler);
    handlerCollection.addHandler(staticResourceHandler);
    handlerCollection.addHandler(webRootHandler);
    handlerCollection.addHandler(requestLogHandler);
    return handlerCollection;
  }

  public ServletContextHandler createBSSHandler() throws Exception
  {
    // set up jersey servlet
    ResourceConfig config = new ResourceConfig();
    // we package everything into a runnable jar using OneJAR, which provides its own class loader.
    // as the result, Jersey classpath scanning won't work properly for now.
    // hopefully this can be fixed soon. right now we need to specify classes.
    config.register(MetadataService.class);
    config.register(MmdService.class);
    config.register(MmdRepoService.class);
    config.register(OntoVizService.class);
    config.register(new AbstractBinder()
    {
      @Override
      protected void configure()
      {
        bind(semanticsServiceScope).to(SemanticsServiceScope.class);
      }
    });
    ServletContainer container = new ServletContainer(config);
    ServletContextHandler handler = new ServletContextHandler();
    handler.setContextPath("/BigSemanticsService");
    handler.addServlet(new ServletHolder(container), "/*");
    return handler;
  }

  public ContextHandler createStaticResourceHandler()
  {
    ContextHandler staticResourceHandler = null;
    String staticDirPath = configs.getString(STATIC_DIR, "./static/");
    try
    {
      File staticDir = new File(staticDirPath).getCanonicalFile();
      Resource staticResource = Resource.newResource(staticDir);
      ResourceHandler resourceHandler = new ResourceHandler();
      resourceHandler.setBaseResource(staticResource);
      resourceHandler.setDirectoriesListed(true);
      staticResourceHandler = new ContextHandler("/static");
      staticResourceHandler.addAliasCheck(new AllowSymLinkAliasChecker());
      staticResourceHandler.setHandler(resourceHandler);
    }
    catch (IOException e)
    {
      logger.error("Exception when configuring static dir " + staticDirPath, e);
    }
    return staticResourceHandler;
  }

  public RequestLogHandler createRequestLogHandler()
  {
    Slf4jRequestLog requestLog = new Slf4jRequestLog()
    {
      @Override
      public void write(String requestEntry) throws IOException
      {
        if (requestEntry.contains("GET /BigSemanticsService/"))
        {
          super.write(requestEntry);
        }
      }
    };
    requestLog.setExtended(true);
    requestLog.setLogLatency(true);
    requestLog.setLogTimeZone("America/Chicago");
    RequestLogHandler requestLogHandler = new RequestLogHandler();
    requestLogHandler.setRequestLog(requestLog);
    return requestLogHandler;
  }

  public void start() throws Exception
  {
    if (dpoolApp != null)
    {
      dpoolApp.getController().startDispatcher();
    }
    super.start();
    adminApp.start();
    logger.info("BigSemantics Service up and running.");
    if (configs.containsKey(POST_STARTUP_MESSAGE))
    {
      logger.info(configs.getString(POST_STARTUP_MESSAGE));
    }
  }

  public void stop() throws Exception
  {
    adminApp.stop();
    super.stop();
    if (dpoolApp != null)
    {
      dpoolApp.getController().stopDispatcher();
    }
    Thread.sleep(1000 * 3);
    logger.info("BigSemantics Service stopped.");
  }

  public static void main(String[] args) throws Exception
  {
    Map<String, String> flags = new HashMap<String, String>();
    Utils.parseCommandlineFlags(flags, args);
    Configuration appConfigs = Configs.loadProperties("service.properties");
    Utils.mergeFlagsToConfigs(appConfigs, flags);

    BigSemanticsServiceApplication app = new BigSemanticsServiceApplication();
    app.configure(appConfigs);
    app.setupServer();
    app.start();
    app.join();
  }

}
