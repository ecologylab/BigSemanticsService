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
  
  static Logger logger = LoggerFactory.getLogger(BigSemanticsServiceApplication.class);
  
  public static class HelloServlet extends HttpServlet
  {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
      resp.setContentType("text/html");
      resp.getWriter().println("Hello world!");
      resp.setStatus(HttpServletResponse.SC_OK);
    }

  }
  
  public static ServletContainer getServiceContainer()
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
  
  public static void main(String[] args) throws ConfigurationException
  {
    CacheManager cacheManager = EhCacheDocumentCache.getDefaultCacheManager();
    ServletContainer dpoolContainer = DownloaderPoolApplication.getDpoolContainer(cacheManager);
    ServletContainer serviceContainer = getServiceContainer();

    // set up jetty handler for servlets
    ServletContextHandler servletContext = new ServletContextHandler();
    servletContext.setContextPath("/");
    servletContext.addServlet(new ServletHolder(new HelloServlet()), "/hello");
    servletContext.addServlet(new ServletHolder(dpoolContainer), "/DownloaderPool/*");
    servletContext.addServlet(new ServletHolder(serviceContainer), "/BigSemanticsService/*");
    
    // set up static resource handler
    ContextHandler staticContext = null;
    try
    {
      File staticDir = new File("./static").getCanonicalFile();
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

    // set up jetty server components
    QueuedThreadPool threadPool = new QueuedThreadPool(500, 50);
    Server server = new Server(threadPool);
    // num of acceptors: num of cores - 1
    // num of selectors: use default guess. in practice, depend on cores, load, etc.
    int cores = Runtime.getRuntime().availableProcessors();
    ServerConnector connector = new ServerConnector(server, cores - 1, -1);
    connector.setPort(8080);
    server.addConnector(connector);

    // misc server settings
    server.setStopAtShutdown(true);

    // connect handlers to server
    ContextHandlerCollection handlers = new ContextHandlerCollection();
    if (staticContext != null)
    {
      handlers.addHandler(staticContext);
    }
    handlers.addHandler(servletContext);
    server.setHandler(handlers);

    try
    {
      // run server
      server.start();

      // run a downloader after a period of time
      Thread.sleep(1000 * 5);
      Configuration configs = new PropertiesConfiguration("dpool.properties");
      Downloader d = new Downloader(configs);
      d.start();

      server.join();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

}
