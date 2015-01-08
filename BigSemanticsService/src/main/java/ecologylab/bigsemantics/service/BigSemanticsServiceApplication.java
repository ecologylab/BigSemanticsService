package ecologylab.bigsemantics.service;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.ehcache.CacheManager;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AllowSymLinkAliasChecker;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.documentcache.EhCacheDocumentCache;
import ecologylab.bigsemantics.downloaderpool.Downloader;
import ecologylab.bigsemantics.downloaderpool.DownloaderPoolApplication;
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
public class BigSemanticsServiceApplication
{

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

  static Logger  logger            = LoggerFactory.getLogger(BigSemanticsServiceApplication.class);

  private Server server;

  private String staticResourceDir = "./static/";

  public BigSemanticsServiceApplication()
  {
    super();
  }

  void initialize() throws ConfigurationException
  {
    if (server == null)
    {
      // step 1: create and configure server
      QueuedThreadPool threadPool = new QueuedThreadPool(500, 50);
      server = new Server(threadPool);
      // num of acceptors: num of cores - 1
      // num of selectors: use default guess. in practice, depend on cores, load, etc.
      int cores = Runtime.getRuntime().availableProcessors();
      ServerConnector connector = new ServerConnector(server, cores - 1, -1);
      connector.setPort(8080);
      server.addConnector(connector);
      // misc server settings
      server.setStopAtShutdown(true);

      // step 2: set up servlet containers and handler
      CacheManager cacheManager = EhCacheDocumentCache.getDefaultCacheManager();
      ServletContainer dpoolContainer = DownloaderPoolApplication.getDpoolContainer(cacheManager);
      ServletContainer serviceContainer = getBssContainer();
      ServletContextHandler servletContext = new ServletContextHandler();
      servletContext.setContextPath("/");
      servletContext.addServlet(new ServletHolder(new HelloServlet()), "/hello");
      servletContext.addServlet(new ServletHolder(dpoolContainer), "/DownloaderPool/*");
      servletContext.addServlet(new ServletHolder(serviceContainer), "/BigSemanticsService/*");

      // step 3: set up static resource handler
      ContextHandler staticContext = getStaticResourceHandler();

      // step 4: connect handlers to server
      ContextHandlerCollection handlers = new ContextHandlerCollection();
      if (staticContext != null)
      {
        handlers.addHandler(staticContext);
      }
      handlers.addHandler(servletContext);
      server.setHandler(handlers);
    }
  }

  ServletContainer getBssContainer()
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
    ServletContainer container = new ServletContainer(config);

    return container;
  }

  ContextHandler getStaticResourceHandler()
  {
    ContextHandler staticContext = null;
    try
    {
      File staticDir = new File(staticResourceDir).getCanonicalFile();
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
      logger.error("Exception when configuring static resources!", e);
    }
    return staticContext;
  }

  void setStaticResourceDir(String staticResourceDir)
  {
    this.staticResourceDir = staticResourceDir;
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
    Configuration configs = new PropertiesConfiguration("dpool.properties"); // TODO
    Downloader d = new Downloader(configs);
    d.start();
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

    server.stop();
  }

  public static void main(String[] args) throws Exception
  {
    String staticDirFlag = "--static_dir=";
    String staticDir = null;
    for (int i = 0; i < args.length; ++i)
    {
      if (args[i].startsWith(staticDirFlag))
      {
        staticDir = args[i].substring(staticDirFlag.length());
        File staticDirFile = new File(staticDir);
        if (staticDirFile.exists() && staticDirFile.isDirectory())
        {
          staticDir = staticDirFile.getCanonicalPath();
        }
        else
        {
          staticDir = null;
        }
      }
    }

    BigSemanticsServiceApplication application = new BigSemanticsServiceApplication();
    if (staticDir != null)
    {
      application.setStaticResourceDir(staticDir);
    }
    application.initialize();
    application.start();
    application.join();
  }

}
