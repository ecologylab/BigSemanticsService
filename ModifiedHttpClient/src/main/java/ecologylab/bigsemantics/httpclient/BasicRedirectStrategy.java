package ecologylab.bigsemantics.httpclient;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.ProtocolException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This strategy fixes some problems with the default redirect strategy.
 * 
 * @author quyin
 */
public class BasicRedirectStrategy extends DefaultRedirectStrategy
{

  private static Logger logger;

  static
  {
    logger = LoggerFactory.getLogger(BasicRedirectStrategy.class);
  }

  @Override
  protected URI createLocationURI(String location) throws ProtocolException
  {
    URIBuilder ub;
    try
    {
      ub = ModifiedHttpClientUtils.getUriBuilder(location);
      return ub.build();
    }
    catch (UnsupportedEncodingException e)
    {
      logger.error("Can't create URI using response's location header: [" + location + "]", e);
    }
    catch (URISyntaxException e)
    {
      logger.error("Can't create URI using response's location header: [" + location + "]", e);
    }
    return null;
  }

}
