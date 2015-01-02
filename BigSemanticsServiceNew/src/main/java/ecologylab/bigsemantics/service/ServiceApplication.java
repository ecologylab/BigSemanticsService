package ecologylab.bigsemantics.service;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * Glues different components of the service together.
 * 
 * @author quyin
 */
public class ServiceApplication
{
  
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


  public static void main(String[] args)
  {
    // set up jersey servlet
    ResourceConfig config = new ResourceConfig();
    config.packages("ecologylab.bigsemantics.service"); // resource scanning
    ServletContainer container = new ServletContainer(config);

    // set up jetty handler for servlets
    ServletContextHandler handler = new ServletContextHandler();
    handler.setContextPath("/");
    handler.addServlet(new ServletHolder(new HelloServlet()), "/hello");
//    handler.addServlet(new ServletHolder(container), "/BigSemanticsService/*");

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

    // connect handler to server
    server.setHandler(handler);

    // run server
    try
    {
      server.start();
      server.join();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

}
