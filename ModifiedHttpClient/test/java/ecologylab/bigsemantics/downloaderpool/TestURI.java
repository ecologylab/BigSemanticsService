package ecologylab.bigsemantics.downloaderpool;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Test;

import ecologylab.bigsemantics.httpclient.HttpClientFactory;
import ecologylab.bigsemantics.httpclient.ModifiedHttpClientUtils;

public class TestURI
{

  @Test
  public void testUri() throws URISyntaxException, ClientProtocolException, IOException
  {
    String url = "http://www.lovelyundergrad.com/search/label/Real+Life+Dorms";
    URIBuilder ub = ModifiedHttpClientUtils.getUriBuilder(url);
    URI uri = ub.build();
    System.out.println(uri);

    HttpGet get = new HttpGet(uri);
    HttpClientFactory factory = new HttpClientFactory();
    HttpClient client = factory.get();
    HttpResponse resp = client.execute(get);
    assertNotNull(resp);
  }

}
