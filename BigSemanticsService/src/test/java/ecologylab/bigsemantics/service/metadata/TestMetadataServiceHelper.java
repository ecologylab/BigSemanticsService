package ecologylab.bigsemantics.service.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.Test;

import ecologylab.bigsemantics.cyberneko.CybernekoWrapper;
import ecologylab.bigsemantics.exceptions.DocumentRecycled;
import ecologylab.bigsemantics.exceptions.ProcessingUnfinished;
import ecologylab.bigsemantics.generated.library.RepositoryMetadataTypesScope;
import ecologylab.bigsemantics.generated.library.commodity.product.AmazonProduct;
import ecologylab.bigsemantics.metadata.builtins.Document;
import ecologylab.bigsemantics.service.SemanticsServiceScope;
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

  static SemanticsServiceScope sss;

  @BeforeClass
  public static void init()
  {
    sss = new SemanticsServiceScope(RepositoryMetadataTypesScope.get(), CybernekoWrapper.class);
  }

  MetadataServiceHelper getMsh(ParsedURL docPurl, boolean reload)
  {
    MetadataService ms = new MetadataService();
    ms.semanticsServiceScope = sss;
    ms.docPurl = docPurl;
    ms.reload = reload;
    MetadataServiceHelper msh = new MetadataServiceHelper(ms);
    return msh;
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
    ParsedURL purl = ParsedURL.getAbsolute("http://www.amazon.com/dp/B003EYV89Q/");
    MetadataServiceHelper msh = getMsh(purl, false);
    Document doc = msh.document;
    checkExtractedSemantics(doc, AmazonProduct.class);
  }

  @Test(timeout = 60000)
  public void testGettingCachedDocument()
  {
    ParsedURL purl = ParsedURL.getAbsolute("http://www.amazon.com/dp/B000RW0GT6/");
    for (int i = 0; i < 50; ++i)
    {
      sleep(100);
      MetadataServiceHelper msh = getMsh(purl, false);
      Document doc = msh.document;
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
      MetadataServiceHelper msh = getMsh(purl, false);
      Document doc = msh.document;
      results.put(index, doc);
    }

  }

  @Test(timeout = 60000)
  public void testMultipleRequestsForSameDocument() throws InterruptedException
  {
    final ParsedURL purl = ParsedURL.getAbsolute("http://www.amazon.com/dp/B000FIZISQ/");
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
      throws DocumentRecycled, IOException, ProcessingUnfinished
  {
    ParsedURL purl = ParsedURL.getAbsolute("http://www.amazon.com/dp/B0018TIADQ/");

    MetadataService ms = new MetadataService();
    ms.semanticsServiceScope = sss;
    ms.clientIp = "192.168.0.1";
    ms.docPurl = purl;
    ms.reload = false;
    MetadataServiceHelper msh = new MetadataServiceHelper(ms);
    msh.getMetadata();

    ServiceLogRecord log = msh.serviceLogRecord;
    assertNotNull(log.getBeginTime());
    assertTrue(log.getMsTotal() > 0);
    assertNotNull(log.getRequesterIp());
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
