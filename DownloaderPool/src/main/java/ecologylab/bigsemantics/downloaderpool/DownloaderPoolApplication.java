package ecologylab.bigsemantics.downloaderpool;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.ehcache.CacheManager;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import ecologylab.bigsemantics.Configs;
import ecologylab.bigsemantics.Utils;
import ecologylab.bigsemantics.downloaderpool.resources.Echo;
import ecologylab.bigsemantics.downloaderpool.resources.LogRequest;
import ecologylab.bigsemantics.downloaderpool.resources.PageRequest;
import ecologylab.bigsemantics.downloaderpool.resources.TaskRequest;

/**
 * Glues different components of the service together.
 * 
 * @author quyin
 */
public class DownloaderPoolApplication implements DpoolConfigNames
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

  private Configuration configs;

  private CacheManager  cacheManager;

  private Controller    controller;

  private Server        server;

  private Downloader    downloader;

  public DownloaderPoolApplication()
  {
    super();
    configs = Configs.loadProperties("dpool.properties");
  }

  public Configuration getConfigs()
  {
    return configs;
  }

  public void setCacheManager(CacheManager cacheManager)
  {
    this.cacheManager = cacheManager;
  }

  public void initialize() throws ConfigurationException
  {
    if (server == null)
    {
      ServletContainer container = getDpoolContainer();

      // set up jetty handler for servlets
      ServletContextHandler handler = new ServletContextHandler();
      handler.setContextPath("/");
      handler.addServlet(new ServletHolder(new HelloServlet()), "/hello");
      handler.addServlet(new ServletHolder(container), "/DownloaderPool/*");

      // set up jetty server components
      QueuedThreadPool threadPool = new QueuedThreadPool(500, 50);
      server = new Server(threadPool);
      // num of acceptors: num of cores - 1
      // num of selectors: use default guess. in practice, depend on cores, load, etc.
      int cores = Runtime.getRuntime().availableProcessors();
      ServerConnector connector = new ServerConnector(server, cores - 1, -1);
      connector.setPort(configs.getInt(CONTROLLER_PORT, 8080));
      server.addConnector(connector);

      // misc server settings
      server.setStopAtShutdown(true);

      // connect handler to server
      server.setHandler(handler);
    }

    downloader = new Downloader(configs);
  }

  public ServletContainer getDpoolContainer() throws ConfigurationException
  {
    controller = new Controller(configs, cacheManager);
    controller.start();

    // set up jersey servlet
    ResourceConfig config = new ResourceConfig();
    // we package everything into a runnable jar using OneJAR, which provides its own class loader.
    // as the result, Jersey classpath scanning won't work properly for now.
    // hopefully this can be fixed soon. right now we need to specify classes.
    config.register(Echo.class);
    config.register(LogRequest.class);
    config.register(PageRequest.class);
    config.register(TaskRequest.class);
    // binder for HK2 to inject the controller to Jersey resource instances
    config.register(new AbstractBinder()
    {
      @Override
      protected void configure()
      {
        bind(controller).to(Controller.class);
      }
    });
    ServletContainer container = new ServletContainer(config);

    return container;
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
    server.stop();
  }

  public static void main(String[] args) throws Exception
  {
    Map<String, String> flags = new HashMap<String, String>();
    Utils.parseCommandlineFlags(flags, args);

    DownloaderPoolApplication app = new DownloaderPoolApplication();
    Configuration appConfigs = app.getConfigs();
    Utils.mergeFlagsToConfigs(appConfigs, flags);
    app.setCacheManager(GlobalCacheManager.getSingleton());
    app.initialize();
    app.start();
  }

}
