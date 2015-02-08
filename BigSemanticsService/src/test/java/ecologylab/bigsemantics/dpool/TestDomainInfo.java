package ecologylab.bigsemantics.dpool;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestDomainInfo
{

  static class FakeDomainInfo extends DomainInfo
  {

    long currentTime;

    public long getCurrentTime()
    {
      return currentTime;
    }

  }

  @Test
  public void testNormalDelay()
  {
    FakeDomainInfo domainInfo = new FakeDomainInfo();

    domainInfo.setDomain("test.com");
    domainInfo.setMinDelay(5); // 5 sec
    domainInfo.setLongDelayThreshold(3);
    domainInfo.setLongDelay(60); // 60 sec, or 1 min

    domainInfo.currentTime = 1000; // here the unit is millisecond
    domainInfo.beginAccess();
    domainInfo.endAccess(200);

    assertTrue(domainInfo.getCurrentDelay() >= 5000);
    assertTrue(domainInfo.getCurrentDelay() <= 7500);
  }

  @Test
  public void testLongDelay()
  {
    FakeDomainInfo domainInfo = new FakeDomainInfo();

    domainInfo.setDomain("test.com");
    domainInfo.setMinDelay(5);
    domainInfo.setLongDelayThreshold(3);
    domainInfo.setLongDelay(60);

    domainInfo.currentTime = 1000;
    domainInfo.beginAccess();
    domainInfo.endAccess(500);
    domainInfo.beginAccess();
    domainInfo.endAccess(500); // 2 failures till now

    assertTrue(domainInfo.getCurrentDelay() >= 5000);
    assertTrue(domainInfo.getCurrentDelay() <= 7500);

    domainInfo.beginAccess();
    domainInfo.endAccess(500); // 3 failures now, should do long delay

    assertTrue(domainInfo.getCurrentDelay() >= 60000);

    domainInfo.beginAccess();
    domainInfo.endAccess(500); // fail again, should delay 60x2=120 sec now

    assertTrue(domainInfo.getCurrentDelay() >= 120000);

    domainInfo.beginAccess();
    domainInfo.endAccess(200); // success, should go back to min delay

    assertTrue(domainInfo.getCurrentDelay() >= 5000);
    assertTrue(domainInfo.getCurrentDelay() <= 7500);
  }

}
