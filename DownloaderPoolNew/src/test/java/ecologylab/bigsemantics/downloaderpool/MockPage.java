package ecologylab.bigsemantics.downloaderpool;

import ecologylab.net.ParsedURL;

/**
 * A mock page. Can be used to change the behavior of performDownload().
 * 
 * @author quyin
 * 
 */
public class MockPage extends Page
{

  boolean performed = false;

  public MockPage(String id, ParsedURL location, String userAgent)
  {
    super(id, location, userAgent);
  }

  @Override
  public void performDownload()
  {
    System.err.println("MockPage.performDownload()");
    performed = true;
  }

}
