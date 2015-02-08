package ecologylab.bigsemantics.dpool;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import ecologylab.bigsemantics.Configs;
import ecologylab.bigsemantics.Configurable;
import ecologylab.bigsemantics.Utils;
import ecologylab.bigsemantics.dpool.resources.Echo;
import ecologylab.bigsemantics.dpool.resources.LogService;
import ecologylab.bigsemantics.dpool.resources.PageService;
import ecologylab.bigsemantics.dpool.resources.TaskService;
import ecologylab.bigsemantics.service.AbstractServiceApplication;
import ecologylab.bigsemantics.service.ServiceParams;

/**
 * Glues different components of the service together.
 * 
 * @author quyin
 */
public class DownloaderPoolApplication extends AbstractServiceApplication
    implements Configurable, DpoolConfigNames
{

  private Configuration configs;

  private Controller    controller;

  private Downloader    downloader;

  public Controller getController()
  {
    return controller;
  }

  public Downloader getLocalDownloader()
  {
    return downloader;
  }

  public void setLocalDownloader(Downloader downloader)
  {
    this.downloader = downloader;
  }

  @Override
  public void configure(Configuration configuration) throws Exception
  {
    this.configs = configuration;

    // set up controller
    controller = new Controller();
    controller.configure(configs);

    // set up a local downloader
    if (downloader == null)
    {
      downloader = new LocalDownloader("local-downloader", 4);
      controller.getDispatcher().addWorker(downloader);
    }
  }

  @Override
  public Configuration getConfiguration()
  {
    return configs;
  }

  @Override
  public Handler createHandler() throws ConfigurationException
  {
    // set up jersey servlet
    ResourceConfig config = new ResourceConfig();
    // we package everything into a runnable jar using OneJAR, which provides its own class loader.
    // as the result, Jersey classpath scanning won't work properly for now.
    // hopefully this can be fixed soon. right now we need to specify classes.
    config.register(Echo.class);
    config.register(LogService.class);
    config.register(PageService.class);
    config.register(TaskService.class);
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
    ServletContextHandler handler = new ServletContextHandler();
    handler.setContextPath("/DownloaderPool");
    handler.addServlet(new ServletHolder(container), "/*");
    return handler;
  }

  @Override
  public void setupServer() throws Exception
  {
    int cores = Runtime.getRuntime().availableProcessors();
    setupServer(new ServiceParams(500, 50, 8080, cores - 1, -1));
  }

  public void start() throws Exception
  {
    controller.startDispatcher();
    super.start();
  }

  public void stop() throws Exception
  {
    super.stop();
    controller.stopDispatcher();
  }

  public static void main(String[] args) throws Exception
  {
    Map<String, String> flags = new HashMap<String, String>();
    Utils.parseCommandlineFlags(flags, args);
    Configuration appConfigs = Configs.loadProperties("dpool.properties");
    Utils.mergeFlagsToConfigs(appConfigs, flags);

    DownloaderPoolApplication app = new DownloaderPoolApplication();
    app.configure(appConfigs);
    app.setupServer();
    app.start();
  }

}
