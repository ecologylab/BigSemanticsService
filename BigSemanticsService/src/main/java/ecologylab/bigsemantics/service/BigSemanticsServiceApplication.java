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
import ecologylab.bigsemantics.dpool.Downloader;
import ecologylab.bigsemantics.dpool.DownloaderPoolApplication;
import ecologylab.bigsemantics.dpool.DpoolConfigNames;
import ecologylab.bigsemantics.dpool.GlobalCacheManager;
import ecologylab.bigsemantics.generated.library.RepositoryMetadataTypesScope;
import ecologylab.bigsemantics.service.resources.LogService;
import ecologylab.bigsemantics.service.resources.MetadataService;
import ecologylab.bigsemantics.service.resources.MmdRepoService;
import ecologylab.bigsemantics.service.resources.MmdService;

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
  
  private Server                    adminServer;

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

  public void initialize() throws Exception
  {
    if (server == null)
    {
      // step 1: create and configure server
      server = createServer(configs);

      int port = configs.getInt(PORT);
      ServerConnector connector = createConnector(configs, server, port);
      server.addConnector(connector);

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
        dpoolApp.setCacheManager(EhCacheMan.getSingleton());
        ServletContainer dpoolContainer = dpoolApp.getDpoolServletContainer();
        servletContext.addServlet(new ServletHolder(dpoolContainer), "/DownloaderPool/*");
      }
      // bigsemantics service
      ServletContainer serviceContainer = getBssContainer();
      servletContext.addServlet(new ServletHolder(new HelloServlet()), "/hello");
      servletContext.addServlet(new ServletHolder(serviceContainer), "/BigSemanticsService/*");

      // step 3: set up static resource handler
      ContextHandler staticContext = getStaticResourceHandler();

      // step 4: set up a request log
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
    
    // step 5: set up the admin server
    if (adminServer == null)
    {
      adminServer = createServer(configs);
      int adminPort = configs.getInt(ADMIN_PORT);
      ServerConnector adminConnector = createConnector(configs, adminServer, adminPort);
      adminServer.addConnector(adminConnector);

      ServletContainer adminContainer = getAdminContainer();
      ServletContextHandler adminContext = new ServletContextHandler();
      adminContext.setContextPath("/");
      adminContext.addServlet(new ServletHolder(adminContainer), "/admin/*");
      adminServer.setHandler(adminContext);
    }
  }

  private ServerConnector createConnector(Configuration configs, Server server, int port)
  {
    // num of acceptors: num of cores - 1
    // num of selectors: use default guess. in practice, depend on cores, load, etc.
    int cores = Runtime.getRuntime().availableProcessors();
    int nAcceptors = configs.getInt(NUM_ACCEPTORS, cores - 1);
    int nSelectors = configs.getInt(NUM_SELECTORS, -1);
    ServerConnector connector = new ServerConnector(server, nAcceptors, nSelectors);
    connector.setPort(port);
    return connector;
  }

  private Server createServer(Configuration configs)
  {
    int maxThreads = configs.getInt(MAX_THREADS, 500);
    int minThreads = configs.getInt(MIN_THREADS, 50);
    QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads);
    Server server = new Server(threadPool);
    // misc server settings
    server.setStopAtShutdown(true);
    return server;
  }

  public ServletContainer getBssContainer() throws Exception
  {
    // set up jersey servlet
    ResourceConfig config = new ResourceConfig();

    // we package everything into a runnable jar using OneJAR, which provides its own class loader.
    // as the result, Jersey classpath scanning won't work properly for now.
    // hopefully this can be fixed soon. right now we need to specify classes.
    config.register(MetadataService.class);
    config.register(MmdService.class);
    config.register(MmdRepoService.class);

    semanticsServiceScope =
        new SemanticsServiceScope(RepositoryMetadataTypesScope.get(), CybernekoWrapper.class);
    semanticsServiceScope.configure(configs);
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

  public ServletContainer getAdminContainer() throws Exception
  {
    ResourceConfig config = new ResourceConfig();

    config.register(LogService.class);

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

    // run admin server
    adminServer.start();

    logger.info("BigSemantics Service up and running.");
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
    
    adminServer.stop();
    downloader.stop();
    downloader = null;
    server.stop();
    Thread.sleep(1000 * 3);

    logger.info("BigSemantics Service stopped.");
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
