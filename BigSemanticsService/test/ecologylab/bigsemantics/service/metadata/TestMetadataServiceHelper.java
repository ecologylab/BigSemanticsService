package ecologylab.bigsemantics.service.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import ecologylab.bigsemantics.generated.library.amazonProduct.AmazonProduct;
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.bigsemantics.service.logging.ServiceLogRecord;
import ecologylab.net.ParsedURL;
import ecologylab.serialization.SimplTypesScope;
import ecologylab.serialization.formatenums.StringFormat;

/**
 * 
 * @author quyin
 *
 */
public class TestMetadataServiceHelper
{

  private MetadataServiceHelper msh;

  @Before
  public void init()
  {
    // TODO computationally mimic downloader starting with an empty cache.
    // currently we must purge the cache and start the downloader ourselves!
    msh = new MetadataServiceHelper();
  }

  @Test
  public void testConstruction()
  {
    // mainly for testing if the helper can be constructed correctly, without exceptions
    assertNotNull(msh);
//    assertTrue(!msh.isFinished());
//    assertNotNull(msh.getServiceLogRecord());
  }

  void checkExtractedSemantics(Document doc, Class<? extends Document> type)
  {
    assertNotNull(doc);
    assertTrue(type.isAssignableFrom(doc.getClass()));
    assertNotNull(doc.getTitle());
    assertTrue(doc.getTitle().length() > 0);
    assertNotNull(doc.getLocation());
    assertTrue(doc.getLocation().toString().length() > 0);
  }

  @Test(timeout = 30000)
  public void testGettingSingleUncachedDocument()
  {
    ParsedURL purl = ParsedURL
        .getAbsolute("http://www.amazon.com/Washburn-Series-WG35SCE-Acoustic-Electric/dp/B003EYV89Q/");
    Document doc = msh.getMetadata(purl, false);
    checkExtractedSemantics(doc, AmazonProduct.class);
  }

  @Test(timeout = 60000)
  public void testGettingCachedDocument()
  {
    ParsedURL purl = ParsedURL.getAbsolute("http://www.amazon.com/Seagull-S6-Original-Acoustic-Guitar/dp/B000RW0GT6/");
    Document doc = msh.getMetadata(purl, false);
    for (int i = 0; i < 50; ++i)
    {
      sleep(100);
      doc = msh.getMetadata(purl, false);
      checkExtractedSemantics(doc, AmazonProduct.class);
    }
  }

  private class Runner implements Runnable
  {

    private int                    index;

    private ParsedURL              purl;

    private Map<Integer, Document> results;

    public Runner(int index, ParsedURL purl, Map<Integer, Document> results)
    {
      this.index = index;
      this.purl = purl;
      this.results = results;
    }

    @Override
    public void run()
    {
      Document doc = msh.getMetadata(purl, false);
      results.put(index, doc);
    }

  }

  @Test(timeout = 60000)
  public void testMultipleRequestsForSameDocument() throws InterruptedException
  {
    final ParsedURL purl = ParsedURL.getAbsolute("http://www.amazon.com/Yamaha-FG700S-Acoustic-Guitar/dp/B000FIZISQ/");
    final Map<Integer, Document> docs = new ConcurrentHashMap<Integer, Document>();
    int n = 10;
    ExecutorService exec = Executors.newFixedThreadPool(n);
    for (int i = 0; i < n; ++i)
    {
      exec.submit(new Thread(new Runner(i, purl, docs)));
    }
    exec.shutdown();
    exec.awaitTermination(50000, TimeUnit.MILLISECONDS);
    assertEquals(n, docs.size());
    for (int i = 0; i < n; ++i)
    {
      Document doc = docs.get(i);
      checkExtractedSemantics(doc, AmazonProduct.class);
    }
  }

  static void sleep(long time)
  {
    try
    {
      Thread.sleep(time);
    }
    catch (InterruptedException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  @Test(timeout = 60000)
  public void testServiceLogRecordBeingGeneratedCorrectly()
  {
    ParsedURL purl = ParsedURL.getAbsolute("http://www.amazon.com/Musicians-Gear-Tubular-Guitar-Stand/dp/B0018TIADQ/");
    msh.getMetadataResponse("192.168.0.1", purl, StringFormat.XML, false);
    
//    ServiceLogRecord log = msh.getServiceLogRecord();
    ServiceLogRecord log = new ServiceLogRecord();
    assertNotNull(log.getBeginTime());
    assertTrue(log.getMsTotal() > 0);
    // assertNotNull(log.getRequesterIp());
    assertNotNull(log.getRequestUrl());
    assertTrue(log.getResponseCode() > 0);
    
    assertNotNull(log.getDocumentUrl());
    assertTrue(log.getMsHtmlDownload() > 0);
    assertTrue(log.getMsExtraction() > 0);
    assertTrue(log.getMsSerialization() > 0);
    
    assertNotNull(log.getId());
    
    // for easier debugging:
    System.out.println();
    System.out.println();
    SimplTypesScope.serializeOut(log, "", StringFormat.XML);
  }

}
