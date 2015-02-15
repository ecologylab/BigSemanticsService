package ecologylab.bigsemantics.dpool;

import java.io.File;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.Configs;
import ecologylab.bigsemantics.Configurable;
import ecologylab.bigsemantics.Utils;
import ecologylab.bigsemantics.dpool.resources.Echo;
import ecologylab.bigsemantics.dpool.resources.LogService;
import ecologylab.bigsemantics.dpool.resources.PageService;
import ecologylab.bigsemantics.dpool.resources.TaskService;
import ecologylab.bigsemantics.service.AbstractServiceApplication;
import ecologylab.bigsemantics.service.ServiceParams;
import ecologylab.serialization.formatenums.Format;

/**
 * Glues different components of the service together.
 * 
 * @author quyin
 */
public class DownloaderPoolApplication extends AbstractServiceApplication
    implements Configurable, DpoolConfigNames
{

  static Logger                    logger;

  static
  {
    logger = LoggerFactory.getLogger(DownloaderPoolApplication.class);
  }

  private Configuration            configs;

  private Controller               controller;

  private RemoteCurlDownloaderList downloaders;

  public Controller getController()
  {
    return controller;
  }

  @Override
  public void configure(Configuration configuration) throws Exception
  {
    this.configs = configuration;

    // set up controller
    controller = new Controller();
    controller.configure(configs);

    // set up downloader(s)
    String downloadersFilePath = configuration.getString(DOWNLOADERS_FILE, null);
    File file = downloadersFilePath == null ? null : new File(downloadersFilePath);
    if (file == null || !file.exists() || !file.isFile())
    {
      LocalDownloader localDownloader = new LocalDownloader("local-downloader", 4);
      controller.getDispatcher().addWorker(localDownloader);
    }
    else
    {
      downloaders = (RemoteCurlDownloaderList) MessageScope.get().deserialize(file, Format.JSON);
      Thread downloaderInitThread = new Thread(new Runnable()
      {
        @Override
        public void run()
        {
          for (RemoteCurlDownloader downloader : downloaders.getDownloaders())
          {
            downloader.copyFrom(downloaders.getDefaultConfig());
            try
            {
              downloader.initialize();
              controller.getDispatcher().addWorker(downloader);
              logger.info("Successfully added {} as a downloader", downloader);
            }
            catch (Exception e)
            {
              logger.error("Failed to initialize " + downloader, e);
            }
          }
        }
      }, "downloader-init-thread");
      downloaderInitThread.start();
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
