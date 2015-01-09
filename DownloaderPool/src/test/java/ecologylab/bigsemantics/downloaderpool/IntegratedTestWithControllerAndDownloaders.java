package ecologylab.bigsemantics.downloaderpool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.eclipse.jetty.server.Server;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ecologylab.bigsemantics.httpclient.BasicResponseHandler;
import ecologylab.bigsemantics.httpclient.HttpClientFactory;
import ecologylab.bigsemantics.httpclient.ModifiedHttpClientUtils;

/**
 * Integration tests that involve both a controller and downloaders.
 * 
 * @author quyin
 */
public class IntegratedTestWithControllerAndDownloaders
{

  static Logger        logger;

  static
  {
    logger = LoggerFactory.getLogger(IntegratedTestWithControllerAndDownloaders.class);
  }

  static String        HOST          = "localhost";

  static int           PORT          = 2280;

  static String        CONTEXT       = "/DownloaderPool";

  static String        BASE_URL      = "http://" + HOST + ":" + PORT + CONTEXT + "/";

  static Server        server;

  final static int     DELAY_TIME    = 5000;

  static Configuration configs;

  List<Downloader>     downloaders;

  HttpClientFactory    clientFactory = new HttpClientFactory();

  @BeforeClass
  public static void setUpServer() throws Exception
  {
    server = new Server(PORT);
    // TODO FIXME we can now run the embedded server, so no need to use WebAppContext!
//    server.setHandler(new WebAppContext("src/main/webapp", CONTEXT));
    server.start();
    while (!server.isRunning())
    {
      DPoolUtils.sleep(500);
    }
    logger.info("Server is now running.");
    DPoolUtils.sleep(2000);

    configs = new PropertiesConfiguration("dpool-testing.properties");
  }

  @AfterClass
  public static void tearDownServer() throws Exception
  {
    server.stop();
    logger.info("Server is stopped.");
  }

  /**
   * Just to test sending POST requests.
   * 
   * @throws ClientProtocolException
   * @throws IOException
   */
  @Test
  public void testPost() throws ClientProtocolException, IOException
  {
    String tid = "mytask";

    Map<String, String> params = new HashMap<String, String>();
    params.put("tid", tid);
    HttpPost post = ModifiedHttpClientUtils.generatePostRequest(BASE_URL + "page/report", params);

    HttpClient client = new DefaultHttpClient();
    BasicResponse result = new BasicResponse();
    client.execute(post, new BasicResponseHandler(result));

    assertEquals(HttpStatus.SC_OK, result.getHttpRespCode());
    assertNotNull(result.getContent());
    assertTrue(result.getContent().contains(tid));
  }

  public void runDownloaders(int n)
  {
    downloaders = new ArrayList<Downloader>(n);
    for (int i = 0; i < n; ++i)
    {
      configs.setProperty(Downloader.DOWNLOADER_NAME, "D" + (i + 1));
      Downloader d = new Downloader(configs);
      d.start();
      downloaders.add(d);
    }
  }

  public void stopDownloaders()
  {
    if (downloaders != null)
    {
      for (Downloader d : downloaders)
      {
        d.stop();
        assertEquals(Routine.Status.STOPPED, d.getStatus());
      }
      downloaders = null;
    }
    DPoolUtils.sleep(1000); // allow some grace period for DownloadMonitors to actually exit
  }

  /**
   * Run 1 controller and multiple downloaders (e.g. 10) and download multiple time-consuming pages
   * simultaneously.
   * 
   * @throws InterruptedException
   * @throws ExecutionException
   */
  // running downloaders will have overhead, thus the time limit is 2 DELAY_TIME
  @Test(timeout = DELAY_TIME * 4)
  public void testIntegration() throws InterruptedException, ExecutionException
  {
    int n = 10;
    runDownloaders(n);

    List<Callable<BasicResponse>> downloads = new ArrayList<Callable<BasicResponse>>();
    for (int i = 0; i < n; ++i)
    {
      final int j = i;
      downloads.add(new Callable<BasicResponse>()
      {
        @Override
        public BasicResponse call() throws ClientProtocolException, IOException, URISyntaxException
        {
          HttpClient client = clientFactory.get();

          Map<String, String> params = new HashMap<String, String>();
          params.put("url", BASE_URL + "echo/get?msg=MSG" + j + "&delay=" + DELAY_TIME);
          params.put("int", "1000");
          params.put("natt", "3");
          params.put("tatt", "10000");
          HttpGet get = ModifiedHttpClientUtils.generateGetRequest(BASE_URL + "page/download.xml", params);
          logger.info("HttpGet URI: " + get.getURI());
          BasicResponse resp = client.execute(get, new BasicResponseHandler());
          get.releaseConnection();
          return resp;
        }
      });
    }

    ExecutorService es = Executors.newFixedThreadPool(n);
    List<Future<BasicResponse>> resps = es.invokeAll(downloads);
    for (int i = 0; i < resps.size(); ++i)
    {
      Future<BasicResponse> resp = resps.get(i);
      BasicResponse r = null;
      r = resp.get();
      logger.info("Response for No. " + i + ": " + r.getHttpRespCode());
      assertEquals(HttpStatus.SC_OK, r.getHttpRespCode());
      assertTrue(r.getContent().contains("MSG" + i));
    }
    es.shutdown();
    es.awaitTermination(DELAY_TIME, TimeUnit.MILLISECONDS);

    stopDownloaders();
  }

  public static void main(String[] args) throws Exception
  {
    setUpServer();
  }

}
