package ecologylab.bigsemantics.downloaderpool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Before;
import org.junit.Test;

/**
 * Test downloader behaviors.
 * 
 * @author quyin
 */
public class TestDownloader
{

  /**
   * A mock downloader for test.
   */
  MockDownloader d;

  @Before
  public void init() throws ConfigurationException
  {
    Configuration configs = new PropertiesConfiguration("dpool-testing.properties");
    d = new MockDownloader(configs);
    d.setSleepBetweenLoop(100);
  }

  @Test
  public void testConstruction()
  {
    assertNotNull(d);
    assertNotNull(d.downloadMonitor);
  }

  @Test
  public void testFormRequest()
  {
    DownloaderRequest req = d.formRequest();
    assertNotNull(req);
    assertTrue(req.getMaxTaskCount() > 0);
  }

  @Test
  public void testSendingRequest()
  {
    long dt = d.getSleepBetweenLoop();

    d.presetTasks.add(new Task("1", "http://google.com"));
    d.presetTasks.add(new Task("2", "http://yahoo.com"));
    d.start();

    // d should keep sending requests, since 1) initially there is no tasks ongoing; 2) each task
    // it gets can be processed and finished immediately because we are using a mock page which does
    // not need to really download the page.
    DPoolUtils.sleep(dt * 3);
    // in approx. 3 cycles, it should do 3 requests.
    assertEquals(3, d.numTasksRequested);

    d.stop();
    d.numTasksRequested = 0;
    DPoolUtils.sleep(dt * 3);
    // when the downloader is stopped there should be no requests made.
    assertEquals(0, d.numTasksRequested);
  }

  @Test
  public void testQueuingReceivedTasks()
  {
    long dt = d.getSleepBetweenLoop();

    d.presetTasks.add(new Task("1", "http://google.com"));
    d.presetTasks.add(new Task("2", "http://yahoo.com"));
    d.start();

    DPoolUtils.sleep(dt * 3);
    // in approx. 3 cycles, the downloader should queue 6 pages (2 pages per cycle since there are 2
    // tasks preset).
    assertEquals(6, d.numPagesQueued);

    d.stop();
    d.numPagesQueued = 0;
    DPoolUtils.sleep(dt * 3);
    // when the downloader is stopped there should be no pages queued.
    assertEquals(0, d.numPagesQueued);
  }

  @Test
  public void testCallingCallback()
  {
    long dt = d.getSleepBetweenLoop();

    d.presetTasks.add(new Task("1", "http://1.google.com"));
    d.usePresetTasksOnce = true;
    d.start();
    DPoolUtils.sleep(dt);
    d.stop();

    assertEquals(1, d.presetResponder.numCallbacks);
    MockPage page = (MockPage) d.presetResponder.lastCallbackPage;
    assertNotNull(page);
    assertTrue(page.performed);
  }

  @Test
  public void testFormingBlacklist()
  {
    long dt = d.getSleepBetweenLoop();

    // task.domainInterval will be used as site.downloadInterval.
    // real waiting time will be downloadInterval + random(0 ~ downloadInterval/2)
    Task t1 = new Task("1", "http://google.com");
    // google.com should be available after at most 2dt + 2dt/2 = 3dt
    t1.setDomainInterval((int) dt * 2);
    Task t2 = new Task("2", "http://yahoo.com");
    // yahoo.com should be available after at most 4dt + 4dt/2 = 6dt
    t2.setDomainInterval((int) dt * 4);

    d.presetTasks.add(t1);
    d.presetTasks.add(t2);
    d.usePresetTasksOnce = true;
    d.start();

    // 0.5dt after start: both domains are still busy
    DPoolUtils.sleep(dt / 2);
    assertBlacklisted(d, "google.com");
    assertBlacklisted(d, "yahoo.com");

    DPoolUtils.sleep(dt / 2 + dt);
    // 2dt after start: both are busy
    assertBlacklisted(d, "google.com");
    assertBlacklisted(d, "yahoo.com");

    DPoolUtils.sleep(dt / 2 + dt);
    // 3.5dt after start: google.com should be available after at most 3dt
    assertNotBlacklisted(d, "google.com");
    assertBlacklisted(d, "yahoo.com");

    DPoolUtils.sleep(dt * 3);
    // 6.5dt after start: yahoo.com should be available after at most 6dt
    assertNotBlacklisted(d, "google.com");
    assertNotBlacklisted(d, "yahoo.com");
  }

  private void assertBlacklisted(Downloader d, String domain)
  {
    DownloaderRequest req = d.formRequest();
    List<String> blist = req.getBlacklist();
    assertNotNull(blist);
    assertTrue(blist.contains(domain));
  }

  private void assertNotBlacklisted(Downloader d, String domain)
  {
    DownloaderRequest req = d.formRequest();
    if (req != null)
    {
      List<String> blist = req.getBlacklist();
      if (blist != null)
      {
        assertFalse(blist.contains(domain));
      }
    }
  }

}
