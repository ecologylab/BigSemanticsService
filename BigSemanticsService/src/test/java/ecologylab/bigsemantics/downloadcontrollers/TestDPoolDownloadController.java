package ecologylab.bigsemantics.downloadcontrollers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.junit.Test;

public class TestDPoolDownloadController
{

  @Test
  public void testUriBuilder()
  {
    UriBuilder ub = UriBuilder.fromPath("http://localhost/DownloaderPool/page/download.xml");
    ub.queryParam("url", "http://example.com/article?id=123&page=1");
    ub.queryParam("agent", "Jersey Client 代理");
    ub.queryParam("int", "10000");
    URI uri = ub.build();
    assertNotNull(uri);
    assertEquals("http://localhost/DownloaderPool/page/download.xml?url=http://example.com/article?id%3D123%26page%3D1&agent=Jersey+Client+%E4%BB%A3%E7%90%86&int=10000",
                 uri.toString());
  }

}
