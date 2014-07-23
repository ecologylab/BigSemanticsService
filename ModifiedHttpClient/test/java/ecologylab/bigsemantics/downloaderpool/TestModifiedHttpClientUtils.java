package ecologylab.bigsemantics.downloaderpool;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import ecologylab.bigsemantics.httpclient.ModifiedHttpClientUtils;

public class TestModifiedHttpClientUtils
{

  @Test
  public void testUrlEscaping() throws UnsupportedEncodingException, URISyntaxException
  {
    String url = "http://www.asknature.org/browse?selected=strategy|386&type=aof";
    URI result = ModifiedHttpClientUtils.getUriBuilder(url).build();
    assertEquals("http://www.asknature.org/browse?selected=strategy%7C386&type=aof",
                 result.toString());
  }

}
