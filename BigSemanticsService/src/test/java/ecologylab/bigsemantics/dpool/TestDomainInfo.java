package ecologylab.bigsemantics.dpool;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestDomainInfo
{

  static class FakeDomainRuntimeInfo extends DomainRuntimeInfo
  {

    long currentTime;

    public FakeDomainRuntimeInfo(DomainInfo domainInfo)
    {
      super(domainInfo);
    }

    public long getCurrentTime()
    {
      return currentTime;
    }

  }

  @Test
  public void testNormalDelay()
  {
    DomainInfo domainInfo = new DomainInfo("test.com");

    domainInfo.setMinDelay(5); // 5 sec
    domainInfo.setLongDelayThreshold(3);
    domainInfo.setLongDelay(60); // 60 sec, or 1 min

    FakeDomainRuntimeInfo domainRuntimeInfo = new FakeDomainRuntimeInfo(domainInfo);
    domainRuntimeInfo.currentTime = 1000; // here the unit is millisecond
    domainRuntimeInfo.beginAccess();
    domainRuntimeInfo.endAccess(200);

    assertTrue(domainRuntimeInfo.getCurrentDelay() >= 5000);
    assertTrue(domainRuntimeInfo.getCurrentDelay() <= 7500);
  }

  @Test
  public void testLongDelay()
  {
    DomainInfo domainInfo = new DomainInfo("test.com");

    domainInfo.setMinDelay(5);
    domainInfo.setLongDelayThreshold(3);
    domainInfo.setLongDelay(60);

    FakeDomainRuntimeInfo domainRuntimeInfo = new FakeDomainRuntimeInfo(domainInfo);
    domainRuntimeInfo.currentTime = 1000;
    domainRuntimeInfo.beginAccess();
    domainRuntimeInfo.endAccess(500);
    domainRuntimeInfo.beginAccess();
    domainRuntimeInfo.endAccess(500); // 2 failures till now

    assertTrue(domainRuntimeInfo.getCurrentDelay() >= 5000);
    assertTrue(domainRuntimeInfo.getCurrentDelay() <= 7500);

    domainRuntimeInfo.beginAccess();
    domainRuntimeInfo.endAccess(500); // 3 failures now, should do long delay

    assertTrue(domainRuntimeInfo.getCurrentDelay() >= 60000);

    domainRuntimeInfo.beginAccess();
    domainRuntimeInfo.endAccess(500); // fail again, should delay 60x2=120 sec now

    assertTrue(domainRuntimeInfo.getCurrentDelay() >= 120000);

    domainRuntimeInfo.beginAccess();
    domainRuntimeInfo.endAccess(200); // success, should go back to min delay

    assertTrue(domainRuntimeInfo.getCurrentDelay() >= 5000);
    assertTrue(domainRuntimeInfo.getCurrentDelay() <= 7500);
  }

}
