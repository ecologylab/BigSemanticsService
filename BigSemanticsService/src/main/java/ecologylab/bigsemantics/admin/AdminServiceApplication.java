package ecologylab.bigsemantics.admin;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import ecologylab.bigsemantics.service.AbstractServiceApplication;
import ecologylab.bigsemantics.service.ServiceParams;

/**
 * 
 * @author quyin
 */
public class AdminServiceApplication extends AbstractServiceApplication
{

  public static final String ADMIN_PORT = "admin.port";

  private ResourceConfig     config;

  private int                port;

  public AdminServiceApplication()
  {
    config = new ResourceConfig();
  }

  public void setPort(int port)
  {
    this.port = port;
  }

  public ResourceConfig getResourceConfig()
  {
    return config;
  }

  @Override
  public Handler createHandler() throws Exception
  {
    ServletContainer servletContainer = new ServletContainer(config);
    ServletContextHandler servletContextHandler = new ServletContextHandler();
    servletContextHandler.setContextPath("/admin");
    servletContextHandler.addServlet(new ServletHolder(servletContainer), "/*");
    return servletContextHandler;
  }

  @Override
  public void setupServer() throws Exception
  {
    super.setupServer(new ServiceParams(50, 5, port, 1, 1));
  }

}