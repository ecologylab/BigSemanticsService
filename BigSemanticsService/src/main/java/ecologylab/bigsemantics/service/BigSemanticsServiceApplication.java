package ecologylab.bigsemantics.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.Slf4jRequestLog;
import org.eclipse.jetty.server.handler.AllowSymLinkAliasChecker;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.Configs;
import ecologylab.bigsemantics.Utils;
import ecologylab.bigsemantics.cyberneko.CybernekoWrapper;
import ecologylab.bigsemantics.downloaderpool.Downloader;
import ecologylab.bigsemantics.downloaderpool.DownloaderPoolApplication;
import ecologylab.bigsemantics.downloaderpool.DpoolConfigNames;
import ecologylab.bigsemantics.downloaderpool.GlobalCacheManager;
import ecologylab.bigsemantics.generated.library.RepositoryMetadataTypesScope;
import ecologylab.bigsemantics.service.metadata.MetadataJSONPService;
import ecologylab.bigsemantics.service.metadata.MetadataJSONService;
import ecologylab.bigsemantics.service.metadata.MetadataXMLService;
import ecologylab.bigsemantics.service.mmd.MMDJSONPService;
import ecologylab.bigsemantics.service.mmd.MMDJSONService;
import ecologylab.bigsemantics.service.mmd.MMDXMLService;
import ecologylab.bigsemantics.service.mmdrepository.MMDRepositoryJSONPService;
import ecologylab.bigsemantics.service.mmdrepository.MMDRepositoryJSONService;
import ecologylab.bigsemantics.service.mmdrepository.MMDRepositoryVersion;
import ecologylab.bigsemantics.service.mmdrepository.MMDRepositoryXMLService;

/**
 * Glues different components of the service together.
 * 
 * @author quyin
 */
public class BigSemanticsServiceApplication implements SemanticsServiceConfigNames
{

  static final Logger logger;

  static
  {
    logger = LoggerFactory.getLogger(BigSemanticsServiceApplication.class);
  }

  public static class HelloServlet extends HttpServlet
  {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
      resp.setContentType("text/html");
      resp.getWriter().println("Hello world!");
      resp.setStatus(HttpServletResponse.SC_OK);
    }

  }

  private Configuration             configs;

  private Server                    server;

  private DownloaderPoolApplication dpoolApp;

  private Downloader                downloader;

  private SemanticsServiceScope     semanticsServiceScope;

  public BigSemanticsServiceApplication()
  {
    super();
    configs = Configs.loadProperties("service.properties");
  }

  public Configuration getConfigs()
  {
    return configs;
  }

  public void initialize() throws ConfigurationException, ClassNotFoundException
  {
    if (server == null)
    {
      // step 1: create and configure server
      int maxThreads = configs.getInt(MAX_THREADS, 500);
      int minThreads = configs.getInt(MIN_THREADS, 50);
      QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads);
      server = new Server(threadPool);
      // num of acceptors: num of cores - 1
      // num of selectors: use default guess. in practice, depend on cores, load, etc.
      int cores = Runtime.getRuntime().availableProcessors();
      int nAcceptors = configs.getInt(NUM_ACCEPTORS, cores - 1);
      int nSelectors = configs.getInt(NUM_SELECTORS, -1);
      ServerConnector connector = new ServerConnector(server, nAcceptors, nSelectors);
      // port
      int port = configs.getInt(PORT);
      connector.setPort(port);
      server.addConnector(connector);
      // misc server settings
      server.setStopAtShutdown(true);

      // step 2: set up servlet containers and handler
      ServletContextHandler servletContext = new ServletContextHandler();
      servletContext.setContextPath("/");
      // dpool
      if (!configs.containsKey(DpoolConfigNames.CONTROLLER_HOST))
      {
        dpoolApp = new DownloaderPoolApplication();
        configs.setProperty(DpoolConfigNames.CONTROLLER_HOST, "localhost");
        configs.setProperty(DpoolConfigNames.CONTROLLER_PORT, port);
        Configuration dpoolConfigs = dpoolApp.getConfigs();
        dpoolConfigs.setProperty(DpoolConfigNames.CONTROLLER_HOST, "localhost");
        dpoolConfigs.setProperty(DpoolConfigNames.CONTROLLER_PORT, port);
        dpoolApp.setCacheManager(GlobalCacheManager.getSingleton());
        ServletContainer dpoolContainer = dpoolApp.getDpoolContainer();
        servletContext.addServlet(new ServletHolder(dpoolContainer), "/DownloaderPool/*");
      }
      // bigsemantics service
      ServletContainer serviceContainer = getBssContainer();
      servletContext.addServlet(new ServletHolder(new HelloServlet()), "/hello");
      servletContext.addServlet(new ServletHolder(serviceContainer), "/BigSemanticsService/*");

      // step 3: set up static resource handler
      ContextHandler staticContext = getStaticResourceHandler();
      
      // step 4: set up a request log
      Slf4jRequestLog requestLog = new Slf4jRequestLog() {
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

      // step 5: connect handlers to server
      HandlerCollection handlers = new HandlerCollection();
      ContextHandlerCollection contextHandlers = new ContextHandlerCollection();
      if (staticContext != null)
      {
        contextHandlers.addHandler(staticContext);
      }
      contextHandlers.addHandler(servletContext);
      handlers.addHandler(contextHandlers);
      handlers.addHandler(new DefaultHandler());
      handlers.addHandler(requestLogHandler);
      server.setHandler(handlers);
    }
  }

  public ServletContainer getBssContainer() throws ClassNotFoundException
  {
    // set up jersey servlet
    ResourceConfig config = new ResourceConfig();

    // we package everything into a runnable jar using OneJAR, which provides its own class loader.
    // as the result, Jersey classpath scanning won't work properly for now.
    // hopefully this can be fixed soon. right now we need to specify classes.
    config.register(MetadataXMLService.class);
    config.register(MetadataJSONService.class);
    config.register(MetadataJSONPService.class);
    config.register(MMDXMLService.class);
    config.register(MMDJSONPService.class);
    config.register(MMDJSONService.class);
    config.register(MMDRepositoryXMLService.class);
    config.register(MMDRepositoryJSONService.class);
    config.register(MMDRepositoryJSONPService.class);
    config.register(MMDRepositoryVersion.class);

    semanticsServiceScope =
        new SemanticsServiceScope(RepositoryMetadataTypesScope.get(), CybernekoWrapper.class);
    semanticsServiceScope.configure(configs);
    SemanticsServiceScope.setSingleton(semanticsServiceScope);
    config.register(new AbstractBinder()
    {
      @Override
      protected void configure()
      {
        bind(semanticsServiceScope).to(SemanticsServiceScope.class);
      }
    });

    ServletContainer container = new ServletContainer(config);

    return container;
  }

  public ContextHandler getStaticResourceHandler()
  {
    ContextHandler staticContext = null;
    String staticDirPath = configs.getString(STATIC_DIR, "./static/");
    try
    {
      File staticDir = new File(staticDirPath).getCanonicalFile();
      Resource staticResource = Resource.newResource(staticDir);
      ResourceHandler resourceHandler = new ResourceHandler();
      resourceHandler.setBaseResource(staticResource);
      resourceHandler.setDirectoriesListed(true);
      staticContext = new ContextHandler("/static");
      staticContext.addAliasCheck(new AllowSymLinkAliasChecker());
      staticContext.setHandler(resourceHandler);
    }
    catch (IOException e)
    {
      logger.error("Exception when configuring static dir " + staticDirPath, e);
    }
    return staticContext;
  }

  public void start() throws Exception
  {
    if (server == null)
    {
      throw new RuntimeException("Server uninitialized!");
    }

    // run server
    server.start();

    // run a downloader
    downloader = new Downloader(dpoolApp.getConfigs());
    downloader.start();
  }

  public void join() throws InterruptedException
  {
    if (server == null)
    {
      throw new RuntimeException("Server uninitialized!");
    }

    server.join();
  }

  public void stop() throws Exception
  {
    if (server == null)
    {
      throw new RuntimeException("Server uninitialized!");
    }

    downloader.stop();
    downloader = null;
    server.stop();
  }

  public static void main(String[] args) throws Exception
  {
    Map<String, String> flags = new HashMap<String, String>();
    Utils.parseCommandlineFlags(flags, args);

    BigSemanticsServiceApplication application = new BigSemanticsServiceApplication();
    Configuration appConfigs = application.getConfigs();
    Utils.mergeFlagsToConfigs(appConfigs, flags);
    application.initialize();
    application.start();
    application.join();
  }

}
