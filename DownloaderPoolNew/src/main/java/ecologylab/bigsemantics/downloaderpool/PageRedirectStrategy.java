package ecologylab.bigsemantics.downloaderpool;

import java.net.URI;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.protocol.HttpContext;

import ecologylab.bigsemantics.httpclient.BasicRedirectStrategy;

/**
 * This strategy helps find redirected locations for a page. Used with HttpClient.
 * 
 * @author quyin
 */
public class PageRedirectStrategy extends BasicRedirectStrategy
{

  /**
   * The corresponding DownloaderResult object for this download. Redirected locations will be
   * recorded into this object.
   */
  private DownloaderResult result;

  public PageRedirectStrategy(DownloaderResult result)
  {
    super();
    this.result = result;
  }

  @Override
  public URI getLocationURI(HttpRequest request, HttpResponse response, HttpContext context)
      throws ProtocolException
  {
    URI redirectedUri = super.getLocationURI(request, response, context);
    if (result != null)
    {
      result.addOtherLocation(redirectedUri.toString());
    }
    return redirectedUri;
  }

}
