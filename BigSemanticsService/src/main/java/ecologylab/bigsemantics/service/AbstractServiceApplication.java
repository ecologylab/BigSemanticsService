package ecologylab.bigsemantics.service;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * 
 * @author quyin
 */
public abstract class AbstractServiceApplication
{

  private Server server;

  public Server getServer()
  {
    return server;
  }

  protected Server createServer(int maxThreads, int minThreads)
  {
    QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads, minThreads);
    server = new Server(threadPool);
    // misc server settings
    server.setStopAtShutdown(true);
    return server;
  }

  protected ServerConnector createConnector(Server server, int port, int nAcceptors, int nSelectors)
  {
    // num of acceptors: num of cores - 1
    // num of selectors: use default guess. in practice, depend on cores, load, etc.
    ServerConnector connector = new ServerConnector(server, nAcceptors, nSelectors);
    connector.setPort(port);
    return connector;
  }

  abstract public Handler createHandler() throws Exception;

  abstract public void setupServer() throws Exception;

  protected void setupServer(ServiceParams serverParams) throws Exception
  {
    if (server == null)
    {
      server = createServer(serverParams.maxThreads, serverParams.minThreads);
      ServerConnector connector = createConnector(server,
                                                  serverParams.port,
                                                  serverParams.nAcceptors,
                                                  serverParams.nSelectors);
      server.addConnector(connector);
      Handler handler = createHandler();
      server.setHandler(handler);
    }
  }

  public void start() throws Exception
  {
    if (server == null)
    {
      throw new RuntimeException("Server uninitialized!");
    }
    server.start();
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
    Thread.sleep(1000 * 3);
  }

}
