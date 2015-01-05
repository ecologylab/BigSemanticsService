package ecologylab.bigsemantics.downloaderpool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import ecologylab.concurrent.Site;

/**
 * Test the SimpleSiteTable.
 * 
 * @author quyin
 */
public class TestSimpleSiteTable
{

  SimpleSiteTable sst;

  @Before
  public void init()
  {
    sst = new SimpleSiteTable();
  }

  @Test
  public void testConstruction()
  {
    assertNotNull(sst);
  }

  @Test
  public void testGettingTheSameSite()
  {
    // same domain should map to the same Site object.
    Site s1 = sst.getSite("google.com");
    Site s2 = sst.getSite("google.com");
    assertNotNull(s1);
    assertNotNull(s2);
    assertSame(s1, s2);
    assertEquals("google.com", s1.domain());
  }

  @Test
  public void testFindingBusySites()
  {
    // simulate the execution within a DownloadMonitor.
    // verify that getBusySites() gives the correct set of sites that are busy, considering their
    // download intervals.

    long dt = 100; // the time for one single step

    // the real waiting time is downloadInterval + random(0 ~ downloadInterval/2)
    SimpleSite s1 = (SimpleSite) sst.getSite("google.com");
    s1.setDownloadInterval(dt);
    SimpleSite s2 = (SimpleSite) sst.getSite("yahoo.com");
    s2.setDownloadInterval(dt * 2);
    SimpleSite s3 = (SimpleSite) sst.getSite("bing.com"); // no intervals between downloads

    // 0dt from beginning
    s1.advanceNextAvailableTime();
    s2.advanceNextAvailableTime();
    s3.advanceNextAvailableTime();

    Set<Site> bs = sst.getBusySites();
    assertSiteBusy(bs, "google.com");
    assertSiteBusy(bs, "yahoo.com");
    assertSiteNotBusy(bs, "bing.com");

    DPoolUtils.sleep(dt - dt / 2);
    // 0.5dt from beginning
    bs = sst.getBusySites();
    System.out.println(bs);
    assertSiteBusy(bs, "google.com");
    assertSiteBusy(bs, "yahoo.com");
    assertSiteNotBusy(bs, "bing.com");

    DPoolUtils.sleep(dt / 2 + dt / 2);
    // 1.5dt from beginning
    // google.com can be accessed now (downloadInterval = 1dt)
    bs = sst.getBusySites();
    assertSiteNotBusy(bs, "google.com");
    assertSiteBusy(bs, "yahoo.com");
    assertSiteNotBusy(bs, "bing.com");

    DPoolUtils.sleep(dt + dt / 2);
    // 3dt from beginning
    // yahoo.com can also be accessed now (downloadInterval = 2dt)
    bs = sst.getBusySites();
    assertSiteNotBusy(bs, "google.com");
    assertSiteNotBusy(bs, "yahoo.com");
    assertSiteNotBusy(bs, "bing.com");

    // assume another access to google.com
    s1.advanceNextAvailableTime();
    bs = sst.getBusySites();
    // now google.com becomes busy again
    assertSiteBusy(bs, "google.com");
    DPoolUtils.sleep(dt + dt / 2);
    bs = sst.getBusySites();
    // after 1.5dt, google.com becomes available again
    assertSiteNotBusy(bs, "google.com");
  }

  /**
   * Utility method that asserts that a domain be busy.
   * 
   * @param busySites
   * @param siteDomain
   */
  private void assertSiteBusy(Set<Site> busySites, String siteDomain)
  {
    assertNotNull(busySites);
    for (Site site : busySites)
    {
      if (site.domain().equals(siteDomain))
        return;
    }
    fail("Domain " + siteDomain + " is expected to be busy but not contained in busySites.");
  }

  /**
   * Utility method that asserts that a domain not be busy.
   * 
   * @param busySites
   * @param siteDomain
   */
  private void assertSiteNotBusy(Set<Site> busySites, String siteDomain)
  {
    if (busySites != null)
    {
      for (Site site : busySites)
      {
        if (site.domain().equals(siteDomain))
          fail("Domain " + siteDomain + " is expected not to be busy but contained in busySites.");
      }
    }
  }

}
