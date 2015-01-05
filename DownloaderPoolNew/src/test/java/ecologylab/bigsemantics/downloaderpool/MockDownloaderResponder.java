package ecologylab.bigsemantics.downloaderpool;

/**
 * A mock DownloaderResponder. Can be used to inspect the invocation of the callback() method.
 * 
 * @author quyin
 */
public class MockDownloaderResponder extends DownloaderResponder
{

  int  numCallbacks = 0;

  Page lastCallbackPage;

  public MockDownloaderResponder()
  {
    super(null, null);
  }

  @Override
  public void callback(Page page)
  {
    numCallbacks++;
    System.err.println("callback(): " + page);
    lastCallbackPage = page;
  }

}
